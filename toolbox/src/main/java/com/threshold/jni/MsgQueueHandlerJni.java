package com.threshold.jni;

import android.support.annotation.Keep;

import com.threshold.toolbox.log.llog.LLog;

public class MsgQueueHandlerJni {

    // version that communicate with java jni class.
    // this should as same as JNI_PROTOCOL_VER in jni_msg_queue_handler.c
    private static final int JNI_PROTOCOL_VER = 0;

    static {
        System.loadLibrary("toolbox");
    }

    public static class MsgQueueData {
        public int what;
        public int arg1;
        public int arg2;
        public byte[] obj;
    }

    @Keep
    @SuppressWarnings("all")
    public static class MsgQueueHandlerParam {
        private int protocolVer = JNI_PROTOCOL_VER;
        private int bufSize = 8192;
        private String callbackFunction = "handleEvent";

        public MsgQueueHandlerParam() {
        }

        public MsgQueueHandlerParam(final int bufSize,
                                    final String callbackFunction) {
            this.bufSize = bufSize;
            this.callbackFunction = callbackFunction;
        }

        // handle jni callback. do dot delete or obfuscure it!
        @Keep
        public int handleEvent(int what, int arg1, int arg2, byte[] obj) {
            LLog.i("received event. what=%d, arg1=%d, arg2=%d, obj_len=%d",
                    what, arg1, arg2, null == obj ? 0 : obj.length);
            return 0;
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
    public static native int feedMsg(long handle, int what, int arg1, int arg2, byte[] obj);

    /**
     * destroy msg queue
     *
     * @param handle_holder handle holder
     * @return 0 for succeed, otherwise fail
     */
    public static native int destroy(long[] handle_holder);

}
