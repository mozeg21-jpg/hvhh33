package com.news.kimo.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date and time formatting with Arabic locale support.
 * Provides relative time formatting, date/time string formatting,
 * and date comparison helpers.
 */
public class DateUtils {

    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String TIME_FORMAT = "hh:mm a";
    private static final String DATE_TIME_FORMAT = "dd/MM/yyyy hh:mm a";

    private DateUtils() {
        // Prevent instantiation
    }

    /**
     * Format a timestamp to a relative time string in Arabic.
     * Examples: "منذ ثانية", "منذ دقيقة", "منذ ساعتين", "منذ 3 أيام"
     *
     * @param timestamp The timestamp in milliseconds
     * @return Arabic relative time string
     */
    public static String formatRelativeTimeArabic(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 0) {
            return "الآن";
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 10) {
            return "الآن";
        } else if (seconds < 60) {
            return formatArabicCount(seconds, "ثانية");
        } else if (minutes < 60) {
            return formatArabicCount(minutes, "دقيقة");
        } else if (hours < 24) {
            return formatArabicCount(hours, "ساعة");
        } else if (days < 7) {
            return formatArabicCount(days, "يوم");
        } else if (weeks < 4) {
            return formatArabicCount(weeks, "أسبوع");
        } else if (months < 12) {
            return formatArabicCount(months, "شهر");
        } else {
            return formatArabicCount(years, "سنة");
        }
    }

    /**
     * Helper to format Arabic count with proper pluralization.
     * "منذ ثانية", "منذ دقيقتين", "منذ 3 دقائق"
     *
     * @param count  The count value
     * @param noun   The Arabic singular noun
     * @return Formatted string like "منذ 3 أيام"
     */
    private static String formatArabicCount(long count, String noun) {
        String prefix = "منذ ";
        if (count == 1) {
            return prefix + noun;
        } else if (count == 2) {
            return prefix + dualForm(noun);
        } else if (count >= 3 && count <= 10) {
            return prefix + count + " " + pluralForm(noun);
        } else {
            return prefix + count + " " + noun;
        }
    }

    /**
     * Get the Arabic dual form of a noun.
     */
    private static String dualForm(String noun) {
        switch (noun) {
            case "ثانية": return "ثانيتين";
            case "دقيقة": return "دقيقتين";
            case "ساعة": return "ساعتين";
            case "يوم": return "يومين";
            case "أسبوع": return "أسبوعين";
            case "شهر": return "شهرين";
            case "سنة": return "سنتين";
            default: return noun + "ين";
        }
    }

    /**
     * Get the Arabic plural form (3-10) of a noun.
     */
    private static String pluralForm(String noun) {
        switch (noun) {
            case "ثانية": return "ثوانٍ";
            case "دقيقة": return "دقائق";
            case "ساعة": return "ساعات";
            case "يوم": return "أيام";
            case "أسبوع": return "أسابيع";
            case "شهر": return "أشهر";
            case "سنة": return "سنوات";
            default: return noun + "ات";
        }
    }

    /**
     * Format a timestamp to a date string (dd/MM/yyyy).
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date string
     */
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format a timestamp to a time string (hh:mm a).
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format a timestamp to a full date-time string.
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date-time string
     */
    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Check if the given timestamp is from today.
     *
     * @param timestamp The timestamp in milliseconds
     * @return true if the timestamp is from today
     */
    public static boolean isToday(long timestamp) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check if the given timestamp is from yesterday.
     *
     * @param timestamp The timestamp in milliseconds
     * @return true if the timestamp is from yesterday
     */
    public static boolean isYesterday(long timestamp) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp);
        cal1.add(Calendar.DAY_OF_YEAR, -1);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check if the given timestamp is from this week.
     *
     * @param timestamp The timestamp in milliseconds
     * @return true if the timestamp is from this week
     */
    public static boolean isThisWeek(long timestamp) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp);
        int week1 = cal1.get(Calendar.WEEK_OF_YEAR);
        int week2 = cal2.get(Calendar.WEEK_OF_YEAR);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && week1 == week2;
    }

    /**
     * Get a friendly date string in Arabic.
     * Returns "اليوم", "أمس", "قبل يومين", or the formatted date.
     *
     * @param timestamp The timestamp in milliseconds
     * @return Arabic friendly date string
     */
    public static String getFriendlyDate(long timestamp) {
        long now = System.currentTimeMillis();
        long diffMs = now - timestamp;
        long diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (isToday(timestamp)) {
            return "اليوم";
        } else if (isYesterday(timestamp)) {
            return "أمس";
        } else if (diffDays == 2) {
            return "قبل يومين";
        } else if (diffDays < 7) {
            return "منذ " + diffDays + " أيام";
        } else {
            return formatDate(timestamp);
        }
    }

    /**
     * Format a timestamp for a post header (combines friendly date with time for older posts).
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted timestamp string
     */
    public static String formatPostTimestamp(long timestamp) {
        if (isToday(timestamp)) {
            return formatTime(timestamp);
        } else if (isYesterday(timestamp)) {
            return "أمس " + formatTime(timestamp);
        } else {
            return getFriendlyDate(timestamp);
        }
    }
}