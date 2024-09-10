#define LOG_TAG "JNI_RingBuf"

#include "log/logger.h"
#include "lcu/ring/ringbuffer.h"
#include "lcu/common_macro.h"
#include <jni.h>

//=======================================================================
//  Ring buffer start  
//=======================================================================

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_create(JNIEnv *env, jclass clazz,
                                            jlongArray _handle_holder,
                                            jint _buffer_size) {
    if (_buffer_size < 2) {
        return -1;
    }
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);

    uint32_t buffer_size = (uint32_t) _buffer_size;
    ring_buffer_handle handle = RingBuffer_create(buffer_size);
    handle_holder[0] = PTR_TO_LONG64(handle);

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_availableRead(JNIEnv *env, jclass clazz,
                                                   jlong _handle,
                                                   jintArray _data_len_holder) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    jint *data_len_holder = (*env)->GetIntArrayElements(env, _data_len_holder, NULL);
    data_len_holder[0] = (jint) RingBuffer_available_read(handle);

    (*env)->ReleaseIntArrayElements(env, _data_len_holder, data_len_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_availableWrite(JNIEnv *env, jclass clazz,
                                                    jlong _handle,
                                                    jintArray _data_len_holder) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    jint *data_len_holder = (*env)->GetIntArrayElements(env, _data_len_holder, NULL);
    data_len_holder[0] = (jint) RingBuffer_available_write(handle);

    (*env)->ReleaseIntArrayElements(env, _data_len_holder, data_len_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_discard(JNIEnv *env, jclass clazz,
                                             jlong _handle,
                                             jint _len) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    uint32_t len = (uint32_t) _len;
    len = RingBuffer_discard(handle, len);

    return (jint) len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_peek(JNIEnv *env, jclass clazz,
                                          jlong _handle,
                                          jbyteArray _data, jint _offset, jint _len) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int) _offset;

    uint32_t len = (uint32_t) _len;
    len = RingBuffer_peek(handle, (void *) ((char *) data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    return (jint) len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_read(JNIEnv *env, jclass clazz,
                                          jlong _handle,
                                          jbyteArray _data, jint _offset, jint _len) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int) _offset;

    uint32_t len = (uint32_t) _len;
    len = RingBuffer_read(handle, (void *) ((char *) data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    return (jint) len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_write(JNIEnv *env, jclass clazz,
                                           jlong _handle,
                                           jbyteArray _data, jint _offset, jint _len) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int) _offset;

    uint32_t len = (uint32_t) _len;
    len = RingBuffer_write(handle, (void *) ((char *) data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    return (jint) len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_clear(JNIEnv *env, jclass clazz,
                                           jlong _handle) {
    ring_buffer_handle handle = LONG64_TO_PTR(_handle);

    RingBuffer_clear(handle);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_RingBufferJni_destroy(JNIEnv *env, jclass clazz,
                                             jlongArray _handle_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG64_TO_PTR(handle_holder[0]);

    RingBuffer_destroy(&handle);
    handle_holder[0] = 0;

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

//=======================================================================
//  Ring buffer end 
//=======================================================================

