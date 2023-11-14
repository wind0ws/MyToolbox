#ifndef __LOGGER_H__
#define __LOGGER_H__

#include <jni.h>

#define OPEN_LOGGER (1)
bool g_ShowLog = JNI_FALSE;

#if OPEN_LOGGER
#include <android/log.h>
#define LOG_TAG "NativeJni"
#define ALOGV(TAG, ...) if(g_ShowLog){\
__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__);\
}
#define LOGV(...) if(g_ShowLog) {\
__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__);\
}
#define LOGD(...) if(g_ShowLog) {\
__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);\
}
#define LOGI(...) if(g_ShowLog) {\
__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);\
}
#define LOGW(...) if(g_ShowLog) {\
__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);\
}
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#else
#define ALOGV /\
/
#define LOGV /\
/
#define LOGD /\
/
#define LOGI /\
/
#define LOGW /\
/
#define LOGE /\
/
#endif

#endif
