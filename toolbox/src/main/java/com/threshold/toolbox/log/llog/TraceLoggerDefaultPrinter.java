package com.threshold.toolbox.log.llog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.threshold.toolbox.log.LogcatPrinter;

class TraceLoggerDefaultPrinter extends LogcatPrinter {

    @Override
    public boolean isPrintable(final int priority, @Nullable final String tag) {
        return LLog.Config.allowPrint;
    }

    @Override
    public void print(int priority, @Nullable final String tag, @NonNull final String message) {
        if (LLog.Config.forceLevelE) {
            priority = StackTraceLogger.ERROR;
        }
        super.print(priority, tag, message);
    }
}
