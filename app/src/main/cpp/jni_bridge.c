/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

#include <jni.h>
#include "dsp_core.h"
#include "meter_core.h"
#include "rfa_container_core.h"
#include "rfa_measure_core.h"

/*
 * Platform-owned instances.
 * Mutable state lives here, at the Android boundary, not in the freestanding cores.
 */
static dsp_state g_dsp_state;
static meter_state g_meter_state;
static rfa_exp_sweep_q31 g_sweep_state;

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeReset(
        JNIEnv *env, jclass clazz, jint preset) {
    (void)env;
    (void)clazz;
    dsp_state_reset(&g_dsp_state, (int)preset);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeProcess(
        JNIEnv *env, jclass clazz, jshortArray array, jint count, jint channels) {
    (void)clazz;
    if (array == (jshortArray)0) return;

    jsize length = (*env)->GetArrayLength(env, array);
    if (count < 0) return;
    if (count > length) count = length;

    jshort *data = (*env)->GetShortArrayElements(env, array, (jboolean *)0);
    if (data == (jshort *)0) return;

    dsp_state_process(&g_dsp_state, (rfa_i16 *)data,
                      (int)count, (int)channels);
    (*env)->ReleaseShortArrayElements(env, array, data, 0);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeApplyGain(
        JNIEnv *env, jclass clazz, jshortArray array, jint count, jlong gainQ30) {
    (void)clazz;
    if (array == (jshortArray)0 || gainQ30 < 0) return;

    jsize length = (*env)->GetArrayLength(env, array);
    if (count < 0) return;
    if (count > length) count = length;

    jshort *data = (*env)->GetShortArrayElements(env, array, (jboolean *)0);
    if (data == (jshort *)0) return;

    dsp_apply_gain_q30((rfa_i16 *)data, (int)count, (rfa_u64)gainQ30);
    (*env)->ReleaseShortArrayElements(env, array, data, 0);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterReset(
        JNIEnv *env, jclass clazz, jint channels, jlong gateBlockQ36) {
    (void)env;
    (void)clazz;
    meter_state_reset(&g_meter_state, (int)channels,
                      gateBlockQ36 < 0 ? 0ULL : (rfa_u64)gateBlockQ36);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterPush(
        JNIEnv *env, jclass clazz, jshortArray array, jint count, jint channels) {
    (void)clazz;
    if (array == (jshortArray)0) return;

    jsize length = (*env)->GetArrayLength(env, array);
    if (count < 0) return;
    if (count > length) count = length;

    jshort *data = (*env)->GetShortArrayElements(env, array, (jboolean *)0);
    if (data == (jshort *)0) return;

    meter_state_push(&g_meter_state, (const rfa_i16 *)data,
                     (int)count, (int)channels);
    (*env)->ReleaseShortArrayElements(env, array, data, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterResult(
        JNIEnv *env, jclass clazz, jlongArray output) {
    (void)clazz;
    if (output == (jlongArray)0) return;
    if ((*env)->GetArrayLength(env, output) < 4) return;

    rfa_u64 values[4];
    meter_state_result(&g_meter_state, values);
    jlong out[4];
    for (int i = 0; i < 4; ++i) out[i] = (jlong)values[i];
    (*env)->SetLongArrayRegion(env, output, 0, 4, out);
}

JNIEXPORT jlong JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterRelativeGate(
        JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    return (jlong)meter_state_relative_gate_block(&g_meter_state);
}

JNIEXPORT jlong JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterGain(
        JNIEnv *env, jclass clazz, jlong targetEnergyQ36,
        jlong ceilingQ16) {
    (void)env;
    (void)clazz;
    if (targetEnergyQ36 <= 0 || ceilingQ16 <= 0) return (jlong)(1ULL << 30);
    return (jlong)meter_state_gain_q30(&g_meter_state,
                                       (rfa_u64)targetEnergyQ36,
                                       (rfa_u64)ceilingQ16);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeSpectrum(
        JNIEnv *env, jclass clazz, jshortArray array, jint count,
        jint channels, jlongArray output) {
    (void)clazz;
    if (array == (jshortArray)0 || output == (jlongArray)0) return;
    if ((*env)->GetArrayLength(env, output) < METER_SPECTRUM_BANDS) return;

    jsize length = (*env)->GetArrayLength(env, array);
    if (count < 0) return;
    if (count > length) count = length;

    jshort *data = (*env)->GetShortArrayElements(env, array, (jboolean *)0);
    if (data == (jshort *)0) return;

    rfa_u64 bands[METER_SPECTRUM_BANDS];
    meter_spectrum16((const rfa_i16 *)data, (int)count, (int)channels, bands);
    (*env)->ReleaseShortArrayElements(env, array, data, JNI_ABORT);

    jlong out[METER_SPECTRUM_BANDS];
    for (int i = 0; i < METER_SPECTRUM_BANDS; ++i) out[i] = (jlong)bands[i];
    (*env)->SetLongArrayRegion(env, output, 0, METER_SPECTRUM_BANDS, out);
}


JNIEXPORT jbyteArray JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeContainerHeader(
        JNIEnv *env, jclass clazz, jint kind, jint channels, jint matrixDim,
        jint sampleRate, jint waveCount, jint flags, jint chunkCount,
        jlong payloadBytes, jint blockSamples) {
    rfa_container_desc desc;
    rfa_u8 bytes[RFA_CONTAINER_HEADER_BYTES];
    int written;
    jbyteArray output;
    (void)clazz;

    if (payloadBytes < 0 || payloadBytes > 4294967295LL) return (jbyteArray)0;
    if (waveCount < 0 || flags < 0 || chunkCount < 0 || blockSamples < 0) {
        return (jbyteArray)0;
    }

    desc.kind = (int)kind;
    desc.version = 1;
    desc.channels = (int)channels;
    desc.matrix_dim = (int)matrixDim;
    desc.sample_rate = sampleRate < 0 ? 0U : (rfa_u32)sampleRate;
    desc.wave_count = (rfa_u32)waveCount;
    desc.flags = (rfa_u32)flags;
    desc.chunk_count = (rfa_u32)chunkCount;
    desc.payload_bytes = (rfa_u32)payloadBytes;
    desc.block_samples = (rfa_u32)blockSamples;

    written = rfa_container_write_header(
            bytes, RFA_CONTAINER_HEADER_BYTES, &desc);
    if (written != RFA_CONTAINER_HEADER_BYTES) return (jbyteArray)0;

    output = (*env)->NewByteArray(env, RFA_CONTAINER_HEADER_BYTES);
    if (output == (jbyteArray)0) return (jbyteArray)0;
    (*env)->SetByteArrayRegion(
            env, output, 0, RFA_CONTAINER_HEADER_BYTES, (const jbyte *)bytes);
    return output;
}

JNIEXPORT jbyteArray JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeChunkHeader(
        JNIEnv *env, jclass clazz, jlong type, jlong flags,
        jlong payloadBytes, jlong itemCount) {
    rfa_u8 bytes[RFA_CONTAINER_CHUNK_HEADER_BYTES];
    int written;
    jbyteArray output;
    (void)clazz;

    if (type < 0 || type > 4294967295LL ||
        flags < 0 || flags > 4294967295LL ||
        payloadBytes < 0 || payloadBytes > 4294967295LL ||
        itemCount < 0 || itemCount > 4294967295LL) {
        return (jbyteArray)0;
    }

    written = rfa_container_write_chunk_header(
            bytes, RFA_CONTAINER_CHUNK_HEADER_BYTES,
            (rfa_u32)type, (rfa_u32)flags,
            (rfa_u32)payloadBytes, (rfa_u32)itemCount);
    if (written != RFA_CONTAINER_CHUNK_HEADER_BYTES) return (jbyteArray)0;

    output = (*env)->NewByteArray(env, RFA_CONTAINER_CHUNK_HEADER_BYTES);
    if (output == (jbyteArray)0) return (jbyteArray)0;
    (*env)->SetByteArrayRegion(
            env, output, 0, RFA_CONTAINER_CHUNK_HEADER_BYTES,
            (const jbyte *)bytes);
    return output;
}


JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeSweepReset(
        JNIEnv *env, jclass clazz, jlong startStepQ32, jlong ratioQ31,
        jint gainQ15, jint frames) {
    (void)env;
    (void)clazz;
    if (startStepQ32 < 0 || startStepQ32 > 4294967295LL) return;
    if (ratioQ31 < 0 || ratioQ31 > 4294967295LL) return;
    rfa_exp_sweep_reset_q31(
            &g_sweep_state,
            (rfa_u32)startStepQ32,
            (rfa_u32)ratioQ31,
            (rfa_i32)gainQ15,
            (int)frames);
}

JNIEXPORT jint JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeSweepRender(
        JNIEnv *env, jclass clazz, jshortArray output,
        jint frames, jint channels) {
    jsize length;
    jshort *data;
    int required;
    int produced;
    (void)clazz;

    if (output == (jshortArray)0) return 0;
    if (frames <= 0 || channels <= 0 || channels > 2) return 0;
    if (frames > 1073741823 / channels) return 0;
    required = (int)frames * (int)channels;
    length = (*env)->GetArrayLength(env, output);
    if (required > length) return 0;

    data = (*env)->GetShortArrayElements(env, output, (jboolean *)0);
    if (data == (jshort *)0) return 0;
    produced = rfa_exp_sweep_render_q15(
            &g_sweep_state, (rfa_i16 *)data, (int)frames, (int)channels);
    (*env)->ReleaseShortArrayElements(env, output, data, 0);
    return (jint)produced;
}

JNIEXPORT jint JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeRelativeTransfer(
        JNIEnv *env, jclass clazz,
        jshortArray reference, jint referenceCount,
        jshortArray response, jint responseCount,
        jint minLag, jint maxLag, jlongArray output) {
    jsize refLength;
    jsize respLength;
    jshort *refData;
    jshort *respData;
    rfa_relative_transfer result;
    jlong values[5];
    int ok;
    (void)clazz;

    if (reference == (jshortArray)0 ||
        response == (jshortArray)0 ||
        output == (jlongArray)0) return 0;
    if ((*env)->GetArrayLength(env, output) < 5) return 0;

    refLength = (*env)->GetArrayLength(env, reference);
    respLength = (*env)->GetArrayLength(env, response);
    if (referenceCount < 0 || referenceCount > refLength) return 0;
    if (responseCount < 0 || responseCount > respLength) return 0;

    refData = (*env)->GetShortArrayElements(env, reference, (jboolean *)0);
    if (refData == (jshort *)0) return 0;
    respData = (*env)->GetShortArrayElements(env, response, (jboolean *)0);
    if (respData == (jshort *)0) {
        (*env)->ReleaseShortArrayElements(env, reference, refData, JNI_ABORT);
        return 0;
    }

    ok = rfa_relative_transfer_search(
            (const rfa_i16 *)refData, (int)referenceCount,
            (const rfa_i16 *)respData, (int)responseCount,
            (int)minLag, (int)maxLag, &result);

    (*env)->ReleaseShortArrayElements(env, reference, refData, JNI_ABORT);
    (*env)->ReleaseShortArrayElements(env, response, respData, JNI_ABORT);

    values[0] = (jlong)result.correlation;
    values[1] = (jlong)result.reference_energy;
    values[2] = (jlong)result.response_energy;
    values[3] = (jlong)result.best_lag;
    values[4] = (jlong)result.analyzed;
    (*env)->SetLongArrayRegion(env, output, 0, 5, values);
    return ok ? 1 : 0;
}
