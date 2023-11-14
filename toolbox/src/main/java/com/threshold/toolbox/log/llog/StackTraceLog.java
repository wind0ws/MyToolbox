package com.threshold.toolbox.log.llog;

interface StackTraceLog {

    void dWithTag(String tag, String message, Object... args);

    void vWithTag(String tag, String message, Object... args);

    void aWithTag(String tag, String message, Object... args);

    void iWithTag(String tag, String message, Object... args);

    void eWithTag(String tag, String message, Object... args);

    void wWithTag(String tag, String message, Object... args);

    void jsonWithTag(String tag, String json);

    void objWithTag(String tag, Object obj);

    void d(StackTraceElement element, String message, Object... args);

    void v(StackTraceElement element, String message, Object... args);

    void a(StackTraceElement element, String message, Object... args);

    void i(StackTraceElement element, String message, Object... args);

    void e(StackTraceElement element, String message, Object... args);

    void w(StackTraceElement element, String message, Object... args);

    void json(StackTraceElement element, String json);

    void obj(StackTraceElement element, Object obj);
}