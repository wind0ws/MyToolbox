package com.threshold.toolbox.log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import static com.threshold.toolbox.log.LogPriority.*;

public class LogcatPrinter implements Printer {

    /**
     * default constructor for logcat printer
     */
    public LogcatPrinter() {
    }

    @Override
    public boolean isPrintable(final int priority, @Nullable final String tag) {
        return (LOG_PRIORITY_OFF != LoggerConfig.sCurrentLogPriority) &&
                (priority >= LoggerConfig.sCurrentLogPriority);
    }

    @Override
    public void print(final int priority, @Nullable final String tag, @NonNull final String message) {
        if (!isPrintable(priority, tag)) {
            return;
        }
        switch (priority) {
            case LOG_PRIORITY_VERBOSE:
                Log.v(tag, message);
                break;
            case LOG_PRIORITY_DEBUG:
                Log.d(tag, message);
                break;
            case LOG_PRIORITY_INFO:
                Log.i(tag, message);
                break;
            case LOG_PRIORITY_WARN:
                Log.w(tag, message);
                break;
            case LOG_PRIORITY_ERROR:
                Log.e(tag, message);
                break;
            case LOG_PRIORITY_ASSERT:
                Log.wtf(tag, message);
                break;
            case LOG_PRIORITY_OFF:
            default:
                //unhandled log level
                break;
        }
    }
}
