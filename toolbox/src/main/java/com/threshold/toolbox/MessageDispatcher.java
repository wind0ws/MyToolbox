package com.threshold.toolbox;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"unused"})
public class MessageDispatcher {

    private static final String LOG_TAG = "MessageDispatcher";
    private static volatile MessageDispatcher sDefaultInstance;

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

    private static class WeakEventListener {
        final WeakReference<EventListener> ref;
        final int tag; // For cleanup tracking

        WeakEventListener(int tag, EventListener listener) {
            this.tag = tag;
            this.ref = new WeakReference<>(listener);
        }

        boolean isValid() {
            return ref.get() != null;
        }
    }

    private final DispatcherHandler mDispatcherHandler;
    // Key: Event class, Value: Listeners interested in this event
    private final Map<Class<?>, CopyOnWriteArrayList<WeakEventListener>> mEventListeners = new HashMap<>();
    // Key: Listener tag, Value: Event classes registered by this listener
    private final Map<Integer, List<Class<?>>> mTagEvents = new HashMap<>();
    private final Object mLock = new Object();
    private volatile boolean mQuited = false;

    public MessageDispatcher(String name) {
        HandlerThread handlerThread = new HandlerThread("MsgDispatcher_" + name);
        handlerThread.start();
        mDispatcherHandler = new DispatcherHandler(handlerThread.getLooper());
    }

    public void post(final Object event) {
        if (event == null || mQuited) return;
        mDispatcherHandler.sendEventMsg(event);
    }

    public void clear() {
        if (mQuited) return;
        mDispatcherHandler.removeCallbacksAndMessages(null);
        synchronized (mLock) {
            mEventListeners.clear();
            mTagEvents.clear();
        }
    }

    public void quit() {
        if (mQuited) return;
        mQuited = true;
        mDispatcherHandler.sendQuitMsg();
        synchronized (mLock) {
            mEventListeners.clear();
            mTagEvents.clear();
        }
    }

    public boolean isRegistered(final int tag) {
        synchronized (mLock) {
            return mTagEvents.containsKey(tag);
        }
    }

    /**
     * 注册事件监听器
     * @param tag       唯一标识
     * @param listener  事件回调
     * @param events    监听的事件类型（支持父类监听）
     * @return 是否成功
     */
    public boolean register(final int tag, final EventListener listener, final Class<?>... events) {
        if (listener == null || events == null || events.length == 0 || mQuited) {
            return false;
        }

        synchronized (mLock) {
            // 清理已失效的监听器
            removeInvalidatedListeners();

            if (mTagEvents.containsKey(tag)) {
                Log.w(LOG_TAG, "Tag " + tag + " already registered");
                return false;
            }

            List<Class<?>> registeredEvents = new ArrayList<>(events.length);
            for (Class<?> event : events) {
                if (event == null) continue;

                CopyOnWriteArrayList<WeakEventListener> listeners = mEventListeners.get(event);
                if (listeners == null) {
                    listeners = new CopyOnWriteArrayList<>();
                    mEventListeners.put(event, listeners);
                }

                listeners.add(new WeakEventListener(tag, listener));
                registeredEvents.add(event);
            }

            if (!registeredEvents.isEmpty()) {
                mTagEvents.put(tag, registeredEvents);
                return true;
            }
            return false;
        }
    }

    public boolean unregister(final int tag) {
        if (mQuited) return false;

        synchronized (mLock) {
            List<Class<?>> events = mTagEvents.remove(tag);
            if (events == null) return false;

            for (Class<?> event : events) {
                CopyOnWriteArrayList<WeakEventListener> listeners = mEventListeners.get(event);
                if (listeners == null) continue;

                // 创建待删除列表（避免修改正在遍历的集合）
                List<WeakEventListener> toRemove = new ArrayList<>();
                for (WeakEventListener we : listeners) {
                    if (we.tag == tag) {
                        toRemove.add(we);
                    }
                }

                // 批量移除元素
                listeners.removeAll(toRemove);

                // 清理空列表
                if (listeners.isEmpty()) {
                    mEventListeners.remove(event);
                }
            }
            return true;
        }
    }

