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
        SLog.d("--> Start copy.");

        boolean succeed = AssetsUtil.copyAssetFile(appContext, AssetsUtil.CopyMode.OVERRIDE_IF_MODIFIED,
                "bundles/sub_dir/test.txt",
                "/sdcard/Download/test.txt");
        succeed &= AssetsUtil.copyAssetFolder(appContext,AssetsUtil.CopyMode.OVERRIDE_IF_MODIFIED,
                "bundles",
                "/sdcard/Download/asset_test/");
        Assert.assertTrue(succeed);

        SLog.d("<-- Copy complete.");
    }

}
