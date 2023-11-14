package com.threshold.toolbox;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

public class Base64Util {

    private Base64Util() {
        throw new IllegalStateException("no instance");
    }

    public static String encode(final byte[] data) {
        final byte[] encode = Base64.encode(data, Base64.NO_WRAP);
        try {
            return new String(encode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String encodeString(final String data) {
        try {
            return encode(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] decode(final String encode) {
        return Base64.decode(encode, Base64.NO_WRAP);
    }

    public static String decodeString(final String encode) {
        try {
            return new String(decode(encode), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

}
