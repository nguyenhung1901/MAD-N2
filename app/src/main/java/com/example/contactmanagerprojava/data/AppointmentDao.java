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
 * DAO truy xuất bảng appointments; cung cấp các truy vấn lịch hẹn theo người dùng, ngày, liên hệ, trạng thái và loại lịch hẹn.
 */
public interface AppointmentDao {
    // Thêm bản ghi mới vào Room Database.
    @Insert long insert(AppointmentEntity appointment);
    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    @Update void update(AppointmentEntity appointment);
    // Xóa bản ghi đang chọn khỏi database sau khi người dùng xác nhận.
    @Delete void delete(AppointmentEntity appointment);

    @Query("SELECT a.* FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId ORDER BY a.startAt ASC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    /**
     * Trả về LiveData danh sách dữ liệu theo người dùng để giao diện tự cập nhật khi CSDL thay đổi.
     */
    LiveData<List<AppointmentEntity>> observeAll(long userId);

    @Query("SELECT a.* FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId AND substr(a.startAt,1,10) = :date ORDER BY a.startAt ASC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    LiveData<List<AppointmentEntity>> observeByDate(long userId, String date);

    @Query("SELECT a.* FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE a.id = :id AND c.userId = :userId LIMIT 1")
    /**
     * Truy vấn một bản ghi theo mã định danh để phục vụ màn hình chi tiết hoặc chỉnh sửa.
     */
    AppointmentEntity getById(long id, long userId);

    @Query("SELECT a.* FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE a.contactId = :contactId AND c.userId = :userId ORDER BY a.startAt ASC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    LiveData<List<AppointmentEntity>> observeForContact(long userId, long contactId);

    @Query("SELECT COUNT(*) FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int totalCount(long userId);

    @Query("SELECT COUNT(*) FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId AND a.status = 'UPCOMING'")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int upcomingCount(long userId);

    @Query("SELECT COUNT(*) FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId AND a.status = 'DONE'")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int doneCount(long userId);

    @Query("SELECT COUNT(*) FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId AND a.status = 'CANCELLED'")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int cancelledCount(long userId);

    @Query("SELECT COUNT(*) FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId AND a.status = 'OVERDUE'")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int overdueCount(long userId);

    @Query("SELECT a.type AS label, COUNT(*) AS total FROM appointments a INNER JOIN contacts c ON a.contactId = c.id WHERE c.userId = :userId GROUP BY a.type ORDER BY total DESC")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Thống kê số lượng lịch hẹn theo từng loại để hiển thị trên biểu đồ.
     */
    List<CategoryCountRow> countByType(long userId);
}
