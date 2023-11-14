package com.threshold.toolbox.log.llog;

import com.threshold.toolbox.log.TraceUtil;

public class LLog {

    public static class Config{
        /**
         * 是否允许输出log
         */
        public static boolean allowPrint = true;

        /**
         * 是否强制以LogLevel.error模式输出
         */
        public static boolean forceLevelE = false;
    }


    private static final StackTraceLog STACK_TRACE_LOG;

    static {
        STACK_TRACE_LOG = new StackTraceLogger(new TraceLoggerDefaultPrinter());
    }

    public static void dWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.dWithTag(tag, message, args);
    }

    public static void eWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.eWithTag(tag, message, args);
    }

    public static void iWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.iWithTag(tag, message, args);
    }

    public static void wWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.wWithTag(tag, message, args);
    }

    public static void vWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.vWithTag(tag, message, args);
    }

    public static void aWithTag(String tag, String message, Object... args) {
        STACK_TRACE_LOG.aWithTag(tag, message, args);
    }

    public static void objWithTag(String tag, Object obj) {
        STACK_TRACE_LOG.objWithTag(tag, obj);
    }

    public static void jsonWithTag(String tag, String json) {
        STACK_TRACE_LOG.jsonWithTag(tag, json);
    }

    public static void d(String message, Object... args) {
        STACK_TRACE_LOG.d(TraceUtil.getStackTrace(), message, args);
    }

    public static void e(String message, Object... args) {
        STACK_TRACE_LOG.e(TraceUtil.getStackTrace(), message, args);
    }

    public static void i(String message, Object... args) {
        STACK_TRACE_LOG.i(TraceUtil.getStackTrace(), message, args);
    }

    public static void a(String message, Object... args) {
        STACK_TRACE_LOG.a(TraceUtil.getStackTrace(), message, args);
    }

    public static void w(String message, Object... args) {
        STACK_TRACE_LOG.w(TraceUtil.getStackTrace(), message, args);
    }

    public static void v(String message, Object... args) {
        STACK_TRACE_LOG.v(TraceUtil.getStackTrace(), message, args);
    }

    /**
     * 打印json
     */
    public static void json(String json) {
        STACK_TRACE_LOG.json(TraceUtil.getStackTrace(), json);
    }

    /**
     * 打印对象(支持Collection,Map)
     */
    public static void obj(Object obj) {
        STACK_TRACE_LOG.obj(TraceUtil.getStackTrace(), obj);
    }


}