/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

package io.rafaelia.audiostudio;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

final class CodecCapabilityProbe {
    static final class Result {
        final String audioEncoders;
        final String audioDecoders;

        Result(String audioEncoders, String audioDecoders) {
            this.audioEncoders = audioEncoders;
            this.audioDecoders = audioDecoders;
        }
    }

    private CodecCapabilityProbe() {}

    static Result observe() {
        StringBuilder enc = new StringBuilder();
        StringBuilder dec = new StringBuilder();
        try {
            MediaCodecInfo[] infos =
                    new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
            for (MediaCodecInfo info : infos) {
                String[] types = info.getSupportedTypes();
                for (String type : types) {
                    if (type == null || !type.startsWith("audio/")) continue;
                    StringBuilder target = info.isEncoder() ? enc : dec;
                    if (target.length() != 0) target.append(';');
                    target.append(type).append('@').append(info.getName());
                }
            }
        } catch (Throwable ignored) {
            return new Result("UNAVAILABLE_QUERY", "UNAVAILABLE_QUERY");
        }
        return new Result(
                enc.length() == 0 ? "NONE_REPORTED" : enc.toString(),
                dec.length() == 0 ? "NONE_REPORTED" : dec.toString());
    }
}
