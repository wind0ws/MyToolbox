package com.threshold.toolbox.log;

/**
 * log facade: a log entity should implement this
 */
public interface ILog {

    /**
     * if no tag passed in log function, use this default tag instead.
     * if this default tag is null or empty, it will generate a tag for you.
     *
     * @param defaultTag default tag
     * @return this instance
     */
    ILog setDefaultTag(final String defaultTag);

    ILog d(final String format, final Object... args);

    ILog d(final Throwable tr, final String format, final Object... args);

    ILog d(final String tag, final Throwable tr, final String format, final Object... args);

    ILog v(final String format, final Object... args);

    ILog v(final Throwable tr, final String format, final Object... args);

    ILog v(final String tag, Throwable tr, final String format, final Object... args);

    ILog i(final String format, final Object... args);

    ILog i(final Throwable tr, final String format, final Object... args);

    ILog i(final String tag, final Throwable tr, final String format, final Object... args);

    ILog w(final String format, final Object... args);

    ILog w(final Throwable tr, final String format, final Object... args);

    ILog w(final String tag, final Throwable tr, final String format, final Object... args);

    ILog e(final String format, final Object... args);

    ILog e(final Throwable tr, final String format, final Object... args);

    ILog e(final String tag, final Throwable tr, final String format, final Object... args);

    ILog wtf(final String format, final Object... args);

    ILog wtf(final Throwable tr, final String format, final Object... args);

    ILog wtf(final String tag, final Throwable tr, final String format, final Object... args);

    ILog obj(final Object obj);

    ILog obj(final String tag, final Object obj);

    ILog json(final String json);

    ILog json(final String tag, final String json);

}
