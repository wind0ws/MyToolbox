package com.threshold.toolbox.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * printer facade: work with {@link ILog}
 */
public interface Printer {

    /**
     * Used to determine whether log should be printed out or not.
     *
     * @param priority is the log level e.g. DEBUG, WARNING
     * @param tag is the given tag for the log message
     *
     * @return is used to determine if log should printed.
     *         If it is true, it will be printed, otherwise it'll be ignored.
     */
    boolean isPrintable(@LogPriority int priority, @Nullable String tag);

    /**
     * This is invoked by Logger each time a log message is processed.
     * Interpret this method as last destination of the log in whole pipeline.
     *
     * @param priority is the log level e.g. DEBUG, WARNING
     * @param tag is the given tag for the log message.
     * @param message is the given message for the log message.
     *
     */
    void print(@LogPriority int priority, @Nullable String tag, @NonNull String message);

}
