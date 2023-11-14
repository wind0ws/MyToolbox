package com.threshold.toolbox;

import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ToastUtilTest {

    @Test
    public void testToast() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        for (int i = 0; i < 10; i++) {
            if (i < 5) {
                ToastUtil.showShort(appContext, "Show " + i);
            } else {
                ToastUtil.showShortImmediately(appContext, "ShowImmediately " + i);
            }

            SystemClock.sleep(1000);
        }
    }

}
