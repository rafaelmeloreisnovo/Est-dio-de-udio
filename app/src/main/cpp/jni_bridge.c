#include <jni.h>
#include "dsp_core.h"
#include "meter_core.h"
#include "rfa_container_core.h"

/*
 * Platform-owned instances.
 * Mutable state lives here, at the Android boundary, not in the freestanding cores.
 */
static dsp_state g_dsp_state;
static meter_state g_meter_state;

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
