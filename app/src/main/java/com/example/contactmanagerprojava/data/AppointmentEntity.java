package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "appointments", foreignKeys = @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.CASCADE), indices = {@Index("contactId")})
/**
 * Entity Room đại diện cho một lịch hẹn gắn với một liên hệ trong bảng appointments.
 */
public class AppointmentEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long contactId;
    @NonNull public String title;
    public String description;
    public String location;
    @NonNull public String startAt;
    @NonNull public String endAt;
    @NonNull public String type;
    @NonNull public String status;

    /**
     * Khởi tạo đối tượng AppointmentEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public AppointmentEntity(long contactId, @NonNull String title, @NonNull String startAt, @NonNull String endAt, @NonNull String type, @NonNull String status) {
        this.contactId = contactId;
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.type = type;
        this.status = status;
    }
}
