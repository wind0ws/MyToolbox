#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
//double quotes first search local, if not find, then search system header.
//<angled> include will only search system header
#include "header/log.h"

static char *char_cat(JNIEnv *env, jstring str1, const char *str2) {
    const char *c_str1 = (const char *) env->GetStringUTFChars(str1, 0);
    size_t str1Len = strlen(c_str1);
    size_t str2Len = strlen(str2);
    char *buffer = (char *) malloc(str1Len + str2Len + 1);
    strcpy(buffer, c_str1);
    strcat(buffer, str2);
    env->ReleaseStringUTFChars(str1, c_str1);
    return buffer;
}

static char *jstring2char(JNIEnv *env, jstring jstr) {
    const char *c_str = (const char *) env->GetStringUTFChars(jstr, 0);
    const size_t str_len = strlen(c_str);
    char *result = (char *) malloc(str_len + 1);
    strcpy(result, c_str);
    env->ReleaseStringUTFChars(jstr, c_str);
    return result;
}

static char *malloc_charFromByteArr(JNIEnv *env, jbyteArray byteArr) {
    if (byteArr == NULL) {
        return NULL;
    }
    char *rets = NULL;
    jsize jblen = env->GetArrayLength(byteArr);
    rets = (char *) malloc(sizeof(char) * (jblen + 1));
    env->GetByteArrayRegion(byteArr, 0, jblen, (jbyte *) rets);
    rets[jblen] = '\0';
    return rets;
}

static char *malloc_charFromByteArr(JNIEnv *env, jbyteArray byteArr,jint arrayLen) {
    if (byteArr == NULL) {
        return NULL;
    }
    char *rets = NULL;
    rets = (char *) malloc(sizeof(char) * (arrayLen + 1));
    env->GetByteArrayRegion(byteArr, 0, arrayLen, (jbyte *) rets);
    rets[arrayLen] = '\0';
    return rets;
}

static void *malloc_voidFromByteArr(JNIEnv *env, jbyteArray byteArr) {
    if (byteArr == NULL) {
        return NULL;
    }
    void *rets = NULL;
    jsize jblen = env->GetArrayLength(byteArr);
    rets = (char *) malloc(sizeof(char) * (jblen));
    env->GetByteArrayRegion(byteArr, 0, jblen, (jbyte *) rets);
    return rets;
}

static char *malloc_charFromCharArr(JNIEnv *env, jcharArray charArr) {
    if (charArr == NULL) {
        return NULL;
    }
    char *rets = NULL;
    jsize jclen = env->GetArrayLength(charArr);
    rets = (char *) malloc(sizeof(char) * (jclen + 1));

    jchar *jchars = env->GetCharArrayElements(charArr, 0);
    for (int i = 0; i < jclen; i++) {
        rets[i] = (char) jchars[i];
    }
    rets[jclen] = '\0';
    env->ReleaseCharArrayElements(charArr, jchars, 0);
    return rets;
}

static jcharArray new_charArrFromCharLen(JNIEnv *env, char *value, int length) {
    if (value == NULL || length <= 0) {
        return NULL;
    }
    jcharArray rets = env->NewCharArray(length);
    jchar *jchars = env->GetCharArrayElements(rets, 0);
    for (int i = 0; i < length; i++) {
        jchars[i] = (jchar) value[i];
    }
    env->ReleaseCharArrayElements(rets, jchars, 0);
    return rets;
}

static jcharArray new_charArrFromChar(JNIEnv *env, char *value) {
    if (value == NULL) {
        return NULL;
    }
    size_t len = strlen(value);
    return new_charArrFromCharLen(env, value, (int) len);
}

//使用完jbyteArray，要记得调用 env->DeleteLocalRef(jbyteArray)
static jbyteArray new_byteArrFromVoid(JNIEnv *env, void *data, int length) {
    if (data == NULL) {
        return NULL;
    }
    jbyteArray ret = env->NewByteArray((jsize) length);
    env->SetByteArrayRegion(ret, 0, (jsize) length, (jbyte *) data);
    return ret;
}

//重新采样  resample(outbuffer, inbuffer, byteslen, 8, 48000, 16000, 2, 32);
static int resample(char *outBuf, const char *inBuf, int inLength, int inChannels,
             int inRate, int outRate, int outChannels, int outBit) {
    int outOffset = 0;
    int inOffset = 0;
    int o = 0;
    int i = 0;
    for (i = 0; i < inLength / sizeof(short); i += inChannels) {
        ++inOffset;
        int t = 0;
        for (t = inOffset * outRate / inRate; outOffset < t; ++outOffset) {
            int ch = 0;
            for (ch = 0; ch < outChannels; ++ch) {
                if (outBit == 16) {
                    ((short *) outBuf)[o++] = ((short *) inBuf)[i + ch * inChannels / outChannels];
                } else if (outBit == 32) {
                    ((int *) outBuf)[o++] = ((int) ((short *) inBuf)[i + ch * inChannels / outChannels]) << 16;
                }
            }
        }
    }
    inOffset %= inRate;
    outOffset %= outRate;
    if (outBit == 32) {
        o *= sizeof(int);
    } else {
        o *= sizeof(short);
    }
    return o;
}

static char * pOutCharBuffer = NULL;

extern "C"
JNIEXPORT jint JNICALL
Java_com_threshold_mytoolbox_jni_NativeResampleJni_init(JNIEnv *env, jclass type) {
    if (pOutCharBuffer) {
        return -1;
    }
    pOutCharBuffer = (char*)malloc(8192 * 2);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_threshold_mytoolbox_jni_NativeResampleJni_deinit(JNIEnv *env, jclass type) {
    if (!pOutCharBuffer) {
        return -1;
    }
    free(pOutCharBuffer);
    pOutCharBuffer = NULL;
    return 0;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_threshold_mytoolbox_jni_NativeResampleJni_resample(JNIEnv *env, jclass type, jbyteArray inBuffer_, jint inBufferLen,
                                                       jint inChannels, jint inRate, jint outRate, jint outChannels, jint outBit) {
    jbyte *inBuffer = env->GetByteArrayElements(inBuffer_, NULL);

    char* pInCharBuffer = malloc_charFromByteArr(env, inBuffer_,inBufferLen);
    int outSize = inBufferLen * (outRate/inRate) * (outChannels/inChannels);

    int resampleSize = resample(pInCharBuffer,pOutCharBuffer,inBufferLen,inChannels,inRate,outRate,outChannels,outBit);
    jbyteArray jbytes = new_byteArrFromVoid(env,pOutCharBuffer,resampleSize);

    env->ReleaseByteArrayElements(inBuffer_, inBuffer, 0);
    free(pInCharBuffer);
    pInCharBuffer = NULL;
    return jbytes;
}