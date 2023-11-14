package com.threshold.toolbox;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import java.lang.ref.WeakReference;

@SuppressWarnings("unused")
public class MessageDispatcher {

    private static final String LOG_TAG = "MessageDispatcher";

    private static volatile MessageDispatcher sDefaultInstance = null;

    public static MessageDispatcher getDefault() {
        if (sDefaultInstance == null) {
            synchronized (MessageDispatcher.class) {
                if (sDefaultInstance == null) {
                    sDefaultInstance = new MessageDispatcher("default");
                }
            }
        }
        return sDefaultInstance;
    }

    public interface EventListener {
        void onEvent(final Object event);
    }

    @SuppressWarnings("rawtypes")
    private static class ListenerInfo {
        WeakReference<EventListener> mEventListener;
        Class[] mListenEventClz;

        ListenerInfo(final EventListener eventListener, final Class... listenEventClz) {
            this.mListenEventClz = listenEventClz;
            this.mEventListener = new WeakReference<>(eventListener);
        }
    }

    private final DispatcherHandler mDispatcherHandler;
    private final SparseArray<ListenerInfo> mListeners = new SparseArray<>();
    private final byte[] mListenersLock = new byte[0];

    public MessageDispatcher(String name) {
        final HandlerThread handlerThread = new HandlerThread("hdl_" + name);
        handlerThread.start();
        mDispatcherHandler = new DispatcherHandler(handlerThread.getLooper());
    }

    /**
     * post event
     * @param event event obj
     */
    public void post(final Object event) {
        if (event == null) {
            return;
        }
        mDispatcherHandler.sendEventMsg(event);
    }

    public void clear() {
        mDispatcherHandler.removeCallbacksAndMessages(null);
    }

    public void quit() {
        mDispatcherHandler.sendQuitMsg();
    }

    public boolean isRegistered(final int tag) {
        synchronized (mListenersLock) {
            return mListeners.get(tag) != null;
        }
    }

    /**
     * register callback to handle event type
     * @param tag your id, which unique, this mark for listener
     * @param listener callback, to handle event
     * @param listenEvents event type you want to handle
     * @return true for succeed
     */
    @SuppressWarnings("rawtypes")
    public boolean register(final int tag, final EventListener listener, Class... listenEvents) {
        synchronized (mListenersLock) {
            if (mListeners.get(tag) != null) {
                return false;
            }
            mListeners.put(tag, new ListenerInfo(listener, listenEvents));
            return true;
        }
    }

    /**
     * unregister listener
     * @param tag unique id on register
     * @return true for succeed
     */
    public boolean unregister(final int tag) {
        synchronized (mListenersLock) {
            final boolean isRegistered = mListeners.get(tag) != null;
            if (isRegistered) {
                mListeners.remove(tag);
            }
            return isRegistered;
        }
    }

    private class DispatcherHandler extends Handler {

        private static final int MSG_WHAT_POST_EVENT = 1;
        private static final int MSG_WHAT_QUIT = 2;

        private boolean mQuited = false;
        DispatcherHandler(final Looper looper) {
            super(looper);
        }

        void sendEventMsg(final Object event) {
            if (mQuited) {
                return;
            }
            obtainMessage(MSG_WHAT_POST_EVENT, event).sendToTarget();
        }

        void sendQuitMsg() {
            if (mQuited) {
                return;
            }
            mQuited = true;
            obtainMessage(MSG_WHAT_QUIT).sendToTarget();
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == MSG_WHAT_QUIT) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        getLooper().quitSafely();
                    } else {
                        getLooper().quit();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
            if (msg.what != MSG_WHAT_POST_EVENT) {
                Log.e(LOG_TAG, "unhandled msg.what=" + msg.what);
                return;
            }
            final Object event = msg.obj;
            if (event == null) {
                return;
            }
            final String eventClzName = event.getClass().getName();
            synchronized (mListenersLock) {
                final int listenerSize = mListeners.size();
                for (int i = 0; i < listenerSize; i++) {
                    final ListenerInfo listenerInfo = mListeners.valueAt(i);
                    if (listenerInfo == null) {
                        continue;
                    }
                    EventListener eventListener;
                    // noinspection rawtypes
                    for (final Class listenEventClz : listenerInfo.mListenEventClz) {
                        if (!eventClzName.equals(listenEventClz.getName()) ||
                                (eventListener = listenerInfo.mEventListener.get()) == null) {
                            continue;
                        }
                        try {
                            eventListener.onEvent(event);
                        } catch (Exception ex) {
                            Log.e(LOG_TAG, "error on dispatch event", ex);
                        }
                    }
                }
            }
//            super.handleMessage(msg);
        }
    }

}
