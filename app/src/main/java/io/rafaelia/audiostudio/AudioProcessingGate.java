/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One active platform DSP/JNI processing session at a time. The current native
 * bridge owns shared DSP/meter instances, so parallel masters are not safe.
 * This lock is Android-independent Java, tested without external libraries.
 */
final class AudioProcessingGate {
    private final AtomicBoolean occupied = new AtomicBoolean(false);

    boolean tryEnter() {
        return occupied.compareAndSet(false, true);
    }

    boolean busy() {
        return occupied.get();
    }

    void leave() {
        occupied.set(false);
    }
}
