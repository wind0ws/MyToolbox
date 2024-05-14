package com.threshold.toolbox.log.llog;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.threshold.toolbox.log.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
class StackTraceLogger implements StackTraceLog,Printer {

    private static final int VERBOSE = LogPriority.LOG_PRIORITY_VERBOSE;
    private static final int DEBUG = LogPriority.LOG_PRIORITY_DEBUG;
    private static final int INFO = LogPriority.LOG_PRIORITY_INFO;
    private static final int WARN = LogPriority.LOG_PRIORITY_WARN;
    /*private*/ static final int ERROR = LogPriority.LOG_PRIORITY_ERROR;
    private static final int ASSERT = LogPriority.LOG_PRIORITY_ASSERT;
    private static final int JSON = ASSERT + 1;
    private static final int OBJECT = JSON + 1;

    private static final char TOP_LEFT_CORNER = '╔';
    private static final char BOTTOM_LEFT_CORNER = '╚';
    private static final char MIDDLE_CORNER = '╟';
    private static final char HORIZONTAL_DOUBLE_LINE = '║';
    private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
    private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

    private final TagFinderCache mTagFinderCache = new TagFinderCache();
    private final Printer mLogPrinter;
    private final String LINE_SEPARATOR;

    StackTraceLogger(final Printer printer) {
        this.mLogPrinter = printer;
        final String lineSeparator = System.getProperty("line.separator");
        LINE_SEPARATOR = (lineSeparator == null ? "\n" : lineSeparator);
    }

