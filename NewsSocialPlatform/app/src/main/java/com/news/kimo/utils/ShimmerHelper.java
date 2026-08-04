package com.news.kimo.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Utility class for creating shimmer loading effect drawables.
 * Provides shimmer color constants for light and dark themes,
 * and methods to create and apply shimmer effects to views.
 */
public class ShimmerHelper {

    // ============================================================
    // Light Theme Shimmer Colors
    // ============================================================
    /** Light shimmer base color (light gray) */
    @ColorInt
    public static final int LIGHT_SHIMMER_BASE = 0xFFE0E0E0;

    /** Light shimmer highlight color (white) */
    @ColorInt
    public static final int LIGHT_SHIMMER_HIGHLIGHT = 0xFFFFFFFF;

    // ============================================================
    // Dark Theme Shimmer Colors
    // ============================================================
    /** Dark shimmer base color (dark gray) */
    @ColorInt
    public static final int DARK_SHIMMER_BASE = 0xFF2C2C2C;

    /** Dark shimmer highlight color (slightly lighter gray) */
    @ColorInt
    public static final int DARK_SHIMMER_HIGHLIGHT = 0xFF424242;

    // ============================================================
    // Rounded Corner Radius
    // ============================================================
    /** Default corner radius for shimmer placeholders (8dp in pixels) */
    private static final float DEFAULT_CORNER_RADIUS_DP = 8f;

    private ShimmerHelper() {
        // Prevent instantiation
    }

    /**
     * Create a shimmer drawable with default light theme colors.
     * This creates a simple rounded rectangle gradient as a placeholder shimmer.
     *
     * @param context Context for density calculations
     * @return A GradientDrawable suitable for view backgrounds
     */
    public static GradientDrawable createShimmerDrawable(@NonNull Context context) {
        return createShimmerDrawable(context, LIGHT_SHIMMER_BASE, LIGHT_SHIMMER_HIGHLIGHT,
                DEFAULT_CORNER_RADIUS_DP);
    }

    /**
     * Create a shimmer drawable with the specified colors and corner radius.
     *
     * @param context       Context for density calculations
     * @param baseColor     The base shimmer color
     * @param highlightColor The shimmer highlight color
     * @param cornerRadiusDp Corner radius in dp
     * @return A GradientDrawable with the specified appearance
     */
    public static GradientDrawable createShimmerDrawable(@NonNull Context context,
                                                         @ColorInt int baseColor,
                                                         @ColorInt int highlightColor,
                                                         float cornerRadiusDp) {
        float density = context.getResources().getDisplayMetrics().density;
        float cornerRadiusPx = cornerRadiusDp * density;

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(cornerRadiusPx);
        drawable.setColor(baseColor);

        return drawable;
    }

    /**
     * Create a shimmer drawable for dark theme.
     *
     * @param context Context for density calculations
     * @return A GradientDrawable with dark theme shimmer colors
     */
    public static GradientDrawable createDarkShimmerDrawable(@NonNull Context context) {
        return createShimmerDrawable(context, DARK_SHIMMER_BASE, DARK_SHIMMER_HIGHLIGHT,
                DEFAULT_CORNER_RADIUS_DP);
    }

    /**
     * Setup shimmer effect on a view using a simple drawable background.
     * The view will display a rounded gray rectangle as a loading placeholder.
     *
     * @param view    The view to apply the shimmer effect to
     * @param isDark  Whether to use dark theme colors
     */
    public static void setupShimmer(@NonNull View view, boolean isDark) {
        Context context = view.getContext();
        GradientDrawable shimmerDrawable;
        if (isDark) {
            shimmerDrawable = createDarkShimmerDrawable(context);
        } else {
            shimmerDrawable = createShimmerDrawable(context);
        }
        view.setBackground(shimmerDrawable);
    }

    /**
     * Setup shimmer effect on a view with custom colors.
     *
     * @param view           The view to apply the shimmer effect to
     * @param baseColor      The base shimmer color
     * @param highlightColor The shimmer highlight color
     * @param cornerRadiusDp Corner radius in dp
     */
    public static void setupShimmer(@NonNull View view,
                                    @ColorInt int baseColor,
                                    @ColorInt int highlightColor,
                                    float cornerRadiusDp) {
        Context context = view.getContext();
        GradientDrawable shimmerDrawable = createShimmerDrawable(
                context, baseColor, highlightColor, cornerRadiusDp);
        view.setBackground(shimmerDrawable);
    }

    /**
     * Setup shimmer on a view with default light theme and a specific corner radius.
     *
     * @param view           The view to apply the shimmer effect to
     * @param cornerRadiusDp Corner radius in dp
     */
    public static void setupShimmer(@NonNull View view, float cornerRadiusDp) {
        Context context = view.getContext();
        GradientDrawable shimmerDrawable = createShimmerDrawable(
                context, LIGHT_SHIMMER_BASE, LIGHT_SHIMMER_HIGHLIGHT, cornerRadiusDp);
        view.setBackground(shimmerDrawable);
    }

    /**
     * Create a circular shimmer drawable (for avatar placeholders).
     *
     * @param context Context
     * @param sizeDp  Diameter in dp
     * @param isDark  Whether to use dark theme colors
     * @return A GradientDrawable with circular corners
     */
    public static GradientDrawable createCircularShimmerDrawable(@NonNull Context context,
                                                                  float sizeDp,
                                                                  boolean isDark) {
        @ColorInt int baseColor = isDark ? DARK_SHIMMER_BASE : LIGHT_SHIMMER_BASE;
        float cornerRadius = sizeDp / 2f;

        GradientDrawable drawable = new GradientDrawable();
        float density = context.getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(cornerRadius * density);
        drawable.setColor(baseColor);

        return drawable;
    }

    /**
     * Remove shimmer effect from a view by clearing its background.
     *
     * @param view The view to clear the shimmer from
     */
    public static void clearShimmer(@NonNull View view) {
        view.setBackground(null);
    }
}