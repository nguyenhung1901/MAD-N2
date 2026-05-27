package com.example.contactmanagerprojava.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
/**
 * DAO truy xuất bảng interactions; lưu và thống kê lịch sử tương tác với từng liên hệ.
 */
public interface InteractionDao {
    // Thêm bản ghi mới vào Room Database.
    @Insert long insert(InteractionEntity interaction);

    @Query("SELECT * FROM interactions WHERE contactId = :contactId ORDER BY createdAt DESC")
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    LiveData<List<InteractionEntity>> observeForContact(long contactId);

    @Query("SELECT COUNT(*) FROM interactions i INNER JOIN contacts c ON c.id = i.contactId WHERE c.userId = :userId")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Đếm số lượng bản ghi phù hợp với điều kiện truy vấn.
     */
    int count(long userId);

    @Query("SELECT COUNT(*) FROM (" +
            " SELECT i.id AS rowId FROM interactions i INNER JOIN contacts c ON c.id = i.contactId WHERE c.userId = :userId" +
            " UNION ALL SELECT n.id AS rowId FROM notes n WHERE n.userId = :userId AND n.contactId IS NOT NULL" +
            " UNION ALL SELECT a.id AS rowId FROM appointments a INNER JOIN contacts c2 ON c2.id = a.contactId WHERE c2.userId = :userId" +
            ")")
    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    int countDerived(long userId);

    @Query("SELECT i.contactId AS contactId, c.fullName AS contactName, COUNT(*) as total FROM interactions i INNER JOIN contacts c ON c.id = i.contactId WHERE c.userId = :userId GROUP BY i.contactId, c.fullName ORDER BY total DESC LIMIT 5")
    List<TopContactRow> topContacts(long userId);

    @Query("SELECT x.contactId AS contactId, c.fullName AS contactName, COUNT(*) AS total FROM (" +
            " SELECT i.contactId AS contactId FROM interactions i INNER JOIN contacts c0 ON c0.id = i.contactId WHERE c0.userId = :userId" +
            " UNION ALL SELECT n.contactId AS contactId FROM notes n WHERE n.userId = :userId AND n.contactId IS NOT NULL" +
            " UNION ALL SELECT a.contactId AS contactId FROM appointments a INNER JOIN contacts c1 ON c1.id = a.contactId WHERE c1.userId = :userId" +
            ") x INNER JOIN contacts c ON c.id = x.contactId GROUP BY x.contactId, c.fullName ORDER BY total DESC LIMIT 5")
    List<TopContactRow> topContactsDerived(long userId);
}
