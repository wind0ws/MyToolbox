package com.threshold.toolbox;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ByteUtilTest {

    @Test
    public void testByteNum(){
        String str= "你好";
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        System.out.println("bytes.len="+bytes.length);
    }

    /**
     * 32bit双声道转单声道
     */
    @Test
    public void test16k32bitStereoToMono() throws IOException {
        //32bit --> 32/8 = 4byte.
        //第一个4byte是第一个声道，第二个4byte是第二个声道，如此交替
        InputStream inputStream = new BufferedInputStream(new FileInputStream("E:/TMP/6麦音频/ref_0.pcm"));
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/6麦音频/mono16k32bit.pcm"));
//        byte[] buffer = new byte[4 * 2 * 200];
        ByteUtil.BufferWrapper inBuffer = new ByteUtil.BufferWrapper(4 * 2 * 200);
        ByteUtil.BufferWrapper outBuffer = new ByteUtil.BufferWrapper(2 * 2 * 200);
        int readCount;
        while ((readCount = inputStream.read(inBuffer.getBuffer())) > 0) {
            inBuffer.setBufferUsed(readCount);
            ByteUtil.transform32bitStereoTo32bitMono(inBuffer, outBuffer);
            outputStream.write(outBuffer.getBuffer(), 0, outBuffer.getBufferUsed());
        }
        outputStream.close();
        inputStream.close();
        System.out.println("16k 32bit stereo 转换 16k 32bit mono 结束...");
    }

    /**
     * 16k 16bit mono 转 16k 32bit mono
     */
    @Test
    public void test16k16bitMonoTo16k32bitMono() throws IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream("E:/TMP/16k16bit/20181010 110312.pcm"));
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/16k16bit/mono16k32bit.pcm"));
//        byte[] buffer = new byte[2 * 1024];
        ByteUtil.BufferWrapper inBuffer = new ByteUtil.BufferWrapper(2 * 1024);
        ByteUtil.BufferWrapper outBuffer = new ByteUtil.BufferWrapper(4 * 1024);
        int readCount;
        while ((readCount = inputStream.read(inBuffer.getBuffer())) > 0) {
            inBuffer.setBufferUsed(readCount);
            ByteUtil.transform16bitTo32bitSigned(inBuffer, outBuffer);
            outputStream.write(outBuffer.getBuffer(), 0, outBuffer.getBufferUsed());
        }
        outputStream.close();
        inputStream.close();
        System.out.println("16bit 转换 32bit 结束...");
    }

    /**
     * 32bit signed音频左移n位
     */
    @Test
    public void testShift32bitSigned() throws IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream("E:/TMP/文曲星/stereo_ref0.pcm"));
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/文曲星/test_ref0.pcm"));
        byte[] buffer = new byte[4 * 1024];
        int readCount = 0;
        while ((readCount = inputStream.read(buffer)) > 0) {
            for (int i = 0; i < readCount; i += 4) {
                final int shiftInt = ByteUtil.littleEndianBytesToInt(buffer, i) << 4;
                byte[] tmpData = ByteUtil.intToLittleEndianBytes(shiftInt);
                outputStream.write(tmpData);
            }
        }
        outputStream.close();
        inputStream.close();
        System.out.println(" 转换结束...");
    }

    /**
     * 降采样率
     */
    @Test
    public void testDownRate() throws IOException {
        final int inRate = 48000;//48000
        final int outRate = 16000;//16000
        final int sampleBit = 32;//32bit
        final int channels = 8;//8 channel

//        final int sampleSize = sampleBit / 8;
//        final int skipByteLen = sampleSize * (inRate / outRate) * channels;
        InputStream inputStream = new BufferedInputStream(new FileInputStream("E:/TMP/48k32bit/sample_ai.pcm"), 128 * 1024);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/48k32bit/test16k32bit.pcm"), 128 * 1024);
        ByteUtil.BufferWrapper inBuffer = new ByteUtil.BufferWrapper(8 * 4 * 512);
        ByteUtil.BufferWrapper outBuffer = new ByteUtil.BufferWrapper(8 * 4 * 512);
//        byte[] buffer = new byte[8 * 4 * 1024];
        int readCount;
        while ((readCount = inputStream.read(inBuffer.getBuffer())) > 0) {
            inBuffer.setBufferUsed(readCount);
            ByteUtil.resampleForDownRate(inBuffer, channels, inRate, sampleBit, outRate, outBuffer);
            outputStream.write(outBuffer.getBuffer(), 0, outBuffer.getBufferUsed());
//            for (int i = 0; i < readCount; i += 4*8*3) {
//                outputStream.write(buffer, i, 4*8);
//            }
        }

        outputStream.close();
        inputStream.close();
        System.out.println(" 转换结束...");
    }

    @Test
    public void testSplitHisiAudio() throws IOException {
        final int inRate = 48000;//48000
        final int outRate = 16000;//16000
        final int sampleBit = 32;//32bit
        final int channels = 8;//8 channel

//        final int sampleSize = sampleBit / 8;
//        final int skipByteLen = sampleSize * (inRate / outRate) * channels;
        InputStream inputStream = new BufferedInputStream(new FileInputStream("E:/TMP/48k32bit/sample_ai.pcm"), 128 * 1024);

        OutputStream micOutputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/48k32bit/testMic16k32bit.pcm"), 128 * 1024);
        OutputStream refOutputStream = new BufferedOutputStream(new FileOutputStream("E:/TMP/48k32bit/testRef16k32bit.pcm"), 128 * 1024);

        ByteUtil.BufferWrapper inBuffer = new ByteUtil.BufferWrapper(8 * 4 * 512);
        ByteUtil.BufferWrapper outBuffer = new ByteUtil.BufferWrapper(8 * 4 * 512);
//        byte[] buffer = new byte[8 * 4 * 1024];
        int readCount;
        while ((readCount = inputStream.read(inBuffer.getBuffer())) > 0) {
            inBuffer.setBufferUsed(readCount);
            ByteUtil.resampleForDownRate(inBuffer, channels, inRate, sampleBit, outRate, outBuffer);

            final byte[] buffer = outBuffer.getBuffer();
            final int bufferLen = outBuffer.getBufferUsed();
            final int num = bufferLen / (8 * 4);
            final byte[] micData = new byte[4 * 4 * num];//4路麦克风
            final byte[] refData = new byte[2 * 4 * num];//2路参考
            int micIndex = 0, refIndex = 0;
            for (int i = 0; i < bufferLen; i += 4 * 8) {
                // N N m1 m0 m3 m2 e0 e1
                //mic0
                System.arraycopy(buffer, i + 4 * 3, micData, micIndex, 4);
                //mic1
                System.arraycopy(buffer, i + 4 * 2, micData, micIndex + 4, 4);
                //mic2
                System.arraycopy(buffer, i + 4 * 5, micData, micIndex + 4 * 2, 4);
                //mic3
                System.arraycopy(buffer, i + 4 * 4, micData, micIndex + 4 * 3, 4);

                //ref0
                System.arraycopy(buffer, i + 4 * 6, refData, refIndex, 4);
                //ref1
                System.arraycopy(buffer, i + 4 * 7, refData, refIndex + 4, 4);

                micIndex += 4 * 4;
                refIndex += 2 * 4;
            }

            micOutputStream.write(micData);
            refOutputStream.write(refData);
        }

        micOutputStream.close();
        inputStream.close();
        System.out.println(" 转换结束...");
    }


    @Test
    public void test(){
        for (int i = 1; i < 9; i++) {
            int num = 0x1 << i;
            System.out.println("0x1 << "+i+"  => "+num);
        }
    }


}
