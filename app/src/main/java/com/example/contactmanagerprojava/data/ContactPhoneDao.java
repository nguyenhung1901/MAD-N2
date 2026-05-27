package com.example.contactmanagerprojava.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
/**
 * DAO truy xuất bảng contact_phones; quản lý nhiều số điện thoại thuộc cùng một liên hệ.
 */
public interface ContactPhoneDao {
    // Thêm bản ghi mới vào Room Database.
    @Insert void insertAll(List<ContactPhoneEntity> items);
    @Query("DELETE FROM contact_phones WHERE contactId = :contactId")
    // Xóa bản ghi khỏi Room Database.
    void deleteByContact(long contactId);
    @Query("SELECT * FROM contact_phones WHERE contactId = :contactId ORDER BY primaryPhone DESC, id ASC")
    List<ContactPhoneEntity> getByContact(long contactId);

    @Query("SELECT p.* FROM contact_phones p INNER JOIN contacts c ON c.id = p.contactId WHERE c.userId = :userId")
    List<ContactPhoneEntity> getByUser(long userId);
}
