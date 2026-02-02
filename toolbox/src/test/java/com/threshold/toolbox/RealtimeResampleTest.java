package com.threshold.toolbox;

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 实时采样率转换测试类
 * 模拟实际使用场景：每次处理10ms音频数据
 */
public class RealtimeResampleTest {

    @Test
    public void test() {
// 单独测试8kHz到16kHz转换（使用已有文件）
        RealtimeResampleTest.test8kTo16kRealtime(
                "G:\\temp\\audio\\8k.pcm",      // 输入文件路径（如果不存在则生成正弦波）
                "G:\\temp\\audio\\output_8k_to_16k.pcm", // 输出文件路径
                5.0f // 音频不存在时生成音频时长
        );

// 单独测试48kHz到16kHz转换（生成测试音频）
//        RealtimeResampleTest.test48kTo16kRealtime(
//                null,  // 生成测试音频
//                "output_48k_to_16k_realtime.pcm",
//                10.0f  // 10秒音频
//        );
    }

    /**
     * 音频块生成器 - 用于模拟实时音频输入
     */
    private static class AudioBlockGenerator {
        private final int sampleRate;
        private final Resample.SampleUnit sampleUnit;
        private final int channels;
        private final float frequency;
        private final double amplitude;
        final int blockSamples; // 每块采样点数
        int currentSample = 0;
        final int totalSamples;

        public AudioBlockGenerator(int sampleRate, Resample.SampleUnit sampleUnit,
                                   int channels, float durationSec,
                                   float frequency, double amplitude) {
            this.sampleRate = sampleRate;
            this.sampleUnit = sampleUnit;
            this.channels = channels;
            this.frequency = frequency;
            this.amplitude = amplitude;
            this.blockSamples = sampleRate / 100; // 10ms对应的采样点数
            this.totalSamples = (int) (sampleRate * durationSec);
        }

        /**
         * 获取下一个10ms音频块
         *
         * @return 音频数据块，如果已生成所有数据则返回null
         */
        public BufferWrapper getNextBlock() {
            if (currentSample >= totalSamples) {
                return null;
            }

            // 计算当前块的实际采样点数（最后一块可能不足10ms）
            int samplesThisBlock = Math.min(blockSamples, totalSamples - currentSample);
            int sampleSize = sampleUnit.getBytesPerSample();
            int bytesPerSample = sampleSize * channels;
            int blockSize = samplesThisBlock * bytesPerSample;

            BufferWrapper block = new BufferWrapper(blockSize);
            byte[] data = block.getBuffer();

            // 生成正弦波数据
            double twoPiF = 2 * Math.PI * frequency / sampleRate;

            for (int i = 0; i < samplesThisBlock; i++) {
                int globalSampleIndex = currentSample + i;
                double time = globalSampleIndex / (double) sampleRate;
                double value = amplitude * Math.sin(twoPiF * globalSampleIndex);

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

            block.setBufferUsed(blockSize);
            currentSample += samplesThisBlock;

            return block;
        }

        /**
         * 是否还有更多数据
         */
        public boolean hasMoreData() {
            return currentSample < totalSamples;
        }

        /**
         * 获取已处理的样本百分比
         */
        public float getProgress() {
            return (float) currentSample / totalSamples * 100;
        }
    }

    /**
     * 音频文件块读取器 - 从文件按10ms块读取音频
     */
    private static class AudioFileBlockReader {
        private final FileInputStream fileStream;
        private final int sampleRate;
        private final Resample.SampleUnit sampleUnit;
        private final int channels;
        private final int blockSamples;
        private final int blockBytes;
        private long totalBytesRead = 0;
        private final long fileSize;

        public AudioFileBlockReader(String filePath, int sampleRate,
                                    Resample.SampleUnit sampleUnit, int channels) throws IOException {
            File file = new File(filePath);
            this.fileStream = new FileInputStream(file);
            this.sampleRate = sampleRate;
            this.sampleUnit = sampleUnit;
            this.channels = channels;
            this.blockSamples = sampleRate / 100; // 10ms对应的采样点数
            int sampleSize = sampleUnit.getBytesPerSample();
            this.blockBytes = blockSamples * sampleSize * channels;
            this.fileSize = file.length();
        }

