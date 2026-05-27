package com.example.contactmanagerprojava.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tiện ích quản lý phiên đăng nhập local bằng SharedPreferences.
 */
public class SessionManager {
    private static final String PREF = "session_pref";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences preferences;

    /**
     * Khởi tạo đối tượng SessionManager với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public SessionManager(Context context) {
        // API Android SharedPreferences: lưu cài đặt/phiên đăng nhập dạng key-value cục bộ.
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /**
     * Kiểm tra thông tin đăng nhập của người dùng và trả về tài khoản nếu hợp lệ.
     */
    public void login(long userId) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_USER_ID, userId)
                .commit();
    }

    // Xóa trạng thái đăng nhập local khỏi SharedPreferences.
    public void logout() {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .commit();
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false) && getUserId() > 0L;
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }
}
