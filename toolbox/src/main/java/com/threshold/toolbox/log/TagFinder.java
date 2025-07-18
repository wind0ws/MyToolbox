package com.threshold.toolbox.log;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class TagFinder {

    private static final String[] TAG_FIELD_NAMES = {"TAG", "LOG_TAG", "tag"};
    private static final int DEFAULT_MAX_CACHE_SIZE = 512;

    // 线程安全缓存（软引用+LRU）
    private static final Map<String, SoftReference<String>> CLASS_TAG_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, SoftReference<String>>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SoftReference<String>> eldest) {
                    return size() > maxCacheSize;
                }
            });

    private static volatile int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
    private static volatile boolean enableDebugLog = false;

    private TagFinder() {
        throw new IllegalStateException("No instance allowed");
    }

    public static void setDebugEnabled(boolean enabled) {
        enableDebugLog = enabled;
    }

    public static void setMaxCacheSize(int maxSize) {
        maxCacheSize = Math.max(16, maxSize);
    }

    private static String searchTagFieldInClass(Class<?> targetClz) {
        for (String fieldName : TAG_FIELD_NAMES) {
            try {
                final Field field = targetClz.getDeclaredField(fieldName);
                if (!String.class.equals(field.getType())) {
                    continue;
                }

                boolean wasAccessible = field.isAccessible();
                try {
                    if (!wasAccessible) {
                        field.setAccessible(true);
                    }

                    Object value = field.get(null);
                    if (value instanceof String) {
                        return ((String) value).trim();
                    }
                } finally {
                    if (!wasAccessible) {
                        try {
                            field.setAccessible(false);
                        } catch (SecurityException e) {
                            if (enableDebugLog) {
                                System.err.printf("Reset accessibility failed: %s%n", e.getMessage());
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException ignored) {
                // Continue to next field
            } catch (Exception e) {
                if (enableDebugLog) {
                    System.err.printf(Locale.US, "Field access error [%s]: %s%n",
                            fieldName, e.getMessage());
                }
            }
        }
        return "";
    }

    private static String searchTagAnnotationInClass(Class<?> targetClz) {
        try {
            LogTag annotation = targetClz.getAnnotation(LogTag.class);
            if (annotation != null) {
                String value = annotation.value().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            if (enableDebugLog) {
                System.err.printf(Locale.US, "Annotation error: %s%n", e.getMessage());
            }
        }
        return "";
    }

    public static String findTag(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }

        // 尝试从缓存获取
        SoftReference<String> cachedRef = CLASS_TAG_CACHE.get(className);
        String cachedTag = (cachedRef != null) ? cachedRef.get() : null;
        if (cachedTag != null) {
            return cachedTag;
        }

        try {
            Class<?> targetClz = Class.forName(className);

            // 优先检查注解
            String tag = searchTagAnnotationInClass(targetClz);
            if (!tag.isEmpty()) {
                cacheTag(className, tag);
                return tag;
            }

            // 其次检查字段
            tag = searchTagFieldInClass(targetClz);
            if (!tag.isEmpty()) {
                cacheTag(className, tag);
                return tag;
            }
        } catch (ClassNotFoundException e) {
            if (enableDebugLog) {
                System.err.printf("Class not found: %s%n", className);
            }
        } catch (Exception e) {
            if (enableDebugLog) {
                System.err.printf("Unexpected error: %s%n", e.getMessage());
            }
        }

        // 缓存空结果
        cacheTag(className, "");
        return "";
    }

    public static String findTag(StackTraceElement stackTraceElement) {
        return (stackTraceElement != null) ?
                findTag(stackTraceElement.getClassName()) : "";
    }

    private static void cacheTag(String className, String tag) {
        CLASS_TAG_CACHE.put(className, new SoftReference<>(tag));
    }

    public static void clearCache() {
        CLASS_TAG_CACHE.clear();
    }
}
