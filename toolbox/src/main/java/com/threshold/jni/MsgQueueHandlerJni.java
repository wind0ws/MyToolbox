package com.threshold.jni;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

@LogTag("MsgQJni")
public class MsgQueueHandlerJni {

    static {
        System.loadLibrary("toolbox");
    }

    /**
     * Helper for operate with MsgQueueHandlerJni
     */
    public static class Helper {
        public static final int CODE_SUCCESS = 0;
        public static final int CODE_GENERAL_FAIL = 1;
        public static final int CODE_ERROR_FULL = 5;

        private final long[] mHandleHolder = new long[]{0};

        public Helper(final MsgQueueHandlerParam handlerParam) {
            int ret;
            if (0 != (ret = MsgQueueHandlerJni.init(mHandleHolder, handlerParam))) {
                throw new IllegalArgumentException("invalid param of MsgQueueHandlerJni.init: " + ret);
            }
            SLog.i("succeed create MsgQueueHandlerJni(%d)", mHandleHolder[0]);
        }

        /**
         * feed msg to queue.
         * <p>
         * If you call in multiple threads, you should use locks to protect this method.
         *
         * @param queueData data for feed to queue.
         *                  after feedMsg done, this queueData is no need anymore,
         *                  so you can reuse it, especially when you produce a large amount of msg.
         * @return code of MsgQueueHandlerJni.feedMsg. 0 for success, otherwise fail. see CODE_XX
         */
        public int feedMsg(MsgQueueData queueData) {
            if (0 == mHandleHolder[0]) {
                throw new IllegalStateException("MsgQueueHandlerJni already destroyed, " +
                        "you can't feedMsg anymore. this obj should be discarded.");
            }
            if (null == queueData) {
                throw new IllegalArgumentException("can't feed null data to queue");
            }
            if (null != queueData.obj && queueData.objLen > queueData.obj.length) {
                throw new IllegalArgumentException(String.format(
                        "invalid queueData.objLen(%d), because of it is bigger than obj.length(%d)",
                        queueData.objLen, queueData.obj.length));
            }
            if (null == queueData.obj && 0 != queueData.objLen) {
                throw new IllegalArgumentException(String.format(
                        "invalid queueData.objLen(%d), because of null queueData.obj",
                        queueData.objLen));
            }
            return MsgQueueHandlerJni.feedMsg(mHandleHolder[0], queueData.what,
                    queueData.arg1, queueData.arg2, queueData.obj, queueData.objLen);
        }

        /**
         * destroy MsgQueueHandlerJni
         *
         * @return 0 for success, otherwise fail.
         */
        public int destroy() {
            if (0 == mHandleHolder[0]) {
                return 0;
            }
            int ret = MsgQueueHandlerJni.destroy(mHandleHolder);
            mHandleHolder[0] = 0;
            return ret;
        }

        @Override
        protected void finalize() throws Throwable {
            if (0 != mHandleHolder[0]) {
                SLog.e("you forgot to destroy MsgQueueHandlerJni(%d). " +
                        "now try destroy it for you", mHandleHolder[0]);
                destroy(); // <-- final option: for prevent mem leak
            }
            super.finalize();
        }
    }


    /**
     * for encapsulate msg data
     */
    public static class MsgQueueData {
        public int what;
        public int arg1;
        public int arg2;
        public byte[] obj = null;
        public int objLen;

        public MsgQueueData(final int what, final int arg1, final int arg2,
                            final int objLen) {
            this.what = what;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.objLen = objLen;
            if (this.objLen < 0) {
                throw new IllegalArgumentException("must provide valid objLen");
            }
            if (this.objLen > 0) {
                this.obj = new byte[this.objLen];
            }
        }

        public MsgQueueData(final int what, final int arg1, final int arg2) {
            this(what, arg1, arg2, 0);
        }
    }

    /**
     * receive msg of queue.
     */
    @Keep
    public interface OnReceiveMsgListener {

        /**
         * handle each received msg.
         * <p>
         * notice: The msg obj is reusable, so DO NOT run around the world with it.
         * If necessary, please copy it by yourself.
         */
        int handleMsg(int what, int arg1, int arg2, @Nullable byte[] obj, int objLen);

    }

    @Keep
    @SuppressWarnings("all")
    public static class MsgQueueHandlerParam {
        // version that communicate with java jni class.
        // this should as same as JNI_PROTOCOL_VER in jni_msg_queue_handler.c
        private static final int JNI_PROTOCOL_VER = 0;

        private int protocolVer = JNI_PROTOCOL_VER;
        private int bufSize = 8192;
        private final String callbackFunction = "handleEvent";
        private final OnReceiveMsgListener listener;

        /**
         * create msg queue handler
         *
         * @param bufSize  msg queue total mem size.
         * @param listener handle each msg in queue.
         */
        public MsgQueueHandlerParam(final int bufSize, OnReceiveMsgListener listener) {
            if (bufSize < 4096) {
                SLog.w("maybe bufSize(%d) is too small, " +
                        "if you feed large msg, maybe you will failed of feedMsg " +
                        "on first time, that is horrible. consider increase the bufSize.", bufSize);
            }
            if (null == listener) {
                throw new IllegalArgumentException("must provide valid listener!");
            }
            this.bufSize = bufSize;
            this.listener = listener;
        }

        // handle jni callback. do NOT delete or obfuscure it!
        @Keep
        public int handleEvent(int what, int arg1, int arg2, byte[] obj, int objLen) {
//            SLog.i("received event. what=%d, arg1=%d, arg2=%d, obj_len=%d",
//                    what, arg1, arg2, objLen);
            return listener.handleMsg(what, arg1, arg2, obj, objLen);
        }
    }


    /**
     * init msg queue
     *
     * @param handleHolder handle holder
     * @param initParam    init params
     * @return 0 for succeed, otherwise fail
     */
    private static native int init(long[] handleHolder, MsgQueueHandlerParam initParam);

    /**
     * feed msg to queue
     *
     * @param handle handle
     * @param what   what
     * @param arg1   arg1
     * @param arg2   arg2
     * @param obj    obj
     * @param objLen length of obj
     * @return 0 for succeed, otherwise fail
     */
    private static native int feedMsg(long handle, int what,
                                      int arg1, int arg2, byte[] obj, int objLen);

    /**
     * destroy msg queue
     *
     * @param handleHolder handle holder
     * @return 0 for succeed, otherwise fail
     */
    private static native int destroy(long[] handleHolder);

}
