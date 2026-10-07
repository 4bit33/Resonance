// JNI bridge to Chromaprint for com.resonance.player.data.fingerprint.Chromaprint.
#include <jni.h>
#include <cstdlib>
#include "chromaprint.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_resonance_player_data_fingerprint_Chromaprint_nativeStart(JNIEnv *, jclass, jint sampleRate, jint channels) {
    ChromaprintContext *ctx = chromaprint_new(CHROMAPRINT_ALGORITHM_DEFAULT);
    if (ctx == nullptr) return 0;
    if (!chromaprint_start(ctx, sampleRate, channels)) {
        chromaprint_free(ctx);
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jboolean JNICALL
Java_com_resonance_player_data_fingerprint_Chromaprint_nativeFeed(JNIEnv *env, jclass, jlong handle, jshortArray samples, jint count) {
    auto *ctx = reinterpret_cast<ChromaprintContext *>(handle);
    jshort *data = env->GetShortArrayElements(samples, nullptr);
    int ok = chromaprint_feed(ctx, data, count);
    env->ReleaseShortArrayElements(samples, data, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

/** Finishes and returns the compressed, base64 fingerprint AcoustID expects, or null. */
JNIEXPORT jstring JNICALL
Java_com_resonance_player_data_fingerprint_Chromaprint_nativeFinish(JNIEnv *env, jclass, jlong handle) {
    auto *ctx = reinterpret_cast<ChromaprintContext *>(handle);
    if (!chromaprint_finish(ctx)) return nullptr;
    char *fp = nullptr;
    if (!chromaprint_get_fingerprint(ctx, &fp) || fp == nullptr) return nullptr;
    jstring result = env->NewStringUTF(fp);
    chromaprint_dealloc(fp);
    return result;
}

JNIEXPORT void JNICALL
Java_com_resonance_player_data_fingerprint_Chromaprint_nativeFree(JNIEnv *, jclass, jlong handle) {
    chromaprint_free(reinterpret_cast<ChromaprintContext *>(handle));
}

}