    private void removeInvalidatedListeners() {
        synchronized (mLock) {
            // 清理失效监听器 (mEventListeners)
            Iterator<Map.Entry<Class<?>, CopyOnWriteArrayList<WeakEventListener>>> eventIter =
                    mEventListeners.entrySet().iterator();

            while (eventIter.hasNext()) {
                Map.Entry<Class<?>, CopyOnWriteArrayList<WeakEventListener>> entry = eventIter.next();
                CopyOnWriteArrayList<WeakEventListener> listeners = entry.getValue();

                // 创建待删除列表
                List<WeakEventListener> toRemove = new ArrayList<>();
                for (WeakEventListener we : listeners) {
                    if (!we.isValid()) {
                        toRemove.add(we);
                    }
                }

                // 批量移除失效监听器
                if (!toRemove.isEmpty()) {
                    listeners.removeAll(toRemove);
                }

                // 清理空列表
                if (listeners.isEmpty()) {
                    eventIter.remove();
                }
            }

            // 清理失效标签 (mTagEvents)
            Iterator<Map.Entry<Integer, List<Class<?>>>> tagIter = mTagEvents.entrySet().iterator();
            while (tagIter.hasNext()) {
                Map.Entry<Integer, List<Class<?>>> entry = tagIter.next();
                int tag = entry.getKey();
                boolean hasValidListener = false;

                // 检查该tag是否还有有效监听器
                for (Class<?> event : entry.getValue()) {
                    CopyOnWriteArrayList<WeakEventListener> listeners = mEventListeners.get(event);
                    if (listeners != null) {
                        for (WeakEventListener we : listeners) {
                            if (we.tag == tag && we.isValid()) {
                                hasValidListener = true;
                                break;
                            }
                        }
                    }
                    if (hasValidListener) break;
                }

                if (!hasValidListener) {
                    tagIter.remove();
                }
            }
        }
    }

    private class DispatcherHandler extends Handler {
        private static final int MSG_POST_EVENT = 1;
        private static final int MSG_QUIT = 2;

        DispatcherHandler(Looper looper) {
            super(looper);
        }

        void sendEventMsg(final Object event) {
            if (mQuited) return;
            obtainMessage(MSG_POST_EVENT, event).sendToTarget();
        }

        void sendQuitMsg() {
            obtainMessage(MSG_QUIT).sendToTarget();
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == MSG_QUIT) {
                quitLooper();
                return;
            }

            if (msg.what != MSG_POST_EVENT || msg.obj == null) {
                Log.w(LOG_TAG, "Unhandled message: " + msg.what);
                return;
            }

            final Object event = msg.obj;
            final Class<?> eventClass = event.getClass();
            List<WeakEventListener> listenersToNotify = null;

            // 1. 获取精确匹配的监听器
            synchronized (mLock) {
                CopyOnWriteArrayList<WeakEventListener> listeners = mEventListeners.get(eventClass);
                if (listeners != null) {
                    listenersToNotify = new ArrayList<>(listeners);
                }
            }

            // 2. 获取父类事件监听器
            synchronized (mLock) {
                for (Map.Entry<Class<?>, CopyOnWriteArrayList<WeakEventListener>> entry :
                        mEventListeners.entrySet()) {

                    Class<?> listenerEventClass = entry.getKey();
                    if (listenerEventClass == eventClass ||
                            !listenerEventClass.isAssignableFrom(eventClass)) {
                        continue;
                    }

                    if (listenersToNotify == null) {
                        listenersToNotify = new ArrayList<>();
                    }
                    listenersToNotify.addAll(entry.getValue());
                }
            }

            // 3. 通知监听器
            if (listenersToNotify != null) {
                // 创建有效监听器列表（避免在通知过程中处理弱引用）
                List<EventListener> validListeners = new ArrayList<>();
                for (WeakEventListener weakRef : listenersToNotify) {
                    EventListener listener = weakRef.ref.get();
                    if (listener != null) {
                        validListeners.add(listener);
                    }
                }

                // 通知所有有效监听器
                for (EventListener listener : validListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception ex) {
                        Log.e(LOG_TAG, "Event dispatch error", ex);
                    }
                }
            }
        }

        private void quitLooper() {
            try {
                final Looper looper = getLooper();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    looper.quitSafely();
                } else {
                    looper.quit();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Looper quit error", e);
            }
        }
    }
}