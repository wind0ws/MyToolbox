package com.threshold.jni;

import android.content.Context;
import android.content.res.AssetManager;

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
@LogTag("AssetsJniTest")
public class AssetsJniTest {

    @BeforeClass
    public static void setup() {
        SLog.init();
        SLog.d("setup, hi..");
    }

    @Test
    public void test() {
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final AssetManager assetManager = appContext.getAssets();
        final long[] resSizeHolder = new long[1];
        int ret;
        if (0 != (ret = AssetsJni.getResSize(assetManager, "bundles/sub_dir/test.txt", resSizeHolder))) {
            SLog.e("failed(%d) on get asset size", ret);
        }
        Assert.assertEquals(ret, 0);
        SLog.i("succeed get asset size: %d", resSizeHolder[0]);
    }


    @AfterClass
    public static void tearDown() {
        SLog.d("tearDown, bye bye..");
    }
}
