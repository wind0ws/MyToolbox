package com.threshold.toolbox;

public class HexUtil {

    private HexUtil() {
        throw new IllegalStateException("no instance");
    }

    /**
     * 字节数组转成十六进制表示
     */
    public static String encode(byte[] src) {
        return encode(src, "");
    }

    public static String encode(byte[] src, String delimiter) {
        if (null == src || 0 == src.length || null == delimiter) {
            throw new IllegalArgumentException("bad argument \"src\". It should be not empty.");
        }
        final boolean shouldAppendDelimiter = !TextUtil.isEmpty(delimiter);
        final StringBuilder sb = new StringBuilder(src.length * (2 + delimiter.length()));
        for (byte aByte : src) {
            final String strHex = Integer.toHexString(aByte & 0xFF);
            // 每个字节由两个字符表示，位数不够，高位补0
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
            if (shouldAppendDelimiter) {
                sb.append(delimiter);
            }
        }
        // remove last redundancy delimiter
        if (shouldAppendDelimiter) {
            sb.delete(sb.length() - delimiter.length(), sb.length());
        }
        return sb.toString().trim();
    }

    /**
     * 连续的16进制字符串转成字节数组
     */
    public static byte[] decode(String src) {
        int m, n;
        final int byteLen = src.length() / 2; // 每两个字符描述一个字节
        final byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            final int intVal = Integer.decode("0x" + src.substring(i * 2, m) + src.substring(m, n));
            ret[i] = (byte) intVal;
        }
        return ret;
    }

    public static byte[] decode(String src, String delimiter) {
        if (null == src || src.length() < 2) {
            throw new IllegalArgumentException("bad argument \"src\". It length shouldn't smaller than 2");
        }
        final boolean hasDelimiter = !TextUtil.isEmpty(delimiter);
        if (hasDelimiter) {
            src = src.replace(delimiter, "");
        }
        return decode(src);
    }
}
