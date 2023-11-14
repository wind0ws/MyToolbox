package com.threshold.toolbox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import java.io.Serializable;

/**
 * 线程安全的Toast工具
 *
 * 帮你把Toast调用转移到主线程上运行
 */
public class ToastUtil {

    @SuppressLint("ShowToast")
    private static class MyToastHandler extends Handler {

        private static class ToastGravity implements Serializable {
            int gravity, xoffset, yoffset;

            ToastGravity(final int gravity, final int xoffset, final int yoffset) {
                this.gravity = gravity;
                this.xoffset = xoffset;
                this.yoffset = yoffset;
            }
        }

        private static class ToastInfo implements Serializable {
            CharSequence text;
            int duration;
            ToastGravity gravity;
        }

        private static final int MSG_WHAT_TOAST = 1;
        private static final int MSG_WHAT_TOAST_IMMEDIATELY = 2;

        private final Context mContext;
        private Toast mLastNormalToast = null;
        private Toast mImmediatelyToast;

        private MyToastHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
            //坑： Toast实例都不可以在两个线程中创建，否则后面调用setText等方法都会报错
//            mImmediatelyToast = Toast.makeText(context.getApplicationContext(), "", Toast.LENGTH_SHORT);
        }

        void toast(CharSequence text, int duration) {
            sendToastMsg(false, text, duration, -1, -1, -1);
        }

        void toast(CharSequence text, int duration, int gravity, int gravityXOffset, int gravityYOffset) {
            sendToastMsg(false, text, duration, gravity, gravityXOffset, gravityYOffset);
        }

        void toastImmediately(CharSequence text, int duration) {
            sendToastMsg(true, text, duration, -1, -1, -1);
        }

        void toastImmediately(CharSequence text, int duration, int gravity, int gravityXOffset, int gravityYOffset) {
            sendToastMsg(true, text, duration, gravity, gravityXOffset, gravityYOffset);
        }

        private void sendToastMsg(boolean isImmediately, CharSequence text, int duration, int gravity, int gravityXOffset, int gravityYOffset) {
            final ToastInfo toastInfo = new ToastInfo();
            toastInfo.text = text;
            toastInfo.duration = duration;
            if (gravity != -1) {
                toastInfo.gravity = new ToastGravity(gravity, gravityXOffset, gravityYOffset);
            }
            if (isImmediately) {
                removeMessages(MSG_WHAT_TOAST);
            }
            obtainMessage(isImmediately ? MSG_WHAT_TOAST_IMMEDIATELY : MSG_WHAT_TOAST, toastInfo).sendToTarget();
        }

        private void showToast(Toast toast, ToastInfo toastInfo) {
            if (toast == null) {
                toast = Toast.makeText(mContext, toastInfo.text, toastInfo.duration);
            } else {
                toast.setText(toastInfo.text);
                toast.setDuration(toastInfo.duration);
            }
            if (toastInfo.gravity != null) {
                toast.setGravity(toastInfo.gravity.gravity, toastInfo.gravity.xoffset, toastInfo.gravity.yoffset);
            }
            toast.show();
            if (toast != mImmediatelyToast) {
                mLastNormalToast = toast;
            }
        }

