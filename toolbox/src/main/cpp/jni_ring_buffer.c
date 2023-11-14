#define LOG_TAG "ToolboxJNI"
#include "mlog.h"
#include "lcu/ring/ringbuffer.h"
#include "lcu/common_macro.h"
#include <jni.h>

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufCreate(JNIEnv *env, jclass clazz,
                                             jlongArray _handle_holder,
                                             jint _buffer_size) {
    if (_buffer_size < 2) {
        return -1;
    }
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);

    uint32_t buffer_size = (uint32_t)_buffer_size;
    ring_buffer_handle handle = RingBuffer_create(buffer_size);
    handle_holder[0] = PTR_TO_LONG(handle);

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufAvailableRead(JNIEnv *env, jclass clazz,
                                                    jlongArray _handle_holder,
                                                    jintArray _data_len_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    jint *data_len_holder = (*env)->GetIntArrayElements(env, _data_len_holder, NULL);
    data_len_holder[0] = (jint)RingBuffer_available_read(handle);

    (*env)->ReleaseIntArrayElements(env, _data_len_holder, data_len_holder, 0);
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufAvailableWrite(JNIEnv *env, jclass clazz,
                                                     jlongArray _handle_holder,
                                                     jintArray _data_len_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    jint *data_len_holder = (*env)->GetIntArrayElements(env, _data_len_holder, NULL);
    data_len_holder[0] = (jint)RingBuffer_available_write(handle);

    (*env)->ReleaseIntArrayElements(env, _data_len_holder, data_len_holder, 0);
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufDiscard(JNIEnv *env, jclass clazz,
                                              jlongArray _handle_holder,
                                              jint _len) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    uint32_t len = (uint32_t)_len;
    len = RingBuffer_discard(handle, len);

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return (jint)len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufPeek(JNIEnv *env, jclass clazz,
                                           jlongArray _handle_holder,
                                           jbyteArray _data, jint _offset, jint _len) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int)_offset;

    uint32_t len = (uint32_t)_len;
    len = RingBuffer_peek(handle, (void *)((char *)data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return (jint)len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufRead(JNIEnv *env, jclass clazz,
                                           jlongArray _handle_holder,
                                           jbyteArray _data, jint _offset, jint _len) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int)_offset;

    uint32_t len = (uint32_t)_len;
    len = RingBuffer_read(handle, (void *)((char *)data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return (jint)len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufWrite(JNIEnv *env, jclass clazz,
                                            jlongArray _handle_holder,
                                            jbyteArray _data, jint _offset, jint _len) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    jbyte *data = (*env)->GetByteArrayElements(env, _data, NULL);
    int offset = (int)_offset;

    uint32_t len = (uint32_t)_len;
    len = RingBuffer_write(handle, (void *)((char *)data + offset), len);

    (*env)->ReleaseByteArrayElements(env, _data, data, 0);
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return (jint)len;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufClear(JNIEnv *env, jclass clazz,
                                            jlongArray _handle_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    RingBuffer_clear(handle);

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_ToolboxJni_rbufDestroy(JNIEnv *env, jclass clazz,
                                              jlongArray _handle_holder) {
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    ring_buffer_handle handle = LONG_TO_PTR(handle_holder[0]);

    RingBuffer_destroy(&handle);
    handle_holder[0] = 0;

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    return 0;
}


