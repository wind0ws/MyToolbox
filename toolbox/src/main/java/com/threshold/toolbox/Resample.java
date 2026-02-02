package com.threshold.toolbox;

/**
 * 采样率转换工具类
 */
public class Resample {

    /**
     * 采样点数据类型枚举
     */
    public enum SampleUnit {
        INT8(1, "8-bit integer"),
        INT16(2, "16-bit integer"),
        INT32(4, "32-bit integer"),
        FLOAT32(4, "32-bit float");

        private final int bytesPerSample;
        private final String description;

        SampleUnit(int bytesPerSample, String description) {
            this.bytesPerSample = bytesPerSample;
            this.description = description;
        }

        public int getBytesPerSample() {
            return bytesPerSample;
        }

        public String getDescription() {
            return description;
        }
    }


    /**
     * 降采样
     *
     * @param inBuffer      输入音频缓冲区
     * @param inChannels    输入声道数
     * @param inSampleRate  输入采样率
     * @param inSampleUnit  输入采样数据类型
     * @param outSampleRate 输出采样率
     * @param outBuffer     输出音频缓冲区
     */
    public static void downSample(final BufferWrapper inBuffer,
                                  final int inChannels,
                                  final int inSampleRate,
                                  final SampleUnit inSampleUnit,
                                  final int outSampleRate,
                                  final BufferWrapper outBuffer) {
        if (inBuffer == null || outBuffer == null) {
            throw new IllegalArgumentException("Input and output buffers cannot be null");
        }
        if (inChannels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive");
        }
        if (inSampleRate <= 0 || outSampleRate <= 0) {
            throw new IllegalArgumentException("Sample rates must be positive");
        }
        if (outSampleRate >= inSampleRate) {
            throw new IllegalArgumentException(
                    String.format("Downsampling requires inSampleRate > outSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }
        if (inSampleRate % outSampleRate != 0) {
            throw new IllegalArgumentException(
                    String.format("For downsampling, inSampleRate must be divisible by outSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }

        final int sampleSize = inSampleUnit.getBytesPerSample();
        final int downRatio = inSampleRate / outSampleRate;
        final int onePointByteLen = sampleSize * inChannels;

        // 验证输入缓冲区大小是否足够
        if (inBuffer.getBufferUsed() < onePointByteLen) {
            throw new IllegalArgumentException("Input buffer too small for at least one sample");
        }

        int outBufferIndex = 0;
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();

        for (int i = 0; i < inBuffer.getBufferUsed(); i += downRatio * onePointByteLen) {
            // 检查输出缓冲区是否足够
            if (outBufferIndex + onePointByteLen > maxOutBytes) {
                break;
            }
            // 复制一个采样点（所有声道）
            System.arraycopy(inArray, i, outArray, outBufferIndex, onePointByteLen);
            outBufferIndex += onePointByteLen;
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 升采样 - 简单重复法
     *
     * @param inBuffer      输入音频缓冲区
     * @param inChannels    输入声道数
     * @param inSampleRate  输入采样率
     * @param inSampleUnit  输入采样数据类型
     * @param outSampleRate 输出采样率
     * @param outBuffer     输出音频缓冲区
     */
    public static void upSampleSimple(final BufferWrapper inBuffer,
                                      final int inChannels,
                                      final int inSampleRate,
                                      final SampleUnit inSampleUnit,
                                      final int outSampleRate,
                                      final BufferWrapper outBuffer) {
        if (inBuffer == null || outBuffer == null) {
            throw new IllegalArgumentException("Input and output buffers cannot be null");
        }
        if (inChannels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive");
        }
        if (inSampleRate <= 0 || outSampleRate <= 0) {
            throw new IllegalArgumentException("Sample rates must be positive");
        }
        if (outSampleRate <= inSampleRate) {
            throw new IllegalArgumentException(
                    String.format("Upsampling requires outSampleRate > inSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }
        if (outSampleRate % inSampleRate != 0) {
            throw new IllegalArgumentException(
                    String.format("For simple upsampling, outSampleRate must be divisible by inSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }

        final int sampleSize = inSampleUnit.getBytesPerSample();
        final int upRatio = outSampleRate / inSampleRate;
        final int onePointByteLen = sampleSize * inChannels;

        // 验证输入缓冲区大小是否足够
        if (inBuffer.getBufferUsed() < onePointByteLen) {
            throw new IllegalArgumentException("Input buffer too small for at least one sample");
        }

        int outBufferIndex = 0;
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();
        final int inSamples = inBuffer.getBufferUsed() / onePointByteLen;

        // 遍历输入采样点
        for (int i = 0; i < inSamples; i++) {
            final int inIndex = i * onePointByteLen;

            // 对每个输入采样点重复 upRatio 次
            for (int j = 0; j < upRatio; j++) {
                // 检查输出缓冲区是否足够
                if (outBufferIndex + onePointByteLen > maxOutBytes) {
                    outBuffer.setBufferUsed(outBufferIndex);
                    return;
                }

                // 复制一个采样点（所有声道）
                System.arraycopy(inArray, inIndex, outArray, outBufferIndex, onePointByteLen);
                outBufferIndex += onePointByteLen;
            }
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 升采样 - 线性插值法
     *
     * @param inBuffer      输入音频缓冲区
     * @param inChannels    输入声道数
     * @param inSampleRate  输入采样率
     * @param inSampleUnit  输入采样数据类型
     * @param outSampleRate 输出采样率
     * @param outBuffer     输出音频缓冲区
     */
    public static void upSampleLinear(final BufferWrapper inBuffer,
                                      final int inChannels,
                                      final int inSampleRate,
                                      final SampleUnit inSampleUnit,
                                      final int outSampleRate,
                                      final BufferWrapper outBuffer) {
        if (inBuffer == null || outBuffer == null) {
            throw new IllegalArgumentException("Input and output buffers cannot be null");
        }
        if (inChannels <= 0) {
            throw new IllegalArgumentException("Channel count must be positive");
        }
        if (inSampleRate <= 0 || outSampleRate <= 0) {
            throw new IllegalArgumentException("Sample rates must be positive");
        }
        if (outSampleRate <= inSampleRate) {
            throw new IllegalArgumentException(
                    String.format("UpSampling requires outSampleRate > inSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }
        if (outSampleRate % inSampleRate != 0) {
            throw new IllegalArgumentException(
                    String.format("For linear upSampling, outSampleRate must be divisible by inSampleRate. Got %d -> %d",
                            inSampleRate, outSampleRate));
        }

        final int sampleSize = inSampleUnit.getBytesPerSample();
        final int upRatio = outSampleRate / inSampleRate;
        final int onePointByteLen = sampleSize * inChannels;

        // 计算输入采样点数
        final int inSamples = inBuffer.getBufferUsed() / onePointByteLen;
        if (inSamples < 2) {
            throw new IllegalArgumentException("At least 2 input samples required for linear interpolation");
        }

        // 根据不同的采样数据类型进行处理
        switch (inSampleUnit) {
            case INT8:
                upSampleLinearInt8(inBuffer, outBuffer, inChannels, inSamples, upRatio, onePointByteLen);
                break;
            case INT16:
                upSampleLinearInt16(inBuffer, outBuffer, inChannels, inSamples, upRatio, onePointByteLen);
                break;
            case INT32:
                upSampleLinearInt32(inBuffer, outBuffer, inChannels, inSamples, upRatio, onePointByteLen);
                break;
            case FLOAT32:
                upSampleLinearFloat32(inBuffer, outBuffer, inChannels, inSamples, upRatio, onePointByteLen);
                break;
            default:
                throw new IllegalArgumentException("Unsupported sample unit: " + inSampleUnit);
        }
    }

    /**
     * 8位整数线性插值升采样
     */
    private static void upSampleLinearInt8(final BufferWrapper inBuffer,
                                           final BufferWrapper outBuffer,
                                           final int inChannels,
                                           final int inSamples,
                                           final int upRatio,
                                           final int onePointByteLen) {
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();
        int outBufferIndex = 0;

        for (int ch = 0; ch < inChannels; ch++) {
            for (int i = 0; i < inSamples - 1; i++) {
                // 读取当前和下一个采样点
                final int inIndex1 = i * onePointByteLen + ch;
                final int inIndex2 = (i + 1) * onePointByteLen + ch;

                // 转换为无符号整数进行计算
                final int sample1 = inArray[inIndex1] & 0xFF;
                final int sample2 = inArray[inIndex2] & 0xFF;

                // 线性插值
                for (int j = 0; j < upRatio; j++) {
                    final float t = (float) j / upRatio;
                    final int interpolated = Math.round(sample1 * (1 - t) + sample2 * t);

                    // 计算输出位置
                    final int outIndex = (i * upRatio + j) * onePointByteLen + ch;

                    // 检查输出缓冲区边界
                    if (outIndex >= maxOutBytes) {
                        break;
                    }

                    outArray[outIndex] = (byte) (interpolated & 0xFF);

                    // 更新输出缓冲区使用量
                    final int currentEnd = outIndex + 1;
                    if (currentEnd > outBufferIndex) {
                        outBufferIndex = currentEnd;
                    }
                }
            }
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 16位整数线性插值升采样
     */
    private static void upSampleLinearInt16(final BufferWrapper inBuffer,
                                            final BufferWrapper outBuffer,
                                            final int inChannels,
                                            final int inSamples,
                                            final int upRatio,
                                            final int onePointByteLen) {
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();
        int outBufferIndex = 0;

        for (int ch = 0; ch < inChannels; ch++) {
            for (int i = 0; i < inSamples - 1; i++) {
                // 读取当前和下一个采样点
                final int inIndex1 = i * onePointByteLen + ch * 2;
                final int inIndex2 = (i + 1) * onePointByteLen + ch * 2;

                final short sample1 = readInt16LE(inArray, inIndex1);
                final short sample2 = readInt16LE(inArray, inIndex2);

                // 线性插值
                for (int j = 0; j < upRatio; j++) {
                    final float t = (float) j / upRatio;
                    final short interpolated = (short) Math.round(sample1 * (1 - t) + sample2 * t);

                    // 计算输出位置
                    final int outIndex = (i * upRatio + j) * onePointByteLen + ch * 2;

                    // 检查输出缓冲区边界
                    if (outIndex + 2 > maxOutBytes) {
                        break;
                    }

                    writeInt16LE(outArray, outIndex, interpolated);

                    // 更新输出缓冲区使用量
                    final int currentEnd = outIndex + 2;
                    if (currentEnd > outBufferIndex) {
                        outBufferIndex = currentEnd;
                    }
                }
            }
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 32位整数线性插值升采样
     */
    private static void upSampleLinearInt32(final BufferWrapper inBuffer,
                                            final BufferWrapper outBuffer,
                                            final int inChannels,
                                            final int inSamples,
                                            final int upRatio,
                                            final int onePointByteLen) {
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();
        int outBufferIndex = 0;

        for (int ch = 0; ch < inChannels; ch++) {
            for (int i = 0; i < inSamples - 1; i++) {
                // 读取当前和下一个采样点
                final int inIndex1 = i * onePointByteLen + ch * 4;
                final int inIndex2 = (i + 1) * onePointByteLen + ch * 4;

                final int sample1 = readInt32LE(inArray, inIndex1);
                final int sample2 = readInt32LE(inArray, inIndex2);

                // 线性插值（使用long防止溢出）
                for (int j = 0; j < upRatio; j++) {
                    final float t = (float) j / upRatio;
                    final long interpolated = Math.round(sample1 * (1.0 - t) + sample2 * t);

                    // 确保结果在32位有符号整数范围内
                    final int clampedValue = clampToInt32(interpolated);

                    // 计算输出位置
                    final int outIndex = (i * upRatio + j) * onePointByteLen + ch * 4;

                    // 检查输出缓冲区边界
                    if (outIndex + 4 > maxOutBytes) {
                        break;
                    }

                    writeInt32LE(outArray, outIndex, clampedValue);

                    // 更新输出缓冲区使用量
                    final int currentEnd = outIndex + 4;
                    if (currentEnd > outBufferIndex) {
                        outBufferIndex = currentEnd;
                    }
                }
            }
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 32位浮点数线性插值升采样
     */
    private static void upSampleLinearFloat32(final BufferWrapper inBuffer,
                                              final BufferWrapper outBuffer,
                                              final int inChannels,
                                              final int inSamples,
                                              final int upRatio,
                                              final int onePointByteLen) {
        final byte[] inArray = inBuffer.getBuffer();
        final byte[] outArray = outBuffer.getBuffer();
        final int maxOutBytes = outBuffer.getCapacity();
        int outBufferIndex = 0;

        for (int ch = 0; ch < inChannels; ch++) {
            for (int i = 0; i < inSamples - 1; i++) {
                // 读取当前和下一个采样点
                final int inIndex1 = i * onePointByteLen + ch * 4;
                final int inIndex2 = (i + 1) * onePointByteLen + ch * 4;

                final float sample1 = readFloat32LE(inArray, inIndex1);
                final float sample2 = readFloat32LE(inArray, inIndex2);

                // 线性插值
                for (int j = 0; j < upRatio; j++) {
                    final float t = (float) j / upRatio;
                    final float interpolated = sample1 * (1 - t) + sample2 * t;

                    // 计算输出位置
                    final int outIndex = (i * upRatio + j) * onePointByteLen + ch * 4;

                    // 检查输出缓冲区边界
                    if (outIndex + 4 > maxOutBytes) {
                        break;
                    }

                    writeFloat32LE(outArray, outIndex, interpolated);

                    // 更新输出缓冲区使用量
                    final int currentEnd = outIndex + 4;
                    if (currentEnd > outBufferIndex) {
                        outBufferIndex = currentEnd;
                    }
                }
            }
        }

        outBuffer.setBufferUsed(outBufferIndex);
    }

    /**
     * 将long值限制在32位有符号整数范围内
     */
    public static int clampToInt32(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    // 字节操作辅助方法 (Little Endian)

    /**
     * 读取16位小端序整数
     */
    public static short readInt16LE(byte[] buffer, int offset) {
        return (short) ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
    }

    /**
     * 写入16位小端序整数
     */
    public static void writeInt16LE(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * 读取32位小端序整数
     */
    public static int readInt32LE(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) |
                ((buffer[offset + 1] & 0xFF) << 8) |
                ((buffer[offset + 2] & 0xFF) << 16) |
                ((buffer[offset + 3] & 0xFF) << 24);
    }

    /**
     * 写入32位小端序整数
     */
    public static void writeInt32LE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /**
     * 读取32位小端序浮点数
     */
    public static float readFloat32LE(byte[] buffer, int offset) {
        int intBits = readInt32LE(buffer, offset);
        return Float.intBitsToFloat(intBits);
    }

    /**
     * 写入32位小端序浮点数
     */
    public static void writeFloat32LE(byte[] buffer, int offset, float value) {
        int intBits = Float.floatToIntBits(value);
        writeInt32LE(buffer, offset, intBits);
    }

}