package com.threshold.toolbox.log;

import android.annotation.SuppressLint;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.threshold.toolbox.AsyncFileWriter;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.threshold.toolbox.log.LogPriority.LOG_PRIORITY_OFF;

@SuppressWarnings("unused")
public class SimpleDiskPrinter implements Printer {
    @SuppressWarnings("all")
    private static final Charset sDefaultCharset = Charset.forName("UTF-8");
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private final boolean mIsAlsoOutputParent;
    private final Printer mParentPrinter;
    private final AsyncFileWriter mFileWriter;
    private final int mMyPid;

    /**
     * Create a disk printer: log it to file
     *
     * @param toSaveLogFile      the destination of log file
     * @param logBufferSize      the buffer size of flush log to file
     * @param parent             a parent printer. could be null.
     * @param isAlsoOutputParent used to determine whether the parent printer is also need print
     */
    public SimpleDiskPrinter(@NonNull final File toSaveLogFile, final int logBufferSize,
                             @Nullable final Printer parent, final boolean isAlsoOutputParent) {
        this.mFileWriter = new AsyncFileWriter(toSaveLogFile, logBufferSize);
        this.mIsAlsoOutputParent = isAlsoOutputParent;
        this.mParentPrinter = parent;
        this.mMyPid = Process.myPid();
    }

    /**
     * Create a disk printer: log it to file
     *
     * @param saveFile the destination of log file
     */
    public SimpleDiskPrinter(@NonNull final File saveFile) {
        this(saveFile, 0, null, false);
    }

    @Override
    public boolean isPrintable(final int priority, @Nullable final String tag) {
        return (mIsAlsoOutputParent && mParentPrinter != null) ?
                mParentPrinter.isPrintable(priority, tag) :
                (LoggerConfig.sCurrentLogPriority != LOG_PRIORITY_OFF && priority >= LoggerConfig.sCurrentLogPriority);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void print(final int priority, @Nullable final String tag, @NonNull final String message) {
        if (mIsAlsoOutputParent && mParentPrinter != null) {
            mParentPrinter.print(priority, tag, message);
        }
        mFileWriter.write(String.format("%s %d-%d/? %s/%s: %s\n", sDateFormat.format(new Date()),
                        mMyPid, Process.myTid(), priorityString(priority), tag, message)
                .getBytes(sDefaultCharset));
    }

    private static String priorityString(final int priority) {
        switch (priority) {
            case LogPriority.LOG_PRIORITY_VERBOSE:
                return "V";
            case LogPriority.LOG_PRIORITY_DEBUG:
                return "D";
            case LogPriority.LOG_PRIORITY_INFO:
                return "I";
            case LogPriority.LOG_PRIORITY_WARN:
                return "W";
            case LogPriority.LOG_PRIORITY_ERROR:
                return "E";
            case LogPriority.LOG_PRIORITY_ASSERT:
                return "WTF";
            case LogPriority.LOG_PRIORITY_OFF:
            default:
                return "U";
        }
    }
}
