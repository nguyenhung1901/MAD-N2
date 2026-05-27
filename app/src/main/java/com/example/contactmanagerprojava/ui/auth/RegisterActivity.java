package com.example.contactmanagerprojava.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.databinding.ActivityRegisterBinding;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.PasswordSecurity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Màn hình đăng ký tài khoản bằng Firebase Authentication và gửi email xác minh.
 */
public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    // Lifecycle method được gọi khi màn hình/lớp được tạo; dùng để khởi tạo binding, repository, sự kiện và dữ liệu ban đầu.
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
    protected void onCreate(Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        // API ngoài - Firebase Authentication: lấy instance Auth dùng để đăng nhập, đăng ký, xác minh email và reset mật khẩu.
        firebaseAuth = FirebaseAuth.getInstance();

        binding.btnRegister.setOnClickListener(v -> register());
        binding.btnTogglePassword.setOnClickListener(v -> UiAuthHelper.togglePassword(binding.etPassword, binding.btnTogglePassword));
        binding.btnToggleConfirmPassword.setOnClickListener(v -> UiAuthHelper.togglePassword(binding.etConfirmPassword, binding.btnToggleConfirmPassword));
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Tạo tài khoản người dùng cục bộ sau khi xác thực thông tin đăng ký.
     */
    private void register() {
        String name = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim().toLowerCase();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etFullName.setError("Nhập họ tên");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email không hợp lệ");
            return;
        }
        if (!PasswordSecurity.isStrongPassword(password)) {
            binding.etPassword.setError("Tối thiểu 8 ký tự, gồm chữ và số");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        setLoading(true);
        // API ngoài - Firebase Authentication: tạo tài khoản bằng email/password trên Firebase.
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(this, describeTaskError(task, "Không thể đăng ký"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "Không thể lấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfileThenSendVerification(user, name);
                });
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateProfileThenSendVerification(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
            if (!profileTask.isSuccessful()) {
                setLoading(false);
                Toast.makeText(this, describeTaskError(profileTask, "Đăng ký thành công nhưng chưa lưu được tên hiển thị"), Toast.LENGTH_LONG).show();
                return;
            }
            // API ngoài - Firebase Authentication: gửi email xác minh tài khoản tới địa chỉ của người dùng.
            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                firebaseAuth.signOut();
                setLoading(false);
                if (verifyTask.isSuccessful()) {
                    Toast.makeText(this, "Đăng ký thành công. Đã gửi email xác minh, vui lòng kiểm tra hộp thư và thư rác/spam rồi đăng nhập.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, describeTaskError(verifyTask, "Đăng ký thành công nhưng chưa gửi được email xác minh. Hãy thử đăng nhập để gửi lại"), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        });
    }

    private String describeTaskError(Task<?> task, String fallback) {
        if (task == null || task.getException() == null || TextUtils.isEmpty(task.getException().getMessage())) {
            return fallback;
        }
        return fallback + ": " + task.getException().getMessage();
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
        binding.btnTogglePassword.setEnabled(!loading);
        binding.btnToggleConfirmPassword.setEnabled(!loading);
        binding.btnRegister.setText(loading ? "Đang đăng ký..." : "Đăng ký");
    }
}
