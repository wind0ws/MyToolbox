package com.threshold.jni;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;

import com.threshold.toolbox.log.llog.LLog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MsgQueueHandlerJniTest {

    private final long[] mHandleHolder = new long[]{0};

    @Test
    public void test() {
        int ret;
        final MsgQueueHandlerJni.MsgQueueHandlerParam queueHandlerParam =
                new MsgQueueHandlerJni.MsgQueueHandlerParam(8192,
                        new MsgQueueHandlerJni.OnReceiveMsgListener() {
                    @Override
                    public int handleMsg(final int what, final int arg1, final int arg2,
                                         final byte[] obj, final int objLen) {
                        LLog.i("received event: what=%d, arg1=%d, arg2=%d, obj_len=%d, my_id=%d",
                                what, arg1, arg2, objLen, (int)(obj[arg2]));
                        return 0;
                    }
                });
        ret = MsgQueueHandlerJni.init(mHandleHolder, queueHandlerParam);
        LLog.d("init ret=%d, handle=%d", ret, mHandleHolder[0]);
        Assert.assertEquals(ret, 0);

        final MsgQueueHandlerJni.MsgQueueData data = new MsgQueueHandlerJni.MsgQueueData();
        data.what = 1;
        data.arg1 = 2;
        data.arg2 = 3;
        data.obj = new byte[1024];
        for (int i = 0; i < data.obj.length; i++) {
            data.obj[i] = (byte) i;

            data.arg2 = i;
            ret = MsgQueueHandlerJni.feedMsg(mHandleHolder[0],
                    data.what, data.arg1, data.arg2, data.obj, data.obj.length);
            LLog.d("feed msg ret=%d", ret);
            Assert.assertEquals(ret, 0);
//            SystemClock.sleep(100);
        }
        SystemClock.sleep(1000);

        ret = MsgQueueHandlerJni.destroy(mHandleHolder);
        LLog.d("destroy ret=%d", ret);
        Assert.assertEquals(ret, 0);

        LLog.i("bye bye... %d", ret);
    }

}
