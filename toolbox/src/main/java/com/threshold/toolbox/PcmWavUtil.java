package com.threshold.toolbox;

@SuppressWarnings("unused")
public class PcmWavUtil {

    public static byte[] wavToPcm(final byte[] wavData) {
        if (wavData == null || wavData.length <= 44) {
            throw new IllegalStateException("no enough data convert to pcm. wavData.length=" +
                    (wavData == null ? "null" : String.valueOf(wavData.length)));
        }
        final int pcmLen = wavData.length - 44;
        final byte[] pcm = new byte[pcmLen];
        System.arraycopy(wavData, 44, pcm, 0, pcmLen);
        return pcm;
    }

    /**
     * pcm 转 wav
     *
     * @param pcmData      pcm原始数据
     * @param numChannels  声道设置, mono = 1, stereo = 2
     * @param sampleRate   采样频率, 例如16000
     * @param bitPerSample 采样精度, 例如16bits
     * @return wav数据
     */
    public static byte[] pcmToWav(byte[] pcmData, int numChannels, int sampleRate, int bitPerSample) {
        final byte[] wavData = new byte[pcmData.length + 44];
        final byte[] header = generateWavHeader(pcmData.length, numChannels, sampleRate, bitPerSample);
        System.arraycopy(header, 0, wavData, 0, header.length);
        System.arraycopy(pcmData, 0, wavData, header.length, pcmData.length);
        return wavData;
    }

    /**
     * @param pcmLen       pcm数据长度
     * @param numChannels  声道设置, mono = 1, stereo = 2
     * @param sampleRate   采样频率, 例如16000
     * @param bitPerSample 采样精度, 例如16bits
     * @return wav头部信息(总共44字节)
     */
    public static byte[] generateWavHeader(int pcmLen, int numChannels, int sampleRate, int bitPerSample) {
        final byte[] header = new byte[44];
        // ChunkID, RIFF, 占4bytes
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        // ChunkSize, pcmLen + 36, 占4bytes
        final long chunkSize = pcmLen + 36;
        header[4] = (byte) (chunkSize & 0xff);
        header[5] = (byte) ((chunkSize >> 8) & 0xff);
        header[6] = (byte) ((chunkSize >> 16) & 0xff);
        header[7] = (byte) ((chunkSize >> 24) & 0xff);
        // Format, WAVE, 占4bytes
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // Subchunk1ID, 'fmt ', 占4bytes
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // Subchunk1Size, 16, 占4bytes
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // AudioFormat, pcm = 1, 占2bytes
        header[20] = 1;
        header[21] = 0;
        // NumChannels, mono = 1, stereo = 2, 占2bytes
        header[22] = (byte) numChannels;
        header[23] = 0;
        // SampleRate, 占4bytes
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        // ByteRate = SampleRate * NumChannels * BitsPerSample / 8, 占4bytes
        final long byteRate = (long)sampleRate * numChannels * bitPerSample / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // BlockAlign = NumChannels * BitsPerSample / 8, 占2bytes
        header[32] = (byte) (numChannels * bitPerSample / 8);
        header[33] = 0;
        // BitsPerSample, 占2bytes
        header[34] = (byte) bitPerSample;
        header[35] = 0;
        // Subhunk2ID, data, 占4bytes
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        // Subchunk2Size, 占4bytes
        header[40] = (byte) (pcmLen & 0xff);
        header[41] = (byte) ((pcmLen >> 8) & 0xff);
        header[42] = (byte) ((pcmLen >> 16) & 0xff);
        header[43] = (byte) ((pcmLen >> 24) & 0xff);

        return header;
    }

}
