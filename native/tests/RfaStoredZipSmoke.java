/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Host-side interoperability oracle. java.util.zip is used ONLY HERE in CI;
 * the shipping APK uses RfaStoredZip and does not import java.util.zip.
 */
package io.rafaelia.audiostudio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class RfaStoredZipSmoke {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        eq(0L, RfaStoredZip.crc32(new byte[0]) & 0xffffffffL, "CRC empty");
        eq(0xcbf43926L, RfaStoredZip.crc32(
                "123456789".getBytes(StandardCharsets.US_ASCII)) & 0xffffffffL, "CRC KAT");
        byte[] sample = new byte[32771];
        int state = 0x13f4a5c1;
        for (int i = 0; i < sample.length; ++i) {
            state ^= state << 13;
            state ^= state >>> 17;
            state ^= state << 5;
            sample[i] = (byte) state;
        }

        byte[] first = pack(sample);
        byte[] second = pack(sample);
        ok(Arrays.equals(first, second), "deterministic archive bytes");

        Path zipPath = Files.createTempFile("rafaelia-zipraf-host-", ".zip");
        try {
            Files.write(zipPath, first);
            try (ZipFile reader = new ZipFile(zipPath.toFile())) {
                eq(3L, reader.size(), "entry count");
                verify(reader, "00_manifest.json", "{}".getBytes(StandardCharsets.US_ASCII));
                verify(reader, "80_raw/evidence.txt", sample);
                verify(reader, "99_SHA256SUMS.txt", new byte[0]);
            }
        } finally {
            Files.deleteIfExists(zipPath);
        }

        // Fail-closed malformed-name/duplicate and bounded-payload contracts.
        RfaStoredZip archive = new RfaStoredZip(new ByteArrayOutputStream());
        rejectName(archive, "../escape");
        rejectName(archive, "/root");
        rejectName(archive, "segment\\evil");
        rejectName(archive, "é.txt");
        rejectName(archive, "a//b");
        rejectName(archive, "");
        archive.add("valid.txt", new byte[0]);
        try {
            archive.add("valid.txt", new byte[]{1});
            throw new AssertionError("duplicate was accepted");
        } catch (IOException expected) {
            ++assertions;
        }
        try {
            archive.add("too-large.bin", new byte[RfaStoredZip.MAX_ENTRY_BYTES + 1]);
            throw new AssertionError("oversize entry accepted");
        } catch (IOException expected) {
            ++assertions;
        }
        archive.finish();
        archive.finish(); // idempotent
        try {
            archive.add("after-finish", new byte[0]);
            throw new AssertionError("write-after-finish accepted");
        } catch (IllegalStateException expected) {
            ++assertions;
        }

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        RfaStoredZip bounded = new RfaStoredZip(full);
        for (int i = 0; i < RfaStoredZip.MAX_ENTRIES; ++i) {
            bounded.add("entry-" + i, new byte[]{(byte) i});
        }
        try {
            bounded.add("extra", new byte[0]);
            throw new AssertionError("entry count overflow was accepted");
        } catch (IOException expected) {
            ++assertions;
        }
        bounded.finish();

        RfaBoundedBytes accumulator = new RfaBoundedBytes(9);
        accumulator.append(new byte[]{1, 2, 3, 4}, 1, 3);
        accumulator.append(new byte[]{5, 6, 7, 8, 9, 10}, 0, 6);
        ok(Arrays.equals(
                new byte[]{2, 3, 4, 5, 6, 7, 8, 9, 10},
                accumulator.exactBytes()), "exact window/byte accumulator");
        eq(9, accumulator.size(), "bounded byte length");
        try {
            accumulator.append(new byte[]{0}, 0, 1);
            throw new AssertionError("bounded accumulator overflow accepted");
        } catch (IOException expected) {
            ++assertions;
        }
        try {
            accumulator.append(new byte[]{0}, -1, 1);
            throw new AssertionError("negative slice accepted");
        } catch (IOException expected) {
            ++assertions;
        }

        System.out.println("ZIPRAF_AUTHORIAL_ZIP32=PASS");
        System.out.println("ZIPRAF_JDK_READER_ROUNDTRIP=PASS");
        System.out.println("ZIPRAF_ARCHIVE_DETERMINISM=PASS");
        System.out.println("ZIPRAF_CRC32_KAT=PASS");
        System.out.println("ZIPRAF_NEGATIVE_BOUNDARIES=PASS");
        System.out.println("ZIPRAF_SMOKE_ASSERTIONS=" + assertions);
    }

    private static byte[] pack(byte[] sample) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RfaStoredZip writer = new RfaStoredZip(bytes);
        writer.add("00_manifest.json", "{}".getBytes(StandardCharsets.US_ASCII));
        writer.add("80_raw/evidence.txt", sample);
        writer.add("99_SHA256SUMS.txt", new byte[0]);
        writer.finish();
        return bytes.toByteArray();
    }

    private static void verify(ZipFile zip, String name, byte[] expected)
            throws Exception {
        ZipEntry entry = zip.getEntry(name);
        ok(entry != null, "missing " + name);
        eq(ZipEntry.STORED, entry.getMethod(), "STORED " + name);
        eq(expected.length, entry.getSize(), "size " + name);
        eq(expected.length, entry.getCompressedSize(), "compressed size " + name);
        eq(RfaStoredZip.crc32(expected) & 0xffffffffL, entry.getCrc(), "CRC " + name);
        byte[] restored = zip.getInputStream(entry).readAllBytes();
        ok(Arrays.equals(expected, restored), "byte-exact restored " + name);
    }

    private static void rejectName(RfaStoredZip zip, String name) throws Exception {
        try {
            zip.add(name, new byte[0]);
            throw new AssertionError("unsafe name accepted: " + name);
        } catch (IOException expected) {
            ++assertions;
        }
    }

    private static void eq(long expected, long actual, String context) {
        ++assertions;
        if (expected != actual) {
            throw new AssertionError(context + " expected=" + expected + " got=" + actual);
        }
    }

    private static void ok(boolean result, String context) {
        ++assertions;
        if (!result) throw new AssertionError(context);
    }
}
