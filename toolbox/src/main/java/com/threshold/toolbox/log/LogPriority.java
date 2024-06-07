package com.threshold.toolbox.log;

import androidx.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.*;

import static com.threshold.toolbox.log.LogPriority.*;

@Documented
@IntDef({
        LOG_PRIORITY_OFF,
        LOG_PRIORITY_VERBOSE, LOG_PRIORITY_DEBUG, LOG_PRIORITY_INFO,
        LOG_PRIORITY_WARN, LOG_PRIORITY_ERROR, LOG_PRIORITY_ASSERT
})
@Target({
        ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD
})
@Retention(RetentionPolicy.SOURCE)
public @interface LogPriority {
    int LOG_PRIORITY_OFF = -1;
    int LOG_PRIORITY_VERBOSE = Log.VERBOSE;
    int LOG_PRIORITY_DEBUG = Log.DEBUG;
    int LOG_PRIORITY_INFO = Log.INFO;
    int LOG_PRIORITY_WARN = Log.WARN;
    int LOG_PRIORITY_ERROR = Log.ERROR;
    int LOG_PRIORITY_ASSERT = Log.ASSERT;
}
