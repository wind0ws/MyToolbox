package com.threshold.toolbox;

import androidx.annotation.Keep;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @noinspection CallToPrintStackTrace
 */
@SuppressWarnings("unused")
public class ByteUtil {

    private ByteUtil() {
        throw new IllegalStateException("no instance");
    }

    // Java 默认是大端字节序
    // C 和编译环境有关，通常是小端字节序
    // 这个是int转short的系数，乘以这个系数后的float强转成short即可以以16bit方式打开听。
    // 若是想以float形式打开听，需要归一化，也就是限制采样点范围到 -1 ~ 1， int需要乘2次这个系数
    private static final float INT2FLOAT_RATIO = 0.0000152587890625f;

    /**
     * 将int转成 大端模式 byte[]
     */
    public static byte[] intToBigEndianBytes(int value) {
        final byte[] out = new byte[4];
        intToBigEndianBytes(value, out, 0);
        return out;
    }

    public static void intToBigEndianBytes(int value, byte[] out, int outOffset) {
        out[outOffset] = (byte) ((value >> 24) & 0xFF);
        out[outOffset + 1] = (byte) ((value >> 16) & 0xFF);
        out[outOffset + 2] = (byte) ((value >> 8) & 0xFF);
        out[outOffset + 3] = (byte) (value & 0xFF);
    }

    /**
     * 将int转成 小端模式 byte[]
     */
    public static byte[] intToLittleEndianBytes(int value) {
        final byte[] out = new byte[4];
        intToLittleEndianBytes(value, out, 0);
        return out;
    }

    public static void intToLittleEndianBytes(int value, byte[] out, int outOffset) {
        out[outOffset + 3] = (byte) ((value >> 24) & 0xFF);
        out[outOffset + 2] = (byte) ((value >> 16) & 0xFF);
        out[outOffset + 1] = (byte) ((value >> 8) & 0xFF);
        out[outOffset] = (byte) (value & 0xFF);
    }

    public static byte[] shortToBigEndianBytes(short value) {
        final byte[] out = new byte[2];
        shortToBigEndianBytes(value, out, 0);
        return out;
    }

    public static void shortToBigEndianBytes(short value, byte[] out, int outOffset) {
        out[outOffset] = (byte) ((value >> 8) & 0xFF);
        out[outOffset + 1] = (byte) (value & 0xFF);
    }

    public static byte[] shortToLittleEndianBytes(short value) {
        final byte[] out = new byte[2];
        shortToLittleEndianBytes(value, out, 0);
        return out;
    }

    public static void shortToLittleEndianBytes(short value, byte[] out, int outOffset) {
        out[outOffset + 1] = (byte) ((value >> 8) & 0xFF);
        out[outOffset] = (byte) (value & 0xFF);
    }

