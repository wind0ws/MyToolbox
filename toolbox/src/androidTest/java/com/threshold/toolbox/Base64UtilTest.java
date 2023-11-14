package com.threshold.toolbox;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Base64UtilTest {

    private static final String TAG = Base64UtilTest.class.getSimpleName();

    @Test
    public void testEncodeDecode() {

        String origin = "{main:{\"key1\"=\"value1\"},sub:\"abc\"}";
        String originEncode = "e21haW46eyJrZXkxIj0idmFsdWUxIn0sc3ViOiJhYmMifQ==";
        String encode = Base64Util.encodeString(origin);
        Log.d(TAG, encode);
        Assert.assertEquals("encode Not equals",originEncode,encode);
        String decode = Base64Util.decodeString(originEncode);
        Log.d(TAG, decode);
        Assert.assertEquals("decode not equals",origin,decode);

    }

}
