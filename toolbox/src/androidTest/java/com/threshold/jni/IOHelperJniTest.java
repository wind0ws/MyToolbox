package com.threshold.jni;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LogTag("IOHelperJniTest")
@SuppressWarnings({"SpellCheckingInspection"})
public class IOHelperJniTest {

    @BeforeClass
    public static void setup() {
        SLog.d("setup, hi..");
    }

    @Test
    public void test() {
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        String currentNativeWorkDir = IOHelperJni.nativeGetCwd();
        SLog.d("before chdir, currentNativeWorkDir: " + currentNativeWorkDir);

        final String appFilesPath = appContext.getFilesDir().getPath();
        int ret = IOHelperJni.nativeChdir(appFilesPath);

        currentNativeWorkDir = IOHelperJni.nativeGetCwd();
        SLog.d("after chdir, currentNativeWorkDir: " + currentNativeWorkDir);

        Assert.assertEquals(ret, 0);
        SLog.i("~~~ all done ~~~");
    }

    @AfterClass
    public static void tearDown() {
        SLog.d("tearDown, bye bye..");
    }
}
