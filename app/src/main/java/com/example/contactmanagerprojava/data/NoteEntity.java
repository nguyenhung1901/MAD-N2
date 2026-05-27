package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notes",
        foreignKeys = @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("contactId"), @Index("userId")}
)
/**
 * Entity Room đại diện cho một ghi chú do người dùng tạo trong bảng notes.
 */
public class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public Long contactId;
    @NonNull public String title;
    public String content;
    @NonNull public String category;
    public long createdAt;

    /**
     * Khởi tạo đối tượng NoteEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public NoteEntity(long userId, Long contactId, @NonNull String title, String content, @NonNull String category) {
        this.userId = userId;
        this.contactId = contactId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
    }
}
