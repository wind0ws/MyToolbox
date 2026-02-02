package com.threshold.toolbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ResampleTest {

    /**
     * 测试示例
     */
    @Test
    public void test() {
        // 测试降采样：从48000Hz降到24000Hz，16位整数，立体声
//        testDownSample();
        // 测试升采样：从16000Hz升到48000Hz，32位浮点数，单声道
//        testUpSample();

        ResampleTestCase.test8kTo16k(
                "G:\\temp\\audio\\8k.pcm",      // 输入文件路径（如果不存在则生成正弦波）
                "G:\\temp\\audio\\output_8k_to_16k.pcm", // 输出文件路径
                5.0f                  // 音频时长（秒）
        );
    }

    private static void testDownSample() {
        System.out.println("=== 测试降采样 ===");

        // 创建模拟输入数据：48000Hz, 16-bit, 立体声，0.1秒音频
        int inputSamples = 48000 / 10; // 0.1秒
        int inputChannels = 2;
        Resample.SampleUnit sampleUnit = Resample.SampleUnit.INT16;
        int inputSize = inputSamples * sampleUnit.getBytesPerSample() * inputChannels;

        byte[] inputData = new byte[inputSize];
        // 填充测试数据（简单的正弦波模式）
        for (int i = 0; i < inputSamples; i++) {
            short sampleValue = (short) (Math.sin(i * 0.1) * 10000);
            int offset = i * 4; // 每个采样点4字节（2声道 * 2字节）
            Resample.writeInt16LE(inputData, offset, sampleValue);
            Resample.writeInt16LE(inputData, offset + 2, sampleValue);
        }

        BufferWrapper inputBuffer = new BufferWrapper(inputData, inputSize);

        // 创建输出缓冲区（24000Hz输出，大小约为输入的一半）
        int outputSize = inputSize / 2;
        BufferWrapper outputBuffer = new BufferWrapper(outputSize);

        // 执行降采样
        Resample.downSample(inputBuffer, inputChannels, 48000, sampleUnit, 24000, outputBuffer);

        System.out.println("降采样完成");
        System.out.println("输入大小: " + inputBuffer.getBufferUsed() + " 字节");
        System.out.println("输出大小: " + outputBuffer.getBufferUsed() + " 字节");
        System.out.println("降采样比例: 48000Hz -> 24000Hz");
    }

    private static void testUpSample() {
        System.out.println("\n=== 测试升采样 ===");

        // 创建模拟输入数据：16000Hz, 32-bit float, 单声道，0.1秒音频
        int inputSamples = 16000 / 10; // 0.1秒
        int inputChannels = 1;
        Resample.SampleUnit sampleUnit = Resample.SampleUnit.FLOAT32;
        int inputSize = inputSamples * sampleUnit.getBytesPerSample() * inputChannels;

        byte[] inputData = new byte[inputSize];
        // 填充测试数据（简单的正弦波模式）
        for (int i = 0; i < inputSamples; i++) {
            float sampleValue = (float) Math.sin(i * 0.2) * 0.5f;
            int offset = i * 4; // 每个采样点4字节
            Resample.writeFloat32LE(inputData, offset, sampleValue);
        }

        final BufferWrapper inputBuffer = new BufferWrapper(inputData, inputSize);

        // 创建输出缓冲区（48000Hz输出，大小约为输入的3倍）
        int outputSize = inputSize * 3;
        final BufferWrapper outputBuffer = new BufferWrapper(outputSize);

        // 执行升采样（线性插值）
        Resample.upSampleLinear(inputBuffer, inputChannels, 16000, sampleUnit, 48000, outputBuffer);

        System.out.println("升采样完成");
        System.out.println("输入大小: " + inputBuffer.getBufferUsed() + " 字节");
        System.out.println("输出大小: " + outputBuffer.getBufferUsed() + " 字节");
        System.out.println("升采样比例: 16000Hz -> 48000Hz");
        System.out.println("使用算法: 线性插值");
    }


    /**
     * 采样率转换测试类
     */
    public static class ResampleTestCase {

        /**
         * 测试用例1: 8kHz int16 转 16kHz int16 (升采样)
         *
         * @param inputPcmPath  输入PCM文件路径
         * @param outputPcmPath 输出PCM文件路径
         * @param durationSec   音频时长(秒)，当inputPcmPath为null时使用
         */
        public static void test8kTo16k(String inputPcmPath, String outputPcmPath, float durationSec) {
            System.out.println("\n=== 测试用例1: 8kHz int16 -> 16kHz int16 ===");

            final int inputSampleRate = 8000;
            final int outputSampleRate = 16000;
            final Resample.SampleUnit sampleUnit = Resample.SampleUnit.INT16;
            final int channels = 1; // 单声道

            try {
                // 准备输入缓冲区
                BufferWrapper inputBuffer;
                if (inputPcmPath != null && Files.exists(Paths.get(inputPcmPath))) {
                    // 从文件读取输入数据
                    inputBuffer = loadPcmFile(inputPcmPath);
                    System.out.println("从文件加载输入音频: " + inputPcmPath);
                    System.out.println("文件大小: " + inputBuffer.getBufferUsed() + " 字节");
                } else {
                    // 生成正弦波音频
                    System.out.println("生成正弦波音频，时长: " + durationSec + " 秒");
                    int inputSamples = (int) (inputSampleRate * durationSec);
                    inputBuffer = generateSineWave(inputSampleRate, inputSamples, 440, 0.8, sampleUnit, channels);
                }

                // 计算输出缓冲区大小
                int upRatio = outputSampleRate / inputSampleRate;
                int outputBytes = inputBuffer.getBufferUsed() * upRatio;
                final BufferWrapper outputBuffer = new BufferWrapper(outputBytes);

                // 执行升采样
                long startTime = System.currentTimeMillis();
                Resample.upSampleLinear(inputBuffer, channels, inputSampleRate, sampleUnit, outputSampleRate, outputBuffer);
                long endTime = System.currentTimeMillis();

                System.out.println("升采样完成，耗时: " + (endTime - startTime) + "ms");
                System.out.println("输入大小: " + inputBuffer.getBufferUsed() + " 字节");
                System.out.println("输出大小: " + outputBuffer.getBufferUsed() + " 字节");
                System.out.println("采样率转换: " + inputSampleRate + "Hz -> " + outputSampleRate + "Hz");

                // 保存输出文件
                if (outputPcmPath != null) {
                    savePcmFile(outputPcmPath, outputBuffer);
                    System.out.println("输出音频已保存: " + outputPcmPath);
                }

                // 打印音频信息
                printAudioInfo(inputBuffer, outputBuffer, sampleUnit, channels);

            } catch (Exception e) {
                System.err.println("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 测试用例2: 48kHz int16 转 16kHz int16 (降采样)
         *
         * @param inputPcmPath  输入PCM文件路径
         * @param outputPcmPath 输出PCM文件路径
         * @param durationSec   音频时长(秒)，当inputPcmPath为null时使用
         */
        public static void test48kTo16k(String inputPcmPath, String outputPcmPath, float durationSec) {
            System.out.println("\n=== 测试用例2: 48kHz int16 -> 16kHz int16 ===");

            final int inputSampleRate = 48000;
            final int outputSampleRate = 16000;
            final Resample.SampleUnit sampleUnit = Resample.SampleUnit.INT16;
            final int channels = 1; // 单声道

            try {
                // 准备输入缓冲区
                BufferWrapper inputBuffer;
                if (inputPcmPath != null && Files.exists(Paths.get(inputPcmPath))) {
                    // 从文件读取输入数据
                    inputBuffer = loadPcmFile(inputPcmPath);
                    System.out.println("从文件加载输入音频: " + inputPcmPath);
                    System.out.println("文件大小: " + inputBuffer.getBufferUsed() + " 字节");
                } else {
                    // 生成正弦波音频
                    System.out.println("生成正弦波音频，时长: " + durationSec + " 秒");
                    int inputSamples = (int) (inputSampleRate * durationSec);
                    // 使用更复杂的音频信号（两个频率的正弦波叠加）
                    inputBuffer = generateComplexWave(inputSampleRate, inputSamples, sampleUnit, channels);
                }

                // 计算输出缓冲区大小（降采样，大小约为输入的1/3）
                int downRatio = inputSampleRate / outputSampleRate;
                int outputBytes = inputBuffer.getBufferUsed() / downRatio;
                BufferWrapper outputBuffer = new BufferWrapper(outputBytes);

                // 执行降采样
                long startTime = System.currentTimeMillis();
                Resample.downSample(inputBuffer, channels, inputSampleRate, sampleUnit, outputSampleRate, outputBuffer);
                long endTime = System.currentTimeMillis();

                System.out.println("降采样完成，耗时: " + (endTime - startTime) + "ms");
                System.out.println("输入大小: " + inputBuffer.getBufferUsed() + " 字节");
                System.out.println("输出大小: " + outputBuffer.getBufferUsed() + " 字节");
                System.out.println("采样率转换: " + inputSampleRate + "Hz -> " + outputSampleRate + "Hz");

                // 保存输出文件
                if (outputPcmPath != null) {
                    savePcmFile(outputPcmPath, outputBuffer);
                    System.out.println("输出音频已保存: " + outputPcmPath);
                }

                // 打印音频信息
                printAudioInfo(inputBuffer, outputBuffer, sampleUnit, channels);

            } catch (Exception e) {
                System.err.println("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 生成正弦波音频
         */
        private static BufferWrapper generateSineWave(int sampleRate, int numSamples,
                                                      float frequency, double amplitude,
                                                      Resample.SampleUnit sampleUnit,
                                                      int channels) {
            int sampleSize = sampleUnit.getBytesPerSample();
            int bytesPerSample = sampleSize * channels;
            int bufferSize = numSamples * bytesPerSample;

            BufferWrapper buffer = new BufferWrapper(bufferSize);
            byte[] data = buffer.getBuffer();

            double twoPiF = 2 * Math.PI * frequency / sampleRate;

            for (int i = 0; i < numSamples; i++) {
                double time = i / (double) sampleRate;
                double value = amplitude * Math.sin(twoPiF * i);

                for (int ch = 0; ch < channels; ch++) {
                    int offset = i * bytesPerSample + ch * sampleSize;

                    switch (sampleUnit) {
                        case INT8:
                            int int8Value = (int) (value * 127);
                            data[offset] = (byte) (int8Value & 0xFF);
                            break;
                        case INT16:
                            short int16Value = (short) (value * 32767);
                            writeInt16LE(data, offset, int16Value);
                            break;
                        case INT32:
                            int int32Value = (int) (value * 2147483647);
                            writeInt32LE(data, offset, int32Value);
                            break;
                        case FLOAT32:
                            float floatValue = (float) value;
                            writeFloat32LE(data, offset, floatValue);
                            break;
                    }
                }
            }

            buffer.setBufferUsed(bufferSize);
            return buffer;
        }

        /**
         * 生成复杂波形（两个正弦波叠加）
         */
        private static BufferWrapper generateComplexWave(int sampleRate, int numSamples,
                                                         Resample.SampleUnit sampleUnit,
                                                         int channels) {
            int sampleSize = sampleUnit.getBytesPerSample();
            int bytesPerSample = sampleSize * channels;
            int bufferSize = numSamples * bytesPerSample;

            BufferWrapper buffer = new BufferWrapper(bufferSize);
            byte[] data = buffer.getBuffer();

            double freq1 = 440.0; // A4
            double freq2 = 880.0; // A5
            double twoPiF1 = 2 * Math.PI * freq1 / sampleRate;
            double twoPiF2 = 2 * Math.PI * freq2 / sampleRate;

            for (int i = 0; i < numSamples; i++) {
                // 两个正弦波叠加，使用不同的相位和振幅
                double value1 = 0.6 * Math.sin(twoPiF1 * i);
                double value2 = 0.4 * Math.sin(twoPiF2 * i + Math.PI / 4);
                double value = value1 + value2;

                // 防止削波
                if (value > 1.0) value = 1.0;
                if (value < -1.0) value = -1.0;

                for (int ch = 0; ch < channels; ch++) {
                    int offset = i * bytesPerSample + ch * sampleSize;

                    switch (sampleUnit) {
                        case INT8:
                            int int8Value = (int) (value * 127);
                            data[offset] = (byte) (int8Value & 0xFF);
                            break;
                        case INT16:
                            short int16Value = (short) (value * 32767);
                            writeInt16LE(data, offset, int16Value);
                            break;
                        case INT32:
                            int int32Value = (int) (value * 2147483647);
                            writeInt32LE(data, offset, int32Value);
                            break;
                        case FLOAT32:
                            float floatValue = (float) value;
                            writeFloat32LE(data, offset, floatValue);
                            break;
                    }
                }
            }

            buffer.setBufferUsed(bufferSize);
            return buffer;
        }

        /**
         * 从PCM文件加载音频数据
         */
        private static BufferWrapper loadPcmFile(String filePath) throws IOException {
            File file = new File(filePath);
            long fileSize = file.length();

            if (fileSize > Integer.MAX_VALUE) {
                throw new IOException("文件太大: " + fileSize + " 字节");
            }

            byte[] buffer = new byte[(int) fileSize];

            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead = fis.read(buffer);
                if (bytesRead != fileSize) {
                    System.err.println("警告: 期望读取 " + fileSize + " 字节，实际读取 " + bytesRead + " 字节");
                }
                return new BufferWrapper(buffer, bytesRead);
            }
        }

        /**
         * 保存音频数据到PCM文件
         */
        private static void savePcmFile(String filePath, BufferWrapper buffer) throws IOException {
            File file = new File(filePath);

            // 确保目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(buffer.getBuffer(), 0, buffer.getBufferUsed());
                fos.flush();
            }
        }

        /**
         * 打印音频信息
         */
        private static void printAudioInfo(BufferWrapper inputBuffer,
                                           BufferWrapper outputBuffer,
                                           Resample.SampleUnit sampleUnit,
                                           int channels) {
            int sampleSize = sampleUnit.getBytesPerSample();

            int inputSamples = inputBuffer.getBufferUsed() / (sampleSize * channels);
            int outputSamples = outputBuffer.getBufferUsed() / (sampleSize * channels);

            System.out.println("\n音频信息:");
            System.out.println("  格式: " + sampleUnit.getDescription() +
                    " (" + sampleUnit.getBytesPerSample() + "字节/采样点)");
            System.out.println("  声道数: " + channels);
            System.out.println("  输入音频时长: " + String.format("%.3f", inputSamples / 16000.0) + " 秒 (16kHz基准)");
            System.out.println("  输出音频时长: " + String.format("%.3f", outputSamples / 16000.0) + " 秒 (16kHz基准)");

            // 计算输入输出的RMS（均方根）值，用于评估音频质量
            double inputRms = calculateRMS(inputBuffer, sampleUnit, channels);
            double outputRms = calculateRMS(outputBuffer, sampleUnit, channels);
            System.out.println("  输入RMS: " + String.format("%.6f", inputRms));
            System.out.println("  输出RMS: " + String.format("%.6f", outputRms));
            System.out.println("  RMS变化: " + String.format("%.2f", (outputRms / inputRms) * 100) + "%");
        }

        /**
         * 计算音频数据的RMS值
         */
        private static double calculateRMS(BufferWrapper buffer,
                                           Resample.SampleUnit sampleUnit,
                                           int channels) {
            int sampleSize = sampleUnit.getBytesPerSample();
            int bytesPerSample = sampleSize * channels;
            int numSamples = buffer.getBufferUsed() / bytesPerSample;

            if (numSamples == 0) return 0.0;

            double sum = 0.0;
            byte[] data = buffer.getBuffer();

            for (int i = 0; i < numSamples; i++) {
                double sampleValue = 0.0;
                int offset = i * bytesPerSample;

                // 只取第一个声道计算RMS
                switch (sampleUnit) {
                    case INT8:
                        sampleValue = (data[offset] & 0xFF) / 128.0 - 1.0;
                        break;
                    case INT16:
                        short int16Value = readInt16LE(data, offset);
                        sampleValue = int16Value / 32768.0;
                        break;
                    case INT32:
                        int int32Value = readInt32LE(data, offset);
                        sampleValue = int32Value / 2147483648.0;
                        break;
                    case FLOAT32:
                        sampleValue = readFloat32LE(data, offset);
                        break;
                }

                sum += sampleValue * sampleValue;
            }

            return Math.sqrt(sum / numSamples);
        }

        // 字节操作辅助方法 (与Resample类中的方法相同)
        private static short readInt16LE(byte[] buffer, int offset) {
            return (short) ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
        }

        private static void writeInt16LE(byte[] buffer, int offset, short value) {
            buffer[offset] = (byte) (value & 0xFF);
            buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        }

        private static int readInt32LE(byte[] buffer, int offset) {
            return (buffer[offset] & 0xFF) |
                    ((buffer[offset + 1] & 0xFF) << 8) |
                    ((buffer[offset + 2] & 0xFF) << 16) |
                    ((buffer[offset + 3] & 0xFF) << 24);
        }

        private static void writeInt32LE(byte[] buffer, int offset, int value) {
            buffer[offset] = (byte) (value & 0xFF);
            buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
            buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
            buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
        }

        private static float readFloat32LE(byte[] buffer, int offset) {
            int intBits = readInt32LE(buffer, offset);
            return Float.intBitsToFloat(intBits);
        }

        private static void writeFloat32LE(byte[] buffer, int offset, float value) {
            int intBits = Float.floatToIntBits(value);
            writeInt32LE(buffer, offset, intBits);
        }

        /**
         * 主方法 - 运行测试用例
         */
        public static void main(String[] args) {
            System.out.println("=== 采样率转换测试 ===");

            // 测试用例1: 8kHz int16 转 16kHz int16
            // 如果input_8k.pcm不存在，会生成正弦波音频
            test8kTo16k(
                    "input_8k.pcm",      // 输入文件路径（如果不存在则生成正弦波）
                    "output_8k_to_16k.pcm", // 输出文件路径
                    2.0f                  // 音频时长（秒）
            );

            // 测试用例2: 48kHz int16 转 16kHz int16
            // 如果input_48k.pcm不存在，会生成复杂波形音频
            test48kTo16k(
                    "input_48k.pcm",      // 输入文件路径（如果不存在则生成正弦波）
                    "output_48k_to_16k.pcm", // 输出文件路径
                    2.0f                   // 音频时长（秒）
            );

            System.out.println("\n=== 测试完成 ===");
            System.out.println("说明：");
            System.out.println("1. 如果输入文件不存在，会自动生成测试音频");
            System.out.println("2. 输出文件为原始PCM格式，无文件头");
            System.out.println("3. 可以使用Audacity等工具播放PCM文件：");
            System.out.println("   - 导入 -> 原始数据 -> 选择正确的格式");
            System.out.println("   - 测试用例1输出：16000Hz, 16-bit, 单声道");
            System.out.println("   - 测试用例2输出：16000Hz, 16-bit, 单声道");
        }

        /**
         * 使用示例：运行特定测试
         */
        public static void runAllTests() {
            // 清除旧的输出文件
            try {
                Files.deleteIfExists(Paths.get("output_8k_to_16k.pcm"));
                Files.deleteIfExists(Paths.get("output_48k_to_16k.pcm"));
            } catch (IOException e) {
                System.err.println("无法删除旧文件: " + e.getMessage());
            }

            // 运行所有测试
            main(new String[]{});
        }

        /**
         * 验证生成的文件是否正确
         */
        public static void verifyOutputFiles() {
            System.out.println("\n=== 验证输出文件 ===");

            try {
                File file1 = new File("output_8k_to_16k.pcm");
                if (file1.exists()) {
                    System.out.println("文件1存在: " + file1.getName() +
                            " 大小: " + file1.length() + " 字节");
                } else {
                    System.out.println("文件1不存在: output_8k_to_16k.pcm");
                }

                File file2 = new File("output_48k_to_16k.pcm");
                if (file2.exists()) {
                    System.out.println("文件2存在: " + file2.getName() +
                            " 大小: " + file2.length() + " 字节");
                } else {
                    System.out.println("文件2不存在: output_48k_to_16k.pcm");
                }

            } catch (Exception e) {
                System.err.println("验证失败: " + e.getMessage());
            }
        }
    }
}
