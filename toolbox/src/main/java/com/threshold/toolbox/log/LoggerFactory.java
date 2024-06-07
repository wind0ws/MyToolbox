package com.threshold.toolbox.log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.threshold.toolbox.log.llog.TracerLogger;

import java.lang.annotation.*;

/**
 * A ILog factory, for generate {@link LogcatLogger} / {@link TracerLogger}
 */
public class LoggerFactory {

    public static final int LOG_STRATEGY_DEFAULT = 1;
    public static final int LOG_STRATEGY_WITH_TRACE = 2;

    @Documented
    @IntDef({LOG_STRATEGY_WITH_TRACE, LOG_STRATEGY_DEFAULT})
    @Target({
            ElementType.PARAMETER,
            ElementType.FIELD,
            ElementType.METHOD
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogStrategy {
    }

    /**
     * create ILog impl.
     * @param logStrategy the log strategy, determine {@link ILog} impl.
     * @param defaultLogTag the tag for override all log tag, can be NULL.
     * @param methodOffset the method offset for find call stack.
     * @param printer the log printer
     * @return {@link ILog} impl.
     */
    @NonNull
    public static ILog create(@LogStrategy int logStrategy, @Nullable final String defaultLogTag,
                              final int methodOffset, @NonNull final Printer printer) {
        ILog iLog;
        if (logStrategy == LOG_STRATEGY_WITH_TRACE) {
            iLog = new TracerLogger(defaultLogTag, methodOffset, printer);
        } else if (logStrategy == LOG_STRATEGY_DEFAULT) {
            iLog = new LogcatLogger(defaultLogTag, methodOffset, printer);
        } else {
            throw new IllegalArgumentException("not adapt this logStrategy=" + logStrategy);
        }
        return iLog;
    }

    @NonNull
    public static ILog create(@LogStrategy int logMode, @Nullable final String defaultLogTag) {
        return create(logMode, defaultLogTag, 0, new LogcatPrinter());
    }

    @NonNull
    public static ILog create(@LogStrategy int logMode) {
        return create(logMode, null);
    }


}
