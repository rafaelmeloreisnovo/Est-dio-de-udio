/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Standalone host test: compile alongside WaveLabCore.java with javac (no Android SDK).
 */
package io.rafaelia.audiostudio;
final class WaveLabCoreTest {
    static void near(double x, double y, double tol, String label) {
        if (!Double.isFinite(x) || Math.abs(x-y) > tol) throw new AssertionError(label + ": " + x);
    }
    public static void main(String[] args) {
        if (WaveLabCore.poseSector(0) != 0 || WaveLabCore.poseSector(360) != 0 ||
            WaveLabCore.poseSector(-45) != 7 || WaveLabCore.poseSector(Double.NaN) != -1)
            throw new AssertionError("pose sector");
        near(WaveLabCore.modeledEchoRangeM(0.02, 340), 3.4, 1e-8, "roundtrip");
        if (!Double.isNaN(WaveLabCore.modeledEchoRangeM(-1, 340))) throw new AssertionError("invalid delay");
        near(WaveLabCore.haversineMeters(0,0,0,0),0,1e-8,"same point");
        if (!Double.isNaN(WaveLabCore.haversineMeters(91,0,0,0))) throw new AssertionError("invalid geo");
        if (WaveLabCore.sectionCrossing(-0.1,0.1)!=1 ||
            WaveLabCore.sectionCrossing(0.1,-0.1)!=0) throw new AssertionError("section");
        short[] signal = new short[4096];
        for(int i=0;i<signal.length;i++)
            signal[i]=(short)Math.round(10000*Math.sin(2*Math.PI*1000*i/48000.0));
        double bin = WaveLabCore.relativeDbfs(signal,signal.length,1000);
        near(bin,20*Math.log10(10000.0/32768.0),1.0,"goertzel bin");
        if (!Double.isNaN(WaveLabCore.relativeDbfs(null,0,125)))
            throw new AssertionError("null");
        System.out.println("WaveLabCoreTest PASS: 8 contracts");
    }
}
