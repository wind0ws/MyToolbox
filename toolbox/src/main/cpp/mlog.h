#pragma once
#ifndef MLOG_H_
#define MLOG_H_

#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LINE_STAR "****************************************************************"
#ifndef LOG_TAG
#define LOG_TAG "SOCKET_SERVER"
#endif

extern int g_showLog;

#define LOGV(...) if(g_showLog) {\
__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__);\
}
#define LOGD(...) if(g_showLog) {\
__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);\
}
#define LOGI(...) if(g_showLog) {\
__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);\
}
#define LOGW(...) if(g_showLog) {\
__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);\
}
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);

void log_chars_hex(const char *chars, int length);

#ifdef __cplusplus
}
#endif

#endif /* MLOG_H_ */
