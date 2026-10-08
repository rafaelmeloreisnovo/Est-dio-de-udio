/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Fixed-bounded, deterministic ZIPRAF entry table. No TreeMap/Map collection.
 */
package io.rafaelia.audiostudio;

/** Ordered archive entries held in caller-owned arrays, no collection provider. */
final class RfaOrderedEntries {
    private final String[] names = new String[RfaStoredZip.MAX_ENTRIES];
    private final byte[][] payloads = new byte[RfaStoredZip.MAX_ENTRIES][];
    private int used;

    void add(String name, byte[] payload) {
        if (name == null || name.isEmpty() || payload == null ||
                payload.length > RfaStoredZip.MAX_ENTRY_BYTES) {
            throw new IllegalArgumentException("invalid ZIPRAF entry");
        }
        if (used >= names.length) throw new IllegalStateException("ZIPRAF entry limit");
        int place = 0;
        while (place < used) {
            int cmp = names[place].compareTo(name);
            if (cmp == 0) throw new IllegalArgumentException("duplicate ZIPRAF entry");
            if (cmp > 0) break;
            ++place;
        }
        for (int i = used; i > place; --i) {
            names[i] = names[i - 1];
            payloads[i] = payloads[i - 1];
        }
        names[place] = name;
        payloads[place] = payload;
        ++used;
    }

    int size() {
        return used;
    }

    String nameAt(int index) {
        if (index < 0 || index >= used) throw new IndexOutOfBoundsException();
        return names[index];
    }

    byte[] bytesAt(int index) {
        if (index < 0 || index >= used) throw new IndexOutOfBoundsException();
        return payloads[index];
    }
}
