package com.threshold.toolbox.log;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@LogTag("LogTest")
public class LogTest {

    private static class StackTraceLoggerHolder4Test {

        public static final ILog sLog;

        static {
            sLog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_WITH_TRACE,
                    null, 0,
                    new SimpleDiskPrinter(new File("/sdcard/log_test.log"),
                            0, new LogcatPrinter(), true));
//            sLog = new TracerLogger();
        }

    }

    private static final ILog sLog = StackTraceLoggerHolder4Test.sLog;

    @BeforeClass
    public static void setupClz() {
//        sLog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_DEFAULT, LogTest.class.getSimpleName());
        sLog.d("before clz");
        new Thread(new Runnable() {
            @Override
            public void run() {
                sLog.i("log in another thread:%d", Thread.currentThread().getId());
            }
        }).start();
    }

    @Before
    public void setup() {
        sLog.i("call setup. %s", "abcd");
    }

    @Test
    public void testLog() {
        sLog.i("Hello %s", "World");
        sLog.d("ManualLogTag", null, "Hello Manual log tag");
        sLog.obj(Arrays.asList("called", "log", "obj"));
        sLog.json("{\"key\":\"value\",\"key2\":\"value2\"}");
        SystemClock.sleep(1000);
    }

    @After
    public void cleanup() {
        sLog.w("now called cleanup");
    }

    @AfterClass
    public static void cleanupClz() {
        sLog.e("now called cleanupClz");
    }


}
