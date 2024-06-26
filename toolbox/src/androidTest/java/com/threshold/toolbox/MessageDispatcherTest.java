package com.threshold.toolbox;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.threshold.toolbox.log.SLog;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class MessageDispatcherTest implements MessageDispatcher.EventListener {

    private static final String TAG = MessageDispatcherTest.class.getSimpleName();

    @SuppressWarnings("all")
    public static class AEvent {
    }

    @SuppressWarnings("all")
    public static class BEvent {
    }

    @Test
    public void test() {
        final MessageDispatcher dispatcher = MessageDispatcher.getDefault();

        dispatcher.register(TAG.hashCode(), this, AEvent.class, BEvent.class);

        dispatcher.post(new AEvent());
        dispatcher.post(new BEvent());

        SystemClock.sleep(2000);
        dispatcher.unregister(TAG.hashCode());
    }

    @Override
    public void onEvent(final Object event) {
        SLog.d("receive event => " + event.toString());
        if (event instanceof AEvent) {
            SLog.d("it is AEvent");
        } else if (event instanceof BEvent) {
            SLog.d("it is BEvent");
        }
    }
}
