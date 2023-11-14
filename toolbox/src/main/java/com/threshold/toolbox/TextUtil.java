package com.threshold.toolbox;

import android.support.annotation.Nullable;

public class TextUtil {

    private TextUtil() { /* cannot be instantiated */ }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }
}
