/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * No Android dependencies; host deterministic numerical fixtures only.
 */
package io.rafaelia.audiostudio;

final class WaveLabDOperatorsTest {
    private static int checks;
    private static void require(boolean value, String why) {
        ++checks;
        if (!value) throw new AssertionError(why);
    }
    private static void near(double got,double expected,double eps,String why) {
        require(Double.isFinite(got) && Math.abs(got-expected)<=eps,why +
                " got=" + got + " expected=" + expected);
    }
    public static void main(String[] ignored) {
        final double d0=0.1,dNeg=-0.4,p=0.3;
        double off=WaveLabDOperators.diffusivity(d0,dNeg,p,0.0,1.0);
        double on =WaveLabDOperators.diffusivity(d0,dNeg,p,2.0,1.0);
        require(off>0.0 && on<0.0,"D sign flips at declared threshold");
        near(on-off,dNeg,1e-12,"threshold discontinuity is D_neg");
        near(WaveLabDOperators.diffusivity(d0,dNeg,p,1.0,1.0),off,1e-12,
                "H(0) is defined as zero");
        require(Double.isNaN(WaveLabDOperators.diffusivity(-1,dNeg,p,0,1)),
                "illegal base coefficient returns missing");
        require(Double.isNaN(WaveLabDOperators.diffusivity(d0,0,p,0,1)),
                "D_neg must be strictly negative");
        double h=1e-5;
        double dNumerical=(WaveLabDOperators.diffusivity(d0,dNeg,p+h,2,1)-
                WaveLabDOperators.diffusivity(d0,dNeg,p-h,2,1))/(2*h);
        near(WaveLabDOperators.dDiffusivityDPlasticity(d0,p),dNumerical,2e-10,
                "partial derivative dD/dP");
        double pNumerical=(WaveLabDOperators.primitivePlasticity(d0,dNeg,p+h,2,1)-
                WaveLabDOperators.primitivePlasticity(d0,dNeg,p-h,2,1))/(2*h);
        near(pNumerical,on,2e-10,"primitive derivative recovers D");
        near(WaveLabDOperators.inversePlasticity(on,d0,dNeg,true),p,1e-12,
                "fixed threshold branch D inverse");
        require(Double.isNaN(WaveLabDOperators.inversePlasticity(on,0,dNeg,true)),
                "D inverse ambiguous at D0=0");
        near(WaveLabDOperators.removeModulation(on*1.3,0.3),on,1e-12,
                "modulation inverse with known M");
        require(Double.isNaN(WaveLabDOperators.removeModulation(1,-1)),
                "noninvertible modulation factor");

        double logProxy=WaveLabDOperators.logarithmicProxy(1,2,0.01);
        double logExact=WaveLabDOperators.logarithmicIncrement(1,2,0.01);
        require(logProxy>logExact+0.2,"log finite ratio is only an approximation");
        near(logExact,Math.log((2.01)/(1.01)),1e-12,"exact log increment");
        near(WaveLabDOperators.reverseLogarithmicIncrement(1,logExact,0.01),
                2,1e-12,"exact reverse logarithm");
        require(Double.isNaN(WaveLabDOperators.logarithmicIncrement(-2,3,0.01)),
                "log domain fail closed");
        require(Double.isNaN(WaveLabDOperators.reverseLogarithmicIncrement(
                1,10000,0.01)),"overflow not serialized as numeric");

        double[] seq={2.0,4.0,7.0,12.0,20.0};
        double[] diff=WaveLabDOperators.discreteDifferences(seq);
        double[] rebuilt=WaveLabDOperators.reverseDifferences(seq[0],diff);
        require(rebuilt.length==seq.length,"telescoping size");
        for(int i=0;i<seq.length;i++) near(rebuilt[i],seq[i],1e-12,"telescoping item "+i);
        require(WaveLabDOperators.discreteDifferences(new double[]{1,Double.NaN})==null,
                "TOKEN_VAZIO numeric inputs not imputed");

        double[] values={2,4,7,12};
        int[] map={2,0,3,1};
        double[] permutation=WaveLabDOperators.permute(values,map);
        double[] inverse=WaveLabDOperators.inversePermutation(permutation,map);
        double e0=0,ep=0;
        for(int i=0;i<values.length;i++){
            near(inverse[i],values[i],1e-12,"inverse permutation "+i);
            e0+=values[i]*values[i]; ep+=permutation[i]*permutation[i];
        }
        near(e0,ep,0.0,"permutations preserve sum squares");
        require(WaveLabDOperators.permute(values,new int[]{0,0,2,3})==null,
                "reject duplicate index not bijective");

        double theta=Math.PI/3,a=3,b=4;
        double angularNumerical=(WaveLabDOperators.angularCorrection(a,b,theta+h)-
                WaveLabDOperators.angularCorrection(a,b,theta-h))/(2*h);
        near(angularNumerical,WaveLabDOperators.dAngularDTheta(a,b,theta),1e-9,
                "Dtheta derivative");
        double primitiveNumerical=(WaveLabDOperators.primitiveAngular(a,b,theta+h)-
                WaveLabDOperators.primitiveAngular(a,b,theta-h))/(2*h);
        near(primitiveNumerical,WaveLabDOperators.angularCorrection(a,b,theta),1e-9,
                "Dtheta primitive");
        near(WaveLabDOperators.inverseAngularPrincipal(a,b,
                WaveLabDOperators.angularCorrection(a,b,theta)),
                theta,1e-12,"Dtheta inverse principal branch");
        require(Double.isNaN(WaveLabDOperators.inverseAngularPrincipal(a,b,-100)),
                "infeasible angular value blocked");
        near(WaveLabDOperators.quadraticDeficit(1,2,1),0,1e-12,
                "quadratic discriminant zero");
        require(Double.isNaN(WaveLabDOperators.quadraticDeficit(0,2,1)),
                "quadratic A must not be zero");

        require(WaveLabDOperators.boundedHexDiffusion(1.0/6.0),
                "hex sufficient max");
        require(!WaveLabDOperators.boundedHexDiffusion(-0.01),
                "negative D is NOT stable diffusion pass");
        require(!WaveLabDOperators.boundedHexDiffusion(0.2),
                "too-large D fails sufficient bound");
        double[] smooth=WaveLabDOperators.twoNodeStep(1,0,0.25);
        double[] grow=WaveLabDOperators.twoNodeStep(1,0,-0.25);
        require(Math.abs(smooth[0]-smooth[1])<1.0,
                "positive diffusion smooths synthetic contrast");
        require(Math.abs(grow[0]-grow[1])>1.0,
                "negative diffusion amplifies synthetic contrast");
        double[] varying=WaveLabDOperators.twoNodeStepNonuniform(2,5,0.1,0.2);
        double[] reindexed=WaveLabDOperators.twoNodeStepNonuniform(5,2,0.2,0.1);
        near(varying[0],reindexed[1],1e-12,
                "state and coefficient permutation covariance left");
        near(varying[1],reindexed[0],1e-12,
                "state and coefficient permutation covariance right");
        double[] mismatch=WaveLabDOperators.twoNodeStepNonuniform(5,2,0.1,0.2);
        require(Math.abs(varying[0]-mismatch[1])>1e-4,
                "changing state ordering alone breaks covariance");
        double[] backward=WaveLabDOperators.inverseTwoNodeStep(
                varying[0],varying[1],0.1,0.2);
        near(backward[0],2,1e-12,"invertible two-node D operator left");
        near(backward[1],5,1e-12,"invertible two-node D operator right");
        require(WaveLabDOperators.inverseTwoNodeStep(2.5,2.5,0.5,0.5)==null,
                "singular D matrix has no unique reverse");
        near(WaveLabDOperators.inverseSignedLog1p(
                WaveLabDOperators.signedLog1p(-0.35,0.1),0.1),-0.35,1e-12,
                "signed D logarithm is bijective with scale");
        require(Double.isNaN(WaveLabDOperators.signedLog1p(-0.35,0)),
                "signed logarithm missing normalization");
        near(WaveLabDOperators.inverseLogLogRatio(
                WaveLabDOperators.logLogRatio(10,2),2),10,1e-12,
                "normalized log-log roundtrip");
        require(Double.isNaN(WaveLabDOperators.logLogRatio(1,2)),
                "log-log normalized domain x/scale > 1");
        double qA=2,qB=3,qC=5;
        double dqNumerical=(WaveLabDOperators.quadraticDeficit(qA,qB+h,qC)-
                WaveLabDOperators.quadraticDeficit(qA,qB-h,qC))/(2*h);
        near(WaveLabDOperators.dQuadraticDeficitDB(qA,qB),dqNumerical,2e-10,
                "quadratic D derivative wrt B");
        double qpNumerical=(WaveLabDOperators.primitiveQuadraticDeficitB(qA,qB+h,qC)-
                WaveLabDOperators.primitiveQuadraticDeficitB(qA,qB-h,qC))/(2*h);
        near(qpNumerical,WaveLabDOperators.quadraticDeficit(qA,qB,qC),2e-10,
                "quadratic D antiderivative wrt B");
        double invB=WaveLabDOperators.inverseQuadraticDeficitBPrincipal(
                qA,qC,WaveLabDOperators.quadraticDeficit(qA,qB,qC));
        near(invB,Math.abs(qB),1e-12,"quadratic inverse principal B branch");
        require(Double.isNaN(WaveLabDOperators.inverseQuadraticDeficitBPrincipal(
                2,0,2)),"quadratic inverse must reject negative radicand");
        System.out.println("WaveLabDOperatorsTest PASS: " + checks + " host assertions");
    }
}
