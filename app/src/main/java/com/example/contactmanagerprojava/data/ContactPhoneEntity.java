package com.example.contactmanagerprojava.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "contact_phones", foreignKeys = @ForeignKey(entity = ContactEntity.class, parentColumns = "id", childColumns = "contactId", onDelete = ForeignKey.CASCADE), indices = {@Index("contactId")})
/**
 * Entity Room đại diện cho một số điện thoại của liên hệ, bao gồm nhãn và trạng thái số chính.
 */
public class ContactPhoneEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long contactId;
    @NonNull public String phoneNumber;
    public String label;
    public boolean primaryPhone;

    /**
     * Khởi tạo đối tượng ContactPhoneEntity với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public ContactPhoneEntity(long contactId, @NonNull String phoneNumber, String label, boolean primaryPhone) {
        this.contactId = contactId;
        this.phoneNumber = phoneNumber;
        this.label = label;
        this.primaryPhone = primaryPhone;
    }
}
