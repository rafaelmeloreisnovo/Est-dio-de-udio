/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 *
 * Typed, pure numerical study operators. Java runs on a JVM / Android ART;
 * no claim of freestanding native C execution or validated physical diffusion.
 * Invalid mathematical domains return NaN; callers must report a typed
 * TOKEN_VAZIO_* state (NEVER serialize invalid input as numeric zero).
 */
package io.rafaelia.audiostudio;

final class WaveLabDOperators {
    static final int HEX_MAX_DEGREE = 6;
    private static final double LOG_2 = Math.log(2.0);
    private WaveLabDOperators() {}

    private static boolean finite(double... values) {
        for (double x : values) if (!Double.isFinite(x)) return false;
        return true;
    }

    /** RLL D_i before M_i modulation; strict H(x)=1 only for x>0. */
    static double diffusivity(double d0, double dNeg, double plasticity,
                              double state, double threshold) {
        if (!finite(d0, dNeg, plasticity, state, threshold)
                || d0 < 0.0 || dNeg >= 0.0) return Double.NaN;
        return d0 * (1.0 - Math.tanh(plasticity))
                + (state > threshold ? dNeg : 0.0);
    }

    /** Partial derivative ∂D_i/∂P_i for fixed state, threshold and D0. */
    static double dDiffusivityDPlasticity(double d0, double plasticity) {
        if (!finite(d0, plasticity) || d0 < 0.0) return Double.NaN;
        double t = Math.tanh(plasticity);
        return -d0 * (1.0 - t * t);
    }

    /** Stable log(cosh(x)); avoids overflow of cosh on large |x|. */
    static double logCosh(double x) {
        if (!Double.isFinite(x)) return Double.NaN;
        double a = Math.abs(x);
        return a + Math.log1p(Math.exp(-2.0 * a)) - LOG_2;
    }

    /**
     * Primitive in P_i at fixed threshold branch:
     * D0*(P-log(cosh P)) + Dneg*H*(P) + constant.
     * State/Theta/P adaptation and M_i are held fixed/outside this primitive.
     */
    static double primitivePlasticity(double d0, double dNeg, double p,
                                      double state, double threshold) {
        if (!finite(d0, dNeg, p, state, threshold)
                || d0 < 0.0 || dNeg >= 0.0) return Double.NaN;
        // For P>0, P-logcosh(P)=log(2)-log1p(exp(-2P)).
        double base = p >= 0.0
                ? LOG_2 - Math.log1p(Math.exp(-2.0 * p))
                : p - logCosh(p);
        return d0 * base + (state > threshold ? dNeg * p : 0.0);
    }

    /**
     * Analytic inverse for P on a FIXED Heaviside branch and D0>0.
     * Does not invert the full coupled adaptive dynamical system.
     */
    static double inversePlasticity(double observedD, double d0,
                                    double dNeg, boolean thresholdOpen) {
        if (!finite(observedD, d0, dNeg) || !(d0 > 0.0)
                || !(dNeg < 0.0)) return Double.NaN;
        double effectiveD = observedD - (thresholdOpen ? dNeg : 0.0);
        double t = 1.0 - effectiveD / d0;
        if (!(t > -1.0 && t < 1.0)) return Double.NaN;
        return 0.5 * (Math.log1p(t) - Math.log1p(-t));
    }

    /** The exact inverse of the M modulation when m in (-1,1). */
    static double removeModulation(double modulatedD, double m) {
        if (!finite(modulatedD,m) || !(m > -1.0 && m < 1.0))
            return Double.NaN;
        return modulatedD / (1.0 + m);
    }

    /** RLL historical first-order ratio (NOT exact log difference). */
    static double logarithmicProxy(double previous, double current, double eps) {
        if (!finite(previous, current, eps) || !(eps > 0.0)
                || !(previous + eps > 0.0) || !(current + eps > 0.0))
            return Double.NaN;
        return (current - previous) / (previous + eps);
    }

    /** Exact finite log increment, with same positive shifted states. */
    static double logarithmicIncrement(double previous, double current, double eps) {
        double proxy = logarithmicProxy(previous, current, eps);
        return Double.isFinite(proxy) && proxy > -1.0
                ? Math.log1p(proxy) : Double.NaN;
    }

    /** Inverse of exact logarithmic increment, given previous and eps. */
    static double reverseLogarithmicIncrement(double previous, double exactLogDelta,
                                              double eps) {
        if (!finite(previous,exactLogDelta,eps) || !(eps > 0.0)
                || !(previous + eps > 0.0)) return Double.NaN;
        double out = (previous + eps) * Math.exp(exactLogDelta) - eps;
        return Double.isFinite(out) ? out : Double.NaN;
    }

