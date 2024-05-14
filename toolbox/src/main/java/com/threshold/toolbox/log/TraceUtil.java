package com.threshold.toolbox.log;

import java.lang.reflect.Field;

public class TraceUtil {

    /**
     * get currentThread StackTraceElement
     */
    public static StackTraceElement getStackTrace() {
        return Thread.currentThread().getStackTrace()[4];
    }

    public static StackTraceElement getStackTrace(final int offset) {
        final StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
//        int findOffset = -1;
//        for (int i = 0; i < stackTraces.length; i++) {
//            Log.d("StackTraceLogger", String.format("stack[%d]=%s", i, stackTraces[i]));
//            if (stackTraces[i].getClassName().equals(TraceUtil.class.getName())){
//                findOffset = i;
////                break;
//            }
//        }
//        Log.i("StackTraceLogger", "offset=" + findOffset);
        return stackTraces.length > offset ? stackTraces[offset] : stackTraces[stackTraces.length - 1];
    }

    private final static String[] BASIC_TYPES = {"int", "java.lang.String", "boolean", "char",
            "float", "double", "long", "short", "byte"};

    /**
     * convert object to string
     *
     * @param object object
     * @return string of object
     */
    public static <T> String objectToString(T object) {
        if (object == null) {
            return "Object{obj is null}";
        }
        final String obj2Str = object.toString();
        if (!obj2Str.startsWith(object.getClass().getName() + "@")) {
            return obj2Str;
        }

        final StringBuilder builder = new StringBuilder(512);
        builder.append(object.getClass().getSimpleName()).append(" { ");
        final Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            boolean flag = false;
            for (final String type : BASIC_TYPES) {
                if (!field.getType().getName().equalsIgnoreCase(type)) {
                    continue;
                }
                flag = true;
                Object value = null;
                try {
                    value = field.get(object);
                } catch (IllegalAccessException e) {
                    value = e;
                } finally {
                    builder.append(String.format("%s=%s, ", field.getName(),
                            null == value ? "null" : value.toString()));
                }
                break;
            }
            if (!flag) {
                builder.append(String.format("%s=%s, ", field.getName(), "Object"));
            }
        }
        return builder.replace(builder.length() - 2, builder.length() - 1, " }").toString();
    }

}