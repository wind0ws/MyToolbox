package com.threshold.toolbox.log;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import static com.threshold.toolbox.log.LogPriority.*;

public class LogcatLogger extends AbsLogger {

    private final Printer mLogPrinter;
    private final int mStackTraceOffset;

    public LogcatLogger(@Nullable final String logTag,
                        final int methodOffset, @NonNull final Printer printer) {
        super(logTag);
        this.mLogPrinter = printer;
        this.mStackTraceOffset = 5 + methodOffset;
    }

    /**
     * 在连续多帧栈上查找 TAG，避免在 Android 仪器测试等环境下因调用栈更深导致取到运行器类而非实际调用类。
     * 先尝试固定偏移的一帧，若无 TAG 则向后尝试若干帧，取第一个非空 TAG。
     */
    private static final int STACK_SEARCH_WINDOW = 5;

    @Override
    protected String currentLogTag() {
        String currentTag = super.currentLogTag();
        if (TextUtils.isEmpty(currentTag)) {
            final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 0; i < STACK_SEARCH_WINDOW; i++) {
                int index = mStackTraceOffset + i;
                if (index >= stack.length) {
                    break;
                }
                currentTag = TagFinder.findTag(stack[index]);
                if (!TextUtils.isEmpty(currentTag)) {
                    break;
                }
            }
            if (TextUtils.isEmpty(currentTag)) {
                currentTag = "NO_TAG";
            }
        }
        return currentTag;
    }

    @Override
    protected ILog log(final int methodOffset, @LogPriority final int logLevel,
                       final String tag, final Throwable tr,
                       final String format, final Object... args) {
        if (mLogPrinter.isPrintable(logLevel, tag)) {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            if (null != tr) {
                msg += String.format("\noccurred throwable:\n%s", Log.getStackTraceString(tr));
            }
            mLogPrinter.print(logLevel, tag, msg);
        }
        return this;
    }

    @SuppressLint("LogTagMismatch")
    @Override
    protected ILog logObj(final int methodOffset, final String tag, final Object object) {
        if (mLogPrinter.isPrintable(LOG_PRIORITY_DEBUG, tag)) {
            mLogPrinter.print(LOG_PRIORITY_DEBUG, tag,
                    String.format("object => %s", object == null ? "null" : object.toString()));
        }
        return this;
    }

    @SuppressLint("LogTagMismatch")
    @Override
    protected ILog logJson(final int methodOffset, final String tag, final String json) {
        if (mLogPrinter.isPrintable(LOG_PRIORITY_DEBUG, tag)) {
            mLogPrinter.print(LOG_PRIORITY_DEBUG, tag,
                    String.format("json => %s", json == null ? "null" : json));
        }
        return this;
    }
}
