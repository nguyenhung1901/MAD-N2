package com.example.contactmanagerprojava.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiện ích cung cấp executor nền dùng để chạy tác vụ nặng ngoài UI thread.
 */
public class AppExecutors {
    private static final ExecutorService DB = Executors.newSingleThreadExecutor();
    public static ExecutorService db() { return DB; }
}
