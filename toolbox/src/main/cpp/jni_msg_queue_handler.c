#define LOG_TAG "JNI_MSG_Q_HDL"

#include "log/logger.h"
#include "ring/msg_queue_handler.h"
#include "mem/strings.h"
#include "common_macro.h"
#include <jni.h>

// version that communicate with java jni class
#define JNI_PROTOCOL_VER   0

typedef struct {
    msg_queue_handler obj;

    struct {
        JavaVM *jvm;
        jclass m_cls;
        jmethodID m_method_id;
        jobject m_obj;
        bool m_obj_global_referenced;
    } cb_data;

    size_t cache_msg_max_obj_len;
    queue_msg_t cache_msg;
} msg_queue_jni_context_t;

static int handle_queue_msg(queue_msg_t *msg_p, void *user_data);

JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_init(JNIEnv *env, jclass clazz,
                                               jlongArray _handle_holder,
                                               jobject _init_param) {
    (void) clazz;
    jint ret = -1;

    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    handle_holder[0] = 0;

    const size_t cache_msg_max_obj_len = 40960U;
    const size_t mem_size_jni_context = sizeof(msg_queue_jni_context_t) + cache_msg_max_obj_len;
    msg_queue_jni_context_t *context = (msg_queue_jni_context_t *) malloc(mem_size_jni_context);
    do {
        if (!context) {
            LOGE("failed alloc jni context! mem_size=%zu", mem_size_jni_context);
            break;
        }
        memset(context, 0, mem_size_jni_context);
        context->cache_msg_max_obj_len = cache_msg_max_obj_len;

        (*env)->GetJavaVM(env, &(context->cb_data.jvm));
        context->cb_data.m_cls = (*env)->GetObjectClass(env, _init_param);
        jfieldID field_id_protocol_ver =
                (*env)->GetFieldID(env, context->cb_data.m_cls, "protocolVer", "I");
        if (NULL == field_id_protocol_ver) {
            LOGE("can't find \"protocolVer\" in class");
            break;
        }
        int protocol_ver = (int) ((*env)->GetIntField(env, _init_param, field_id_protocol_ver));
        if (JNI_PROTOCOL_VER != protocol_ver) {
            LOGE("protocol_ver not matched, jni class not match with libraries! expected=%d, yours=%d",
                 JNI_PROTOCOL_VER, protocol_ver);
            break;
        }

        jfieldID field_id_buf_size =
                (*env)->GetFieldID(env, context->cb_data.m_cls, "bufSize", "I");
        jfieldID field_id_cb_name =
                (*env)->GetFieldID(env, context->cb_data.m_cls,
                                   "callbackFunction", "Ljava/lang/String;");
        if (NULL == field_id_buf_size || NULL == field_id_cb_name) {
            LOGE("can't find \"bufSize\" or \"callbackFunction\" in class");
            break;
        }

        jint j_buf_size = (*env)->GetIntField(env, _init_param, field_id_buf_size);
        jstring j_cb_name = (jstring) ((*env)->GetObjectField(env, _init_param, field_id_cb_name));

        context->cb_data.m_obj = (*env)->NewGlobalRef(env, _init_param);
        context->cb_data.m_obj_global_referenced = true;

        const char *str_cb_name = (*env)->GetStringUTFChars(env, j_cb_name, NULL);
        LOGI("callback function name => \"%s\"", str_cb_name);
        context->cb_data.m_method_id = (*env)->GetMethodID(env, context->cb_data.m_cls,
                                                           str_cb_name, "(III[B)I");
        (*env)->ReleaseStringUTFChars(env, j_cb_name, str_cb_name);
        if (NULL == context->cb_data.m_method_id) {
            LOGE("failed on lookup callback function: \"%s\"", str_cb_name);
            break;
        }

        if (NULL == (context->obj = msg_queue_handler_create(j_buf_size,
                                                             handle_queue_msg,
                                                             context))) {
            LOGI("failed on init msg_queue_handler_create");
            break;
        }
        handle_holder[0] = PTR_TO_LONG64(context);
        ret = 0;
    } while (0);

    if (0 != ret) {
        if (NULL != context) {
            if (context->cb_data.m_obj_global_referenced) {
                (*env)->DeleteGlobalRef(env, context->cb_data.m_obj);
                context->cb_data.m_obj_global_referenced = false;
            }
            free(context);
        }
    }

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    LOGI("init finished. ret=%d", ret);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_feedMsg(JNIEnv *env, jclass clazz,
                                                  jlong _handle, jint _what, jint _arg1, jint _arg2,
                                                  jbyteArray _obj) {
    (void) clazz;
    MSG_Q_CODE msg_push_status = MSG_Q_CODE_GENERIC_FAIL;
    msg_queue_jni_context_t *context = LONG64_TO_PTR(_handle);
    if (!context) {
        return -1;
    }

    context->cache_msg.what = _what;
    context->cache_msg.arg1 = _arg1;
    context->cache_msg.arg2 = _arg2;
    context->cache_msg.obj_len = 0;

    jbyteArray j_obj_array = _obj;
    bool valid_msg = false;
    do {
        if (NULL == j_obj_array) {
            valid_msg = true;
            break;
        }
        jsize obj_len = (*env)->GetArrayLength(env, j_obj_array);
        if (0 == obj_len) {
            valid_msg = true;
            break;
        }
        if (obj_len < 1) {
            LOGE("invalid obj_len=%d", obj_len);
            break;
        }

        jbyte *obj_bytes = (*env)->GetByteArrayElements(env, j_obj_array, NULL);
        if (obj_len <= (int) context->cache_msg_max_obj_len) {
            context->cache_msg.obj_len = (int) obj_len;
            memcpy(&(context->cache_msg.obj[0]), obj_bytes, (size_t) obj_len);
            valid_msg = true;
        } else {
            LOGE("fatal error: lost message! obj too large! obj_len=%d", obj_len);
        }
        (*env)->ReleaseByteArrayElements(env, j_obj_array, obj_bytes, 0);
    } while (0);

    if (valid_msg) {
//        LOGV("--> try push");
        do {
            if (MSG_Q_CODE_SUCCESS ==
                (msg_push_status = msg_queue_handler_push(context->obj,
                                                          &context->cache_msg))) {
                break;
            }
            if (MSG_Q_CODE_FULL != msg_push_status) {
                LOGE("failed(%d) on push msg", msg_push_status);
                break;
            }
        } while (1);
//        LOGV("<-- push out with status: %d", msg_push_status);
    }
    return (jint) msg_push_status;
}


JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_destroy(JNIEnv *env, jclass clazz,
                                                  jlongArray _handle_holder) {
    (void) clazz;
    jint ret = -1;

    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    msg_queue_jni_context_t *context = LONG64_TO_PTR(handle_holder[0]);
    do {
        if (NULL == context) {
            break;
        }
        if (context->cb_data.m_obj_global_referenced) {
            (*env)->DeleteGlobalRef(env, context->cb_data.m_obj);
            context->cb_data.m_obj_global_referenced = false;
        }
        msg_queue_handler_destroy(&context->obj);
        ret = 0;
    } while (0);

    handle_holder[0] = 0;
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    LOGI("destroyed done!");
    return ret;
}


static int handle_queue_msg(queue_msg_t *msg_p, void *user_data) {
    msg_queue_jni_context_t *context = (msg_queue_jni_context_t *) user_data;
    JNIEnv *env = NULL;
    jboolean jvm_attached = JNI_FALSE;
    int status;

    // get env from current thread
    status = (*(context->cb_data.jvm))->GetEnv(context->cb_data.jvm,
                                               (void **) &env, JNI_VERSION_1_4);
    if (status < 0) {
        status = (*(context->cb_data.jvm))->AttachCurrentThread(context->cb_data.jvm, &env, NULL);
        if (status < 0) {
            LOGE("oops: vm AttachCurrentThread error. lost msg!!!");
            return 0;
        }
        jvm_attached = JNI_TRUE;
    }

    jbyteArray msg_obj_array = NULL;
    if (msg_p->obj_len > 0) {
        msg_obj_array = (*env)->NewByteArray(env, (jsize) msg_p->obj_len);
        (*env)->SetByteArrayRegion(env, msg_obj_array, 0, (jsize) msg_p->obj_len,
                                   (jbyte *) msg_p->obj);
    }

    jint user_ret = (*env)->CallIntMethod(env, context->cb_data.m_obj, context->cb_data.m_method_id,
                                          msg_p->what, msg_p->arg1, msg_p->arg2, msg_obj_array);
    if (0 != user_ret) {
        LOGE("user handle msg err. %d", user_ret);
    }
    if (jvm_attached) {
        (*(context->cb_data.jvm))->DetachCurrentThread(context->cb_data.jvm);
    }
    return (int) user_ret;
}


