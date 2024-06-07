package com.threshold.toolbox;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@LogTag("Base64UtilTest")
@RunWith(AndroidJUnit4.class)
public class Base64UtilTest {

    @Test
    public void testEncodeDecode() {

        String origin = "{main:{\"key1\"=\"value1\"},sub:\"abc\"}";
        String originEncode = "e21haW46eyJrZXkxIj0idmFsdWUxIn0sc3ViOiJhYmMifQ==";
        String encode = Base64Util.encodeString(origin);
        SLog.d("encode: %s", encode);
        Assert.assertEquals("encode Not equals",originEncode,encode);
        String decode = Base64Util.decodeString(originEncode);
        SLog.d("decode: %s", decode);
        Assert.assertEquals("decode not equals",origin,decode);

    }

}
