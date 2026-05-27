package com.example.contactmanagerprojava.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.contactmanagerprojava.util.PasswordSecurity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tầng Repository/Business Layer; gom logic nghiệp vụ và điều phối truy vấn giữa UI với các DAO của Room Database.
 */
public class AppRepository {
    private final AppDatabase db;

    /**
     * Khởi tạo đối tượng AppRepository với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public AppRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    /**
     * Kiểm tra thông tin đăng nhập của người dùng và trả về tài khoản nếu hợp lệ.
     */
    public UserEntity login(String email, String password) {
        if (email == null || password == null) return null;
        UserEntity user = db.userDao().findByEmail(email.trim().toLowerCase());
        if (user == null) return null;
    /**
     * Kiểm tra mật khẩu người dùng nhập có khớp với giá trị đã băm hay không.
     */
        boolean verified = PasswordSecurity.verifyPassword(password.trim(), user.password);
        if (!verified) return null;
        if (PasswordSecurity.needsMigration(user.password)) {
    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt để không lưu mật khẩu dạng rõ.
     */
            user.password = PasswordSecurity.hashPassword(password.trim());
            db.userDao().update(user);
        }
        return user;
    }

    /**
     * Tạo tài khoản người dùng cục bộ sau khi xác thực thông tin đăng ký.
     */
    public long register(String fullName, String email, String password, String provider) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt để không lưu mật khẩu dạng rõ.
     */
        String hashed = PasswordSecurity.hashPassword(password);
        return db.userDao().insert(new UserEntity(fullName, normalizedEmail, hashed, provider));
    }

    // Tìm bản ghi phù hợp theo điều kiện truyền vào.
    /**
     * Tìm người dùng cục bộ theo địa chỉ email.
     */
    public UserEntity findUserByEmail(String email) {
        return db.userDao().findByEmail(email == null ? null : email.trim().toLowerCase());
    }

    /**
     * Lấy thông tin người dùng theo userId đang được lưu trong phiên đăng nhập.
     */
    public UserEntity getUser(long userId) {
        return db.userDao().findById(userId);
    }

    // Tìm hoặc tạo user cục bộ tương ứng với tài khoản Firebase để liên kết dữ liệu Room theo userId local.
    /**
     * Liên kết tài khoản Firebase với bản ghi người dùng cục bộ; nếu chưa có thì tạo mới.
     */
    public UserEntity findOrCreateFirebaseUser(String fullName, String email, String provider) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isEmpty()) return null;

        UserEntity existing = db.userDao().findByEmail(normalizedEmail);
        String safeName = fullName == null ? "" : fullName.trim();
        if (safeName.isEmpty()) {
            int at = normalizedEmail.indexOf('@');
            safeName = at > 0 ? normalizedEmail.substring(0, at) : normalizedEmail;
        }

        if (existing != null) {
            boolean changed = false;
            if (existing.fullName == null || existing.fullName.trim().isEmpty() || existing.fullName.equalsIgnoreCase(existing.email)) {
                existing.fullName = safeName;
                changed = true;
            }
            if (provider != null && !provider.trim().isEmpty() && !provider.equals(existing.provider)) {
                existing.provider = provider;
                changed = true;
            }
            if (changed) db.userDao().update(existing);
            return existing;
        }

    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt để không lưu mật khẩu dạng rõ.
     */
        String localPasswordPlaceholder = PasswordSecurity.hashPassword("firebase:" + UUID.randomUUID());
        long id = db.userDao().insert(new UserEntity(safeName, normalizedEmail, localPasswordPlaceholder, provider == null ? "firebase" : provider));
        return db.userDao().findById(id);
    }

    // Đổi mật khẩu local khi còn dùng tài khoản nội bộ; với Firebase nên ưu tiên luồng đổi/reset mật khẩu của Firebase.
    /**
     * Đổi mật khẩu người dùng cục bộ sau khi kiểm tra mật khẩu hiện tại.
     */
    public boolean changePassword(long userId, String currentPassword, String newPassword) {
        UserEntity user = db.userDao().findById(userId);
        if (user == null) return false;
        if (!PasswordSecurity.verifyPassword(currentPassword == null ? "" : currentPassword.trim(), user.password)) return false;
    /**
     * Băm mật khẩu bằng PBKDF2 kèm salt để không lưu mật khẩu dạng rõ.
     */
        user.password = PasswordSecurity.hashPassword(newPassword == null ? "" : newPassword.trim());
        db.userDao().update(user);
        return true;
    }

    // Gán userId cho các ghi chú cũ chưa có chủ sở hữu để tương thích dữ liệu sau migration.
    public void adoptLegacyContactNotes(long userId) {
        db.noteDao().adoptLegacyContactNotes(userId);
    }

    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<ContactEntity>> observeContacts(long userId) { return db.contactDao().observeAll(userId); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<ContactEntity>> observeFavoriteContacts(long userId) { return db.contactDao().observeFavorites(userId); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<AppointmentEntity>> observeAppointments(long userId) { return db.appointmentDao().observeAll(userId); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<AppointmentEntity>> observeAppointmentsByDate(long userId, String date) { return db.appointmentDao().observeByDate(userId, date); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<AppointmentEntity>> observeAppointmentsForContact(long userId, long contactId) { return db.appointmentDao().observeForContact(userId, contactId); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<NoteEntity>> observeNotes(long userId) { return db.noteDao().observeAll(userId); }
    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    public LiveData<List<InteractionEntity>> observeInteractions(long contactId) { return db.interactionDao().observeForContact(contactId); }

    public ContactEntity getContact(long id) { return db.contactDao().getById(id); }
    public List<ContactPhoneEntity> getPhones(long contactId) { return db.contactPhoneDao().getByContact(contactId); }
    public List<ContactPhoneEntity> getPhonesForUser(long userId) { return db.contactPhoneDao().getByUser(userId); }
    public AppointmentEntity getAppointment(long id, long userId) { return db.appointmentDao().getById(id, userId); }
    public NoteEntity getNote(long id, long userId) { return db.noteDao().getById(id, userId); }
    public Long firstContactId(long userId) { return db.contactDao().firstContactId(userId); }
    public List<ContactEntity> getContactsSync(long userId) { return db.contactDao().getAllSync(userId); }
    public void clearLegacyDeviceImportTag(long userId) { db.contactDao().clearExactTag(userId, "Nh\u1eadp t\u1eeb m\u00e1y"); }

    // Thêm mới hoặc cập nhật liên hệ, sau đó ghi lại danh sách số điện thoại tương ứng.
    /**
     * Lưu thông tin liên hệ và danh sách số điện thoại tương ứng vào Room Database.
     */
    public void saveContact(ContactEntity contact, List<ContactPhoneEntity> phones) {
        if (contact.id == 0L) {
            long id = db.contactDao().insert(contact);
            for (ContactPhoneEntity phone : phones) phone.contactId = id;
            db.contactPhoneDao().insertAll(phones);
        } else {
            contact.updatedAt = System.currentTimeMillis();
            db.contactDao().update(contact);
            db.contactPhoneDao().deleteByContact(contact.id);
            for (ContactPhoneEntity phone : phones) phone.contactId = contact.id;
            db.contactPhoneDao().insertAll(phones);
        }
    }

    // Xóa bản ghi khỏi Room Database.
    public void deleteContact(ContactEntity contact) { db.contactDao().delete(contact); }

    // Thêm mới hoặc cập nhật lịch hẹn và ghi nhận tương tác khi tạo lịch hẹn mới.
    /**
     * Lưu lịch hẹn và ghi nhận tương tác nếu đây là lịch hẹn mới.
     */
    public void saveAppointment(AppointmentEntity appointment) {
    /**
     * Tính trạng thái thực tế của lịch hẹn dựa trên thời gian và trạng thái đang lưu.
     */
        appointment.status = computeStatus(appointment.startAt, appointment.endAt, appointment.status);
        boolean isNew = appointment.id == 0L;
        if (isNew) {
            long id = db.appointmentDao().insert(appointment);
            appointment.id = id;
    /**
     * Ghi nhận một lần tương tác với liên hệ, ví dụ gọi điện, gửi email, tạo ghi chú hoặc lịch hẹn.
     */
            addInteraction(appointment.contactId, "APPOINTMENT", "Tạo lịch hẹn: " + (appointment.title == null ? "" : appointment.title));
        } else {
            db.appointmentDao().update(appointment);
        }
    }

    // Xóa bản ghi khỏi Room Database.
    public void deleteAppointment(AppointmentEntity appointment) { db.appointmentDao().delete(appointment); }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    /**
     * Cập nhật trạng thái lịch hẹn như hoàn thành hoặc hủy.
     */
    public void updateAppointmentStatus(AppointmentEntity appointment, String status) {
        appointment.status = status;
        db.appointmentDao().update(appointment);
    }

    // Thêm mới hoặc cập nhật ghi chú và ghi nhận tương tác khi tạo ghi chú gắn với liên hệ.
    /**
     * Lưu ghi chú mới hoặc cập nhật ghi chú đã có trong bảng notes.
     */
    public void saveNote(NoteEntity note) {
        boolean isNew = note.id == 0L;
        if (isNew) {
            long id = db.noteDao().insert(note);
            note.id = id;
            if (note.contactId != null && note.contactId > 0) {
    /**
     * Ghi nhận một lần tương tác với liên hệ, ví dụ gọi điện, gửi email, tạo ghi chú hoặc lịch hẹn.
     */
                addInteraction(note.contactId, "NOTE", "Tạo ghi chú: " + (note.title == null ? "" : note.title));
            }
        } else {
            db.noteDao().update(note);
        }
    }

    // Xóa bản ghi khỏi Room Database.
    public void deleteNote(NoteEntity note) { db.noteDao().delete(note); }

    // Ghi một sự kiện tương tác vào bảng interactions.
    /**
     * Ghi nhận một lần tương tác với liên hệ, ví dụ gọi điện, gửi email, tạo ghi chú hoặc lịch hẹn.
     */
    public void addInteraction(long contactId, String type, String content) {
        db.interactionDao().insert(new InteractionEntity(contactId, type, content));
    }

    // Tổng hợp toàn bộ số liệu cho Dashboard/Stats từ nhiều DAO khác nhau.
    /**
     * Tổng hợp dữ liệu từ nhiều DAO để phục vụ màn hình Dashboard và Thống kê.
     */
    public DashboardStats dashboard(long userId) {
        DashboardStats stats = new DashboardStats();
        stats.totalContacts = db.contactDao().count(userId);
        stats.favoriteContacts = db.contactDao().favoriteCount(userId);
        stats.totalNotes = db.noteDao().count(userId);
        stats.totalInteractions = db.interactionDao().count(userId);
        stats.totalAppointments = db.appointmentDao().totalCount(userId);
        stats.upcomingAppointments = db.appointmentDao().upcomingCount(userId);
        stats.doneAppointments = db.appointmentDao().doneCount(userId);
        stats.cancelledAppointments = db.appointmentDao().cancelledCount(userId);
        stats.overdueAppointments = db.appointmentDao().overdueCount(userId);
        stats.topContacts = db.interactionDao().topContacts(userId);
        stats.noteCategories = db.noteDao().countByCategory(userId);
        stats.appointmentTypes = db.appointmentDao().countByType(userId);
        return stats;
    }

    // Tính trạng thái lịch hẹn dựa trên thời gian bắt đầu/kết thúc và thời điểm hiện tại.
    /**
     * Tính trạng thái thực tế của lịch hẹn dựa trên thời gian và trạng thái đang lưu.
     */
    private String computeStatus(String startAt, String endAt, String current) {
        if ("DONE".equals(current) || "CANCELLED".equals(current)) return current;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime start = LocalDateTime.parse(startAt, f);
        LocalDateTime end = LocalDateTime.parse(endAt, f);
        LocalDateTime now = LocalDateTime.now();
        if (end.isBefore(now)) return "OVERDUE";
        if (start.isAfter(now) || start.isEqual(now)) return "UPCOMING";
        return "UPCOMING";
    }

    public static class DashboardStats {
        public int totalContacts;
        public int favoriteContacts;
        public int totalNotes;
        public int totalInteractions;
        public int totalAppointments;
        public int upcomingAppointments;
        public int doneAppointments;
        public int cancelledAppointments;
        public int overdueAppointments;
        public List<TopContactRow> topContacts = new ArrayList<>();
        public List<CategoryCountRow> noteCategories = new ArrayList<>();
        public List<CategoryCountRow> appointmentTypes = new ArrayList<>();
    }
}
