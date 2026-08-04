package com.news.kimo.utils;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for string operations.
 * Provides methods for extracting hashtags/mentions, text truncation,
 * text highlighting, HTML stripping, and URL validation.
 */
public class StringUtils {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final String ELLIPSIS = "...";

    private StringUtils() {
        // Prevent instantiation
    }

    /**
     * Extract all hashtags from the given text.
     *
     * @param text The input text
     * @return List of hashtag strings without the '#' prefix
     */
    public static List<String> extractHashtags(String text) {
        List<String> hashtags = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return hashtags;
        }
        Matcher matcher = HASHTAG_PATTERN.matcher(text);
        while (matcher.find()) {
            hashtags.add(matcher.group(1));
        }
        return hashtags;
    }

    /**
     * Extract all mentions from the given text.
     *
     * @param text The input text
     * @return List of mention strings without the '@' prefix
     */
    public static List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return mentions;
        }
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    /**
     * Truncate text to the specified maximum length and append an ellipsis if truncated.
     *
     * @param text      The input text
     * @param maxLength The maximum length before truncation (not counting the ellipsis)
     * @return Truncated text with ellipsis, or the original text if it fits
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim() + ELLIPSIS;
    }

    /**
     * Highlight all hashtags in the text by wrapping them in a colored span.
     *
     * @param text      The input text
     * @param color     The color to apply to the hashtag text
     * @return A SpannableString with colored hashtags
     */
    public static SpannableString highlightHashtags(String text, int color) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }
        SpannableString spannableString = new SpannableString(text);
        Matcher matcher = HASHTAG_PATTERN.matcher(text);
        while (matcher.find()) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
            spannableString.setSpan(colorSpan, matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * Highlight all mentions in the text by wrapping them in a colored span.
     *
     * @param text      The input text
     * @param color     The color to apply to the mention text
     * @return A SpannableString with colored mentions
     */
    public static SpannableString highlightMentions(String text, int color) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }
        SpannableString spannableString = new SpannableString(text);
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
            spannableString.setSpan(colorSpan, matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * Highlight both hashtags and mentions in the text.
     *
     * @param text          The input text
     * @param hashtagColor  The color for hashtags
     * @param mentionColor  The color for mentions
     * @return A SpannableString with colored hashtags and mentions
     */
    public static SpannableString highlightAll(String text, int hashtagColor, int mentionColor) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }
        SpannableString spannableString = new SpannableString(text);

        Matcher hashtagMatcher = HASHTAG_PATTERN.matcher(text);
        while (hashtagMatcher.find()) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(hashtagColor);
            spannableString.setSpan(colorSpan, hashtagMatcher.start(), hashtagMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Matcher mentionMatcher = MENTION_PATTERN.matcher(text);
        while (mentionMatcher.find()) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(mentionColor);
            spannableString.setSpan(colorSpan, mentionMatcher.start(), mentionMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    /**
     * Strip all HTML tags from the given text.
     *
     * @param text The input text containing HTML
     * @return Plain text without HTML tags
     */
    public static String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Check if the given string is a valid URL.
     *
     * @param url The string to check
     * @return true if the string is a valid URL
     */
    public static boolean isUrlValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }
}
