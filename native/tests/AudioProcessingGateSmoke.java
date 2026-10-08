/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable host test for the same gate used by MainActivity. No Android SDK. */
public final class AudioProcessingGateSmoke {
    public static void main(String[] args) throws Exception {
        AudioProcessingGate gate = new AudioProcessingGate();
        check(gate.tryEnter(), "initial entry");
        check(gate.busy(), "occupied reported");
        check(!gate.tryEnter(), "double entry must be blocked");
        gate.leave();
        check(!gate.busy(), "release reported");
        check(gate.tryEnter(), "reentry after release");
        gate.leave();

        final int workers = 12;
        final int turns = 1000;
        final CountDownLatch ready = new CountDownLatch(workers);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger inside = new AtomicInteger();
        final AtomicInteger accepted = new AtomicInteger();
        final AtomicBoolean violation = new AtomicBoolean();
        Thread[] threads = new Thread[workers];
        for (int i = 0; i < workers; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int j = 0; j < turns; j++) {
                        if (!gate.tryEnter()) continue;
                        try {
                            if (inside.incrementAndGet() != 1) violation.set(true);
                            accepted.incrementAndGet();
                            Thread.yield();
                        } finally {
                            inside.decrementAndGet();
                            gate.leave();
                        }
                    }
                } catch (InterruptedException interrupted) {
                    violation.set(true);
                    Thread.currentThread().interrupt();
                }
            }, "audio-gate-test-" + i);
            threads[i].start();
        }
        ready.await();
        start.countDown();
        for (Thread worker : threads) {
            worker.join(10000);
            check(!worker.isAlive(), "worker deadline");
        }
        check(!violation.get(), "exclusive JNI session violated");
        check(accepted.get() > 0, "no session accepted");
        check(inside.get() == 0 && !gate.busy(), "gate not released");
        System.out.println("AUDIO_SESSION_SINGLEFLIGHT=PASS");
        System.out.println("AUDIO_SESSION_CONCURRENT_ATTEMPTS=" + (workers * turns));
        System.out.println("AUDIO_SESSION_ACCEPTED=" + accepted.get());
    }

    private static void check(boolean allowed, String detail) {
        if (!allowed) throw new AssertionError(detail);
    }
}
