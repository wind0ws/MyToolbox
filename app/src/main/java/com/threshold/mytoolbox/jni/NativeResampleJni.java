package com.threshold.mytoolbox.jni;

public class NativeResampleJni {

    static{
        System.loadLibrary("native-resample");
    }

    public static native int init();

//    public static native byte[] resampleDownRate(byte[] inBuffer,int inBufferLen,int inRate,int inBit,int outRate);

    public static native byte[] resample(byte[] inBuffer, int inBufferLen,int inChannels, int inRate,
                                         int outRate, int outChannels, int outBit);

    public static native int deinit();
}