    @Override
    public void dWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, DEBUG, message, args);
    }

    @Override
    public void vWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, VERBOSE, message, args);
    }

    @Override
    public void aWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, ASSERT, message, args);
    }

    @Override
    public void iWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, INFO, message, args);
    }

    @Override
    public void eWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, ERROR, message, args);
    }

    @Override
    public void wWithTag(String tag, String message, Object... args) {
        internalPrintLog(tag, WARN, message, args);
    }

    @Override
    public void jsonWithTag(String tag, String json) {
        internalPrintJson(tag, null, json);
    }

    @Override
    public void objWithTag(String tag, Object obj) {
        internalPrintObject(tag, null, obj);
    }

    @Override
    public void d(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, DEBUG, message, args);
    }

    @Override
    public void v(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, VERBOSE, message, args);
    }

    @Override
    public void a(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, ASSERT, message, args);
    }

    @Override
    public void i(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, INFO, message, args);
    }

    @Override
    public void e(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, ERROR, message, args);
    }

    @Override
    public void w(StackTraceElement element, String message, Object... args) {
        internalPrintLog(element, WARN, message, args);
    }

    @Override
    public void json(StackTraceElement element, String message) {
        internalPrintJson(element, message);
    }

    @Override
    public void obj(StackTraceElement element, Object obj) {
        internalPrintObject(element, obj);
    }

    @Override
    public boolean isPrintable(int priority, @Nullable String tag) {
        return mLogPrinter.isPrintable(DEBUG, tag);
    }

    @Override
    public void print(final int priority, @Nullable final String tag,
                      @NonNull final String message) {
        mLogPrinter.print(priority, tag, message);
    }

    private void internalPrintJson(StackTraceElement element, String json) {
        final String[] values = generateTagAndFileName(element);
        final String tag = values[0];
        final String fileName = values[1];
        internalPrintJson(fileName, tag, json);
    }

    private void internalPrintJson(final String myTag,
                                   final String methodCallPosition, String json) {
        if (TextUtils.isEmpty(json)) {
            internalPrintLog(methodCallPosition, DEBUG, "JSON{json is empty}");
            return;
        }
        final String tag = getRealTag(myTag, methodCallPosition);
        if (!isPrintable(DEBUG, tag)) {
            return;
        }
        try {
            json = json.trim();
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                json = jsonObject.toString(4);
            } else if (json.startsWith("[")) {
                JSONArray array = new JSONArray(json);
                json = array.toString(4);
            }
            final String[] lines = json.split(LINE_SEPARATOR);
            final StringBuilder contentBuilder = new StringBuilder(
                    20 + json.length() + (TOP_BORDER.length() + 5) * 3
                            + lines.length + tag.length());
            contentBuilder.append("json ==>").append(LINE_SEPARATOR)
                    .append(TOP_BORDER).append(LINE_SEPARATOR);
            if (!TextUtils.isEmpty(methodCallPosition)) {
                contentBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                        .append(methodCallPosition).append(LINE_SEPARATOR)
                        .append(MIDDLE_BORDER).append(LINE_SEPARATOR);
            }
            for (final String line : lines) {
                contentBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                        .append(line).append(LINE_SEPARATOR);
            }
            contentBuilder.append(BOTTOM_BORDER);
            internalPrintLog(tag, DEBUG, contentBuilder.toString());
        } catch (JSONException e) {
            internalPrintLog(tag, ERROR,
                    "print json occurred error:%s\nto print json=%s",
                    e.getMessage(), json);
        }
    }

    private void internalPrintObject(StackTraceElement element, Object obj) {
        if (obj == null) {
            internalPrintLog(element, ERROR, "obj == null");
            return;
        }

        final String[] values = generateTagAndFileName(element);
        final String tag = values[0];
        final String fileName = values[1];
        internalPrintObject(fileName, tag, obj);
    }

    @SuppressLint("DefaultLocale")
    private void internalPrintObject(final String myTag,
                                     final String methodCallPosition, final Object obj) {
        final String tag = getRealTag(myTag, methodCallPosition);
        if (!mLogPrinter.isPrintable(DEBUG, tag)) {
            return;
        }
        if (obj == null) {
            internalPrintLog(tag, ERROR, "obj == null");
            return;
        }
        final String simpleName = obj.getClass().getSimpleName();

        if (obj instanceof String) {
            internalPrintLog(methodCallPosition, DEBUG, obj.toString());
        } else if (obj instanceof Collection) {
            @SuppressWarnings("rawtypes")
            final Collection collection = (Collection) obj;
            String msg = " %s size = %d \n%s {\n";
            msg = String.format(msg, simpleName, collection.size(), HORIZONTAL_DOUBLE_LINE);
            if (!collection.isEmpty()) {
                final StringBuilder stringBuilder = new StringBuilder(256);
                stringBuilder.append("collection =>").append(LINE_SEPARATOR)
                        .append(TOP_BORDER).append(LINE_SEPARATOR);
                if (!TextUtils.isEmpty(methodCallPosition)) {
                    stringBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                            .append(methodCallPosition).append(LINE_SEPARATOR)
                            .append(MIDDLE_BORDER).append(LINE_SEPARATOR);
                }
                stringBuilder.append(HORIZONTAL_DOUBLE_LINE).append(msg);
                //noinspection all
                final Iterator<Object> iterator = collection.iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    final String itemString = "%s  [%d]:%s%s";
                    final Object item = iterator.next();
                    stringBuilder.append(String.format(itemString, HORIZONTAL_DOUBLE_LINE, index,
                            TraceUtil.objectToString(item),
                            index++ < collection.size() - 1 ? ",\n" : "\n"));
                }
                stringBuilder.append(HORIZONTAL_DOUBLE_LINE + " }\n").append(BOTTOM_BORDER);

                internalPrintLog(tag, DEBUG, stringBuilder.toString());
            } else {
                internalPrintLog(tag, ERROR, msg + " and collection is empty ]");
            }
        } else if (obj instanceof Map) {
            //noinspection all
            final Map<Object, Object> map = (Map<Object, Object>) obj;
            final Set<Object> keys = map.keySet();
            if (keys.size() > 0) {
                final StringBuilder stringBuilder = new StringBuilder(256);
                stringBuilder.append("map =>").append(LINE_SEPARATOR)
                        .append(TOP_BORDER).append(LINE_SEPARATOR);
                if (!TextUtils.isEmpty(methodCallPosition)) {
                    stringBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                            .append(methodCallPosition).append(LINE_SEPARATOR)
                            .append(MIDDLE_BORDER).append(LINE_SEPARATOR);
                }
                stringBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                        .append(simpleName).append(" {").append(LINE_SEPARATOR);

                for (final Object key : keys) {
                    stringBuilder.append(HORIZONTAL_DOUBLE_LINE).append(" ")
                            .append(String.format("[%s -> %s]\n",
                                    TraceUtil.objectToString(key),
                                    TraceUtil.objectToString(map.get(key))));
                }
                stringBuilder.append(HORIZONTAL_DOUBLE_LINE)
                        .append(" ").append("}").append(LINE_SEPARATOR)
                        .append(BOTTOM_BORDER);
                internalPrintLog(tag, DEBUG, stringBuilder.toString());
            } else {
                internalPrintLog(tag, ERROR, simpleName + " is Empty");
            }
        } else {
            final String message = TraceUtil.objectToString(obj);
            String content = "obj =>" + LINE_SEPARATOR +
                    TOP_BORDER + LINE_SEPARATOR;
            if (!TextUtils.isEmpty(methodCallPosition)) {
                content += HORIZONTAL_DOUBLE_LINE + " " + methodCallPosition + LINE_SEPARATOR +
                        MIDDLE_BORDER + LINE_SEPARATOR;
            }
            content += HORIZONTAL_DOUBLE_LINE + " " + message + LINE_SEPARATOR +
                    BOTTOM_BORDER;
            internalPrintLog(tag, DEBUG, content);
        }
    }

    @NonNull
    private String getRealTag(@Nullable final String myTag,
                              @Nullable final String methodCallPosition) {
        final String tag;
        if (!TextUtils.isEmpty(myTag)) {
            tag = myTag;
        } else if (!TextUtils.isEmpty(methodCallPosition)) {
            tag = methodCallPosition;
        } else {
            tag = "LLog";
        }
        //noinspection all
        return tag;
    }

    private void internalPrintLog(final StackTraceElement element,
                                  int logType, String message, Object... args) {
        final String[] values = generateTagAndFileName(element);
        final String tag = values[0];
//        String fileName = values[1];
        internalPrintLog(tag, logType, message, args);
    }

    private void internalPrintLog(final String tag, int logLevel, String message, Object... args) {
        if (!mLogPrinter.isPrintable(logLevel, tag)) {
            return;
        }
        if (!TextUtils.isEmpty(message) && null != args && args.length > 0) {
            message = String.format(message, args);
        }
        if (null == message) {
            message = "null";
        }
        print(logLevel, tag, message);
    }

    private String[] generateTagAndFileName(final StackTraceElement element) {
        final String[] values = new String[2];

        final String className = element.getClassName();
        final int lineNumber = element.getLineNumber();
        final String fileName = element.getFileName();
        final String methodName = element.getMethodName();
        String tag = null;

        if (lineNumber < 0) {
            // no source. maybe minifyEnabled, now we try to search TAG filed in class
            final String tagFoundFromCache = mTagFinderCache.findTag(className);
            //if tag cache is "" represent that we searched TAG before, but no TAG found in class.
            if (!"".equals(tagFoundFromCache)) {
                tag = tagFoundFromCache;
            }
        }
        if (tag == null) {
            tag = className.substring(className.lastIndexOf(".") + 1) + "." + methodName
                    + " (" + fileName + ":" + lineNumber + ") ";
        }

        values[0] = tag;
        values[1] = fileName == null ? "" : fileName;
        return values;
    }
}