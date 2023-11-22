#include <jni.h>
#include <string>
#include <cstring>
#include "header/log.h"
#include "header/main.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_threshold_mytoolbox_jni_NativeLibJni_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_threshold_mytoolbox_jni_NativeLibJni_setLoggable(JNIEnv *env, jclass type, jboolean isLoggable) {
    g_ShowLog = isLoggable;
    ALOGV("JNITag", "testLog");
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_threshold_mytoolbox_jni_NativeLibJni_stringBytesFromJni(JNIEnv *env, jclass type, jstring content) {
    LOGD("enter %s:%d", __func__, __LINE__)
    const char *chars = env->GetStringUTFChars(content, NULL);

    int strLenOfChars = (int) strlen(chars);
    jsize jstringLen = env->GetStringLength(content);
    LOGD("chars:%s, strLenOfChars=%d, jstringLen=%d", chars, strLenOfChars, jstringLen)
    jbyteArray stringjbytes = env->NewByteArray(strLenOfChars);
    env->SetByteArrayRegion(stringjbytes, 0, strLenOfChars, reinterpret_cast<const jbyte *>(chars));

    env->ReleaseStringUTFChars(content, chars);
    return stringjbytes;
}

extern int main(int argc, char **argv) {
    LOGD("called main");
    return 0;
}

extern int daemon_init(int token) {
    LOGE("called daemon_init. token=%d", token);
    return 0;
}