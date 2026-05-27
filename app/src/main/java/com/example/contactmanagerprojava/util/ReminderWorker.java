package com.example.contactmanagerprojava.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Worker nền dùng WorkManager để đặt nền tảng nhắc lịch hẹn định kỳ.
 */
public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    @NonNull @Override public Result doWork() { return Result.success(); }
    // Đăng ký tác vụ nền định kỳ bằng WorkManager.
    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class, 12, TimeUnit.HOURS).build();
        // API ngoài - WorkManager: đăng ký tác vụ nền định kỳ cho ứng dụng.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("appointment-reminder", ExistingPeriodicWorkPolicy.UPDATE, request);
    }
}
