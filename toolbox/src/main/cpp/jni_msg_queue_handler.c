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

    // flag to stop handle msg.
    bool flag_exit;

    struct {
        // global vm. get it from env.
        JavaVM *jvm;
        // each native thread should have it's own env.
        // here we only have one thread(in "handle_queue_msg") callback to java, so just one env.
        JNIEnv *env;
        // indicate native thread whether attached to jvm.
        bool attached_thread;
        jclass m_cls;
        jmethodID m_method_id;
        jobject m_obj;
    } cb_data;

    // cache for callback jbyteArray to java
    struct {
        jsize obj_capacity;
        jbyteArray j_obj;
    } cache;

    // transform for feedMsg
    struct {
        size_t in_obj_capacity;
        queue_msg_t *in;
    } msg;
} msg_queue_jni_context_t;

#define CLEANUP_MSG(context, type) do { if ((context)->msg. type ) \
{                                          \
  free((context)->msg. type );             \
  (context)->msg. type = NULL;             \
  (context)->msg. type##_obj_capacity = 0; \
} } while(0)

#define RECREATE_MSG(context, type, new_size) do {         \
  if ((new_size) <= (context)->msg. type##_obj_capacity) { \
    break;                                                 \
  }                                                        \
  CLEANUP_MSG((context), type);                            \
  (context)->msg. type##_obj_capacity = (new_size);        \
  (context)->msg. type = (queue_msg_t *) malloc(sizeof(queue_msg_t) + (context)->msg. type##_obj_capacity);\
  if (NULL == (context)->msg. type ) {                     \
    LOGE_TRACE("failed on alloc msg.in. expect_mem_size=%zu",\
               sizeof(queue_msg_t) + (context)->msg. type##_obj_capacity);\
    break;                                                 \
  }                                                        \
  memset((context)->msg. type , 0, sizeof(queue_msg_t) + (context)->msg. type##_obj_capacity);\
} while(0)

static int pri_destroy_jni_context(JNIEnv *env, msg_queue_jni_context_t *context);

static void pri_cleanup_jobj_cache(JNIEnv *env, msg_queue_jni_context_t *context);

static void pri_recreate_jobj_cache(JNIEnv *env,
                                    msg_queue_jni_context_t *context, jsize new_obj_capacity);

static void pri_on_msg_q_handler_status_changed(msg_q_handler_status_e status, void *user_data);

static int handle_queue_msg(queue_msg_t *msg_p, void *user_data);

JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_init(JNIEnv *env, jclass clazz,
                                               jlongArray _handle_holder,
                                               jobject _init_param) {
    (void) clazz;
    jint ret = -1;

    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    handle_holder[0] = 0;

    msg_queue_jni_context_t *context = (msg_queue_jni_context_t *) malloc(
            sizeof(msg_queue_jni_context_t));
    do {
        if (!context) {
            LOGE("failed alloc jni context! mem_size=%zu", sizeof(msg_queue_jni_context_t));
            break;
        }
        memset(context, 0, sizeof(msg_queue_jni_context_t));
        RECREATE_MSG(context, in, 1024U);
        pri_recreate_jobj_cache(env, context, 512);

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
            LOGE("protocol_ver not matched: jni java class not matched with library! "
                 "expected=%d, yours=%d",
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
        if (j_buf_size < 64) {
            LOGE_TRACE("queue buffer size(%d) is too small.", j_buf_size);
            break;
        }
        if (j_buf_size <= 2048) {
            LOGW_TRACE("queue buffer size(%d) is so small, you should be careful.", j_buf_size);
        }
        jstring j_cb_name = (jstring) ((*env)->GetObjectField(env, _init_param, field_id_cb_name));

        context->cb_data.m_obj = (*env)->NewGlobalRef(env, _init_param);

        const char *str_cb_name = (*env)->GetStringUTFChars(env, j_cb_name, NULL);
        LOGI("callback function name => \"%s\"", str_cb_name);
        context->cb_data.m_method_id = (*env)->GetMethodID(env, context->cb_data.m_cls,
                                                           str_cb_name, "(III[BI)I");
        (*env)->ReleaseStringUTFChars(env, j_cb_name, str_cb_name);
        if (NULL == context->cb_data.m_method_id) {
            LOGE("failed on lookup callback function: \"%s\"", str_cb_name);
            break;
        }
        msg_queue_handler_init_param_t msg_q_init_param;
        memset(&msg_q_init_param, 0, sizeof(msg_queue_handler_init_param_t));
        msg_q_init_param.user_data = context;
        msg_q_init_param.fn_handle_msg = handle_queue_msg;
        msg_q_init_param.fn_on_status_changed = pri_on_msg_q_handler_status_changed;
        if (NULL == (context->obj = msg_queue_handler_create(j_buf_size,
                                                             &msg_q_init_param))) {
            LOGI("failed on init msg_queue_handler_create");
            break;
        }
        handle_holder[0] = PTR_TO_LONG64(context);
        ret = 0;
    } while (0);

    if (0 != ret && NULL != context) {
        pri_destroy_jni_context(env, context);
    }

    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    LOGI("init finished. ret=%d", ret);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_feedMsg(JNIEnv *env, jclass clazz,
                                                  jlong _handle, jint _what, jint _arg1, jint _arg2,
                                                  jbyteArray _obj, jint _obj_len) {
    (void) clazz;
    msg_q_code_e msg_push_status = MSG_Q_CODE_GENERIC_FAIL;
    msg_queue_jni_context_t *context = LONG64_TO_PTR(_handle);
    if (!context) {
        return -1;
    }
    if (context->flag_exit) {
        LOGE_TRACE("you can't feedMsg anymore, because now is going to destroy!");
        return -2;
    }

    jbyteArray j_obj_array = _obj;
    bool valid_msg = false;
    int obj_len = _obj_len;
    do {
        if (NULL == j_obj_array || 0 == obj_len) {
            valid_msg = true;
            break;
        }
#if 0
        jsize obj_capacity = (*env)->GetArrayLength(env, j_obj_array);
        if ((int)obj_capacity < obj_len) {
            abort();
            break;
        }
#endif
        if (obj_len < 1) {
            LOGE("invalid obj_len=%d", obj_len);
            break;
        }
        if (obj_len > (int) (context->msg.in_obj_capacity)) {
            RECREATE_MSG(context, in, ((size_t) obj_len * 2U));
        }
        valid_msg = true;
        jbyte *obj_bytes = (*env)->GetByteArrayElements(env, j_obj_array, NULL);
        memcpy(&(context->msg.in->obj[0]), obj_bytes, (size_t) obj_len);
        (*env)->ReleaseByteArrayElements(env, j_obj_array, obj_bytes, 0);
    } while (0);
    context->msg.in->what = _what;
    context->msg.in->arg1 = _arg1;
    context->msg.in->arg2 = _arg2;
    context->msg.in->obj_len = obj_len;

//        LOGV("--> try push");
#define MAX_PUSH_RETRY_COUNT 4U
    static uint32_t retry_count = 0;
    retry_count = 0;
    do {
        if (!valid_msg) {
            break;
        }
        if (MSG_Q_CODE_SUCCESS == (msg_push_status = msg_queue_handler_push(
                context->obj, context->msg.in))) {
            break;
        }
        if (MSG_Q_CODE_FULL != msg_push_status) {
            LOGE("failed(%d) on push msg", msg_push_status);
            break;
        }
        usleep(10U * 1000U); // <-- full, wait a moment and retry.
    } while (++retry_count < MAX_PUSH_RETRY_COUNT);
    if (MSG_Q_CODE_SUCCESS != msg_push_status && 0 != retry_count) {
        LOGE_TRACE("failed(%d) on push after retry_count:%u", msg_push_status, retry_count);
    }
//        LOGV("<-- push finished with status: %d", msg_push_status);

    return (jint) msg_push_status;
}


JNIEXPORT jint JNICALL
Java_com_threshold_jni_MsgQueueHandlerJni_destroy(JNIEnv *env, jclass clazz,
                                                  jlongArray _handle_holder) {
    (void) clazz;
    int ret;

    LOGD(" --> destroy in...");
    jlong *handle_holder = (*env)->GetLongArrayElements(env, _handle_holder, NULL);
    msg_queue_jni_context_t *context = LONG64_TO_PTR(handle_holder[0]);
    ret = pri_destroy_jni_context(env, context);
    handle_holder[0] = 0;
    (*env)->ReleaseLongArrayElements(env, _handle_holder, handle_holder, 0);
    LOGI(" <-- destroyed done! %d", (int) ret);
    return (jint) ret;
}

//==================================================================================================

static void pri_on_msg_q_handler_status_changed(msg_q_handler_status_e status, void *user_data) {
    LOGI_TRACE("hey, detected msg_queue_handler new status: %d", status);
    msg_queue_jni_context_t *context = (msg_queue_jni_context_t *) user_data;
    switch (status) {
        case MSG_Q_HANDLER_STATUS_READY_TO_GO: {
            // get env and attach to this thread once.
            LOGI("detected msg_queue_handler status: READY_TO_GO, now try attach it.");
            ASSERT_ABORT(NULL == context->cb_data.env);
            int env_status = (*(context->cb_data.jvm))->GetEnv(
                    context->cb_data.jvm, (void **) &(context->cb_data.env), JNI_VERSION_1_6);
            if (env_status < 0) {
                env_status = (*(context->cb_data.jvm))->AttachCurrentThread(
                        context->cb_data.jvm, &(context->cb_data.env), NULL);
                if (env_status < 0) {
                    LOGE_TRACE("oops: vm->AttachCurrentThread error(%d). ", env_status);
                    break;
                }
                context->cb_data.attached_thread = true;
                LOGI_TRACE("succeed on vm->AttachCurrentThread");
            }
        }
            break;
        case MSG_Q_HANDLER_STATUS_ABOUT_TO_STOP: {
            // detach this thread if necessary.
            LOGI("detected msg_queue_handler status: ABOUT_TO_STOP, now try detach it.");
            if (!context->cb_data.attached_thread) {
                break;
            }
            LOGI_TRACE(" now call vm->DetachCurrentThread");
            (*(context->cb_data.jvm))->DetachCurrentThread(context->cb_data.jvm);
            context->cb_data.env = NULL; // <-- env get it from jvm, now dereference it.
            context->cb_data.attached_thread = false;
        }
            break;
        default:
            LOGE_TRACE("unhandled status: %d", status);
            break;
    }
}

static int handle_queue_msg(queue_msg_t *msg_p, void *user_data) {
    msg_queue_jni_context_t *context = (msg_queue_jni_context_t *) user_data;

    if (context->flag_exit) {
        return 0; // return positive number will break the chain. and it's ok to do that.
    }
    // env should succeed get from "MSG_Q_HANDLER_STATUS_STARTED"
    if (NULL == context->cb_data.env) {
        return 1; // we have no options to handle this situation, so we break the chain.
    }

    jbyteArray msg_obj_array = NULL;
    do {
        if (msg_p->obj_len < 1) {
            break;
        }
        //msg_obj_array = (*env)->NewByteArray(env, (jsize) msg_p->obj_len);
        if (msg_p->obj_len > context->cache.obj_capacity) {
            pri_recreate_jobj_cache(context->cb_data.env, context,
                                    ((jsize) msg_p->obj_len * 2));
        }
        msg_obj_array = context->cache.j_obj;
        (*(context->cb_data.env))->SetByteArrayRegion(context->cb_data.env, msg_obj_array, 0,
                                                      (jsize) msg_p->obj_len, (jbyte *) msg_p->obj);
    } while (0);

    jint user_ret = (*(context->cb_data.env))->CallIntMethod(
            (context->cb_data.env), context->cb_data.m_obj, context->cb_data.m_method_id,
            msg_p->what, msg_p->arg1, msg_p->arg2,
            msg_obj_array, msg_p->obj_len);
    if (0 != user_ret) {
        LOGE_TRACE("user handle msg err:%d, queue_msg_handler is going to stop.", user_ret);
        context->flag_exit = true;
    }
    return (int) user_ret;
}

static void pri_cleanup_jobj_cache(JNIEnv *env, msg_queue_jni_context_t *context) {
    if (!context->cache.j_obj) {
        return;
    }
    (*env)->DeleteGlobalRef(env, context->cache.j_obj);
    context->cache.j_obj = NULL;
    context->cache.obj_capacity = 0;
}

static void pri_recreate_jobj_cache(JNIEnv *env,
                                    msg_queue_jni_context_t *context, jsize new_obj_capacity) {
    pri_cleanup_jobj_cache(env, context);
    if (new_obj_capacity < 1) {
        return;
    }
    LOGI("recreate new jobj.cache, capacity=%d", new_obj_capacity);
    context->cache.obj_capacity = new_obj_capacity;
    jbyteArray msg_obj_array = (*env)->NewByteArray(env, new_obj_capacity);
    context->cache.j_obj = (*env)->NewGlobalRef(env, msg_obj_array);
}

static int pri_destroy_jni_context(JNIEnv *env, msg_queue_jni_context_t *context) {
    int ret = -1;
    do {
        if (NULL == context) {
            break;
        }
        context->flag_exit = true;

        if (context->obj) {
            msg_queue_handler_destroy(&(context->obj));
        }
        CLEANUP_MSG(context, in);
        pri_cleanup_jobj_cache(env, context);
        if (context->cb_data.m_obj) {
            (*env)->DeleteGlobalRef(env, context->cb_data.m_obj);
            context->cb_data.m_obj = NULL;
        }
        context->cb_data.env = NULL;
        context->cb_data.jvm = NULL;
        ret = 0;
    } while (0);
    return ret;
}


