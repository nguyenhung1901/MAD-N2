package com.example.contactmanagerprojava.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
/**
 * DAO truy xuất bảng contacts; xử lý danh sách liên hệ, yêu thích, tìm kiếm, đếm số lượng và một số cập nhật nhanh.
 */
public interface ContactDao {
    // Thêm bản ghi mới vào Room Database.
    @Insert long insert(ContactEntity contact);
    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    @Update void update(ContactEntity contact);
    // Xóa bản ghi đang chọn khỏi database sau khi người dùng xác nhận.
    @Delete void delete(ContactEntity contact);
    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY favorite DESC, fullName COLLATE NOCASE ASC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    /**
     * Trả về LiveData danh sách dữ liệu theo người dùng để giao diện tự cập nhật khi CSDL thay đổi.
     */
    LiveData<List<ContactEntity>> observeAll(long userId);
    @Query("SELECT * FROM contacts WHERE userId = :userId AND favorite = 1 ORDER BY fullName COLLATE NOCASE ASC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    LiveData<List<ContactEntity>> observeFavorites(long userId);
    @Query("SELECT * FROM contacts WHERE userId = :userId AND fullName LIKE '%' || :query || '%' ORDER BY favorite DESC, fullName COLLATE NOCASE ASC")
    LiveData<List<ContactEntity>> search(long userId, String query);
    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    /**
     * Truy vấn một bản ghi theo mã định danh để phục vụ màn hình chi tiết hoặc chỉnh sửa.
     */
    ContactEntity getById(long id);
    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Đếm số lượng bản ghi phù hợp với điều kiện truy vấn.
     */
    int count(long userId);
    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId AND favorite = 1")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int favoriteCount(long userId);
    @Query("SELECT id FROM contacts WHERE userId = :userId ORDER BY updatedAt DESC, id ASC LIMIT 1")
    Long firstContactId(long userId);
    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY favorite DESC, fullName COLLATE NOCASE ASC")
    List<ContactEntity> getAllSync(long userId);

    @Query("UPDATE contacts SET tags = NULL WHERE userId = :userId AND tags = :legacyTag")
    void clearExactTag(long userId, String legacyTag);
}
