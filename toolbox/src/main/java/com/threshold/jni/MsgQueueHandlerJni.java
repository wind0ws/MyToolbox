package com.threshold.jni;

import android.support.annotation.Keep;
import android.util.Log;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.llog.LLog;

@LogTag("MsgQJni")
public class MsgQueueHandlerJni {

    // version that communicate with java jni class.
    // this should as same as JNI_PROTOCOL_VER in jni_msg_queue_handler.c
    private static final int JNI_PROTOCOL_VER = 0;

    static {
        System.loadLibrary("toolbox");
    }

    /**
     * for encapsulate msg callback data
     */
    public static class MsgQueueData {
        public int what;
        public int arg1;
        public int arg2;
        public byte[] obj;
    }

    /**
     * receive msg of queue.
     */
    @Keep
    public interface OnReceiveMsgListener {

        /**
         * The msg object is reusable. so DO NOT run around the world with it.
         * If necessary, please copy it by yourself.
         */
        int handleMsg(int what, int arg1, int arg2, byte[] obj, int objLen);

    }

    @Keep
    @SuppressWarnings("all")
    public static class MsgQueueHandlerParam {
        private int protocolVer = JNI_PROTOCOL_VER;
        private int bufSize = 8192;
        private final String callbackFunction = "handleEvent";
        private final OnReceiveMsgListener listener;

        /** create msg queue handler
         * @param bufSize msg queue total mem size
         * @param listener handle msg queue.
         */
        public MsgQueueHandlerParam(final int bufSize, OnReceiveMsgListener listener) {
            if (bufSize < 4096) {
                LLog.w("maybe bufSize(%d) is too small, " +
                        "if you feed large msg, maybe you will stuck at feedMsg method " +
                        "on first time, that is horrible", bufSize);
            }
            this.bufSize = bufSize;
            this.listener = listener;
        }

        // handle jni callback. do NOT delete or obfuscure it!
        @Keep
        public int handleEvent(int what, int arg1, int arg2, byte[] obj, int objLen) {
//            LLog.i("received event. what=%d, arg1=%d, arg2=%d, obj_len=%d",
//                    what, arg1, arg2, objLen);
            return listener.handleMsg(what, arg1, arg2, obj, objLen);
        }
    }


    /**
     * init msg queue
     *
     * @param handle_holder handle holder
     * @param initParam     init params
     * @return 0 for succeed, otherwise fail
     */
    public static native int init(long[] handle_holder, MsgQueueHandlerParam initParam);

    /**
     * feed msg to queue
     *
     * @param handle handle
     * @param what   what
     * @param arg1   arg1
     * @param arg2   arg2
     * @param obj    obj
     * @return 0 for succeed, otherwise fail
     */
    public static native int feedMsg(long handle, int what,
                                     int arg1, int arg2, byte[] obj, int objLen);

    /**
     * destroy msg queue
     *
     * @param handle_holder handle holder
     * @return 0 for succeed, otherwise fail
     */
    public static native int destroy(long[] handle_holder);

}
