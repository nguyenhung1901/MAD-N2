package com.example.contactmanagerprojava.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
/**
 * DAO truy xuất bảng users; quản lý tài khoản cục bộ dùng để map Firebase user với dữ liệu local.
 */
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // Thêm bản ghi mới vào Room Database.
    /**
     * Thêm bản ghi mới vào bảng tương ứng trong Room Database.
     */
    long insert(UserEntity user);

    @Update
    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    /**
     * Cập nhật nội dung của bản ghi đã tồn tại trong Room Database.
     */
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    // Tìm bản ghi phù hợp theo điều kiện truyền vào.
    UserEntity findByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    // Tìm bản ghi phù hợp theo điều kiện truyền vào.
    UserEntity findById(long id);
}
