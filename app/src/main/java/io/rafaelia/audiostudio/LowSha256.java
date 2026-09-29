/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Small provider-free SHA-256 implementation for evidence hashing.
 * No java.security provider and no Android dependency.
 */
package io.rafaelia.audiostudio;

final class LowSha256 {
    private static final int[] K = {
            0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,
            0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
            0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,
            0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
            0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,
            0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
            0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,
            0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
            0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,
            0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
            0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,
            0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
            0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,
            0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
            0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,
            0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };

    private final int[] h = {
            0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,
            0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19
    };
    private final int[] w = new int[64];
    private final byte[] block = new byte[64];
    private int blockUsed;
    private long totalBytes;
    private boolean finished;

    void update(byte[] data, int offset, int length) {
        if (finished || data == null || length <= 0) return;
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new IllegalArgumentException("invalid SHA-256 input range");
        }
        totalBytes += length;
        int p = offset;
        int remaining = length;
        while (remaining > 0) {
            int space = 64 - blockUsed;
            int copy = remaining < space ? remaining : space;
            int i;
            for (i = 0; i < copy; ++i) {
                block[blockUsed + i] = data[p + i];
            }
            blockUsed += copy;
            p += copy;
            remaining -= copy;
            if (blockUsed == 64) {
                transform(block);
                blockUsed = 0;
            }
        }
    }

    byte[] finish() {
        if (finished) throw new IllegalStateException("SHA-256 already finished");
        finished = true;
        long bitLength = totalBytes << 3;

        block[blockUsed++] = (byte)0x80;
        if (blockUsed > 56) {
            while (blockUsed < 64) block[blockUsed++] = 0;
            transform(block);
            blockUsed = 0;
        }
        while (blockUsed < 56) block[blockUsed++] = 0;

        int shift;
        for (shift = 56; shift >= 0; shift -= 8) {
            block[blockUsed++] = (byte)(bitLength >>> shift);
        }
        transform(block);
        blockUsed = 0;

        byte[] out = new byte[32];
        int i;
        for (i = 0; i < 8; ++i) {
            int v = h[i];
            int p = i * 4;
            out[p] = (byte)(v >>> 24);
            out[p + 1] = (byte)(v >>> 16);
            out[p + 2] = (byte)(v >>> 8);
            out[p + 3] = (byte)v;
        }
        return out;
    }

    private void transform(byte[] input) {
        int i;
        for (i = 0; i < 16; ++i) {
            int p = i * 4;
            w[i] = ((input[p] & 0xff) << 24)
                    | ((input[p + 1] & 0xff) << 16)
                    | ((input[p + 2] & 0xff) << 8)
                    | (input[p + 3] & 0xff);
        }
        for (i = 16; i < 64; ++i) {
            int x15 = w[i - 15];
            int x2 = w[i - 2];
            int s0 = rotr(x15, 7) ^ rotr(x15, 18) ^ (x15 >>> 3);
            int s1 = rotr(x2, 17) ^ rotr(x2, 19) ^ (x2 >>> 10);
            w[i] = w[i - 16] + s0 + w[i - 7] + s1;
        }

        int a = h[0];
        int b = h[1];
        int c = h[2];
        int d = h[3];
        int e = h[4];
        int f = h[5];
        int g = h[6];
        int hv = h[7];

        for (i = 0; i < 64; ++i) {
            int s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
            int ch = (e & f) ^ ((~e) & g);
            int t1 = hv + s1 + ch + K[i] + w[i];
            int s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
            int maj = (a & b) ^ (a & c) ^ (b & c);
            int t2 = s0 + maj;

            hv = g;
            g = f;
            f = e;
            e = d + t1;
            d = c;
            c = b;
            b = a;
            a = t1 + t2;
        }

        h[0] += a;
        h[1] += b;
        h[2] += c;
        h[3] += d;
        h[4] += e;
        h[5] += f;
        h[6] += g;
        h[7] += hv;
    }

    private static int rotr(int value, int bits) {
        return (value >>> bits) | (value << (32 - bits));
    }
}
