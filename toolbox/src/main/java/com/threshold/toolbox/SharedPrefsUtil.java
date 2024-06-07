package com.threshold.toolbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Util for SharedPrefs
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SharedPrefsUtil {

    private SharedPrefsUtil() {
        throw new IllegalStateException("no instance!");
    }

    public static void put(@NonNull Context ctx, @Nullable String prefsName, int prefsMode,
                           @NonNull String key, @NonNull Object value) {
        final SharedPreferences.Editor editor = getSharedPrefs(ctx, prefsName, prefsMode).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Set<?>) {
            final Set<?> set = (Set<?>) value;
            final Set<String> strSet = new HashSet<>();
            for (Object obj : set) {
                strSet.add(obj.toString());
            }
            editor.putStringSet(key, strSet);
        } else {
            editor.putString(key, value.toString());
        }
        editor.apply();
    }

    public static void put(@NonNull Context ctx, @Nullable String prefsName, @NonNull String key, @NonNull Object value) {
        put(ctx, prefsName, Context.MODE_PRIVATE, key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(@NonNull Context ctx, @Nullable String prefsName,
                            int prefsMode, @NonNull String key, @NonNull T defaultValue) {
        final SharedPreferences sharedPrefs = getSharedPrefs(ctx, prefsName, prefsMode);
        if (defaultValue instanceof Boolean) {
            return (T) ((Object) sharedPrefs.getBoolean(key, (Boolean) defaultValue));
        } else if (defaultValue instanceof Integer) {
            return (T) ((Object) sharedPrefs.getInt(key, (Integer) defaultValue));
        } else if (defaultValue instanceof Float) {
            return (T) ((Object) sharedPrefs.getFloat(key, (Float) defaultValue));
        } else if (defaultValue instanceof Long) {
            return (T) ((Object) sharedPrefs.getLong(key, (Long) defaultValue));
        } else if (defaultValue instanceof Set<?>) {
            return (T) sharedPrefs.getStringSet(key, (Set<String>) defaultValue);
        } else {
            return (T) sharedPrefs.getString(key, defaultValue.toString());
        }
    }

    @NonNull
    public static <T> T get(@NonNull Context ctx, @Nullable String prefsName,
                            @NonNull String key, @NonNull T defaultValue) {
        return get(ctx, prefsName, Context.MODE_PRIVATE, key, defaultValue);
    }

    public static SharedPreferences getSharedPrefs(@NonNull Context ctx, @Nullable String prefsName, int prefsMode) {
        if (TextUtils.isEmpty(prefsName)) {
            return PreferenceManager.getDefaultSharedPreferences(ctx);
        }
        return ctx.getSharedPreferences(prefsName, prefsMode);
    }

    public static SharedPreferences getSharedPrefs(@NonNull Context ctx, @Nullable String prefsName) {
        return getSharedPrefs(ctx, prefsName, Context.MODE_PRIVATE);
    }


    public static SharedPreferences getDefaultSharedPrefs(@NonNull Context ctx) {
        return getSharedPrefs(ctx, null, Context.MODE_PRIVATE);
    }


    public static boolean containsKey(@NonNull Context ctx, @Nullable String prefsName, int prefsMode, @NonNull String key) {
        return getSharedPrefs(ctx, prefsName, prefsMode)
                .contains(key);
    }

    public static boolean containsKey(@NonNull Context ctx, @Nullable String prefsName, @NonNull String key) {
        return getSharedPrefs(ctx, prefsName, Context.MODE_PRIVATE)
                .contains(key);
    }

    public static void removeKey(@NonNull Context ctx, @Nullable String prefsName, int prefsMode, @NonNull String key) {
        getSharedPrefs(ctx, prefsName, prefsMode)
                .edit()
                .remove(key)
                .apply();
    }

    public static void removeKey(@NonNull Context ctx, @Nullable String prefsName, @NonNull String key) {
        getSharedPrefs(ctx, prefsName, Context.MODE_PRIVATE)
                .edit()
                .remove(key)
                .apply();
    }

    public static void clear(@NonNull Context ctx, @Nullable String prefsName, int prefsMode) {
        getSharedPrefs(ctx, prefsName, prefsMode)
                .edit()
                .clear()
                .apply();
    }

    public static class Default {

        public static SharedPreferences get(@NonNull Context ctx) {
            return SharedPrefsUtil.getDefaultSharedPrefs(ctx);
        }

        public static <T> T get(@NonNull Context ctx, @NonNull String key, @NonNull T defaultValue) {
            return SharedPrefsUtil.get(ctx, null, -1, key, defaultValue);
        }

        public static <T> void put(@NonNull Context ctx, @NonNull String key, @NonNull T value) {
            SharedPrefsUtil.put(ctx, null, -1, key, value);
        }

        public static void removeKey(@NonNull Context ctx, @NonNull String key) {
            SharedPrefsUtil.removeKey(ctx, null, -1, key);
        }

        public static boolean containsKey(@NonNull Context ctx, @NonNull String key) {
            return SharedPrefsUtil.containsKey(ctx, null, -1, key);
        }

        public static void clear(@NonNull Context ctx) {
            SharedPrefsUtil.clear(ctx, null, -1);
        }

    }

}
