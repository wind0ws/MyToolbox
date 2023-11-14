package com.threshold.toolbox;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.threshold.toolbox.log.llog.LLog;

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
        LLog.d("receive event => " + event.toString());
        if (event instanceof AEvent) {
            LLog.d("it is AEvent");
        } else if (event instanceof BEvent) {
            LLog.d("it is BEvent");
        }
    }
}
