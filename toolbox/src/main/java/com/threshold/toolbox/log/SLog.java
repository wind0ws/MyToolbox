package com.threshold.toolbox.log;

import static com.threshold.toolbox.log.LogPriority.*;

import androidx.annotation.NonNull;

/**
 * project name: MyToolBox
 * author: Administrator
 * describe: describe
 * time: 2019/6/6 16:00
 * change:
 */
@SuppressWarnings("unused")
public class SLog {

    private SLog() {
        throw new IllegalStateException("no instance");
    }

    private static ILog sILog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_DEFAULT,
            null, 1, new LogcatPrinter());

    /**
     * setup {@link SLog} with a {@link ILog}
     *
     * @param iLog a instance of {@link ILog}
     * @see LoggerFactory#create(int)
     * @see LoggerFactory#create(int, String)
     * @see LoggerFactory#create(int, String, int, Printer)
     */
    public static void setupImpl(@NonNull ILog iLog) {
        sILog = iLog;
    }

    /**
     * setup {@link SLog} with a {@link Printer}
     * <p>This method should only called once</p>
     *
     * @param printer the printer which to print log
     */
    public static void setupImpl(@NonNull Printer printer) {
        final ILog iLog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_WITH_TRACE,
                null, 1, printer);
        setupImpl(iLog);
    }

    /**
     * Set the tag for all output log
     *
     * @param defaultTag the tag
     * @return {@link ILog} instance
     */
    public static ILog setDefaultTag(final String defaultTag) {
        return sILog.setDefaultTag(defaultTag);
    }

    public static ILog v(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_VERBOSE, format, args);
    }

    public static ILog v(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_VERBOSE, tr, format, args);
    }

    public static ILog v(final String tag, Throwable tr, final String format, final Object... args) {
        return sILog.v(tag, tr, format, args);
    }

    /**
     * log debug message
     *
     * @param format message format
     * @param args   message args
     * @return {@link ILog} instance
     */
    public static ILog d(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_DEBUG, format, args);
    }

    /**
     * log debug message
     *
     * @param tr     throwable
     * @param format message format
     * @param args   message args
     * @return {@link ILog} instance
     */
    public static ILog d(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_DEBUG, tr, format, args);
    }

    /**
     * log debug message
     *
     * @param tag    the specific tag
     * @param tr     throwable
     * @param format message format
     * @param args   message args
     * @return {@link ILog} instance
     */
    public static ILog d(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.d(tag, tr, format, args);
    }

    public static ILog i(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_INFO, format, args);
    }

    public static ILog i(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_INFO, tr, format, args);
    }

    public static ILog i(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.i(tag, tr, format, args);
    }

    public static ILog w(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_WARN, format, args);
    }

    public static ILog w(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_WARN, tr, format, args);
    }

    public static ILog w(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.w(tag, tr, format, args);
    }

    public static ILog e(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_ERROR, format, args);
    }

    public static ILog e(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_ERROR, tr, format, args);
    }

    public static ILog e(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.e(tag, tr, format, args);
    }

    public static ILog wtf(final String format, final Object... args) {
        return logWithArgs(LOG_PRIORITY_ASSERT, format, args);
    }

    public static ILog wtf(final Throwable tr, final String format, final Object... args) {
        return logWithThrowable(LOG_PRIORITY_ASSERT, tr, format, args);
    }

    public static ILog wtf(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.wtf(tag, tr, format, args);
    }

    public static ILog obj(final Object obj) {
        return sILog.obj(obj);
    }

    public static ILog obj(final String tag, final Object obj) {
        return sILog.obj(tag, obj);
    }

    public static ILog json(final String json) {
        return sILog.json(json);
    }

    public static ILog json(final String tag, final String json) {
        return sILog.json(tag, json);
    }

    private static ILog logWithArgs(final int priority, String format, Object... args) {
        switch (priority) {
            case LOG_PRIORITY_VERBOSE:
                return sILog.v(format, args);
            case LOG_PRIORITY_DEBUG:
                return sILog.d(format, args);
            case LOG_PRIORITY_INFO:
                return sILog.i(format, args);
            case LOG_PRIORITY_WARN:
                return sILog.w(format, args);
            case LOG_PRIORITY_ERROR:
                return sILog.e(format, args);
            case LOG_PRIORITY_ASSERT:
                return sILog.wtf(format, args);
            default:
                return sILog;
        }
    }

    private static ILog logWithThrowable(final int priority, Throwable tr, String format, Object... args) {
        switch (priority) {
            case LOG_PRIORITY_VERBOSE:
                return sILog.v(tr, format, args);
            case LOG_PRIORITY_DEBUG:
                return sILog.d(tr, format, args);
            case LOG_PRIORITY_INFO:
                return sILog.i(tr, format, args);
            case LOG_PRIORITY_WARN:
                return sILog.w(tr, format, args);
            case LOG_PRIORITY_ERROR:
                return sILog.e(tr, format, args);
            case LOG_PRIORITY_ASSERT:
                return sILog.wtf(tr, format, args);
            default:
                return sILog;
        }
    }
}
