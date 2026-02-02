package com.threshold.toolbox.log;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 根据类名解析 Log 使用的 TAG。
 * <ul>
 *   <li>优先从类的 {@link LogTag} 注解或 TAG/LOG_TAG/tag 静态字段取值。</li>
 *   <li>匿名内部类、局部类、成员内部类：若自身无 TAG，会沿 {@link Class#getEnclosingClass()}
 *       向上查找外部类的注解/字段，因此只需在外部类上标注 {@link LogTag} 或定义 TAG 即可。</li>
 *   <li>若仍无 TAG，再沿 {@link Class#getSuperclass()} 在父类上查找。</li>
 * </ul>
 */
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

    private static void setFieldAccessible(Field field) {
        if (field == null) {
            return;
        }
        try {
            if (!field.isAccessible()) {
                // Java 9+: use setAccessible(boolean) which may be restricted
                try {
                    Method trySetAccessible = Field.class.getMethod("trySetAccessible");
                    Boolean ok = (Boolean) trySetAccessible.invoke(field);
                    if (Boolean.TRUE.equals(ok)) {
                        return;
                    }
                } catch (NoSuchMethodException ignored) {
                    // pre-Java 9
                }
                field.setAccessible(true);
            }
        } catch (Exception e) {
            if (enableDebugLog) {
                System.err.printf(Locale.US, "setFieldAccessible: %s%n", e.getMessage());
            }
        }
    }

    private static String searchTagFieldInClass(Class<?> targetClz) {
        for (String fieldName : TAG_FIELD_NAMES) {
            try {
                final Field field = targetClz.getDeclaredField(fieldName);
                if (!String.class.equals(field.getType())) {
                    continue;
                }

                setFieldAccessible(field);
                try {
                    Object value = field.get(null);
                    if (value instanceof String) {
                        return ((String) value).trim();
                    }
                } catch (Exception e) {
                    if (enableDebugLog) {
                        System.err.printf(Locale.US, "Field get error [%s]: %s%n",
                                fieldName, e.getMessage());
                    }
                }
            } catch (NoSuchFieldException ignored) {
                // Continue to next field
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

    /**
     * 在单个类上查找 TAG：先注解再字段。
     */
    private static String findTagInClass(Class<?> clz) {
        String tag = searchTagAnnotationInClass(clz);
        if (!tag.isEmpty()) {
            return tag;
        }
        return searchTagFieldInClass(clz);
    }

    /**
     * 沿 enclosing 链再沿 super 链遍历，在第一个有 TAG 的类上返回。
     * 这样匿名内部类、局部类会使用外部类的 LogTag/TAG，子类可使用父类的 TAG。
     */
    private static String findTagByWalkingEnclosingAndSuper(Class<?> startClz) {
        // 1) 当前类 + 所有 enclosing 类（外部类）
        for (Class<?> c = startClz; c != null; c = c.getEnclosingClass()) {
            String tag = findTagInClass(c);
            if (!tag.isEmpty()) {
                return tag;
            }
        }
        // 2) 父类链（仅从 startClz 的父类开始，当前及 enclosing 已查过）
        for (Class<?> c = startClz.getSuperclass(); c != null; c = c.getSuperclass()) {
            String tag = findTagInClass(c);
            if (!tag.isEmpty()) {
                return tag;
            }
        }
        return "";
    }

    public static String findTag(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }

        SoftReference<String> cachedRef = CLASS_TAG_CACHE.get(className);
        String cachedTag = (cachedRef != null) ? cachedRef.get() : null;
        if (cachedTag != null) {
            return cachedTag;
        }

        try {
            Class<?> targetClz = Class.forName(className);
            String tag = findTagByWalkingEnclosingAndSuper(targetClz);
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
