#define LOG_TAG "JNI_ASSETS"

#include "log/logger.h"
#include "common_macro.h"
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

JNIEXPORT jint JNICALL
Java_com_threshold_jni_AssetsJni_getResSize(JNIEnv *env, jclass clazz,
                                            jobject _asset_mgr,
                                            jstring _res_path,
                                            jlongArray _res_size_holder) {
    UNUSED(clazz);
    if (NULL == _asset_mgr || NULL == _res_path || NULL == _res_size_holder) {
        LOGE_TRACE("invalid param");
        return -1;
    }
    const char *res_path_str = (*env)->GetStringUTFChars(env, _res_path, NULL);
    if (!res_path_str) {
        LOGE_TRACE("failed on get res_path_str");
        return -1;
    }

    AAssetManager *asset_mgr = AAssetManager_fromJava(env, _asset_mgr);
    AAsset *asset = AAssetManager_open(asset_mgr, res_path_str, AASSET_MODE_UNKNOWN);
    if (NULL == asset) {
        LOGE_TRACE("failed on open asset: \"%s\"", res_path_str);
        (*env)->ReleaseStringUTFChars(env, _res_path, res_path_str);
        return -1;
    }

    jlong *res_size_holder = (*env)->GetLongArrayElements(env, _res_size_holder, NULL);
    jlong file_size = AAsset_getLength64(asset);
    res_size_holder[0] = file_size;
    (*env)->ReleaseLongArrayElements(env, _res_size_holder, res_size_holder, 0);

    AAsset_close(asset);
    (*env)->ReleaseStringUTFChars(env, _res_path, res_path_str);
    return 0;
}

