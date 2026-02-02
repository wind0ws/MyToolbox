package com.threshold.toolbox;

public class BitConverter {

    private BitConverter() {
        throw new IllegalStateException("no instance");
    }

    public static short toInt16(byte[] bytes, int offset) {
        short result = (short) ((int) bytes[offset] & 0xff);
        result |= (short) (((int) bytes[offset + 1] & 0xff) << 8);
        return (short) (result & 0xffff);
    }

    public static int toUInt16(byte[] bytes, int offset) {
        int result = (int) bytes[offset + 1] & 0xff;
        result |= ((int) bytes[offset] & 0xff) << 8;
        return result & 0xffff;
    }

    public static int toInt32(byte[] bytes, int offset) {
        int result = (int) bytes[offset] & 0xff;
        result |= ((int) bytes[offset + 1] & 0xff) << 8;
        result |= ((int) bytes[offset + 2] & 0xff) << 16;
        result |= ((int) bytes[offset + 3] & 0xff) << 24;
        return result;
    }

    public static long toUInt32(byte[] bytes, int offset) {
        long result = (int) bytes[offset] & 0xff;
        result |= ((long) bytes[offset + 1] & 0xff) << 8;
        result |= ((long) bytes[offset + 2] & 0xff) << 16;
        result |= ((long) bytes[offset + 3] & 0xff) << 24;
        return result & 0xFFFFFFFFL;
    }

    public static long toInt64(byte[] buffer, int offset) {
        long values = 0;
        for (int i = 0; i < 8; ++i) {
            values <<= 8;
            values |= (buffer[offset + i] & 0xFF);
        }
        return values;
    }

    public static long toUInt64(byte[] bytes, int offset) {
        long result = 0;
        for (int i = 0; i <= 56; i += 8) {
            result |= ((long) bytes[offset++] & 0xff) << i;
        }
        return result;
    }

    public static float toFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(toInt32(bytes, offset));
    }

    public static double toDouble(byte[] bytes, int offset) {
        return Double.longBitsToDouble(toUInt64(bytes, offset));
    }

    public static boolean toBoolean(byte[] bytes, int offset) {
        return bytes[offset] != 0x00;
    }

    public static void toBytes(short value, byte[] bytes, int offset) {
        //byte[] bytes = new byte[2];
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value & 0xff00) >> 8);
        //return bytes;
    }

    public static byte[] getBytes(short value) {
        byte[] bytes = new byte[2];
        toBytes(value, bytes, 0);
        return bytes;
    }

    public static void toBytes(int value, byte[] bytes, int offset) {
        //byte[] bytes = new byte[4];
        bytes[offset] = (byte) ((value) & 0xFF); //最低位
        bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24)); //最高位，无符号右移
        //return bytes;
    }

    public static byte[] getBytes(int value) {
        byte[] bytes = new byte[4];
        toBytes(value, bytes, 0);
        return bytes;
    }

    public static byte[] getBytes(long values) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    public static void toBytes(long value, byte[] bytes, int offset) {
        //byte[] bytes = new byte[8];
        for (int i = 0; i < 8; ++i) {
            int shift = 64 - (i + 1) * 8;
            bytes[i + offset] = (byte) ((value >> shift) & 0xff);
        }
        //return bytes;
    }

    public static byte[] getBytes(float value) {
        return getBytes(Float.floatToIntBits(value));
    }

    public static void toBytes(float value, byte[] bytes, int offset) {
        toBytes(Float.floatToIntBits(value), bytes, offset);
    }

    public static byte[] getBytes(double val) {
        return getBytes(Double.doubleToLongBits(val));
    }
    public static void toBytes(double val, byte[] bytes, int offset) {
        toBytes(Double.doubleToLongBits(val), bytes, offset);
    }

    public static byte[] getBytes(boolean value) {
        return new byte[]{(byte) (value ? 1 : 0)};
    }

    public static void toBytes(boolean value, byte[] bytes, int offset) {
        bytes[offset] = value ? (byte) 0x1 : (byte) 0x0;
    }

    public static byte intToByte(int x) {
        return (byte) x;
    }

    public static int byteToInt(byte b) {
        return b & 0xFF;
    }

    public static char toChar(byte[] bs, int offset) {
        return (char) (((bs[offset] & 0xFF) << 8) | (bs[offset + 1] & 0xFF));
    }

    public static byte[] getBytes(char value) {
        byte[] b = new byte[2];
        b[0] = (byte) ((value & 0xFF00) >> 8);
        b[1] = (byte) (value & 0xFF);
        return b;
    }

    public static byte[] concat(byte[]... bs) {
        int len = 0, idx = 0;
        for (byte[] b : bs) len += b.length;
        byte[] buffer = new byte[len];
        for (byte[] b : bs) {
            System.arraycopy(b, 0, buffer, idx, b.length);
            idx += b.length;
        }
        return buffer;
    }

}
