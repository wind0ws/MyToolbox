package com.threshold.jni;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class IOHelperJni {

    static {
        System.loadLibrary("toolbox");
    }

    /**
     * get native current work dir.
     * @return native work dir
     */
    public static native String nativeGetCwd();

    /**
     * change native work dir.
     * @param dirPath target work dir, must accessible
     * @return 0 for success, otherwise fail
     */
    public static native int nativeChdir(String dirPath);

}
