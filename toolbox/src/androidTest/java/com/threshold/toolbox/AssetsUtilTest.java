package com.threshold.toolbox;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AssetsUtilTest {

    private static final String TAG = "AssetsUtilTest";

    @Test
    public void testCopyAssets() throws IOException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        Log.d(TAG, "Start copy.");
//        final String[] cae_bundles = appContext.getAssets().list("cae_bundles");
//        Log.d(TAG,"cae_bundles =" + cae_bundles.length);
//        org.junit.Assert.assertTrue("cae_bundles length == 0",cae_bundles.length > 0);

        boolean succeed = AssetsUtil.copyAssetFolder(appContext.getAssets(),
                "bundles",
                "/sdcard/asset_test/");
        Assert.assertTrue(succeed);
//        AssetsUtil.copyAssetFile(appContext.getAssets(), "cfg/optm.cfg", "/sdcard/test/optm.cfg");
        Log.d(TAG, "Copy complete.");
    }

}
