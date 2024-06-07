package com.threshold.toolbox.log;

import android.support.annotation.NonNull;

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
     * init {@link SLog} with a {@link ILog}
     * @param iLog a instance of {@link ILog}
     *
     *  @see  LoggerFactory#create(int)
     *  @see  LoggerFactory#create(int, String)
     *  @see  LoggerFactory#create(int, String, int, Printer)
     */
    public static void init(@NonNull ILog iLog) {
        sILog = iLog;
    }

    /**
     * init {@link SLog} with a {@link Printer}
     * <p>This method should only called once</p>
     * @param printer the printer which to print log
     */
    public static void init(@NonNull Printer printer) {
        final ILog iLog = LoggerFactory.create(LoggerFactory.LOG_STRATEGY_WITH_TRACE,
                null, 1, printer);
        init(iLog);
    }

    /**
     * init {@link SLog} with a {@link LogcatPrinter}.
     * only needs to be initialized once throughout the entire process runtime.
     */
    public static void init() {
        init(new LogcatPrinter());
    }

    /**
     * Set the tag for all output log
     * @param defaultTag the tag
     * @return {@link ILog} instance
     */
    public static ILog setDefaultTag(final String defaultTag) {
        return sILog.setDefaultTag(defaultTag);
    }

    /**
     * log debug message
     * @param format message format
     * @param args message args
     * @return {@link ILog} instance
     */
    public static ILog d(final String format, final Object... args) {
        return sILog.d(format, args);
    }

    /**
     * log debug message
     * @param tr throwable
     * @param format message format
     * @param args message args
     * @return {@link ILog} instance
     */
    public static ILog d(final Throwable tr, final String format, final Object... args) {
        return sILog.d(tr, format, args);
    }

    /**
     * log debug message
     * @param tag the specific tag
     * @param tr throwable
     * @param format message format
     * @param args message args
     * @return {@link ILog} instance
     */
    public static ILog d(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.d(tag, tr, format, args);
    }

    public static ILog v(final String format, final Object... args) {
        return sILog.v(format, args);
    }

    public static ILog v(final Throwable tr, final String format, final Object... args) {
        return sILog.v(tr, format, args);
    }

    public static ILog v(final String tag, Throwable tr, final String format, final Object... args) {
        return sILog.v(tag, tr, format, args);
    }

    public static ILog i(final String format, final Object... args) {
        return sILog.i(format, args);
    }

    public static ILog i(final Throwable tr, final String format, final Object... args) {
        return sILog.i(tr, format, args);
    }

    public static ILog i(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.i(tag, tr, format, args);
    }

    public static ILog w(final String format, final Object... args) {
        return sILog.w(format, args);
    }

    public static ILog w(final Throwable tr, final String format, final Object... args) {
        return sILog.w(tr, format, args);
    }

    public static ILog w(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.w(tag, tr, format, args);
    }

    public static ILog e(final String format, final Object... args) {
        return sILog.e(format, args);
    }

    public static ILog e(final Throwable tr, final String format, final Object... args) {
        return sILog.e(tr, format, args);
    }

    public static ILog e(final String tag, final Throwable tr, final String format, final Object... args) {
        return sILog.e(tag, tr, format, args);
    }

    public static ILog wtf(final String format, final Object... args) {
        return sILog.wtf(format, args);
    }

    public static ILog wtf(final Throwable tr, final String format, final Object... args) {
        return sILog.wtf(tr, format, args);
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

}
