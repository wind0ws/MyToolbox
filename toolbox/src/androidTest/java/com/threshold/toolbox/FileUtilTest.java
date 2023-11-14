package com.threshold.toolbox;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FileUtilTest {

    @Test
    public void testFileIsExists(){
        Assert.assertFalse("failed ", FileUtil.isDirExists("/sdcard/abcd"));
        Assert.assertFalse("failed ", FileUtil.isDirExists("/sdcard/abcd.txt"));
        Assert.assertTrue("failed ", FileUtil.isDirExists("/sdcard"));
        Assert.assertTrue("failed ", FileUtil.isFileExists("/sdcard/xiritest.log"));
    }

}
