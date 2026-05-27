package com.example.contactmanagerprojava;

import android.app.Application;

import com.example.contactmanagerprojava.data.AppDatabase;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.ReminderWorker;

/**
 * Lớp Application chạy đầu tiên khi ứng dụng khởi động; áp dụng theme đã lưu, khởi tạo Room Database và lập lịch Worker nền.
 */
public class ContactManagerApplication extends Application {
    private AppDatabase database;

    @Override
    // Lifecycle method được gọi khi màn hình/lớp được tạo; dùng để khởi tạo binding, repository, sự kiện và dữ liệu ban đầu.
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
    public void onCreate() {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate();
        AppSettings.applySavedTheme(this);
        database = AppDatabase.getInstance(this);
        ReminderWorker.schedule(this);
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
