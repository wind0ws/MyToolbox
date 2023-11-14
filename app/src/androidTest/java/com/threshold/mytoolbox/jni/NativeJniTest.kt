package com.threshold.mytoolbox.jni

import android.support.test.runner.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import com.threshold.toolbox.HexUtil
import com.threshold.toolbox.log.llog.LLog
import java.nio.charset.Charset

@RunWith(AndroidJUnit4::class)
class NativeJniTest {

    companion object {
        @BeforeClass
        fun setUp(){
            NativeLibJni.setLoggable(true)
        }
    }

    @Test
    fun test(){
        NativeLibJni.setLoggable(true);
    }

    @Test
    fun testString() {
        val string = "Hello.你好呀"
        val utf8Bytes = string.toByteArray(Charset.forName("UTF-8"))
        val javaEncode = HexUtil.encode(utf8Bytes)
        LLog.d("utf8Bytes => %s", javaEncode)
        val utfBytesFromNative = NativeLibJni.stringBytesFromJni(string)
        val nativeEncode = HexUtil.encode(utfBytesFromNative)
        LLog.d("nativeUtfBytes=>%s",nativeEncode)
        assertEquals("java utf8bytes not equals native bytes",javaEncode,nativeEncode)
    }

}