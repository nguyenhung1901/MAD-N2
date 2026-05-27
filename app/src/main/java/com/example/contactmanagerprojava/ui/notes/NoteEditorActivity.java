package com.example.contactmanagerprojava.ui.notes;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.NoteEntity;
import com.example.contactmanagerprojava.databinding.ActivityNoteEditorBinding;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.SessionManager;

/**
 * Màn hình thêm/sửa ghi chú; nhập tiêu đề, phân loại và nội dung ghi chú.
 */
public class NoteEditorActivity extends AppCompatActivity {
    private ActivityNoteEditorBinding binding;
    private AppRepository repository;
    private long noteId = 0L;
    private long userId = 0L;

    @Override protected void onCreate(Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        binding = ActivityNoteEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        repository = new AppRepository(this);
        userId = new SessionManager(this).getUserId();
        String[] categories = new String[]{"Chung", "Cuộc gọi", "Cuộc họp", "Ý tưởng", "Việc cần làm"};
        binding.spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories));
        noteId = getIntent().getLongExtra("note_id", 0L);
        if (noteId != 0L) {
            NoteEntity existing = repository.getNote(noteId, userId);
            if (existing != null) {
                binding.tvTitleScreen.setText("Sửa ghi chú");
                binding.btnSave.setText("Lưu");
                binding.etTitle.setText(existing.title);
                binding.etContent.setText(existing.content == null ? "" : existing.content);
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(existing.category)) {
                        binding.spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
        }
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                binding.etTitle.setError("Vui lòng nhập tiêu đề");
                binding.etTitle.requestFocus();
                return;
            }
            NoteEntity note;
            if (noteId != 0L) {
                note = repository.getNote(noteId, userId);
                if (note == null) {
                    note = new NoteEntity(userId, null, title, binding.etContent.getText().toString().trim(), binding.spinnerCategory.getSelectedItem().toString());
                } else {
                    note.userId = userId;
                    note.title = title;
                    note.content = binding.etContent.getText().toString().trim();
                    note.category = binding.spinnerCategory.getSelectedItem().toString();
                }
            } else {
                note = new NoteEntity(userId, null, title, binding.etContent.getText().toString().trim(), binding.spinnerCategory.getSelectedItem().toString());
            }
    /**
     * Lưu ghi chú mới hoặc cập nhật ghi chú đã có trong bảng notes.
     */
            repository.saveNote(note);
            Toast.makeText(this, noteId == 0L ? "Đã lưu ghi chú" : "Đã cập nhật ghi chú", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
