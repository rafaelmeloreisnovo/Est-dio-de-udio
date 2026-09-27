#include <jni.h>
#include "dsp_core.h"

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

    jboolean copied = JNI_FALSE;
    jshort *data = (*env)->GetShortArrayElements(env, array, &copied);
    if (data == (jshort *)0) return;

    dsp_process((signed short *)data, (int)count, (int)channels);
    (*env)->ReleaseShortArrayElements(env, array, data, 0);
}