        @Override
        public void handleMessage(final Message msg) {
            final ToastInfo toastInfo = (ToastInfo) msg.obj;
            if (toastInfo == null) {
                return;
            }
            switch (msg.what) {
                case MSG_WHAT_TOAST:
                    showToast(null, toastInfo);
                    break;
                case MSG_WHAT_TOAST_IMMEDIATELY:
                    if (mLastNormalToast != null) {
                        mLastNormalToast.cancel();
                        mLastNormalToast = null;
                    }
                    if (null == mImmediatelyToast) {
                        mImmediatelyToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
                    }
                    showToast(mImmediatelyToast, toastInfo);
                    break;
                default:
                    Log.e("ToastUtil", "unhandled msg.what=" + msg.what);
                    break;
            }
        }
    }


    private static volatile ToastUtil sToastUtil;

    private final MyToastHandler mToastHandler;

    private ToastUtil(@NonNull Context context) {
        HandlerThread toastHandlerThread = new HandlerThread("ToastUtilHandler");
        toastHandlerThread.start();
        mToastHandler = new MyToastHandler(context, toastHandlerThread.getLooper());
    }

    public static void showShort(@NonNull Context context, @NonNull CharSequence msg) {
        show(context, msg, Toast.LENGTH_SHORT);
    }

    public static void showShortImmediately(@NonNull Context context, @NonNull CharSequence msg,
                                            int gravity, int gravityXOffset, int gravityYOffset) {
        showImmediately(context, msg, Toast.LENGTH_SHORT, gravity, gravityXOffset, gravityYOffset);
    }

    public static void showShortImmediately(@NonNull Context context, @NonNull CharSequence msg) {
        showShortImmediately(context, msg, -1, -1, -1);
    }

    public static void showShort(@NonNull Context context, @StringRes int resId) {
        show(context, context.getResources().getString(resId), Toast.LENGTH_SHORT);
    }

    public static void showShortImmediately(@NonNull Context context, @StringRes int resId,
                                            int gravity, int gravityXOffset, int gravityYOffset) {
        showImmediately(context, context.getResources().getString(resId), Toast.LENGTH_SHORT,
                gravity, gravityXOffset, gravityYOffset);
    }

    public static void showShortImmediately(@NonNull Context context, @StringRes int resId) {
        showShortImmediately(context, resId, -1, -1, -1);
    }

    public static void showLong(@NonNull Context context, @NonNull CharSequence msg) {
        show(context, msg, Toast.LENGTH_LONG);
    }

    public static void showLongImmediately(@NonNull Context context, @NonNull CharSequence msg,
                                           int gravity, int gravityXOffset, int gravityYOffset) {
        showImmediately(context, msg, Toast.LENGTH_LONG, gravity, gravityXOffset, gravityYOffset);
    }

    public static void showLongImmediately(@NonNull Context context, @NonNull CharSequence msg) {
        showLongImmediately(context, msg, -1, -1, -1);
    }

    public static void showLong(@NonNull Context context, @StringRes int resId) {
        show(context, context.getResources().getString(resId), Toast.LENGTH_LONG);
    }

    public static void showLongImmediately(@NonNull Context context, @StringRes int resId,
                                           int gravity, int gravityXOffset, int gravityYOffset) {
        showImmediately(context, context.getResources().getString(resId), Toast.LENGTH_LONG,
                gravity, gravityXOffset, gravityYOffset);
    }

    public static void showLongImmediately(@NonNull Context context, @StringRes int resId) {
        showLongImmediately(context, resId, -1, -1, -1);
    }

    public static void show(@NonNull Context context, @StringRes int resId, int duration) {
        show(context, context.getResources().getString(resId), duration);
    }

    public static void show(@NonNull Context context, @NonNull CharSequence msg, int duration,
                            int gravity, int gravityXOffset, int gravityYOffset) {
        checkInstance(context);
        sToastUtil.mToastHandler.toast(msg, duration, gravity, gravityXOffset, gravityYOffset);
    }

    public static void show(@NonNull Context context, @NonNull CharSequence msg, int duration) {
        show(context, msg, duration, -1, -1, -1);
    }

    public static void showImmediately(@NonNull Context context, @NonNull CharSequence msg, int duration,
                                       int gravity, int gravityXOffset, int gravityYOffset) {
        checkInstance(context);
        sToastUtil.mToastHandler.toastImmediately(msg, duration, gravity, gravityXOffset, gravityYOffset);
    }

    public static void showImmediately(@NonNull Context context, @NonNull CharSequence msg, int duration) {
        showImmediately(context, msg, duration, -1, -1, -1);
    }

    private static void checkInstance(@NonNull Context context) {
        if (sToastUtil != null) {
            return;
        }
        synchronized (ToastUtil.class) {
            if (sToastUtil == null) {
                sToastUtil = new ToastUtil(context.getApplicationContext());
            }
        }
    }


}
