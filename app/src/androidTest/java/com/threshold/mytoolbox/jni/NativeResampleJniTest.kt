package com.threshold.mytoolbox.jni

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class NativeResampleJniTest {

    @Test
    fun test16kto8k() {
        NativeResampleJni.init()

        val outputStream = BufferedOutputStream(
            FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Audio" + File.separator + "mono8k16bit.pcm"),
            1024 * 256
        )
        val inputStream = BufferedInputStream(
            FileInputStream(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Audio" + File.separator + "mic.pcm"),
            1024 * 256
        )
        val buffer = ByteArray(4096)
        var readSize: Int
        do {
            readSize = inputStream.read(buffer)
            val resampleBytes = NativeResampleJni.resample(buffer, readSize, 1, 16000, 8000, 1, 16)
            outputStream.write(resampleBytes)
        } while (readSize > 0)
        outputStream.close()
        inputStream.close()

        NativeResampleJni.deinit()
    }

}