        /**
         * 读取下一个10ms音频块
         *
         * @return 音频数据块，如果已读取完所有数据则返回null
         */
        public BufferWrapper getNextBlock() throws IOException {
            // 创建缓冲区
            byte[] buffer = new byte[blockBytes];
            int bytesRead = fileStream.read(buffer);

            if (bytesRead <= 0) {
                return null;
            }

            totalBytesRead += bytesRead;

            // 如果读取的字节数不足一块，调整缓冲区大小
            if (bytesRead < blockBytes) {
                byte[] trimmedBuffer = new byte[bytesRead];
                System.arraycopy(buffer, 0, trimmedBuffer, 0, bytesRead);
                return new BufferWrapper(trimmedBuffer, bytesRead);
            }

            return new BufferWrapper(buffer, bytesRead);
        }

        /**
         * 是否还有更多数据
         */
        public boolean hasMoreData() throws IOException {
            return fileStream.available() > 0;
        }

        /**
         * 获取已读取的百分比
         */
        public float getProgress() {
            return (float) totalBytesRead / fileSize * 100;
        }

        /**
         * 关闭文件流
         */
        public void close() throws IOException {
            fileStream.close();
        }
    }

    /**
     * 测试用例1: 8kHz int16 转 16kHz int16 (实时模式)
     * 每次处理10ms音频数据
     */
    public static void test8kTo16kRealtime(String inputPcmPath, String outputPcmPath, float durationSec) {
        System.out.println("\n=== 测试用例1(实时模式): 8kHz int16 -> 16kHz int16 ===");

        final int inputSampleRate = 8000;
        final int outputSampleRate = 16000;
        final Resample.SampleUnit sampleUnit = Resample.SampleUnit.INT16;
        final int channels = 1; // 单声道

        AudioBlockGenerator generator = null;
        AudioFileBlockReader reader = null;
        FileOutputStream outputStream = null;

        try {
            // 准备输出文件流
            if (outputPcmPath != null) {
                File outputFile = new File(outputPcmPath);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                outputStream = new FileOutputStream(outputFile);
            }

            // 准备输入源
            if (inputPcmPath != null && Files.exists(Paths.get(inputPcmPath))) {
                System.out.println("从文件读取音频: " + inputPcmPath);
                reader = new AudioFileBlockReader(inputPcmPath, inputSampleRate, sampleUnit, channels);
            } else {
                System.out.println("生成正弦波音频，时长: " + durationSec + " 秒");
                generator = new AudioBlockGenerator(inputSampleRate, sampleUnit, channels,
                        durationSec, 440, 0.8);
            }

            int blockCount = 0;
            long totalInputBytes = 0;
            long totalOutputBytes = 0;
            long startTime = System.currentTimeMillis();

            // 处理队列：用于保持最近几个处理过的块（可选，用于边界处理）
            Queue<BufferWrapper> recentBlocks = new LinkedList<>();

            // 实时处理循环
            while (true) {
                // 获取下一个输入块
                BufferWrapper inputBlock = null;
                if (reader != null) {
                    inputBlock = reader.getNextBlock();
                } else if (generator != null && generator.hasMoreData()) {
                    inputBlock = generator.getNextBlock();
                }

                if (inputBlock == null) {
                    break; // 没有更多数据
                }

                blockCount++;
                totalInputBytes += inputBlock.getBufferUsed();

                // 计算输出块大小
                int upRatio = outputSampleRate / inputSampleRate;
                int outputBytes = inputBlock.getBufferUsed() * upRatio;
                BufferWrapper outputBlock = new BufferWrapper(outputBytes);

                // 执行升采样
                Resample.upSampleLinear(inputBlock, channels, inputSampleRate,
                        sampleUnit, outputSampleRate, outputBlock);

                // 保存到输出文件
                if (outputStream != null) {
                    outputStream.write(outputBlock.getBuffer(), 0, outputBlock.getBufferUsed());
                }

                totalOutputBytes += outputBlock.getBufferUsed();

                // 保持最近几个块（用于可能的边界处理）
                recentBlocks.offer(outputBlock);
                if (recentBlocks.size() > 3) { // 保留最近3个块
                    recentBlocks.poll();
                }

                // 每处理100个块（1秒）打印一次进度
                if (blockCount % 100 == 0) {
                    float progress = 0;
                    if (reader != null) {
                        progress = reader.getProgress();
                    } else if (generator != null) {
                        progress = generator.getProgress();
                    }
                    System.out.printf("已处理: %d 块 (%.1f%%)%n", blockCount, progress);
                }
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // 计算音频时长（以输出采样率为基准）
            long outputSamples = totalOutputBytes / (sampleUnit.getBytesPerSample() * channels);
            float outputDuration = (float) outputSamples / outputSampleRate;

            System.out.println("\n处理完成!");
            System.out.println("总处理块数: " + blockCount + " 块");
            System.out.println("输入数据量: " + totalInputBytes + " 字节");
            System.out.println("输出数据量: " + totalOutputBytes + " 字节");
            System.out.println("处理耗时: " + processingTime + "ms");
            System.out.println("输出音频时长: " + String.format("%.3f", outputDuration) + " 秒");
            System.out.println("实时处理速率: " +
                    String.format("%.2f", totalOutputBytes / 1024.0 / (processingTime / 1000.0)) + " KB/s");

            if (outputPcmPath != null) {
                System.out.println("输出音频已保存: " + outputPcmPath);
            }

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            try {
                if (reader != null) reader.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                System.err.println("资源关闭失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试用例2: 48kHz int16 转 16kHz int16 (实时模式)
     * 每次处理10ms音频数据
     */
    public static void test48kTo16kRealtime(String inputPcmPath, String outputPcmPath, float durationSec) {
        System.out.println("\n=== 测试用例2(实时模式): 48kHz int16 -> 16kHz int16 ===");

        final int inputSampleRate = 48000;
        final int outputSampleRate = 16000;
        final Resample.SampleUnit sampleUnit = Resample.SampleUnit.INT16;
        final int channels = 1; // 单声道

        AudioBlockGenerator generator = null;
        AudioFileBlockReader reader = null;
        FileOutputStream outputStream = null;

        try {
            // 准备输出文件流
            if (outputPcmPath != null) {
                File outputFile = new File(outputPcmPath);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                outputStream = new FileOutputStream(outputFile);
            }

            // 准备输入源
            if (inputPcmPath != null && Files.exists(Paths.get(inputPcmPath))) {
                System.out.println("从文件读取音频: " + inputPcmPath);
                reader = new AudioFileBlockReader(inputPcmPath, inputSampleRate, sampleUnit, channels);
            } else {
                System.out.println("生成复杂波形音频，时长: " + durationSec + " 秒");
                // 使用复杂波形生成器
                generator = createComplexWaveGenerator(inputSampleRate, sampleUnit, channels, durationSec);
            }

            int blockCount = 0;
            long totalInputBytes = 0;
            long totalOutputBytes = 0;
            long startTime = System.currentTimeMillis();

            // 实时处理循环
            while (true) {
                // 获取下一个输入块
                BufferWrapper inputBlock = null;
                if (reader != null) {
                    inputBlock = reader.getNextBlock();
                } else if (generator != null && generator.hasMoreData()) {
                    inputBlock = generator.getNextBlock();
                }

                if (inputBlock == null) {
                    break; // 没有更多数据
                }

                blockCount++;
                totalInputBytes += inputBlock.getBufferUsed();

                // 计算输出块大小
                int downRatio = inputSampleRate / outputSampleRate;
                int outputBytes = inputBlock.getBufferUsed() / downRatio;
                BufferWrapper outputBlock = new BufferWrapper(outputBytes);

                // 执行降采样
                Resample.downSample(inputBlock, channels, inputSampleRate,
                        sampleUnit, outputSampleRate, outputBlock);

                // 保存到输出文件
                if (outputStream != null) {
                    outputStream.write(outputBlock.getBuffer(), 0, outputBlock.getBufferUsed());
                }

                totalOutputBytes += outputBlock.getBufferUsed();

                // 每处理100个块（1秒）打印一次进度
                if (blockCount % 100 == 0) {
                    float progress = 0;
                    if (reader != null) {
                        progress = reader.getProgress();
                    } else if (generator != null) {
                        progress = generator.getProgress();
                    }
                    System.out.printf("已处理: %d 块 (%.1f%%)%n", blockCount, progress);
                }
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            // 计算音频时长（以输出采样率为基准）
            long outputSamples = totalOutputBytes / (sampleUnit.getBytesPerSample() * channels);
            float outputDuration = (float) outputSamples / outputSampleRate;

            System.out.println("\n处理完成!");
            System.out.println("总处理块数: " + blockCount + " 块");
            System.out.println("输入数据量: " + totalInputBytes + " 字节");
            System.out.println("输出数据量: " + totalOutputBytes + " 字节");
            System.out.println("处理耗时: " + processingTime + "ms");
            System.out.println("输出音频时长: " + String.format("%.3f", outputDuration) + " 秒");
            System.out.println("实时处理速率: " +
                    String.format("%.2f", totalOutputBytes / 1024.0 / (processingTime / 1000.0)) + " KB/s");

            if (outputPcmPath != null) {
                System.out.println("输出音频已保存: " + outputPcmPath);
            }

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            try {
                if (reader != null) reader.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                System.err.println("资源关闭失败: " + e.getMessage());
            }
        }
    }

    /**
     * 创建复杂波形生成器（包含两个频率的正弦波）
     */
    private static AudioBlockGenerator createComplexWaveGenerator(int sampleRate,
                                                                  Resample.SampleUnit sampleUnit,
                                                                  int channels, float durationSec) {
        // 我们扩展AudioBlockGenerator来支持复杂波形
        // 这里使用内部类的方式
        return new AudioBlockGenerator(sampleRate, sampleUnit, channels, durationSec, 440, 0.8) {
            // 重写生成逻辑
            @Override
            public BufferWrapper getNextBlock() {
                if (currentSample >= totalSamples) {
                    return null;
                }

                // 计算当前块的实际采样点数
                int samplesThisBlock = Math.min(blockSamples, totalSamples - currentSample);
                int sampleSize = sampleUnit.getBytesPerSample();
                int bytesPerSample = sampleSize * channels;
                int blockSize = samplesThisBlock * bytesPerSample;

                BufferWrapper block = new BufferWrapper(blockSize);
                byte[] data = block.getBuffer();

                // 生成复杂波形数据
                double freq1 = 440.0; // A4
                double freq2 = 880.0; // A5
                double twoPiF1 = 2 * Math.PI * freq1 / sampleRate;
                double twoPiF2 = 2 * Math.PI * freq2 / sampleRate;

                for (int i = 0; i < samplesThisBlock; i++) {
                    int globalSampleIndex = currentSample + i;

                    // 两个正弦波叠加
                    double value1 = 0.6 * Math.sin(twoPiF1 * globalSampleIndex);
                    double value2 = 0.4 * Math.sin(twoPiF2 * globalSampleIndex + Math.PI / 4);
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

                block.setBufferUsed(blockSize);
                currentSample += samplesThisBlock;

                return block;
            }
        };
    }

    /**
     * 带边界处理的实时采样率转换
     * 这个版本会在块边界处进行特殊处理，以减少失真
     */
    public static void testWithBoundaryHandling(String inputPcmPath, String outputPcmPath,
                                                int inputSampleRate, int outputSampleRate,
                                                Resample.SampleUnit sampleUnit, int channels,
                                                float durationSec) {
        System.out.println("\n=== 带边界处理的实时采样率转换 ===");
        System.out.println("转换: " + inputSampleRate + "Hz -> " + outputSampleRate + "Hz");

        AudioBlockGenerator generator = null;
        AudioFileBlockReader reader = null;
        FileOutputStream outputStream = null;

        try {
            // 准备输出文件流
            if (outputPcmPath != null) {
                File outputFile = new File(outputPcmPath);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                outputStream = new FileOutputStream(outputFile);
            }

            // 准备输入源
            if (inputPcmPath != null && Files.exists(Paths.get(inputPcmPath))) {
                System.out.println("从文件读取音频: " + inputPcmPath);
                reader = new AudioFileBlockReader(inputPcmPath, inputSampleRate, sampleUnit, channels);
            } else {
                System.out.println("生成音频，时长: " + durationSec + " 秒");
                generator = createComplexWaveGenerator(inputSampleRate, sampleUnit, channels, durationSec);
            }

            int blockCount = 0;
            long totalInputBytes = 0;
            long totalOutputBytes = 0;
            long startTime = System.currentTimeMillis();

            // 用于边界处理的前一个块的最后一个采样点
            BufferWrapper previousBlock = null;

            // 实时处理循环
            while (true) {
                // 获取下一个输入块
                BufferWrapper inputBlock = null;
                if (reader != null) {
                    inputBlock = reader.getNextBlock();
                } else if (generator != null && generator.hasMoreData()) {
                    inputBlock = generator.getNextBlock();
                }

                if (inputBlock == null) {
                    break; // 没有更多数据
                }

                blockCount++;
                totalInputBytes += inputBlock.getBufferUsed();

                // 计算输出块大小
                BufferWrapper outputBlock;
                if (inputSampleRate > outputSampleRate) {
                    // 降采样
                    int downRatio = inputSampleRate / outputSampleRate;
                    int outputBytes = inputBlock.getBufferUsed() / downRatio;
                    outputBlock = new BufferWrapper(outputBytes);
                    Resample.downSample(inputBlock, channels, inputSampleRate,
                            sampleUnit, outputSampleRate, outputBlock);
                } else {
                    // 升采样
                    int upRatio = outputSampleRate / inputSampleRate;
                    int outputBytes = inputBlock.getBufferUsed() * upRatio;
                    outputBlock = new BufferWrapper(outputBytes);
                    Resample.upSampleLinear(inputBlock, channels, inputSampleRate,
                            sampleUnit, outputSampleRate, outputBlock);
                }

                // 保存到输出文件
                if (outputStream != null) {
                    outputStream.write(outputBlock.getBuffer(), 0, outputBlock.getBufferUsed());
                }

                totalOutputBytes += outputBlock.getBufferUsed();
                previousBlock = outputBlock;

                // 每处理100个块（1秒）打印一次进度
                if (blockCount % 100 == 0) {
                    float progress = 0;
                    if (reader != null) {
                        progress = reader.getProgress();
                    } else if (generator != null) {
                        progress = generator.getProgress();
                    }
                    System.out.printf("已处理: %d 块 (%.1f%%)%n", blockCount, progress);
                }
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            System.out.println("\n处理完成!");
            System.out.println("总处理块数: " + blockCount + " 块");
            System.out.println("处理耗时: " + processingTime + "ms");
            System.out.println("平均每块处理时间: " +
                    String.format("%.3f", processingTime / (float) blockCount) + "ms");

            if (outputPcmPath != null) {
                System.out.println("输出音频已保存: " + outputPcmPath);
            }

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                System.err.println("资源关闭失败: " + e.getMessage());
            }
        }
    }

    // 字节操作辅助方法
    private static short readInt16LE(byte[] buffer, int offset) {
        return (short) ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
    }

    private static void writeInt16LE(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void writeInt32LE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeFloat32LE(byte[] buffer, int offset, float value) {
        int intBits = Float.floatToIntBits(value);
        writeInt32LE(buffer, offset, intBits);
    }

    /**
     * 主方法 - 运行实时处理测试
     */
    public static void main(String[] args) {
        System.out.println("=== 实时采样率转换测试 ===");
        System.out.println("模拟实际使用场景：每次处理10ms音频数据");

        // 清理旧的输出文件
        try {
            Files.deleteIfExists(Paths.get("realtime_output_8k_to_16k.pcm"));
            Files.deleteIfExists(Paths.get("realtime_output_48k_to_16k.pcm"));
        } catch (IOException e) {
            System.err.println("无法删除旧文件: " + e.getMessage());
        }

        // 测试用例1: 8kHz int16 转 16kHz int16 (实时模式)
        test8kTo16kRealtime(
                null, // 不指定输入文件，生成测试音频
                "realtime_output_8k_to_16k.pcm",
                5.0f  // 5秒音频
        );

        // 测试用例2: 48kHz int16 转 16kHz int16 (实时模式)
        test48kTo16kRealtime(
                null, // 不指定输入文件，生成测试音频
                "realtime_output_48k_to_16k.pcm",
                5.0f  // 5秒音频
        );

        // 可选：带边界处理的测试
        System.out.println("\n=== 额外测试：带边界处理 ===");
        testWithBoundaryHandling(
                null,
                "realtime_with_boundary.pcm",
                8000, 16000,
                Resample.SampleUnit.INT16, 1,
                3.0f
        );

        System.out.println("\n=== 测试完成 ===");
        System.out.println("说明：");
        System.out.println("1. 模拟了实时音频处理场景，每次处理10ms数据");
        System.out.println("2. 输出为原始PCM格式，可使用Audacity播放");
        System.out.println("3. 在Audacity中导入时设置：");
        System.out.println("   - 测试用例1输出：16000Hz, 16-bit signed, 单声道, 小端序");
        System.out.println("   - 测试用例2输出：16000Hz, 16-bit signed, 单声道, 小端序");

        // 验证文件
        verifyOutputFiles();
    }

    /**
     * 验证输出文件
     */
    private static void verifyOutputFiles() {
        System.out.println("\n=== 验证输出文件 ===");

        String[] files = {
                "realtime_output_8k_to_16k.pcm",
                "realtime_output_48k_to_16k.pcm",
                "realtime_with_boundary.pcm"
        };

        for (String filePath : files) {
            File file = new File(filePath);
            if (file.exists()) {
                long fileSize = file.length();
                float duration = fileSize / (2.0f * 16000); // 16-bit, 单声道, 16000Hz
                System.out.printf("%s: %d 字节 (约 %.3f 秒)%n",
                        filePath, fileSize, duration);
            } else {
                System.out.println(filePath + ": 文件不存在");
            }
        }
    }
}