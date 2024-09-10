package com.threshold.toolbox.log;

import com.threshold.toolbox.TextUtil;

import java.lang.reflect.Field;

/** @noinspection CallToPrintStackTrace*/
public class TagFinder {

    private TagFinder() {
        throw new IllegalStateException("no instance");
    }

    private static String searchTagFiledInClass(final Class<?> targetClz) {
        Field tagField = null;
        try {
            tagField = targetClz.getDeclaredField("TAG");
        } catch (NoSuchFieldException e0) {
            try {
                tagField = targetClz.getDeclaredField("LOG_TAG");
            } catch (NoSuchFieldException e1) {
                try {
                    tagField = targetClz.getDeclaredField("tag");
                } catch (NoSuchFieldException e2) {
                    //ignore
                }
            }
        }
        if (null != tagField) {
            try {
                tagField.setAccessible(true);
                final Object tagFieldValue = tagField.get(null);
                return tagFieldValue == null ? "" : tagFieldValue.toString();
            } catch (Exception ex) {
                //ignore
            }
        }
        return "";
    }

    private static String searchTagAnnotationInClass(final Class<?> targetClz) {
        if (!targetClz.isAnnotationPresent(LogTag.class)) {
            return "";
        }
        try {
            final LogTag logTagAnnotation = targetClz.getAnnotation(LogTag.class);
            return null == logTagAnnotation ? "" : logTagAnnotation.value();
        } catch (Exception e) {
            //ignore
        }
        return "";
    }

    /**
     * find TAG from class annotation.
     * if fail, fallback find it from class field.
     * @param className the class name
     * @return tag
     */
    public static String searchTagInClass(final String className) {
        try {
            final Class<?> targetClz = Class.forName(className);
            String tag = searchTagAnnotationInClass(targetClz);
            if (TextUtil.isEmpty(tag)) {
                tag = searchTagFiledInClass(targetClz);
            }
            return tag;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
