package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts", indices = {@Index("userId")})
/**
 * Entity Room đại diện cho thông tin chính của một liên hệ trong bảng contacts.
 */
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    @NonNull public String fullName;
    public String company;
    public String position;
    public String department;
    public String email;
    public String address;
    public String birthday;
    public String tags;
    public String note;
    public boolean favorite;
    public long updatedAt;

    /**
     * Khởi tạo đối tượng ContactEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public ContactEntity(long userId, @NonNull String fullName) {
        this.userId = userId;
        this.fullName = fullName;
        this.updatedAt = System.currentTimeMillis();
    }
}
