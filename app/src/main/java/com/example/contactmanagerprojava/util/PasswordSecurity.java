package com.example.contactmanagerprojava.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Tiện ích bảo mật mật khẩu local bằng PBKDF2-SHA256 và kiểm tra độ mạnh mật khẩu.
 */
public final class PasswordSecurity {
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 120_000;

    private PasswordSecurity() {}

    // Kiểm tra mật khẩu tối thiểu có độ dài hợp lệ và gồm cả chữ lẫn số.
    /**
     * Kiểm tra mật khẩu có đáp ứng độ dài và độ phức tạp tối thiểu hay không.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    // Sinh salt ngẫu nhiên và hash mật khẩu bằng PBKDF2-SHA256.
    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt để không lưu mật khẩu dạng rõ.
     */
    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password == null");
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_BYTES);
        return String.format(Locale.US, "%s$%d$%s$%s",
                PREFIX,
                ITERATIONS,
                Base64.encodeToString(salt, Base64.NO_WRAP),
                Base64.encodeToString(hash, Base64.NO_WRAP));
    }

    // So sánh mật khẩu người dùng nhập với giá trị đã lưu, hỗ trợ cả dữ liệu cũ chưa hash.
    /**
     * Kiểm tra mật khẩu người dùng nhập có khớp với giá trị đã băm hay không.
     */
    public static boolean verifyPassword(String password, String storedValue) {
        if (password == null || storedValue == null || storedValue.isEmpty()) return false;
        if (!isHashed(storedValue)) {
            return MessageDigest.isEqual(
                    password.getBytes(StandardCharsets.UTF_8),
                    storedValue.getBytes(StandardCharsets.UTF_8)
            );
        }
        try {
            String[] parts = storedValue.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // Kiểm tra mật khẩu cũ có cần migrate sang định dạng hash mới hay không.
    /**
     * Xác định mật khẩu cũ cần chuyển sang dạng băm PBKDF2 hay không.
     */
    public static boolean needsMigration(String storedValue) {
        return storedValue != null && !storedValue.isEmpty() && !isHashed(storedValue);
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    public static boolean isHashed(String storedValue) {
        return storedValue != null && storedValue.startsWith(PREFIX + "$");
    }

    // Gọi thuật toán PBKDF2WithHmacSHA256 để sinh hash mật khẩu.
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int outputBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, outputBytes * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không thể hash mật khẩu", e);
        }
    }
}
