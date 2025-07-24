package com.threshold.toolbox;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.threshold.toolbox.log.LogTag;
import com.threshold.toolbox.log.SLog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LogTag("MsgDispatcherTest")
public class MessageDispatcherTest implements MessageDispatcher.EventListener {

    private static final String TAG = MessageDispatcherTest.class.getSimpleName();

    @SuppressWarnings("all")
    public static class AEvent {
    }

    @SuppressWarnings("all")
    public static class BEvent {
    }

    @SuppressWarnings("all")
    public static class BaseEvent {
    }

    @SuppressWarnings("all")
    public static class DerivedEvent extends BaseEvent {
    }

    @Test
    public void testMsgDispatcher() {
        final MessageDispatcher dispatcher = MessageDispatcher.getDefault();

        boolean result = dispatcher.register(TAG.hashCode(), this,
                AEvent.class, BEvent.class, BaseEvent.class);
        Assert.assertTrue(result);

        dispatcher.post(new AEvent());
        dispatcher.post(new BEvent());
        dispatcher.post(new DerivedEvent());

        SystemClock.sleep(2000);
        result = dispatcher.unregister(TAG.hashCode());
        Assert.assertTrue(result);
    }

    @Override
    public void onEvent(final Object event) {
        SLog.d("receive event => " + event.toString());
        if (event instanceof AEvent) {
            SLog.d("it is AEvent");
        } else if (event instanceof BEvent) {
            SLog.d("it is BEvent");
        } else if (event instanceof BaseEvent) {
            SLog.d("it is BaseEvent(maybe child)");
        }
    }
}
