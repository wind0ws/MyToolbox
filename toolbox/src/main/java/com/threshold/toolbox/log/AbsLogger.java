package com.threshold.toolbox.log;

import androidx.annotation.Nullable;

import static com.threshold.toolbox.log.LogPriority.*;

public abstract class AbsLogger implements ILog {

    private final ThreadLocal<String> mDefaultLogTag = new ThreadLocal<>();

    public AbsLogger(@Nullable final String defaultLogTag) {
        this.mDefaultLogTag.set(defaultLogTag);
    }

    @Override
    public ILog setDefaultTag(final String defaultTag) {
        mDefaultLogTag.set(defaultTag);
        return this;
    }

    protected String currentLogTag() {
        return mDefaultLogTag.get();
    }

    protected abstract ILog log(int methodOffset, int logPriority, String tag, @Nullable Throwable tr, String format, Object... args);

    protected abstract ILog logObj(final int methodOffset, final String tag, final Object obj);

    protected abstract ILog logJson(final int methodOffset, final String tag, final String json);

    @Override
    public ILog d(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_DEBUG, currentLogTag(), null, format, args);
    }

    @Override
    public ILog d(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_DEBUG, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog d(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_DEBUG, tag, tr, format, args);
    }

    @Override
    public ILog v(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_VERBOSE, tag, tr, format, args);
    }

    @Override
    public ILog v(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_VERBOSE, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog v(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_VERBOSE, currentLogTag(), null, format, args);
    }

    @Override
    public ILog i(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_INFO, tag, tr, format, args);
    }

    @Override
    public ILog i(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_INFO, currentLogTag(), null, format, args);
    }

    @Override
    public ILog i(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_INFO, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog w(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_WARN, tag, tr, format, args);
    }

    @Override
    public ILog w(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_WARN, currentLogTag(), null, format, args);
    }

    @Override
    public ILog w(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_WARN, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog e(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ERROR, tag, tr, format, args);
    }

    @Override
    public ILog e(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ERROR, currentLogTag(), null, format, args);
    }

    @Override
    public ILog e(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ERROR, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog wtf(final String tag, final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ASSERT, tag, tr, format, args);
    }

    @Override
    public ILog wtf(final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ASSERT, currentLogTag(), null, format, args);
    }

    @Override
    public ILog wtf(final Throwable tr, final String format, final Object... args) {
        return log(0, LOG_PRIORITY_ASSERT, currentLogTag(), tr, format, args);
    }

    @Override
    public ILog obj(final String tag, final Object obj) {
        return logObj(0, tag, obj);
    }

    @Override
    public ILog obj(final Object obj) {
        return logObj(0, currentLogTag(), obj);
    }

    @Override
    public ILog json(final String tag, final String json) {
        return logJson(0, tag, json);
    }

    @Override
    public ILog json(final String json) {
        return logJson(0, currentLogTag(), json);
    }

}

