package com.threshold.jni;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import java.io.Closeable;

@SuppressWarnings("unused")
@LogTag("MsgQJni")
public class MsgQueueHandlerJni {

    static {
        System.loadLibrary("toolbox");
    }

    /**
     * Helper for operate with MsgQueueHandlerJni
     */
    @LogTag("MsgQJniHelper")
    public static class Helper implements Closeable {
        public static final int CODE_SUCCESS = 0;
        public static final int CODE_GENERAL_FAIL = 1;
        public static final int CODE_ERROR_FULL = 5;

        private final long[] mHandleHolder = new long[]{0};

        public Helper(final MsgQueueHandlerParam handlerParam) {
            int ret;
            if (0 != (ret = MsgQueueHandlerJni.init(mHandleHolder, handlerParam))) {
                throw new IllegalArgumentException("invalid MsgQueueHandlerParam for MsgQueueHandlerJni.init: " + ret);
            }
            SLog.i("succeed create MsgQueueHandlerJni(%d)", mHandleHolder[0]);
        }

        /**
         * push msg to queue.
         * <p>
         * If you call in multiple threads, you should use locks to protect this method.
         *
         * @param msg msg for push to queue.
         *            after pushMsg done, this msg is no need anymore,
         *            so you can reuse it, especially when you produce a large amount of msg.
         * @return code of MsgQueueHandlerJni.pushMsg. 0 for success, otherwise fail. see CODE_XX
         */
        public int pushMsg(final Message msg) {
            if (0 == mHandleHolder[0]) {
                throw new IllegalStateException("MsgQueueHandlerJni already destroyed, " +
                        "you can't pushMsg anymore. this msg should be discarded.");
            }
            if (null == msg) {
                throw new IllegalArgumentException("can't push null msg to queue");
            }
            if (null != msg.obj && msg.objLen > msg.obj.length) {
                throw new IllegalArgumentException(String.format(
                        "invalid msg.objLen(%d), because of it is bigger than obj.length(%d)",
                        msg.objLen, msg.obj.length));
            }
            if (null == msg.obj && 0 != msg.objLen) {
                throw new IllegalArgumentException(String.format(
                        "invalid msg.objLen(%d), because of null msg.obj",
                        msg.objLen));
            }
            return MsgQueueHandlerJni.pushMsg(mHandleHolder[0], msg.what,
                    msg.arg1, msg.arg2, msg.obj, msg.objLen);
        }

        @Override
        public void close() {
            if (0 == mHandleHolder[0]) {
                return;
            }
            final int ret = MsgQueueHandlerJni.destroy(mHandleHolder);
            mHandleHolder[0] = 0;
            if (0 != ret) {
                SLog.e("failed(%d) on destroy MsgQueueHandlerJni", ret);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            if (0 != mHandleHolder[0]) {
                SLog.e("you forgot to destroy MsgQueueHandlerJni(%d). " +
                        "now try destroy it for you", mHandleHolder[0]);
                close(); // <-- final option: for prevent mem leak
            }
            super.finalize();
        }

    }

    /**
     * for encapsulate msg
     */
    @Keep
    public static class Message {
        public int what;
        public int arg1;
        public int arg2;
        public byte[] obj = null;
        public int objLen;

        public Message(final int what, final int arg1, final int arg2,
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

        public Message(final int what, final int arg1, final int arg2) {
            this(what, arg1, arg2, 0);
        }

        @NonNull
        @Override
        public String toString() {
            return "Message{" +
                    "what=" + what +
                    ", arg1=" + arg1 +
                    ", arg2=" + arg2 +
//                    ", obj=" + Arrays.toString(obj) +
                    ", objLen=" + objLen +
                    '}';
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
    @LogTag("MsgQueueHandlerParam")
    public static class MsgQueueHandlerParam {
        // version that communicate with jni. DO NOT rename or obfuscure it!
        //   this should as same as JNI_PROTOCOL_VER in jni_msg_queue_handler.c
        private static final int JNI_PROTOCOL_VER = 0;

        private final int bufSize;
        private final String callbackFunction = "handleEvent";
        private final OnReceiveMsgListener listener;

        /**
         * create msg queue handler
         *
         * @param bufSize  msg queue total mem size.
         * @param listener handle each msg in queue.
         */
        public MsgQueueHandlerParam(final int bufSize, final OnReceiveMsgListener listener) {
            if (bufSize < 4096) {
                SLog.w("maybe your bufSize(%d) is too small, " +
                        "if you feed large msg, you may failed on feedMsg " +
                        "in first time, that is horrible. consider increase the bufSize.", bufSize);
            }
            if (null == listener) {
                throw new IllegalArgumentException("must provide valid listener!");
            }
            this.bufSize = bufSize;
            this.listener = listener;
        }

        // handle jni callback. DO NOT rename or obfuscure it!
        @Keep
        public int handleEvent(int what, int arg1, int arg2, byte[] obj, int objLen) {
//            SLog.i("received event. what=%d, arg1=%d, arg2=%d, obj_len=%d",
//                    what, arg1, arg2, objLen);
            int ret = 0;
            try {
                ret = listener.handleMsg(what, arg1, arg2, obj, objLen);
            } catch (Exception ex) {
                ret = -1;
                SLog.e(ex, "error on user.handleEvent(what=%d, arg1=%d, arg2=%d, objLen=%d)",
                        what, arg1, arg2, objLen);
            }
            return ret;
        }
    }

    //========================== MsgQueueHandler JNI start ==========================

    /**
     * init msg queue
     *
     * @param handleHolder handle holder
     * @param initParam    init params
     * @return 0 for succeed, otherwise fail
     */
    private static native int init(long[] handleHolder, MsgQueueHandlerParam initParam);

    /**
     * push msg to queue.
     * you should protect it. call this synchronized.
     *
     * @param handle handle
     * @param what   what
     * @param arg1   arg1
     * @param arg2   arg2
     * @param obj    obj
     * @param objLen length of obj
     * @return 0 for succeed, otherwise fail
     */
    private static native int pushMsg(long handle, int what,
                                      int arg1, int arg2, byte[] obj, int objLen);

    /**
     * destroy msg queue
     *
     * @param handleHolder handle holder
     * @return 0 for succeed, otherwise fail
     */
    private static native int destroy(long[] handleHolder);

    //========================== MsgQueueHandler JNI end ==========================

}
