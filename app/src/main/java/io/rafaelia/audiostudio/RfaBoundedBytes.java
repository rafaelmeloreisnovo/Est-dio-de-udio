/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Deterministic byte accumulator; no ByteArrayOutputStream, JNI or Android.
 */
package io.rafaelia.audiostudio;

import java.io.IOException;

/** Caller-owned bounded byte storage, with explicit copies and fail-closed growth. */
final class RfaBoundedBytes {
    private final int limit;
    private byte[] data;
    private int size;

    RfaBoundedBytes(int limit) {
        if (limit < 0 || limit > RfaStoredZip.MAX_ENTRY_BYTES) {
            throw new IllegalArgumentException("RFA byte bound out of range");
        }
        this.limit = limit;
        this.data = new byte[limit < 4096 ? limit : 4096];
    }

    void append(byte[] source, int offset, int length) throws IOException {
        if (source == null || offset < 0 || length < 0 ||
                offset > source.length || length > source.length - offset) {
            throw new IOException("RFA invalid byte window");
        }
        if (length > limit - size) {
            throw new IOException("RFA evidence exceeds bounded byte capacity");
        }
        int required = size + length;
        if (required > data.length) {
            int capacity = data.length;
            if (capacity == 0) capacity = 1;
            while (capacity < required) {
                int doubled = capacity > limit / 2 ? limit : capacity * 2;
                if (doubled <= capacity) {
                    throw new IOException("RFA byte capacity overflow");
                }
                capacity = doubled;
            }
            byte[] grown = new byte[capacity];
            for (int i = 0; i < size; ++i) grown[i] = data[i];
            data = grown;
        }
        for (int i = 0; i < length; ++i) data[size + i] = source[offset + i];
        size = required;
    }

    int size() {
        return size;
    }

    byte[] exactBytes() {
        byte[] exact = new byte[size];
        for (int i = 0; i < size; ++i) exact[i] = data[i];
        return exact;
    }
}
