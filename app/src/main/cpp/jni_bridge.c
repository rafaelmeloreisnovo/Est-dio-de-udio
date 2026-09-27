#include <jni.h>
#include "dsp_core.h"
#include "meter_core.h"

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeReset(
        JNIEnv *env, jclass clazz, jint preset) {
    (void)env;
    (void)clazz;
    dsp_reset((int)preset);
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

    dsp_process((signed short *)data, (int)count, (int)channels);
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

    dsp_apply_gain_q30((signed short *)data, (int)count,
                       (dsp_u64)gainQ30);
    (*env)->ReleaseShortArrayElements(env, array, data, 0);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterReset(
        JNIEnv *env, jclass clazz, jint channels, jlong gateBlockQ36) {
    (void)env;
    (void)clazz;
    meter_reset((int)channels,
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

    meter_push((const signed short *)data, (int)count, (int)channels);
    (*env)->ReleaseShortArrayElements(env, array, data, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterResult(
        JNIEnv *env, jclass clazz, jlongArray output) {
    (void)clazz;
    if (output == (jlongArray)0) return;
    if ((*env)->GetArrayLength(env, output) < 4) return;

    rfa_u64 values[4];
    meter_result(values);
    jlong out[4];
    for (int i = 0; i < 4; ++i) out[i] = (jlong)values[i];
    (*env)->SetLongArrayRegion(env, output, 0, 4, out);
}

JNIEXPORT jlong JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterRelativeGate(
        JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    return (jlong)meter_relative_gate_block();
}

JNIEXPORT jlong JNICALL
Java_io_rafaelia_audiostudio_NativeDsp_nativeMeterGain(
        JNIEnv *env, jclass clazz, jlong targetEnergyQ36,
        jlong ceilingQ16) {
    (void)env;
    (void)clazz;
    if (targetEnergyQ36 <= 0 || ceilingQ16 <= 0) return (jlong)(1ULL << 30);
    return (jlong)meter_gain_q30((rfa_u64)targetEnergyQ36,
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
    meter_spectrum16((const signed short *)data, (int)count, (int)channels, bands);
    (*env)->ReleaseShortArrayElements(env, array, data, JNI_ABORT);

    jlong out[METER_SPECTRUM_BANDS];
    for (int i = 0; i < METER_SPECTRUM_BANDS; ++i) out[i] = (jlong)bands[i];
    (*env)->SetLongArrayRegion(env, output, 0, METER_SPECTRUM_BANDS, out);
}
