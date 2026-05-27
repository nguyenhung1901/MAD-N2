package com.example.contactmanagerprojava.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.NoteEntity;
import com.example.contactmanagerprojava.databinding.ItemNoteBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách ghi chú trên RecyclerView.
 *
 * Lớp này nhận danh sách NoteEntity từ NotesFragment, ánh xạ từng ghi chú vào layout
 * item_note.xml và chuyển các thao tác sửa/xóa về Fragment thông qua Listener.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.Holder> {
    /**
     * Interface callback để Adapter báo lại cho NotesFragment khi người dùng bấm sửa hoặc xóa.
     * Adapter chỉ xử lý hiển thị item, còn thao tác điều hướng/xóa dữ liệu được xử lý ở Fragment.
     */
    public interface Listener {
        void onEdit(NoteEntity note);
        void onDelete(NoteEntity note);
    }

    private final List<NoteEntity> items = new ArrayList<>();
    private final Listener listener;

    /**
     * Khởi tạo Adapter cùng Listener nhận sự kiện sửa/xóa ghi chú.
     *
     * @param listener đối tượng xử lý sự kiện khi người dùng bấm nút sửa hoặc xóa trên từng ghi chú
     */
    public NoteAdapter(Listener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách ghi chú cần hiển thị.
     *
     * Hàm này thường được gọi sau khi NotesFragment lấy dữ liệu từ Room Database hoặc sau khi
     * người dùng tìm kiếm/lọc theo danh mục. Adapter xóa dữ liệu cũ, thêm dữ liệu mới rồi gọi
     * notifyDataSetChanged() để RecyclerView vẽ lại danh sách.
     *
     * @param list danh sách ghi chú mới; có thể null nếu không có dữ liệu
     */
    public void submitList(List<NoteEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    /**
     * Tạo ViewHolder mới cho một item ghi chú.
     *
     * ViewBinding được dùng để inflate layout item_note.xml, giúp truy cập các thành phần giao diện
     * trong item mà không cần findViewById.
     */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    /**
     * Gán dữ liệu của ghi chú tại vị trí p vào item tương ứng trên RecyclerView.
     *
     * Hàm này thực hiện các việc chính: chuẩn hóa danh mục rỗng về "Chung", hiển thị tiêu đề,
     * nội dung, thời gian tạo, áp dụng màu/icon theo danh mục và gắn sự kiện cho nút sửa/xóa.
     */
    @Override
    public void onBindViewHolder(@NonNull Holder h, int p) {
        NoteEntity item = items.get(p);
        String category = item.category == null ? "Chung" : item.category;
        if ("Theo dõi".equals(category)) category = "Chung";
        h.binding.tvCategory.setText(category);
        h.binding.tvTitle.setText(item.title == null || item.title.isBlank() ? "Không tiêu đề" : item.title);
        h.binding.tvContent.setText(item.content == null || item.content.isBlank() ? "Không có nội dung" : item.content);
        h.binding.tvTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(item.createdAt)));
        applyCategoryStyle(h.binding, category);
        h.binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        h.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    /**
     * Thiết lập màu sắc, nền và biểu tượng cho item ghi chú theo danh mục.
     *
     * Cách hiển thị này giúp người dùng phân biệt nhanh các loại ghi chú như Cuộc gọi,
     * Cuộc họp, Ý tưởng, Việc cần làm hoặc ghi chú Chung.
     */
    private void applyCategoryStyle(ItemNoteBinding b, String category) {
        switch (category) {
            case "Cuộc gọi":
                b.tvCategory.setBackgroundResource(R.drawable.bg_note_call);
                b.tvCategory.setTextColor(b.getRoot().getContext().getColor(R.color.cm_green_icon));
                b.iconHolder.setBackgroundResource(R.drawable.bg_note_icon_call);
                b.ivCategoryIcon.setImageResource(R.drawable.ic_note_call);
                break;
            case "Cuộc họp":
                b.tvCategory.setBackgroundResource(R.drawable.bg_note_meeting);
                b.tvCategory.setTextColor(b.getRoot().getContext().getColor(R.color.cm_yellow_icon));
                b.iconHolder.setBackgroundResource(R.drawable.bg_note_icon_meeting);
                b.ivCategoryIcon.setImageResource(R.drawable.ic_note_meeting);
                break;
            case "Ý tưởng":
                b.tvCategory.setBackgroundResource(R.drawable.bg_note_idea);
                b.tvCategory.setTextColor(b.getRoot().getContext().getColor(R.color.cm_purple_icon));
                b.iconHolder.setBackgroundResource(R.drawable.bg_note_icon_idea);
                b.ivCategoryIcon.setImageResource(R.drawable.ic_note_idea);
                break;
            case "Việc cần làm":
                b.tvCategory.setBackgroundResource(R.drawable.bg_note_task);
                b.tvCategory.setTextColor(b.getRoot().getContext().getColor(R.color.cm_red_icon));
                b.iconHolder.setBackgroundResource(R.drawable.bg_note_icon_task);
                b.ivCategoryIcon.setImageResource(R.drawable.ic_note_task);
                break;
            default:
                b.tvCategory.setBackgroundResource(R.drawable.bg_note_general);
                b.tvCategory.setTextColor(b.getRoot().getContext().getColor(R.color.cm_blue_icon));
                b.iconHolder.setBackgroundResource(R.drawable.bg_note_icon_general);
                b.ivCategoryIcon.setImageResource(R.drawable.ic_note_general);
                break;
        }
    }

    /**
     * Trả về số lượng ghi chú hiện đang có trong Adapter.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder giữ binding của layout item_note.xml để RecyclerView tái sử dụng item hiệu quả.
     */
    static class Holder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        Holder(ItemNoteBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }
}
