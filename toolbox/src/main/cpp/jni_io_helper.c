#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "UnusedValue"

#define LOG_TAG "JNI_IO_HELPER"

#include "mem/strings.h"
#include "log/logger.h"
#include "common_macro.h"
#include <jni.h>
#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>

JNIEXPORT jstring JNICALL
Java_com_threshold_jni_IOHelperJni_nativeGetCwd(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);

    char cwd[256] = {0};
    if (!getcwd(cwd, sizeof(cwd))) {
        LOGE_TRACE("failed on getcwd");
    }
    return (*env)->NewStringUTF(env, cwd);
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_IOHelperJni_nativeChdir(JNIEnv *env, jclass clazz, jstring path_obj) {
    UNUSED(clazz);

    jint ret = -1;
    const char *path = NULL;
    if ((NULL == path_obj) || NULL == (path = (*env)->GetStringUTFChars(env, path_obj, NULL))) {
        LOGE_TRACE("failed to get path string!");
        return ret;
    }

    do {
        if (0 != access(path, F_OK)){
            LOGE_TRACE("failed on access(%s)", path);
            break;
        }
        if (0 != chdir(path)) {
            LOGE_TRACE("chdir(%s) failed! errno: %d", path, errno);
            break;
        }
        ret = 0;
        LOGD("chdir succeed. your path = \"%s\"", path);

        char cwd[256] = {0};
        if (getcwd(cwd, sizeof(cwd))) {
            LOGI_TRACE("after chdir, now work dir: \"%s\"", cwd);
        } else {
            LOGE("failed on getcwd");
        }
    } while (0);

    (*env)->ReleaseStringUTFChars(env, path_obj, path);
    return ret;
}

#pragma clang diagnostic pop