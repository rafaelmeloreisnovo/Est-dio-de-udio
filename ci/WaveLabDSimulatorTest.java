/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Host-only deterministic tests. No device/physics claims.
 */
package io.rafaelia.audiostudio;

final class WaveLabDSimulatorTest {
    private static int assertions;
    private static void yes(boolean test,String label) {
        ++assertions;
        if(!test)throw new AssertionError(label);
    }
    private static void close(double a,double b,double tol,String label){
        yes(Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a-b)<=tol,label+" "+a+" vs "+b);
    }
    public static void main(String[] args) {
        double[] s={1,0,0,0,0,0,0};
        close(WaveLabDSimulator.mass(s),1,0,"mass initial");
        close(WaveLabDSimulator.variance(s),6.0/7,1e-12,"variance initial");
        double[] next=WaveLabDSimulator.oneStep(s,0.1);
        yes(next!=null,"step available");
        close(next[0],0.4,1e-12,"center with degree six");
        for(int i=1;i<next.length;i++)close(next[i],0.1,1e-12,"ring step");
        close(WaveLabDSimulator.mass(next),1,1e-12,"symmetric graph mass");
        yes(WaveLabDSimulator.variance(next)<WaveLabDSimulator.variance(s),"diffusion decreases contrast");
        double[] anti=WaveLabDSimulator.oneStep(s,-0.1);
        yes(WaveLabDSimulator.variance(anti)>WaveLabDSimulator.variance(s),"anti diffusion increases contrast");
        close(WaveLabDSimulator.mass(anti),1,1e-12,"anti symmetry mass");

        WaveLabDSimulator.Result smooth=WaveLabDSimulator.run(0.1,-0.2,0,false,32,8);
        WaveLabDSimulator.Result unstable=WaveLabDSimulator.run(0.1,-0.2,0,true,32,8);
        yes(smooth.state.equals("PASS_BOUNDED_SYNTHETIC"),"bounded positive pass");
        yes(smooth.steps==32,"complete bounded positive fixture");
        yes(smooth.d>0 && unstable.d<0,"correct threshold sign");
        yes(smooth.varianceAfter<smooth.varianceBefore,"positive branch smooths");
        yes(unstable.varianceAfter>unstable.varianceBefore,"negative branch amplifies");
        yes(unstable.state.equals("STOP_MAGNITUDE_BOUND"),"negative fixture stops at bound");
        yes(unstable.steps<32,"anti branch bounded by steps and magnitude");
        close(smooth.massAfter,smooth.massBefore,1e-9,"positive run mass conserved");
        close(unstable.massAfter,unstable.massBefore,1e-8,"negative run mass conserved");
        yes(WaveLabDSimulator.run(0.1,-0.2,0,false,33,8)
            .state.equals("BLOCKED_INVALID_PARAMS"),"max steps guard");
        yes(WaveLabDSimulator.run(-0.1,-0.2,0,false,32,8)
            .state.equals("BLOCKED_INVALID_PARAMS"),"negative base guard");
        yes(WaveLabDSimulator.oneStep(s,Double.NaN)==null,"missing numeric D");
        yes(WaveLabDSimulator.oneStep(new double[]{1,0},0.1)==null,"wrong graph topology");
        String receipt=WaveLabDSimulator.compareForReceipt();
        yes(receipt.equals(WaveLabDSimulator.compareForReceipt()),"determinism");
        yes(receipt.contains("SIMULATION_ONLY_NOT_PHYSICAL_D"),"physical boundary");
        yes(receipt.contains("STOP_MAGNITUDE_BOUND"),"stop rationale exported");
        yes(!receipt.contains("latitude") && !receipt.contains("SSID"),"no sensor identifiers");
        System.out.println("WaveLabDSimulatorTest PASS: "+assertions+" host assertions");
    }
}
