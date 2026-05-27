package com.example.contactmanagerprojava.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.UserEntity;
import com.example.contactmanagerprojava.databinding.ActivityLoginBinding;
import com.example.contactmanagerprojava.ui.MainActivity;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Màn hình đăng nhập; tích hợp Firebase Email/Password, Google Sign-In và gửi email đặt lại mật khẩu.
 */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AppRepository repository;
    private SessionManager sessionManager;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);

        repository = new AppRepository(this);
        sessionManager = new SessionManager(this);
        // API ngoài - Firebase Authentication: lấy instance Auth dùng để đăng nhập, đăng ký, xác minh email và reset mật khẩu.
        firebaseAuth = FirebaseAuth.getInstance();
        setupGoogleSignIn();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
    /**
     * Hoàn tất đăng nhập Firebase bằng cách ánh xạ tài khoản Firebase vào userId cục bộ.
     */
            completeFirebaseLogin(currentUser, detectProvider(currentUser));
            return;
        }
        if (sessionManager.isLoggedIn()) {
            sessionManager.logout();
        }

        binding.btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        binding.btnGoogleSignIn.setOnClickListener(v -> loginWithGoogle());
        binding.tvForgotPassword.setOnClickListener(v -> resetPassword());
        // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
        binding.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        binding.btnTogglePassword.setOnClickListener(v -> UiAuthHelper.togglePassword(binding.etPassword, binding.btnTogglePassword));
    }

    @Override
    // Lifecycle method được gọi khi màn hình quay lại foreground; dùng để làm mới dữ liệu hoặc trạng thái giao diện.
    /**
     * Làm mới trạng thái màn hình khi người dùng quay lại Activity/Fragment.
     */
    protected void onResume() {
    /**
     * Làm mới trạng thái màn hình khi người dùng quay lại Activity/Fragment.
     */
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            binding.etPassword.setText("");
        }
    }

    // Cấu hình Google Sign-In để lấy ID token gửi sang Firebase Authentication.
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // API ngoài - Google Sign-In: tạo client để mở màn hình chọn tài khoản Google.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        // API AndroidX: đăng ký callback nhận kết quả quyền hoặc Activity Result theo lifecycle.
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // API ngoài - Google Sign-In: đọc kết quả đăng nhập Google từ Intent trả về.
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
    /**
     * Dùng idToken từ Google Sign-In để xác thực với Firebase Authentication.
     */
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                        setAuthLoading(false);
                        Toast.makeText(this, "Không thể đăng nhập Google. Mã lỗi: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Đăng nhập Firebase bằng email/password và kiểm tra trạng thái xác minh email.
    /**
     * Gửi yêu cầu đăng nhập Email/Password lên Firebase Authentication.
     */
    private void loginWithEmailPassword() {
        String email = binding.etEmail.getText().toString().trim().toLowerCase();
        String password = binding.etPassword.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email không hợp lệ");
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Nhập mật khẩu");
            return;
        }

    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
        setAuthLoading(true);
        // API ngoài - Firebase Authentication: đăng nhập bằng email/password đã đăng ký trên Firebase.
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                        setAuthLoading(false);
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                        setAuthLoading(false);
                        Toast.makeText(this, "Không thể lấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser reloadedUser = firebaseAuth.getCurrentUser();
                        if (!reloadTask.isSuccessful() || reloadedUser == null) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                            setAuthLoading(false);
                            Toast.makeText(this, describeTaskError(reloadTask, "Không thể kiểm tra trạng thái xác minh email"), Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!reloadedUser.isEmailVerified()) {
                            // Nếu email chưa xác minh, gọi hàm gửi lại email xác minh rồi đăng xuất để buộc người dùng xác nhận tài khoản.
                            resendEmailVerificationAndSignOut(reloadedUser);
                            return;
                        }
    /**
     * Hoàn tất đăng nhập Firebase bằng cách ánh xạ tài khoản Firebase vào userId cục bộ.
     */
                        completeFirebaseLogin(reloadedUser, "password");
                    });
                });
    }

    // Mở luồng chọn tài khoản Google trên thiết bị.
    /**
     * Mở luồng Google Sign-In để người dùng chọn tài khoản Google.
     */
    private void loginWithGoogle() {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
        setAuthLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // Dùng Google ID token để đăng nhập Firebase và tạo session local tương ứng.
    /**
     * Dùng idToken từ Google Sign-In để xác thực với Firebase Authentication.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        if (TextUtils.isEmpty(idToken)) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
            setAuthLoading(false);
            Toast.makeText(this, "Không nhận được mã xác thực Google", Toast.LENGTH_SHORT).show();
            return;
        }
        // API ngoài - Firebase Auth/Google: chuyển Google ID token thành credential để Firebase xác thực.
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                        setAuthLoading(false);
                        Toast.makeText(this, describeTaskError(task, "Đăng nhập Google thất bại"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                        setAuthLoading(false);
                        Toast.makeText(this, "Không thể lấy thông tin Google", Toast.LENGTH_SHORT).show();
                        return;
                    }
    /**
     * Hoàn tất đăng nhập Firebase bằng cách ánh xạ tài khoản Firebase vào userId cục bộ.
     */
                    completeFirebaseLogin(user, "google");
                });
    }

    // Gửi email đặt lại mật khẩu qua Firebase Authentication.
    /**
     * Gửi email khôi phục mật khẩu thông qua Firebase Authentication.
     */
    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim().toLowerCase();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Nhập email hợp lệ trước");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu")
                .setMessage("Ứng dụng sẽ gửi email đặt lại mật khẩu tới " + email + ".")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi email", (dialog, which) -> {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                    setAuthLoading(true);
                    // API ngoài - Firebase Authentication: gửi email đặt lại mật khẩu.
                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
                                setAuthLoading(false);
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư và thư rác/spam.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, describeTaskError(task, "Không thể gửi email đặt lại mật khẩu"), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .show();
    }

    // Tạo/tìm user local tương ứng với FirebaseUser và chuyển vào MainActivity.
    /**
     * Hoàn tất đăng nhập Firebase bằng cách ánh xạ tài khoản Firebase vào userId cục bộ.
     */
    private void completeFirebaseLogin(FirebaseUser firebaseUser, String provider) {
        if ("password".equals(provider) && !firebaseUser.isEmailVerified()) {
            // Nếu email chưa xác minh, gọi hàm gửi lại email xác minh rồi đăng xuất để buộc người dùng xác nhận tài khoản.
            resendEmailVerificationAndSignOut(firebaseUser);
            return;
        }

        String email = firebaseUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            firebaseAuth.signOut();
            sessionManager.logout();
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
            setAuthLoading(false);
            Toast.makeText(this, "Tài khoản này không có email", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = firebaseUser.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            name = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
        }
    /**
     * Liên kết tài khoản Firebase với bản ghi người dùng cục bộ; nếu chưa có thì tạo mới.
     */
        UserEntity localUser = repository.findOrCreateFirebaseUser(name, email, provider);
        if (localUser == null || localUser.id <= 0) {
            firebaseAuth.signOut();
            sessionManager.logout();
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
            setAuthLoading(false);
            Toast.makeText(this, "Không thể tạo hồ sơ cục bộ", Toast.LENGTH_SHORT).show();
            return;
        }
    /**
     * Kiểm tra thông tin đăng nhập của người dùng và trả về tài khoản nếu hợp lệ.
     */
        sessionManager.login(localUser.id);
        if (!sessionManager.isLoggedIn()) {
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
            setAuthLoading(false);
            Toast.makeText(this, "Không thể lưu phiên đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        startMain();
    }

    // Gửi lại email xác minh cho tài khoản chưa xác minh, sau đó đăng xuất khỏi Firebase.
    private void resendEmailVerificationAndSignOut(FirebaseUser user) {
        // API ngoài - Firebase Authentication: gửi email xác minh tài khoản tới địa chỉ của người dùng.
        user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
            firebaseAuth.signOut();
            sessionManager.logout();
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
            setAuthLoading(false);
            if (verifyTask.isSuccessful()) {
                Toast.makeText(this, "Email chưa được xác minh. Đã gửi lại email xác minh, vui lòng kiểm tra hộp thư và thư rác/spam.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, describeTaskError(verifyTask, "Email chưa được xác minh và chưa gửi lại được email xác minh"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String detectProvider(FirebaseUser user) {
        if (user == null || user.getProviderData() == null) return "firebase";
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) return "google";
            if ("password".equals(info.getProviderId())) return "password";
        }
        return "firebase";
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Bật hoặc tắt trạng thái đang xử lý để tránh người dùng bấm lặp khi đăng nhập.
     */
    private void setAuthLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
        binding.tvRegister.setEnabled(!loading);
        binding.tvForgotPassword.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "Đang xử lý..." : "Đăng nhập");
        binding.btnGoogleSignIn.setText("Đăng nhập bằng Google");
    }

    private String describeTaskError(Task<?> task, String fallback) {
        if (task == null || task.getException() == null || TextUtils.isEmpty(task.getException().getMessage())) {
            return fallback;
        }
        return fallback + ": " + task.getException().getMessage();
    }

    private void startMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
        startActivity(intent);
        finishAffinity();
    }
}
