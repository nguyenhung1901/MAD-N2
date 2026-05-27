package com.example.contactmanagerprojava.ui.notes;

import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.NoteEntity;
import com.example.contactmanagerprojava.databinding.FragmentNotesBinding;
import com.example.contactmanagerprojava.ui.adapters.NoteAdapter;
import com.example.contactmanagerprojava.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment ghi chú; hiển thị danh sách, tìm kiếm, lọc theo danh mục và thao tác thêm/sửa/xóa.
 */
public class NotesFragment extends Fragment {
    private FragmentNotesBinding binding;
    private final List<NoteEntity> allNotes = new ArrayList<>();
    private String activeCategory = "ALL";
    private NoteAdapter adapter;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        adapter = new NoteAdapter(new NoteAdapter.Listener() {
            @Override public void onEdit(NoteEntity note) {
                Intent intent = new Intent(requireContext(), NoteEditorActivity.class);
                intent.putExtra("note_id", note.id);
                // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
                startActivity(intent);
            }

            @Override public void onDelete(NoteEntity note) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa ghi chú")
                        .setMessage("Bạn có chắc muốn xóa ghi chú này?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (d, w) -> new AppRepository(requireContext()).deleteNote(note))
                        .show();
            }
        });
        binding.recyclerNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerNotes.setAdapter(adapter);
        long userId = new SessionManager(requireContext()).getUserId();
        AppRepository repository = new AppRepository(requireContext());
        repository.adoptLegacyContactNotes(userId);
        repository.observeNotes(userId).observe(getViewLifecycleOwner(), notes -> {
            allNotes.clear();
            if (notes != null) allNotes.addAll(notes);
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
        });
        // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
        binding.fabAddNote.setOnClickListener(v -> startActivity(new Intent(requireContext(), NoteEditorActivity.class)));
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipAll, "ALL");
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipGeneral, "Chung");
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipCall, "Cuộc gọi");
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipMeeting, "Cuộc họp");
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipIdea, "Ý tưởng");
    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
        installChip(binding.chipTask, "Việc cần làm");
        binding.etSearchNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    /**
     * Cập nhật lại trạng thái hiển thị của các nút lọc sau khi danh mục được chọn thay đổi.
     */
        refreshChipState();
        return binding.getRoot();
    }

    /**
     * Gắn sự kiện chọn cho một nút lọc danh mục ghi chú.
     */
    private void installChip(TextView view, String category) {
        view.setOnClickListener(v -> {
            activeCategory = category;
    /**
     * Cập nhật lại trạng thái hiển thị của các nút lọc sau khi danh mục được chọn thay đổi.
     */
            refreshChipState();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
        });
    }

    /**
     * Cập nhật lại trạng thái hiển thị của các nút lọc sau khi danh mục được chọn thay đổi.
     */
    private void refreshChipState() {
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipAll, "ALL", "ALL".equals(activeCategory));
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipGeneral, "Chung", "Chung".equals(activeCategory));
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipCall, "Cuộc gọi", "Cuộc gọi".equals(activeCategory));
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipMeeting, "Cuộc họp", "Cuộc họp".equals(activeCategory));
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipIdea, "Ý tưởng", "Ý tưởng".equals(activeCategory));
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
        updateChip(binding.chipTask, "Việc cần làm", "Việc cần làm".equals(activeCategory));
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    /**
     * Đổi nền và màu chữ của nút lọc để phân biệt trạng thái đang chọn và chưa chọn.
     */
    private void updateChip(TextView chip, String category, boolean selected) {
        if (!selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_idle);
            chip.setTextColor(requireContext().getColor(R.color.cm_text_primary));
        } else if ("Cuộc gọi".equals(category)) {
            chip.setBackgroundResource(R.drawable.bg_note_call);
            chip.setTextColor(requireContext().getColor(R.color.cm_green_icon));
        } else if ("Cuộc họp".equals(category)) {
            chip.setBackgroundResource(R.drawable.bg_note_meeting);
            chip.setTextColor(requireContext().getColor(R.color.cm_yellow_icon));
        } else if ("Ý tưởng".equals(category)) {
            chip.setBackgroundResource(R.drawable.bg_note_idea);
            chip.setTextColor(requireContext().getColor(R.color.cm_purple_icon));
        } else if ("Việc cần làm".equals(category)) {
            chip.setBackgroundResource(R.drawable.bg_note_task);
            chip.setTextColor(requireContext().getColor(R.color.cm_red_icon));
        } else {
            chip.setBackgroundResource(R.drawable.bg_note_general);
            chip.setTextColor(requireContext().getColor(R.color.cm_blue_icon));
        }
        chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    // Áp dụng tìm kiếm/lọc/sắp xếp lên danh sách dữ liệu hiển thị.
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
    private void applyFilters() {
        String query = binding.etSearchNote.getText() == null ? "" : binding.etSearchNote.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<NoteEntity> filtered = new ArrayList<>();
        for (NoteEntity item : allNotes) {
            boolean categoryOk = "ALL".equals(activeCategory) || activeCategory.equalsIgnoreCase(item.category);
            String haystack = ((item.title == null ? "" : item.title) + " " + (item.content == null ? "" : item.content) + " " + (item.category == null ? "" : item.category)).toLowerCase(Locale.getDefault());
            boolean queryOk = query.isEmpty() || haystack.contains(query);
            if (categoryOk && queryOk) filtered.add(item);
        }
    /**
     * Cập nhật danh sách dữ liệu mới cho Adapter và yêu cầu RecyclerView vẽ lại danh sách.
     */
        adapter.submitList(filtered);
        binding.tvEmptyNotes.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerNotes.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
