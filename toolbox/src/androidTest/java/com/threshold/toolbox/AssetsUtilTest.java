package com.threshold.toolbox;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@LogTag("AssetsTest")
@RunWith(AndroidJUnit4.class)
public class AssetsUtilTest {

    @Test
    public void testCopyAssets() throws IOException {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SLog.d("Start copy.");
//        final String[] cae_bundles = appContext.getAssets().list("cae_bundles");
//        Log.d(TAG,"cae_bundles =" + cae_bundles.length);
//        org.junit.Assert.assertTrue("cae_bundles length == 0",cae_bundles.length > 0);

        boolean succeed = AssetsUtil.copyAssetFolder(appContext.getAssets(),
                "bundles",
                "/sdcard/asset_test/");
        Assert.assertTrue(succeed);
//        AssetsUtil.copyAssetFile(appContext.getAssets(), "cfg/optm.cfg", "/sdcard/test/optm.cfg");
        SLog.d("Copy complete.");
    }

}
