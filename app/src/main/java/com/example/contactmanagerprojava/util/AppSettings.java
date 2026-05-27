package com.example.contactmanagerprojava.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Tiện ích quản lý cài đặt ứng dụng như theme tối/sáng và Privacy Mode bằng SharedPreferences.
 */
public final class AppSettings {
    private static final String PREFS = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PRIVACY_MODE = "privacy_mode";

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        // API Android SharedPreferences: lưu cài đặt/phiên đăng nhập dạng key-value cục bộ.
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    /**
     * Kiểm tra người dùng đang bật chế độ giao diện tối hay không.
     */
    public static boolean isDarkMode(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Lưu lựa chọn bật/tắt giao diện tối vào SharedPreferences.
     */
    public static void setDarkMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Áp dụng theme sáng/tối đã lưu cho toàn ứng dụng.
    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDarkMode(context) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Kiểm tra trạng thái Privacy Mode từ SharedPreferences.
    /**
     * Kiểm tra trạng thái Privacy Mode để quyết định có che thông tin nhạy cảm hay không.
     */
    public static boolean isPrivacyMode(Context context) {
        return prefs(context).getBoolean(KEY_PRIVACY_MODE, false);
    }

    // Lưu trạng thái Privacy Mode vào SharedPreferences.
    /**
     * Lưu trạng thái Privacy Mode vào SharedPreferences.
     */
    public static void setPrivacyMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply();
    }

    public static void applyPrivacy(Activity activity) {
        if (isPrivacyMode(activity)) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Che một phần tên liên hệ khi Privacy Mode đang bật.
     */
    public static String maskName(Context context, String value) {
        if (!isPrivacyMode(context) || TextUtils.isEmpty(value)) return value == null ? "" : value;
        if (value.length() <= 1) return "•";
        if (value.length() == 2) return value.charAt(0) + "•";
        return value.charAt(0) + repeat('•', Math.max(1, value.length() - 2)) + value.charAt(value.length() - 1);
    }

    /**
     * Che một phần số điện thoại khi Privacy Mode đang bật.
     */
    public static String maskPhone(Context context, String value) {
        if (!isPrivacyMode(context) || TextUtils.isEmpty(value)) return value == null ? "" : value;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) return repeat('•', Math.max(1, digits.length()));
        return repeat('•', digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    /**
     * Che một phần email khi Privacy Mode đang bật.
     */
    public static String maskEmail(Context context, String value) {
        if (!isPrivacyMode(context) || TextUtils.isEmpty(value)) return value == null ? "" : value;
        int at = value.indexOf('@');
        if (at <= 1) return "•••";
        return value.substring(0, 1) + "•••" + value.substring(at);
    }

    public static String maskGeneric(Context context, String value) {
        if (!isPrivacyMode(context) || TextUtils.isEmpty(value)) return value == null ? "" : value;
        return "••••••";
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }
}