    /**
     * 以大端模式将byte[]转成int
     */
    public static int bigEndianBytesToInt(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

    /**
     * 以小端模式将byte[]转成int
     */
    public static int littleEndianBytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    /**
     * 以大端模式将byte[]转成short
     */
    public static short bigEndianBytesToShort(byte[] src, int offset) {
        short value;
        value = (short) (((src[offset] & 0xFF) << 8)
                | (src[offset + 1] & 0xFF));
        return value;
    }

    /**
     * 以小端模式将byte[]转成short
     */
    public static short littleEndianBytesToShort(byte[] src, int offset) {
        short value;
        value = (short) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8));
        return value;
    }

    /**
     * 32bit signed int to 32bit short's float
     *
     * @param input  byte buffer
     * @param offset byte buffer start offset
     * @param length how many byte need to transform to short float
     */
    public static byte[] transformIntToShortFloat(byte[] input, int offset, int length) {
        final int inputSamples = length / 4; // 32 bit input,  4 bytes per sample
        final byte[] out = new byte[length];
//        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        try {
            for (int n = 0; n < inputSamples; n++) {
                //Tip: 原始音频是32bit signed int小尾端的，但Java默认是大端的，所以每4个字节转int的时候要注意下字节序，确保转出来的int不能错。
                //      另外32bit signed int小尾端转 32bit float 要乘以2次ratio系数，这样才能让每个采样点在0~1之间，这样生成的音频才能在Audacity中听.
                //      之所以我在这里只乘以一次系数，那是因为引擎那边在处理的时候会再次乘一次系数，所以嘛你懂的了吧。
                int sample = littleEndianBytesToInt(input, offset + n * 4);
                float f = sample * INT2FLOAT_RATIO;//* FLOAT_RATIO;
                byte[] bytes = float2bytes(f);
                System.arraycopy(bytes, 0, out, n * 4, bytes.length);
//                bos.write(float2bytes(f));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return out;
//        return bos.toByteArray();
    }

    /**
     * 32bit Short's Float To 32bit Signed int
     */
    public static byte[] transformShortFloatToInt(byte[] input, int offset, int length) {
        final int inputSamples = length / 4; // 32 bit input,  4 bytes per sample
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        try {
            for (int n = 0; n < inputSamples; n++) {
                float sample = fourBytes2float(input, offset + n * 4);
                //这里之所以除一次原因同上
                int intSample = (int) (sample / INT2FLOAT_RATIO);// / FLOAT_RATIO);
                bos.write(intToLittleEndianBytes(intSample));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bos.toByteArray();
    }

    //16bit to 32bit float.Here is reference: https://blog.csdn.net/kimmking/article/details/8752737
    public static float[] convertShortToFloat(byte[] input) {
        final int inputSamples = input.length / 2; // 16 bit input, so 2 bytes per sample
        final float[] output = new float[inputSamples];
        int outputIndex = 0;
        for (int n = 0; n < inputSamples; n++) {
            short sample = BitConverter.toInt16(input, n * 2);
            output[outputIndex++] = sample / 32768f;
        }
        return output;
    }

    //16bit to 32bit float.Here is reference: https://blog.csdn.net/kimmking/article/details/8752737
    public static void convertShortToFloat(byte[] input, byte[] output) {
        final int inputSamples = input.length / 2; // 16 bit input, so 2 bytes per sample
        //float[] output = new float[inputSamples];
        for (int n = 0; n < inputSamples; n++) {
            short sample = BitConverter.toInt16(input, n * 2);
            float fSample = sample / 32768f;
            byte[] fBytes = float2bytes(fSample);
            System.arraycopy(fBytes, 0, output, n * 4, 4);
        }
    }

    public static short twoByte2short(byte[] bytes, int offset) {
        final ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(bytes[offset]);
        bb.put(bytes[offset + 1]);
        bb.reset();
        return bb.getShort(0);
    }

    /**
     * 浮点转换为字节
     */
    public static byte[] float2bytes(float f) {
        // 把float转换为byte[]
        int floatBit = Float.floatToIntBits(f);

        byte[] dest = new byte[4];
        for (int i = 0; i < 4; i++) {
            dest[i] = (byte) (floatBit >> (24 - i * 8));
        }

        // 翻转数组
        int len = dest.length;

        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;
    }

    /**
     * 字节转换为浮点
     *
     * @param b     字节（至少4个字节）
     * @param index 开始位置
     */
    public static float fourBytes2float(byte[] b, int index) {
        int l;
        l = b[index];
        l &= 0xff;
        l |= (int) ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= (int) ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= (int) ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }


    /**
     * 32bit 双声道音频 转 32bit 单声道音频
     * Stereo --> Mono
     *
     * @param inBuffer  双声道音频
     * @param outBuffer 单声道音频
     */
    public static void transform32bitStereoTo32bitMono(final BufferWrapper inBuffer,
                                                       final BufferWrapper outBuffer) {
//        byte[] monoData = new byte[bufferLen / 2];
        int outIndex = 0;
        for (int i = 0; i < inBuffer.bufferUsed; i += (4 * 2)) {
            System.arraycopy(inBuffer.buffer, i, outBuffer.buffer, outIndex, 4);
            outIndex += 4;
        }
        outBuffer.bufferUsed = outIndex;
//        return monoData;
    }

    /**
     * 升采样点大小
     * 将 16bit 音频 转 32bit Signed int音频
     * 16bit --> 32bit
     * <p>输出Buffer应该是输入的2倍</p>
     *
     * @param inBuffer  16bit 音频
     * @param outBuffer 32bit 音频
     */
    public static void transform16bitTo32bitSigned(final BufferWrapper inBuffer,
                                                   final BufferWrapper outBuffer) {
//        byte[] outputs = new byte[bufferLen * 2];//16bit --> 32bit
        if (outBuffer.buffer.length < inBuffer.bufferUsed * 2) {
            throw new IllegalArgumentException("outBuffer is not enough.");
        }
        int outputIndex = 0;
        final int inputSamples = inBuffer.bufferUsed / 2; // 16 bit input, so 2 bytes per sample
        for (int n = 0; n < inputSamples; ++n) {
            final int sample = (BitConverter.toInt16(inBuffer.buffer, n * 2) << 16);
            intToLittleEndianBytes(sample, outBuffer.buffer, outputIndex);
            outputIndex += 4;
        }
        outBuffer.setBufferUsed(outputIndex);
//        return outputs;
    }


    /**
     * 降采样率
     * <p> 输入Buffer 是 输出Buffer 的 inSampleRate/outSampleRate 倍</p>
     *
     * @param inBuffer      输入音频 Buffer
     * @param inSampleRate  输入音频 采样率
     * @param inSampleBit   输入音频 采样大小
     * @param outSampleRate 输出音频 采样率
     * @param outBuffer     输出音频 Buffer
     */
    public static void resampleForDownRate(final BufferWrapper inBuffer, final int inChannels,
                                           final int inSampleRate, final int inSampleBit,
                                           final int outSampleRate, final BufferWrapper outBuffer) {
        final int sampleSize = inSampleBit / 8;
        //每次循环中取多少数据出来处理
        final int stepByteLen = (inSampleRate / outSampleRate) * sampleSize * inChannels;
        //一个采样点应该占用多少字节（包括所有声道）
        final int onePointByteLen = sampleSize * inChannels;
        int outBufferIndex = 0;
        for (int i = 0; i < inBuffer.bufferUsed; i += stepByteLen) {
            System.arraycopy(inBuffer.buffer, i, outBuffer.buffer, outBufferIndex, onePointByteLen);
            outBufferIndex += onePointByteLen;
        }
        outBuffer.bufferUsed = outBufferIndex;
    }


}
