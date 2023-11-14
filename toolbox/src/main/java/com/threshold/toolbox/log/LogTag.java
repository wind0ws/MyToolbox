package com.threshold.toolbox.log;

import java.lang.annotation.*;

/**
 * Annotation for LogTag if can't trace current method position
 *
 * in general, it works with SLog
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE
})
public @interface LogTag {
    String value();
}
