package com.threshold.toolbox;

import org.junit.Assert;
import org.junit.Test;

public class FileOrDirectoryDeterminerTest {

    @Test
    public void runTest() {
        final FileUtil.FileOrDirectoryDeterminer determiner =
                new FileUtil.DefaultFileOrDirectoryDeterminer();
        Assert.assertTrue(determiner.isFile("123.mp3"));
        Assert.assertTrue(determiner.isFile("xxx.a"));
        Assert.assertTrue(determiner.isFile("yyy.1"));
        Assert.assertFalse(determiner.isFile(".dir"));
        Assert.assertFalse(determiner.isFile("ab"));
        Assert.assertFalse(determiner.isFile("a.apk.1"));
        System.out.print("all done!\n");
    }

}
