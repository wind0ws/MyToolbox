package com.threshold.toolbox.log;

import java.lang.reflect.Field;

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
        if (targetClz.isAnnotationPresent(LogTag.class)) {
            try {
                final LogTag logTagAnnotation = targetClz.getAnnotation(LogTag.class);
                return null == logTagAnnotation ? "" : logTagAnnotation.value();
            } catch (Exception e) {
                //ignore
            }
        }
        return "";
    }

    public static String searchTagInClass(final String className) {
        try {
            final Class<?> targetClz = Class.forName(className);
            String tag = searchTagAnnotationInClass(targetClz);
            if ("".equals(tag)) {
                tag = searchTagFiledInClass(targetClz);
            }
            return tag;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
