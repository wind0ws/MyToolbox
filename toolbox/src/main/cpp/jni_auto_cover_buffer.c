#define LOG_TAG "JNI_AutoCoverBuf"

#include "log/logger.h"
#include "lcu/ring/autocover_buffer.h"
#include "lcu/common_macro.h"
#include <jni.h>

//=======================================================================
//  auto cover buffer start  
//=======================================================================

JNIEXPORT jint JNICALL
Java_com_threshold_jni_AutoCoverBufferJni_create(JNIEnv *env, jclass clazz,
                                                 jlongArray _handle_holder,
                                                 jint _buffer_size) {
    if (_buffer_size < 2) {
        return -1;
    }
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);

    uint32_t buffer_size = (uint32_t) _buffer_size;
    auto_cover_buf_handle handle = auto_cover_buf_create(buffer_size, NULL);
    handle_holder[0] = PTR_TO_LONG64(handle);

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_AutoCoverBufferJni_availableRead(JNIEnv *env, jclass clazz,
                                                        jlong _handle, jint _read_pos) {
    auto_cover_buf_handle handle = LONG64_TO_PTR(_handle);

    int len = (jint) auto_cover_buf_available_read(handle, (uint32_t) _read_pos);
    return (jint) len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_AutoCoverBufferJni_read(JNIEnv *env, jclass clazz,
                                               jlong _handle, jint _read_pos,
                                               jbyteArray _data, jint _offset, jint _len) {
    auto_cover_buf_handle handle = LONG64_TO_PTR(_handle);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int) _offset;

    uint32_t len = (uint32_t) _len;
    uint32_t read_pos = (uint32_t) _read_pos;
    int ret = auto_cover_buf_read(handle, read_pos, (void *) ((char *) data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    return (jint) ret;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_AutoCoverBufferJni_write(JNIEnv *env, jclass clazz,
                                                jlong _handle,
                                                jbyteArray _data, jint _offset, jint _len) {
    auto_cover_buf_handle handle = LONG64_TO_PTR(_handle);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int) _offset;

    uint32_t len = (uint32_t) _len;
    int ret = auto_cover_buf_write(handle, (void *) ((char *) data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    return (jint) ret;
}


JNIEXPORT jint JNICALL
Java_com_threshold_jni_AutoCoverBufferJni_destroy(JNIEnv *env, jclass clazz,
                                                  jlongArray _handle_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    auto_cover_buf_handle handle = LONG64_TO_PTR(handle_holder[0]);

    auto_cover_buf_destroy(&handle);
    handle_holder[0] = 0;

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

//=======================================================================
//  auto cover buffer end 
//=======================================================================

