package com.threshold.toolbox;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AssetsUtilTest {

    private static final String TAG = AssetsUtilTest.class.getSimpleName();

    @Test
    public void testCopyAssets() throws IOException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        Log.d(TAG, "Start copy.");
//        final String[] cae_bundles = appContext.getAssets().list("cae_bundles");
//        Log.d(TAG,"cae_bundles =" + cae_bundles.length);
//        org.junit.Assert.assertTrue("cae_bundles length == 0",cae_bundles.length > 0);

        AssetsUtil.copyAssetFolder(appContext.getAssets(), "bundles",
                "/sdcard/asset_test/",
                new FileUtil.FileOrDirectoryDeterminer() {
                    @Override
                    public boolean isFile(final String name) {
                        return name.endsWith(".so");
                    }
                });
//        AssetsUtil.copyAssetFolder(appContext.getAssets(), "testfolder", "/sdcard/test/");
//        AssetsUtil.copyAssetFile(appContext.getAssets(), "testfolder/god.reg", "/sdcard/test/god22.reg");
//        AssetsUtil.copyAssetFile(appContext.getAssets(), "cfg/optm.cfg", "/sdcard/test/optm.cfg");
        Log.d(TAG, "Copy complete.");
    }

}
