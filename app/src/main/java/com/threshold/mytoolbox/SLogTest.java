package com.threshold.mytoolbox;

import com.threshold.toolbox.log.ILog;
import com.threshold.toolbox.log.LoggerFactory;
import com.threshold.toolbox.log.LogTag;

import java.util.Arrays;

@LogTag("SLogTest")
public class SLogTest {

//    private static final String TAG = "SLogTest";

    public static final ILog sLog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_WITH_TRACE, null);

    public static void test() {
        sLog.d("Hello %s", "word");
        sLog.json("{\"key2\":\"value2\"}");
        sLog.v("ManualTag", null, "This is manual tag");
        sLog.obj(Arrays.asList("called", "log", "object"));
        final StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTraces.length; i++) {
            sLog.v("stack[%d]=>%s", i, stackTraces[i]);
        }
    }
}
