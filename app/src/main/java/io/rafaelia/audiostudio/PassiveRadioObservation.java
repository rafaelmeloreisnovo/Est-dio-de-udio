/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

final class PassiveRadioObservation {
    static final class Result {
        final String state;
        final String transports;
        final boolean telephonyFeature;

        Result(String state, String transports, boolean telephonyFeature) {
            this.state = state;
            this.transports = transports;
            this.telephonyFeature = telephonyFeature;
        }
    }

    private PassiveRadioObservation() {}

    static Result observe(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean telephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return new Result("UNAVAILABLE_SERVICE", "TOKEN_VAZIO", telephony);
        }

        try {
            Network active = cm.getActiveNetwork();
            if (active == null) return new Result("NO_ACTIVE_NETWORK", "NONE", telephony);
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null) {
                return new Result("UNAVAILABLE_CAPABILITIES", "TOKEN_VAZIO", telephony);
            }

            StringBuilder b = new StringBuilder();
            append(b, caps, NetworkCapabilities.TRANSPORT_CELLULAR, "CELLULAR");
            append(b, caps, NetworkCapabilities.TRANSPORT_WIFI, "WIFI");
            append(b, caps, NetworkCapabilities.TRANSPORT_ETHERNET, "ETHERNET");
            append(b, caps, NetworkCapabilities.TRANSPORT_BLUETOOTH, "BLUETOOTH");
            append(b, caps, NetworkCapabilities.TRANSPORT_VPN, "VPN");
            if (b.length() == 0) b.append("OTHER_OR_UNREPORTED");
            return new Result("PASSIVE_PLATFORM_OBSERVATION", b.toString(), telephony);
        } catch (SecurityException e) {
            return new Result("UNAVAILABLE_PERMISSION", "TOKEN_VAZIO", telephony);
        }
    }

    private static void append(
            StringBuilder b, NetworkCapabilities caps, int transport, String label) {
        if (!caps.hasTransport(transport)) return;
        if (b.length() != 0) b.append(',');
        b.append(label);
    }
}
