package com.threshold.toolbox;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public class JsonUtil {

    private JsonUtil() {
        throw new IllegalStateException("no instance");
    }

    @Nullable
    public static Object opt(@NonNull final JSONObject jsonObject, @NonNull final String key) {
        try {
            return jsonObject.opt(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NonNull
    public static Object opt(@NonNull final JSONObject jsonObject,
                             @NonNull final String key, @NonNull final Object defaultObj) {
        try {
            Object obj = jsonObject.opt(key);
            if (null == obj) {
                return defaultObj;
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return defaultObj;
        }
    }

    @Nullable
    public static Object opt(final String json, final String key) {
        if (TextUtils.isEmpty(json) || "{}".equals(json.trim().replace(" ", ""))) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            return opt(jsonObject, key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String optString(final String json, final String key, final String defaultVal) {
        try {
            return optString(new JSONObject(json), key, defaultVal);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultVal;
    }

    public static String optString(final JSONObject jsonObject,
                                   final String key, final String defaultVal) {
        final Object obj = opt(jsonObject, key);
        if (obj != null) {
            return obj.toString();
        }
        return defaultVal;
    }

    public static boolean optBoolean(final String json, final String key) {
        final String obj = optString(json, key, "");
        return obj != null && "true".equalsIgnoreCase(obj.trim());
    }

    public static int optInt(final JSONObject jsonObject,
                             final String key, final int defaultVal) {
        final String numStr = optString(jsonObject, key, "");
        int ret = defaultVal;
        if (TextUtils.isEmpty(numStr)) {
            try {
                ret = Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}
