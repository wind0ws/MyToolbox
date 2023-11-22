#define LOG_TAG "JNI_Toolbox"

#include "log/logger.h"
#include "lcu.h"
#include <jni.h>

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnLoad called");
    lcu_init();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnUnload called");
    lcu_deinit();
}
