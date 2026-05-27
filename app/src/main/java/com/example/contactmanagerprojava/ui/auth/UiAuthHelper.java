package com.example.contactmanagerprojava.ui.auth;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.contactmanagerprojava.R;

/**
 * Lớp hỗ trợ hiển thị thông báo lỗi xác thực thân thiện với người dùng.
 */
public class UiAuthHelper {
    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    public static boolean isPasswordHidden(EditText editText) {
        return editText.getTransformationMethod() instanceof PasswordTransformationMethod;
    }

    public static void togglePassword(EditText editText, ImageButton toggleButton) {
        boolean hidden = isPasswordHidden(editText);
        if (hidden) {
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            toggleButton.setImageResource(R.drawable.ic_eye_open);
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleButton.setImageResource(R.drawable.ic_eye_closed);
        }
        editText.setSelection(editText.getText() == null ? 0 : editText.getText().length());
    }
}
