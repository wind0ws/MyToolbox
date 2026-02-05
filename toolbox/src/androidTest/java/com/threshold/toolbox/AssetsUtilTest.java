package com.threshold.toolbox;

import android.Manifest;
import android.content.Context;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import android.os.SystemClock;
import android.util.Log;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

@LogTag("AssetsTest")
@RunWith(AndroidJUnit4.class)
public class AssetsUtilTest {

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void testCopyAssets() throws IOException {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final File cacheDir = appContext.getExternalCacheDir();
        Assert.assertNotNull(cacheDir);

        SLog.d("--> Start copy.");

        boolean succeed = AssetsUtil.copyAssetFile(appContext, AssetsUtil.CopyMode.OVERRIDE_IF_MODIFIED,
                "bundles/sub_dir/test.txt", cacheDir.getAbsolutePath() + "/my_folder/test.txt");
        Assert.assertTrue(succeed);
        FileUtil.deleteFile(new File(cacheDir.getAbsolutePath() + "/my_folder/test.txt"));

        succeed = AssetsUtil.copyAssetFolder(appContext, AssetsUtil.CopyMode.OVERRIDE_IF_MODIFIED,
                "bundles", cacheDir.getAbsolutePath() +"/my_folder/assets_test/");
        Assert.assertTrue(succeed);

        final byte[] copiedTxtContent = FileUtil.readFileContent(new File(cacheDir.getAbsolutePath() + "/my_folder/assets_test/sub_dir/test.txt"));
        final byte[] assetsTxtContent = AssetsUtil.read(appContext.getAssets(), "bundles/sub_dir/test.txt");
        Assert.assertArrayEquals(copiedTxtContent, assetsTxtContent);

        SLog.d("<-- Copy complete.");
    }

}
