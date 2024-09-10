package com.threshold.jni;

import android.content.res.AssetManager;

public class AssetsJni {

    static {
        System.loadLibrary("toolbox");
    }

    /**
     * get assets resource file size
     * @param assetManager  see {@link AssetManager}
     * @param resPath       file path in assets. eg: "bundles/sub_dir/test.txt"
     * @param resSizeHolder receive resource file size.
     *                      eg: final long[] resSizeHolder = new resSizeHolder[1];
     * @return status of native call. 0 for succeed, otherwise fail(maybe file not exists).
     */
    public static native int getResSize(AssetManager assetManager,
                                        String resPath, long[] resSizeHolder);

}
