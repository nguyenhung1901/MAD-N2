package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
/**
 * Entity Room đại diện cho người dùng cục bộ của ứng dụng.
 */
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull public String fullName;
    @NonNull public String email;
    @NonNull public String password;
    public String provider;

    /**
     * Khởi tạo đối tượng UserEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public UserEntity(@NonNull String fullName, @NonNull String email, @NonNull String password, String provider) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.provider = provider;
    }
}
