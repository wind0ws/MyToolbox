package com.threshold.toolbox;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

/** @noinspection CharsetObjectCanBeUsed*/
public class Base64Util {

    private static final String UTF8 = "UTF-8";

    private Base64Util() {
        throw new IllegalStateException("no instance");
    }

    public static String encode(final byte[] data) {
        final byte[] encode = Base64.encode(data, Base64.NO_WRAP);
        try {
            return new String(encode, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeString(final String data) {
        try {
            return encode(data.getBytes(UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decode(final String encode) {
        return Base64.decode(encode, Base64.NO_WRAP);
    }

    public static String decodeString(final String encode) {
        try {
            return new String(decode(encode), UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
