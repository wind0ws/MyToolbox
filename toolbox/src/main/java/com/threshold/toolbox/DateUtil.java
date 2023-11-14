package com.threshold.toolbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期时间工具
 */
@SuppressWarnings("unused")
public class DateUtil {
    private static final String[] CHINESE_WEEK_DAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_TIME = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_DATE_IN_STANDARD = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_DATE_IN_CHINESE = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_DATE_TIME_NO_SEPARATOR = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.CHINA);
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_DATE_TIME = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);

    public static String getChineseDate() {
        return getChineseDate(new Date());
    }

    public static String getChineseDate(Date date) {
        return SIMPLE_DATE_FORMAT_DATE_IN_CHINESE.format(date);
    }

    public static Date parseChineseDate(String date) throws ParseException {
        return SIMPLE_DATE_FORMAT_DATE_IN_CHINESE.parse(date);
    }

    public static Date parseStandardDate(String date) throws ParseException {
        return SIMPLE_DATE_FORMAT_DATE_IN_STANDARD.parse(date);
    }

    public static String getWeekOfDate() {
        return getWeekOfDate(new Date());
    }

    public static String getWeekOfDate(Date date) {
        final Calendar calendar = Calendar.getInstance();
        if (date != null) {
            calendar.setTime(date);
        }
        int w = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return CHINESE_WEEK_DAYS[w];
    }

    public static String getDateTime(long time) {
        return getDateTimeString(time, SIMPLE_DATE_FORMAT_DATE_TIME);
    }

    public static String getDateTimeWithoutSeparator(long time) {
        return getDateTimeString(time, SIMPLE_DATE_FORMAT_DATE_TIME_NO_SEPARATOR);
    }

    public static String getTime(long time) {
        return getDateTimeString(time, SIMPLE_DATE_FORMAT_TIME);
    }

    public static String getTime(Date date) {
        return getDateTimeString(date.getTime(), SIMPLE_DATE_FORMAT_TIME);
    }

    public static String getDateInChinese(long time) {
        return getDateTimeString(time, SIMPLE_DATE_FORMAT_DATE_IN_CHINESE);
    }

    public static String getDateByStandardSeparator(Date date) {
        return getDateTimeString(date, SIMPLE_DATE_FORMAT_DATE_IN_STANDARD);
    }

    public static String getDateByStandardSeparator(long time) {
        return getDateTimeString(time, SIMPLE_DATE_FORMAT_DATE_IN_STANDARD);
    }

    public static String getDateTimeString(long time, SimpleDateFormat simpleDateFormat) {
        return getDateTimeString(new Date(time), simpleDateFormat);
    }

    public static String getDateTimeString(Date date, SimpleDateFormat simpleDateFormat) {
        return simpleDateFormat.format(date);
    }

    public static String getDuration(long startTime, long endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException(String.format("getDuration(%d, %d), startTime big than endTime!",
                    startTime, endTime));
        }
        final long totalSeconds = (endTime - startTime) / 1000;
        int hh = (int) (totalSeconds / 3600);
        int mm = (int) ((totalSeconds - hh * 3600) / 60);
        int ss = (int) (totalSeconds - hh * 3600 - mm * 60);
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hh, mm, ss);
    }

    public static String currentDateTimeFileNameString() {
        return getDateTimeString(System.currentTimeMillis(), SIMPLE_DATE_FORMAT_DATE_TIME_NO_SEPARATOR);
    }

}
