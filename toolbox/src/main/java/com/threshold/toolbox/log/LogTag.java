package com.threshold.toolbox.log;

import java.lang.annotation.*;

/**
 * Annotation for LogTag if can't trace current method position
 * <p>
 * in general, it works with SLog and LLog(if can't generate TAG by StackTrace)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE
})
public @interface LogTag {
    String value();
}
