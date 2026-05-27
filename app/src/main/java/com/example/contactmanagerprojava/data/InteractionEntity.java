package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "interactions", foreignKeys = @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.CASCADE), indices = {@Index("contactId")})
/**
 * Entity Room đại diện cho một sự kiện tương tác như gọi điện, gửi email, tạo ghi chú hoặc tạo lịch hẹn.
 */
public class InteractionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long contactId;
    @NonNull public String type;
    @NonNull public String content;
    public long createdAt;

    /**
     * Khởi tạo đối tượng InteractionEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public InteractionEntity(long contactId, @NonNull String type, @NonNull String content) {
        this.contactId = contactId;
        this.type = type;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
    }
}
