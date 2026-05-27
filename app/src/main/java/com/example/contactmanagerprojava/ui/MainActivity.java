package com.example.contactmanagerprojava.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.databinding.ActivityMainBinding;
import com.example.contactmanagerprojava.ui.appointments.AppointmentsFragment;
import com.example.contactmanagerprojava.ui.auth.LoginActivity;
import com.example.contactmanagerprojava.ui.contacts.ContactsFragment;
import com.example.contactmanagerprojava.ui.dashboard.DashboardFragment;
import com.example.contactmanagerprojava.ui.notes.NotesFragment;
import com.example.contactmanagerprojava.ui.settings.SettingsFragment;
import com.example.contactmanagerprojava.ui.stats.StatsFragment;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity chính sau đăng nhập; quản lý Drawer Navigation và chuyển đổi giữa các Fragment chức năng.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ActivityMainBinding binding;
    private SessionManager sessionManager;

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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        sessionManager = new SessionManager(this);
        // API ngoài - Firebase Authentication: lấy instance Auth dùng để đăng nhập, đăng ký, xác minh email và reset mật khẩu.
        if (FirebaseAuth.getInstance().getCurrentUser() == null || !sessionManager.isLoggedIn() || sessionManager.getUserId() <= 0) {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            startActivity(intent);
            finishAffinity();
            return;
        }
        setSupportActionBar(binding.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open, R.string.close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        binding.navigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            navigateTo(R.id.nav_dashboard);
        }
    }

    // Điều hướng MainActivity sang menu/Fragment tương ứng.
    public void navigateTo(int menuId) {
        Fragment fragment = null;
        String title = getString(R.string.menu_dashboard);
        if (menuId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
            title = getString(R.string.menu_dashboard);
        } else if (menuId == R.id.nav_contacts) {
            fragment = new ContactsFragment();
            title = getString(R.string.menu_contacts);
        } else if (menuId == R.id.nav_appointments) {
            fragment = new AppointmentsFragment();
            title = getString(R.string.menu_appointments);
        } else if (menuId == R.id.nav_notes) {
            fragment = new NotesFragment();
            title = getString(R.string.menu_notes);
        } else if (menuId == R.id.nav_stats) {
            fragment = new StatsFragment();
            title = getString(R.string.menu_stats);
        } else if (menuId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            title = getString(R.string.menu_settings);
        }
        if (fragment != null) {
            openFragment(fragment, title);
            binding.navigationView.setCheckedItem(menuId);
            binding.drawerLayout.closeDrawers();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) {
            // API ngoài - Firebase Authentication: lấy instance Auth dùng để đăng nhập, đăng ký, xác minh email và reset mật khẩu.
            FirebaseAuth.getInstance().signOut();
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            startActivity(intent);
            finishAffinity();
        } else {
            navigateTo(id);
        }
        binding.drawerLayout.closeDrawers();
        return true;
    }

    private void openFragment(Fragment fragment, String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }
}
