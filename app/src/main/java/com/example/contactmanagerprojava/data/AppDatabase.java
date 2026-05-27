package com.example.contactmanagerprojava.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {UserEntity.class, ContactEntity.class, ContactPhoneEntity.class, AppointmentEntity.class, NoteEntity.class, InteractionEntity.class}, version = 2, exportSchema = false)
/**
 * Lớp RoomDatabase trung tâm của ứng dụng; khai báo toàn bộ Entity, DAO và cấu hình singleton cho cơ sở dữ liệu SQLite cục bộ.
 */
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        // Migration Room dùng để chuyển đổi schema database cũ sang schema mới mà không mất dữ liệu.
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notes ADD COLUMN userId INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "UPDATE notes SET userId = COALESCE(" +
                            "(SELECT c.userId FROM contacts c WHERE c.id = notes.contactId), " +
                            "CASE WHEN (SELECT COUNT(*) FROM users) = 1 " +
                            "THEN (SELECT id FROM users LIMIT 1) ELSE 0 END" +
                            ")"
            );
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_userId ON notes(userId)");
        }
    };

    public abstract UserDao userDao();
    public abstract ContactDao contactDao();
    public abstract ContactPhoneDao contactPhoneDao();
    public abstract AppointmentDao appointmentDao();
    public abstract NoteDao noteDao();
    public abstract InteractionDao interactionDao();

    // Trả về instance singleton của Room Database để toàn app dùng chung một kết nối.
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // API ngoài - Room: tạo database SQLite cục bộ thông qua Room ORM.
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "contact_manager_java.db")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