    /** Discrete derivative; reconstruct with start and increments, not indefinite integral. */
    static double[] discreteDifferences(double[] series) {
        if (series == null || series.length < 1) return null;
        double[] diff = new double[series.length - 1];
        for (int i = 0; i < diff.length; i++) {
            if (!finite(series[i],series[i+1])) return null;
            diff[i] = series[i+1] - series[i];
        }
        return diff;
    }
    static double[] reverseDifferences(double first, double[] diff) {
        if (!Double.isFinite(first) || diff == null) return null;
        double[] reconstructed = new double[diff.length + 1];
        reconstructed[0] = first;
        for (int i = 0; i < diff.length; i++) {
            if (!Double.isFinite(diff[i])) return null;
            reconstructed[i+1] = reconstructed[i] + diff[i];
        }
        return reconstructed;
    }

    /** A reorder is invertible ONLY with a complete bijective index map. */
    static double[] permute(double[] values, int[] map) {
        if (values == null || map == null || values.length != map.length) return null;
        boolean[] seen = new boolean[map.length];
        double[] result = new double[map.length];
        for (int i=0; i<map.length; i++) {
            int j=map[i];
            if (j<0 || j>=map.length || seen[j] || !Double.isFinite(values[j]))
                return null;
            seen[j]=true;
            result[i]=values[j];
        }
        return result;
    }
    static double[] inversePermutation(double[] permuted, int[] map) {
        if (permuted==null || map==null || permuted.length!=map.length) return null;
        boolean[] seen=new boolean[map.length];
        double[] restored=new double[map.length];
        for(int i=0;i<map.length;i++){
            int j=map[i];
            if(j<0 || j>=map.length || seen[j] || !Double.isFinite(permuted[i]))
                return null;
            seen[j]=true;
            restored[j]=permuted[i];
        }
        return restored;
    }

    /** PBIP angular correction D_theta, in square-length units for a,b lengths. */
    static double angularCorrection(double a,double b,double thetaRad) {
        if (!finite(a,b,thetaRad) || a<0 || b<0) return Double.NaN;
        return -2.0*a*b*Math.cos(thetaRad);
    }
    static double dAngularDTheta(double a,double b,double thetaRad) {
        if (!finite(a,b,thetaRad) || a<0 || b<0) return Double.NaN;
        return 2.0*a*b*Math.sin(thetaRad);
    }
    static double primitiveAngular(double a,double b,double thetaRad) {
        if (!finite(a,b,thetaRad) || a<0 || b<0) return Double.NaN;
        return -2.0*a*b*Math.sin(thetaRad);
    }
    /** Principal branch only. Other angular branches are NOT erased. */
    static double inverseAngularPrincipal(double a,double b,double dTheta) {
        if (!finite(a,b,dTheta) || !(a>0) || !(b>0)) return Double.NaN;
        double c=-dTheta/(2.0*a*b);
        return c>=-1.0 && c<=1.0 ? Math.acos(c) : Double.NaN;
    }

    /** D_Q = -(B^2-4AC)/(4A), with the quadratic A!=0. */
    static double quadraticDeficit(double A,double B,double C) {
        if (!finite(A,B,C) || A==0.0) return Double.NaN;
        return -(B*B-4.0*A*C)/(4.0*A);
    }

    /**
     * Sufficient positive-convexity bound for explicit 6-neighbor averaging
     * with nonnegative neighbor weights C_ij<=1. NOT a necessity theorem for
     * arbitrary directed graphs, anisotropy, signed weights, or coupled control.
     */
    static boolean boundedHexDiffusion(double d) {
        return Double.isFinite(d) && d>=0.0 && d<=1.0/HEX_MAX_DEGREE;
    }
    /** Two-node synthetic diagnostic, each one neighbor. */
    static double[] twoNodeStep(double left,double right,double d) {
        return twoNodeStepNonuniform(left,right,d,d);
    }

    /** Permutation-covariant only when states, coefficients AND graph relabel together. */
    static double[] twoNodeStepNonuniform(double left,double right,
                                          double dLeft,double dRight) {
        if (!finite(left,right,dLeft,dRight)) return null;
        return new double[]{left+dLeft*(right-left),
                right+dRight*(left-right)};
    }

    /**
     * Algebraic previous state given D and labeled graph.
     * det=1-dLeft-dRight; singular evolution has NO unique inverse.
     * Mathematically invertible != lossless physical time reversal.
     */
    static double[] inverseTwoNodeStep(double nextLeft,double nextRight,
                                       double dLeft,double dRight) {
        if (!finite(nextLeft,nextRight,dLeft,dRight)) return null;
        double det=1.0-dLeft-dRight;
        if (Math.abs(det)<1e-12) return null;
        double oldLeft=((1.0-dRight)*nextLeft-dLeft*nextRight)/det;
        double oldRight=((1.0-dLeft)*nextRight-dRight*nextLeft)/det;
        return finite(oldLeft,oldRight) ? new double[]{oldLeft,oldRight} : null;
    }
}
