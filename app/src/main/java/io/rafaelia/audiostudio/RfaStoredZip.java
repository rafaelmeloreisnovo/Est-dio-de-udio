/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Project-authored ZIP (method 0 / STORED) writer. No java.util.zip,
 * compression provider, JNI, Android classes, native calls or reflection.
 * OutputStream is the explicitly hosted byte-delivery boundary.
 */
package io.rafaelia.audiostudio;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Strictly bounded little-endian ZIP32 serializer for ZIPRAF evidence entries.
 * The ZIP structures and CRC32 are written directly using integer operations.
 * This intentionally does NOT support ZIP64, compression, encryption or streaming
 * unknown-length entries. A rejected bound fails before writing an entry.
 */
final class RfaStoredZip {
    static final int MAX_ENTRIES = 32;
    static final int MAX_ENTRY_BYTES = 8 * 1024 * 1024;
    static final long MAX_ZIP_BYTES = 64L * 1024L * 1024L;

    private static final int LOCAL_SIGNATURE = 0x04034b50;
    private static final int CENTRAL_SIGNATURE = 0x02014b50;
    private static final int END_SIGNATURE = 0x06054b50;
    private static final int ZIP_VERSION = 20;
    private static final int DOS_DATE_1980_01_01 = 33;
    private static final int CRC32_POLY = 0xedb88320;

    private final OutputStream output;
    private final byte[][] names = new byte[MAX_ENTRIES][];
    private final int[] sizes = new int[MAX_ENTRIES];
    private final int[] checksums = new int[MAX_ENTRIES];
    private final long[] positions = new long[MAX_ENTRIES];
    private int count;
    private long position;
    private boolean finished;

    RfaStoredZip(OutputStream output) {
        if (output == null) throw new IllegalArgumentException("ZIP output is null");
        this.output = output;
    }

    /** Add one complete, immutable-for-duration entry, without compression. */
    void add(String name, byte[] bytes) throws IOException {
        if (finished) throw new IllegalStateException("ZIP already finished");
        if (count >= MAX_ENTRIES) throw new IOException("ZIPRAF entry count exceeded");
        if (bytes == null || bytes.length > MAX_ENTRY_BYTES) {
            throw new IOException("ZIPRAF embedded entry missing or too large");
        }
        byte[] path = asciiPath(name);
        for (int i = 0; i < count; ++i) {
            if (equalsAscii(names[i], path)) {
                throw new IOException("ZIPRAF duplicate entry");
            }
        }
        final long increment = 30L + path.length + bytes.length;
        // Reserve the upper bound for the central directory and end marker.
        if (position + increment + estimatedCentralBytes(path.length) > MAX_ZIP_BYTES) {
            throw new IOException("ZIPRAF total size exceeded");
        }

        final int crc = crc32(bytes);
        byte[] header = new byte[30 + path.length];
        put32(header, 0, LOCAL_SIGNATURE);
        put16(header, 4, ZIP_VERSION);
        put16(header, 6, 0);  // ASCII path, no data descriptor
        put16(header, 8, 0);  // method STORED
        put16(header, 10, 0); // DOS time: 00:00:00
        put16(header, 12, DOS_DATE_1980_01_01);
        put32(header, 14, crc);
        put32(header, 18, bytes.length);
        put32(header, 22, bytes.length);
        put16(header, 26, path.length);
        put16(header, 28, 0); // no extra fields
        copy(path, 0, header, 30, path.length);

        final int index = count;
        final long localAt = position;
        output.write(header);
        output.write(bytes);
        names[index] = path;
        sizes[index] = bytes.length;
        checksums[index] = crc;
        positions[index] = localAt;
        position += increment;
        ++count;
    }

    /** Complete the central directory without closing the caller-owned stream. */
    void finish() throws IOException {
        if (finished) return;
        long centralStart = position;
        for (int i = 0; i < count; ++i) {
            byte[] path = names[i];
            byte[] header = new byte[46 + path.length];
            put32(header, 0, CENTRAL_SIGNATURE);
            put16(header, 4, ZIP_VERSION); // made by MS-DOS
            put16(header, 6, ZIP_VERSION); // minimum reader
            put16(header, 8, 0);           // flags
            put16(header, 10, 0);          // STORED
            put16(header, 12, 0);          // time
            put16(header, 14, DOS_DATE_1980_01_01);
            put32(header, 16, checksums[i]);
            put32(header, 20, sizes[i]);
            put32(header, 24, sizes[i]);
            put16(header, 28, path.length);
            put16(header, 30, 0); // extra length
            put16(header, 32, 0); // comment length
            put16(header, 34, 0); // disk number
            put16(header, 36, 0); // internal attrs
            put32(header, 38, 0); // external attrs
            put32(header, 42, positions[i]);
            copy(path, 0, header, 46, path.length);
            output.write(header);
            position += header.length;
        }
        long centralSize = position - centralStart;
        if (position + 22 > MAX_ZIP_BYTES ||
                centralStart > 0xffffffffL || centralSize > 0xffffffffL) {
            throw new IOException("ZIPRAF central directory size exceeded");
        }
        byte[] tail = new byte[22];
        put32(tail, 0, END_SIGNATURE);
        put16(tail, 4, 0);
        put16(tail, 6, 0);
        put16(tail, 8, count);
        put16(tail, 10, count);
        put32(tail, 12, centralSize);
        put32(tail, 16, centralStart);
        put16(tail, 20, 0); // no comment
        output.write(tail);
        position += tail.length;
        finished = true;
    }

    static int crc32(byte[] data) {
        if (data == null) throw new IllegalArgumentException("CRC input is null");
        int crc = -1;
        for (int i = 0; i < data.length; ++i) {
            crc ^= data[i] & 255;
            for (int k = 0; k < 8; ++k) {
                int mask = -(crc & 1);
                crc = (crc >>> 1) ^ (CRC32_POLY & mask);
            }
        }
        return ~crc;
    }

    private long estimatedCentralBytes(int nextNameLength) {
        long bytes = 22 + 46L + nextNameLength;
        for (int i = 0; i < count; ++i) bytes += 46L + names[i].length;
        return bytes;
    }

    private static byte[] asciiPath(String path) throws IOException {
        if (path == null || path.isEmpty() || path.length() > 255 ||
                path.charAt(0) == '/' || path.endsWith("/") ||
                path.contains("..") || path.contains("//")) {
            throw new IOException("ZIPRAF unsafe/invalid path");
        }
        byte[] out = new byte[path.length()];
        for (int i = 0; i < path.length(); ++i) {
            char c = path.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                    c == '_' || c == '-' || c == '.' || c == '/';
            if (!safe) throw new IOException("ZIPRAF non-ASCII or unsafe path");
            out[i] = (byte) c;
        }
        return out;
    }

    private static boolean equalsAscii(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; ++i) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private static void copy(byte[] src, int srcStart,
                             byte[] dst, int dstStart, int length) {
        for (int i = 0; i < length; ++i) dst[dstStart + i] = src[srcStart + i];
    }

    private static void put16(byte[] bytes, int at, int value) {
        bytes[at] = (byte) value;
        bytes[at + 1] = (byte) (value >>> 8);
    }

    private static void put32(byte[] bytes, int at, long value) {
        bytes[at] = (byte) value;
        bytes[at + 1] = (byte) (value >>> 8);
        bytes[at + 2] = (byte) (value >>> 16);
        bytes[at + 3] = (byte) (value >>> 24);
    }
}
