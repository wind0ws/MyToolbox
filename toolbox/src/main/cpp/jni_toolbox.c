#define LOG_TAG "JNI_Toolbox"

#include "log/logger.h"
#include "lcu.h"
#include <jni.h>

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    int ret = lcu_global_init();
    LOGI("JNI_OnLoad called. lcu init=%d, ver=%s", ret, lcu_get_version());
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnUnload called");
    lcu_global_cleanup();
}
