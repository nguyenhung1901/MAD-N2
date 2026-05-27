package com.example.contactmanagerprojava.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.UserEntity;
import com.example.contactmanagerprojava.databinding.FragmentSettingsBinding;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.PasswordSecurity;
import com.example.contactmanagerprojava.util.SessionManager;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * Fragment cài đặt; quản lý Dark Mode, Privacy Mode, đổi mật khẩu và đăng xuất.
 */
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    // Lifecycle method của Fragment; inflate layout, gắn sự kiện UI và bắt đầu quan sát dữ liệu.
    /**
     * Tạo giao diện cho Fragment, khởi tạo binding, Adapter và các sự kiện thao tác của người dùng.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        // API ngoài - Firebase Authentication: lấy instance Auth dùng để đăng nhập, đăng ký, xác minh email và reset mật khẩu.
        firebaseAuth = FirebaseAuth.getInstance();

        AppRepository repository = new AppRepository(requireContext());
        long userId = new SessionManager(requireContext()).getUserId();
    /**
     * Lấy thông tin người dùng theo userId đang được lưu trong phiên đăng nhập.
     */
        UserEntity localUser = repository.getUser(userId);
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String email = firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())
                ? firebaseUser.getEmail()
                : (localUser == null ? "Không rõ" : localUser.email);
        binding.tvActiveEmail.setText("Đang đăng nhập: " + email);

        binding.switchDarkMode.setChecked(AppSettings.isDarkMode(requireContext()));
        binding.switchPrivacyMode.setChecked(AppSettings.isPrivacyMode(requireContext()));

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
    /**
     * Lưu lựa chọn bật/tắt giao diện tối vào SharedPreferences.
     */
            AppSettings.setDarkMode(requireContext(), isChecked);
            if (getActivity() != null) getActivity().recreate();
        });

        binding.switchPrivacyMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
    /**
     * Lưu trạng thái Privacy Mode vào SharedPreferences.
     */
            AppSettings.setPrivacyMode(requireContext(), isChecked);
            if (getActivity() instanceof AppCompatActivity) {
                AppSettings.applyPrivacy((AppCompatActivity) getActivity());
                getActivity().recreate();
            }
        });

        setupPasswordSection(firebaseUser);
        return binding.getRoot();
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setupPasswordSection(FirebaseUser firebaseUser) {
        if (firebaseUser == null || !hasPasswordProvider(firebaseUser)) {
            binding.etCurrentPassword.setEnabled(false);
            binding.etNewPassword.setEnabled(false);
            binding.etConfirmPassword.setEnabled(false);
            binding.btnChangePassword.setEnabled(false);
            binding.etCurrentPassword.setHint("Tài khoản Google không đổi mật khẩu trong app");
            binding.btnChangePassword.setText("Đổi mật khẩu trong tài khoản Google");
            return;
        }

        binding.btnChangePassword.setOnClickListener(v -> {
            String current = binding.etCurrentPassword.getText().toString().trim();
            String next = binding.etNewPassword.getText().toString().trim();
            String confirm = binding.etConfirmPassword.getText().toString().trim();
            if (current.isEmpty()) {
                binding.etCurrentPassword.setError("Nhập mật khẩu hiện tại");
                return;
            }
            if (!PasswordSecurity.isStrongPassword(next)) {
                binding.etNewPassword.setError("Tối thiểu 8 ký tự, gồm chữ và số");
                return;
            }
            if (!next.equals(confirm)) {
                binding.etConfirmPassword.setError("Mật khẩu nhập lại không khớp");
                return;
            }
            changeFirebasePassword(firebaseUser, current, next);
        });
    }

    private void changeFirebasePassword(FirebaseUser firebaseUser, String currentPassword, String newPassword) {
        String email = firebaseUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(), "Không tìm thấy email tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        setPasswordLoading(true);
        firebaseUser.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword))
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (!task.isSuccessful()) {
                        setPasswordLoading(false);
                        binding.etCurrentPassword.setError("Mật khẩu hiện tại không đúng");
                        return;
                    }
                    firebaseUser.updatePassword(newPassword)
                            .addOnCompleteListener(updateTask -> {
                                if (!isAdded()) return;
                                setPasswordLoading(false);
                                if (!updateTask.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Không thể đổi mật khẩu", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                binding.etCurrentPassword.setText("");
                                binding.etNewPassword.setText("");
                                binding.etConfirmPassword.setText("");
                                Toast.makeText(requireContext(), "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setPasswordLoading(boolean loading) {
        binding.btnChangePassword.setEnabled(!loading);
        binding.etCurrentPassword.setEnabled(!loading);
        binding.etNewPassword.setEnabled(!loading);
        binding.etConfirmPassword.setEnabled(!loading);
        binding.btnChangePassword.setText(loading ? "Đang đổi mật khẩu..." : "Đổi mật khẩu");
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    private boolean hasPasswordProvider(FirebaseUser user) {
        if (user == null || user.getProviderData() == null) return false;
        for (UserInfo info : user.getProviderData()) {
            if ("password".equals(info.getProviderId())) return true;
        }
        return false;
    }
}
