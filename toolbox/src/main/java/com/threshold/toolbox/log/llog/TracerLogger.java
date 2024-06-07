package com.threshold.toolbox.log.llog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.threshold.toolbox.log.*;

import static com.threshold.toolbox.log.LogPriority.*;

public class TracerLogger extends AbsLogger {

    private final StackTraceLogger mStackTraceLogger;
    private final int mStackTraceOffset;

    /**
     * Constructor for TraceLogger
     *
     * @param defaultLogTag if this is empty(null), use generate log tag instead
     * @param methodOffset  "methodOffset + 5" for getStackTrace.
     */
    public TracerLogger(@Nullable final String defaultLogTag, final int methodOffset, @NonNull final Printer printer) {
        super(defaultLogTag);
        mStackTraceOffset = 5 + methodOffset;
        mStackTraceLogger = new StackTraceLogger(printer);
    }

    @Override
    protected ILog log(final int methodOffset, @LogPriority final int logLevel, final String tag,
                       final Throwable tr, final String format, final Object... args) {
        if (!mStackTraceLogger.isPrintable(logLevel, tag)) {
            return this;
        }
        final boolean hasTag = !TextUtils.isEmpty(tag);
        String msg = (null == args || args.length == 0) ? format : String.format(format, args);
        if (null != tr) {
            msg = String.format("%s\n occurred throwable:\n%s", msg, Log.getStackTraceString(tr));
        }
        switch (logLevel) {
            case LOG_PRIORITY_VERBOSE:
                if (hasTag) {
                    mStackTraceLogger.vWithTag(tag, msg);
                } else {
                    mStackTraceLogger.v(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_DEBUG:
                if (hasTag) {
                    mStackTraceLogger.dWithTag(tag, msg);
                } else {
                    mStackTraceLogger.d(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_INFO:
                if (hasTag) {
                    mStackTraceLogger.iWithTag(tag, msg);
                } else {
                    mStackTraceLogger.i(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_WARN:
                if (hasTag) {
                    mStackTraceLogger.wWithTag(tag, msg);
                } else {
                    mStackTraceLogger.w(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_ERROR:
                if (hasTag) {
                    mStackTraceLogger.eWithTag(tag, msg);
                } else {
                    mStackTraceLogger.e(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_ASSERT:
                if (hasTag) {
                    mStackTraceLogger.aWithTag(tag, msg);
                } else {
                    mStackTraceLogger.a(TraceUtil.getStackTrace(mStackTraceOffset + methodOffset), msg);
                }
                break;
            case LOG_PRIORITY_OFF:
            default:
                //nothing to do
                break;
        }
        return this;
    }

    @Override
    protected ILog logObj(final int methodOffset, final String tag, final Object object) {
        if (mStackTraceLogger.isPrintable(LOG_PRIORITY_DEBUG, tag)) {
            final boolean hasTag = !TextUtils.isEmpty(tag);
            if (hasTag) {
                mStackTraceLogger.objWithTag(tag, object);
            } else {
                mStackTraceLogger.obj(TraceUtil.getStackTrace(mStackTraceOffset), object);
            }
        }
        return this;
    }

    @Override
    protected ILog logJson(final int methodOffset, final String tag, final String json) {
        if (mStackTraceLogger.isPrintable(LOG_PRIORITY_DEBUG, tag)) {
            final boolean hasTag = !TextUtils.isEmpty(tag);
            if (hasTag) {
                mStackTraceLogger.jsonWithTag(tag, json);
            } else {
                mStackTraceLogger.json(TraceUtil.getStackTrace(mStackTraceOffset), json);
            }
        }
        return this;
    }

}
