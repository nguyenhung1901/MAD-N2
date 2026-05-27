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
 * DAO truy xuất bảng notes; quản lý ghi chú theo người dùng, liên hệ và danh mục.
 */
public interface NoteDao {
    // Thêm bản ghi mới vào Room Database.
    @Insert long insert(NoteEntity note);
    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    @Update void update(NoteEntity note);
    // Xóa bản ghi đang chọn khỏi database sau khi người dùng xác nhận.
    @Delete void delete(NoteEntity note);

    @Query("SELECT n.* FROM notes n LEFT JOIN contacts c ON n.contactId = c.id WHERE n.userId = :userId OR (n.userId = 0 AND n.contactId IS NOT NULL AND c.userId = :userId) ORDER BY n.createdAt DESC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    /**
     * Trả về LiveData danh sách dữ liệu theo người dùng để giao diện tự cập nhật khi CSDL thay đổi.
     */
    LiveData<List<NoteEntity>> observeAll(long userId);

    @Query("SELECT COUNT(*) FROM notes n LEFT JOIN contacts c ON n.contactId = c.id WHERE n.userId = :userId OR (n.userId = 0 AND n.contactId IS NOT NULL AND c.userId = :userId)")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Đếm số lượng bản ghi phù hợp với điều kiện truy vấn.
     */
    int count(long userId);

    @Query("SELECT n.category AS label, COUNT(*) AS total FROM notes n LEFT JOIN contacts c ON n.contactId = c.id WHERE n.userId = :userId OR (n.userId = 0 AND n.contactId IS NOT NULL AND c.userId = :userId) GROUP BY n.category ORDER BY total DESC")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Thống kê số lượng ghi chú theo từng danh mục để hiển thị trên biểu đồ.
     */
    List<CategoryCountRow> countByCategory(long userId);

    @Query("SELECT n.* FROM notes n LEFT JOIN contacts c ON n.contactId = c.id WHERE n.id = :id AND (n.userId = :userId OR (n.userId = 0 AND n.contactId IS NOT NULL AND c.userId = :userId)) LIMIT 1")
    /**
     * Truy vấn một bản ghi theo mã định danh để phục vụ màn hình chi tiết hoặc chỉnh sửa.
     */
    NoteEntity getById(long id, long userId);

    @Query("UPDATE notes SET userId = :userId WHERE userId = 0 AND contactId IN (SELECT id FROM contacts WHERE userId = :userId)")
    // Gán userId cho các ghi chú cũ chưa có chủ sở hữu để tương thích dữ liệu sau migration.
    void adoptLegacyContactNotes(long userId);
}
