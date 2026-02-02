package com.threshold.jni;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(AndroidJUnit4.class)
@LogTag("MsgQTest")
public class MsgQueueHandlerJniTest {

    @BeforeClass
    public static void setup() {
        SLog.d("hi");
    }

    @Test
    public void test() {
        int ret;
        final Random random = new Random(SystemClock.currentThreadTimeMillis());
        final MsgQueueHandlerJni.Helper msgQueueHelper = getMsgQueueHelper(random);
        final MsgQueueHandlerJni.Message msg = new MsgQueueHandlerJni.Message(1, 2, 3, 2048);
        Assert.assertNotNull(msg.obj);
        for (int i = 0; i < msg.obj.length; ++i) {
            // generate mock msg.
            msg.objLen = i;
            if (i > 0) {
                msg.arg2 = i - 1;
                msg.obj[i - 1] = (byte) i; // <-- push this on last obj byte array item content.
            }

            while (MsgQueueHandlerJni.Helper.CODE_ERROR_FULL == (ret = msgQueueHelper.pushMsg(msg))) {
                SLog.w("produce msg too fast, maybe we should slower, later we will try again!");
                SystemClock.sleep(100);
            }

            if (0 != ret) {
                SLog.e("failed(%d) on feedMsg, the msg is lost!", ret);
            }
            Assert.assertEquals(ret, 0);
//            SystemClock.sleep(100);
        }
        SystemClock.sleep(1000);

        msgQueueHelper.close();
        SLog.d("close(destroy) done");

        SLog.i("bye bye...");
    }

    private static MsgQueueHandlerJni.Helper getMsgQueueHelper(final Random random) {
        final MsgQueueHandlerJni.MsgQueueHandlerParam queueHandlerParam =
                new MsgQueueHandlerJni.MsgQueueHandlerParam(8192,
                        new MsgQueueHandlerJni.OnReceiveMsgListener() {
                            @Override
                            public int handleMsg(final int what, final int arg1, final int arg2,
                                                 final byte[] obj, final int objLen) {
                                // mock doing heavy task
                                SystemClock.sleep(5 + random.nextInt(10));
                                SLog.i("received event: what=%d, arg1=%d, arg2=%d, obj_len=%d. " +
                                                " last_byte_in_obj=%d",
                                        what, arg1, arg2, objLen,
                                        (null == obj || objLen <= arg2) ? -9999 : (int) (obj[arg2]));
                                return 0;
                            }
                        });

        return new MsgQueueHandlerJni.Helper(queueHandlerParam);
    }

}
