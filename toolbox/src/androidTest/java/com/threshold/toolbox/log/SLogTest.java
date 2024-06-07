package com.threshold.toolbox.log;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;

/**
 * project name: MyToolBox
 * author: Administrator
 * describe: describe
 * time: 2019/6/6 16:12
 * change:
 */
@LogTag("SLogTest")
@RunWith(AndroidJUnit4.class)
public class SLogTest {

//    private static final String TAG = "SLogTag";

    @Before
    public void setup(){
        SLog.init();
    }

    @Test
    public void test(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                SLog.e("Log in another thread: %d", Thread.currentThread().getId());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SLog.w("Log in another thread too: %d", Thread.currentThread().getId());
                    }
                }).start();
            }
        }).start();
        SLog.d("Hello %s", "World");
        SLog.i("ManualTag", null, "This is %s", "manual tag");
        SLog.json("{\"key2\":\"value2\"}");
        SLog.obj(Arrays.asList("called", "log", "object"));
        SystemClock.sleep(20);
    }




}
