package com.threshold.toolbox;

import org.junit.Test;

import java.io.File;

public class PcmWavUtilTest {

    @Test
    public void testPcmToWav(){
        final byte[] pcmContent = FileUtil.readFileContent(new File("D:\\Temp\\rc_1553649490926.pcm"));
        assert pcmContent != null;
        final byte[] wavAudio = PcmWavUtil.pcmToWav(pcmContent, 1, 16000, 16);
        FileUtil.writeToFile(new File("D:\\Temp\\123.wav"), wavAudio, wavAudio.length);
    }

}
