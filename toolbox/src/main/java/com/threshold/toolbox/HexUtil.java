package com.threshold.toolbox;

public class HexUtil {

    private HexUtil(){
        throw new IllegalStateException("no instance");
    }

    /**
     * 字节流转成十六进制表示
     */
    public static String encode(byte[] src) {
        return encode(src, null);
    }

    public static String encode(byte[] src, String delimiter) {
        if (src == null || src.length == 0) {
            throw new IllegalArgumentException("bad argument \"src\". It should be not empty.");
        }
        boolean shouldAppendDelimiter = delimiter != null && !delimiter.isEmpty();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : src) {
            String strHex = Integer.toHexString(aByte & 0xFF);
            // 每个字节由两个字符表示，位数不够，高位补0
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
            if (shouldAppendDelimiter) {
                sb.append(delimiter);
            }
        }
        //remove last redundancy delimiter
        if (shouldAppendDelimiter) {
            sb.delete(sb.length() - delimiter.length(), sb.length());
        }
        return sb.toString().trim();
    }

    /**
     * 字符串转成字节流
     */
    public static byte[] decode(String src) {
        int m, n;
        int byteLen = src.length() / 2; // 每两个字符描述一个字节
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + src.substring(i * 2, m) + src.substring(m, n));
            ret[i] = (byte) intVal;
        }
        return ret;
    }

    public static byte[] decode(String src, String delimiter) {
        if (src == null || src.length() < 2) {
            throw new IllegalArgumentException("bad argument \"src\". It length should bigger than 2");
        }
        boolean hasDelimiter = delimiter != null && !delimiter.isEmpty();
        if (hasDelimiter) {
            src = src.replace(delimiter, "");
        }
        return decode(src);
    }
}
