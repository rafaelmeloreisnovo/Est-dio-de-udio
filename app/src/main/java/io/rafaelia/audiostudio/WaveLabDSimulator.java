/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Deterministic synthetic comparison of D diffusion and anti-diffusion.
 * No Android APIs, sensors, radio reads/writes, heap in an actual freestanding
 * runtime claim, or physical acoustic interpretation. Java is hosted.
 */
package io.rafaelia.audiostudio;

final class WaveLabDSimulator {
    private static final int SIZE = 7;
    private static final double[] INITIAL = {1.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private static final double LIMIT = 8.0;
    private static final int MAX_STEPS = 32;
    private WaveLabDSimulator() { }

    static final class Result {
        final String state;
        final int steps;
        final double d;
        final double massBefore;
        final double massAfter;
        final double varianceBefore;
        final double varianceAfter;
        final double maxAbs;
        Result(String state,int steps,double d,double massBefore,double massAfter,
               double varianceBefore,double varianceAfter,double maxAbs) {
            this.state=state;this.steps=steps;this.d=d;
            this.massBefore=massBefore;this.massAfter=massAfter;
            this.varianceBefore=varianceBefore;this.varianceAfter=varianceAfter;
            this.maxAbs=maxAbs;
        }
        String receipt(String id) {
            return "study=" + id + " state=" + state +
                " steps=" + steps + " D_step=" + d +
                " mass_initial=" + massBefore + " mass_final=" + massAfter +
                " variance_initial=" + varianceBefore +
                " variance_final=" + varianceAfter + " max_abs=" + maxAbs;
        }
    }

    private static boolean valid(double[] t) {
        if(t==null||t.length!=SIZE)return false;
        for(double v:t) if(!Double.isFinite(v))return false;
        return true;
    }

    static double mass(double[] t) {
        if(!valid(t))return Double.NaN;
        double x=0;
        for(double a:t)x+=a;
        return x;
    }

    static double variance(double[] t) {
        if(!valid(t))return Double.NaN;
        double mean=mass(t)/t.length, v=0.0;
        for(double a:t){double d=a-mean;v+=d*d;}
        return v;
    }

    /**
     * 7-node wheel: 0=center (6 neighbors), k=1..6 ring with center
     * and clockwise/counter-clockwise neighbors (3). Undirected symmetrical.
     * Fixed D per step; not the full adaptive RLL system.
     */
    static double[] oneStep(double[] state,double gain) {
        if(!valid(state)||!Double.isFinite(gain))return null;
        double[] result=new double[SIZE];
        double sum=0.0;
        for(int k=1;k<SIZE;k++)sum+=state[k]-state[0];
        result[0]=state[0]+gain*sum;
        for(int k=1;k<SIZE;k++) {
            int previous=k==1?6:k-1, next=k==6?1:k+1;
            double flow=(state[0]-state[k]) +
                (state[previous]-state[k])+(state[next]-state[k]);
            result[k]=state[k]+gain*flow;
        }
        return valid(result)?result:null;
    }

    /**
     * Hard bounded experiment; synthetic d comes from the *fixed* threshold
     * branch of D_i = D0(1-tanh(P)) + Dneg*H(T-Theta).
     * Outputs a reason-typed stop, never a "measured real-world D".
     */
    static Result run(double d0,double dNeg,double plasticity,boolean thresholdOn,
                      int steps,int maxMagnitude) {
        double d=WaveLabDOperators.diffusivity(d0,dNeg,plasticity,
                                               thresholdOn?1.0:0.0,0.5);
        if(!Double.isFinite(d)||steps<0||steps>MAX_STEPS||
           maxMagnitude<1||maxMagnitude>64)
            return new Result("BLOCKED_INVALID_PARAMS",0,Double.NaN,
                              Double.NaN,Double.NaN,Double.NaN,Double.NaN,
                              Double.NaN);
        double[] state=INITIAL.clone();
        double m0=mass(state), v0=variance(state),max=1.0;
        int count=0;
        String stateCode="PASS_BOUNDED_SYNTHETIC";
        while(count<steps) {
            double[] next=oneStep(state,d);
            if(next==null) {stateCode="STOP_NONFINITE";break;}
            state=next;
            count++;
            for(double x:state)max=Math.max(max,Math.abs(x));
            if(max>Math.min(LIMIT,maxMagnitude)) {
                stateCode="STOP_MAGNITUDE_BOUND";
                break;
            }
        }
        return new Result(stateCode,count,d,m0,mass(state),v0,
                          variance(state),max);
    }

    static String compareForReceipt() {
        Result smooth=run(0.10,-0.20,0.0,false,32,8);
        Result anti=run(0.10,-0.20,0.0,true,32,8);
        return "SIMULATION_ONLY_NOT_PHYSICAL_D\n"+
             "topology=seven_node_wheel_undirected_center_degree6_ring_degree3\n" +
             "calibration=NOT_RUN model=D0*(1-tanh_P)+Dneg*H fixed_branch\n"+
             "baseline="+smooth.receipt("D_POSITIVE") + "\n"+
             "anti="+anti.receipt("D_NEGATIVE") + "\n" +
             "radio_mutation=NONE code_boundary=MATHEMATICAL_ONLY\n";
    }
}
