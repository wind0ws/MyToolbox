package com.threshold.toolbox

import org.junit.Test
import java.io.File

class HexUtilTest {

    @Test
    fun testStringBuilder() {
        val stringBuilder = StringBuilder()
        stringBuilder.append("AB CD ")
        stringBuilder.delete(stringBuilder.length - 1, stringBuilder.length)
        println("$stringBuilder,  length is=${stringBuilder.length}")
    }

    @Test
    fun testEncode() {
//        val toEncodeBytes = ByteArray(16) {
//            return@ByteArray 0x01
//        }
        val toEncodeBytes = "你好像没有说话?".toByteArray()
        val encoded = HexUtil.encode(toEncodeBytes, " ")
        println("encoded=$encoded")
    }

    @Test
    fun testDecode() {
        val toDecode = "01 02 03 04 05 06 07 08 09"
        val decodeBytes = HexUtil.decode(toDecode, " ")
        println(decodeBytes)
    }

    @Test
    fun testDecodeFromFile() {
        val hexStr = FileUtil.readAllAsUTF8String(File("E:\\工作文档\\hex.txt"))
        println(hexStr)
        val decodeBytes = HexUtil.decode(hexStr, " ")
        FileUtil.writeToFile(File("E:\\工作文档\\tts.ico"), decodeBytes, decodeBytes.size)
    }

}