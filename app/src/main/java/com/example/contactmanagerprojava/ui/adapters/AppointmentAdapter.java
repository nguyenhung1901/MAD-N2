package com.example.contactmanagerprojava.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.AppointmentEntity;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.data.ContactPhoneEntity;
import com.example.contactmanagerprojava.databinding.ItemAppointmentBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter hiển thị danh sách lịch hẹn và gửi sự kiện thao tác về màn hình gọi.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.Holder> {
    public interface Listener {
        void onEdit(AppointmentEntity item);
        void onDelete(AppointmentEntity item);
        void onDone(AppointmentEntity item);
        void onCancel(AppointmentEntity item);
    }

    private final List<AppointmentEntity> items = new ArrayList<>();
    private final Listener listener;
    private final Context context;
    private final AppRepository repository;

    /**
     * Khởi tạo đối tượng AppointmentAdapter với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public AppointmentAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.repository = new AppRepository(context);
    }

    /**
     * Cập nhật danh sách dữ liệu mới cho Adapter và yêu cầu RecyclerView vẽ lại danh sách.
     */
    public void submitList(List<AppointmentEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAppointmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int p) {
        AppointmentEntity item = items.get(p);
        ContactEntity contact = repository.getContact(item.contactId);
        List<ContactPhoneEntity> phones = repository.getPhones(item.contactId);
        String phone = phones.isEmpty() ? null : phones.get(0).phoneNumber;
        h.binding.tvTitle.setText(item.title);
        h.binding.tvDate.setText(safeDate(item.startAt));
        h.binding.tvTime.setText(safeTime(item.startAt) + " - " + safeTime(item.endAt));
        h.binding.tvLocation.setText(TextUtils.isEmpty(item.location) ? "" : item.location);
        h.binding.tvLocation.setVisibility(TextUtils.isEmpty(item.location) ? View.GONE : View.VISIBLE);
        h.binding.tvDescription.setText(TextUtils.isEmpty(item.description) ? "" : item.description);
        h.binding.tvDescription.setVisibility(TextUtils.isEmpty(item.description) ? View.GONE : View.VISIBLE);
        h.binding.tvContactName.setText(contact != null ? contact.fullName : "");
        h.binding.tvContactName.setVisibility(contact != null && !TextUtils.isEmpty(contact.fullName) ? View.VISIBLE : View.GONE);
        h.binding.tvStatus.setText(statusLabel(item.status));
        h.binding.tvStatus.setBackgroundResource("DONE".equals(item.status) ? R.drawable.bg_icon_green : ("CANCELLED".equals(item.status) ? R.drawable.bg_icon_red : R.drawable.bg_chip_selected));
        h.binding.tvStatus.setTextColor(context.getColor("DONE".equals(item.status) ? R.color.cm_green_icon : ("CANCELLED".equals(item.status) ? R.color.cm_red_icon : R.color.cm_primary)));
        h.binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        h.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        h.binding.btnDone.setOnClickListener(v -> listener.onDone(item));
        h.binding.btnCancel.setOnClickListener(v -> listener.onCancel(item));
        h.binding.btnCall.setVisibility(TextUtils.isEmpty(phone) ? View.GONE : View.VISIBLE);
        // API ngoài - Android Intent: mở ứng dụng gọi điện và để người dùng xác nhận cuộc gọi.
        h.binding.btnCall.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
        h.binding.btnEmail.setVisibility(contact == null || TextUtils.isEmpty(contact.email) ? View.GONE : View.VISIBLE);
        if (contact != null && !TextUtils.isEmpty(contact.email)) {
            // API ngoài - Android Intent: mở ứng dụng email với địa chỉ người nhận.
            h.binding.btnEmail.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact.email))));
        }
        boolean canChange = !"DONE".equals(item.status) && !"CANCELLED".equals(item.status);
        h.binding.btnDone.setVisibility(canChange ? View.VISIBLE : View.GONE);
        h.binding.btnCancel.setVisibility(canChange ? View.VISIBLE : View.GONE);
    }

    private String statusLabel(String status) {
        if ("DONE".equals(status)) return "Hoàn thành";
        if ("CANCELLED".equals(status)) return "Hủy";
        return "Đã lên lịch";
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private String safeDate(String value) {
        return value != null && value.length() >= 10 ? value.substring(0, 10).replace('-', '/') : value;
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private String safeTime(String value) {
        return value != null && value.length() >= 16 ? value.substring(11, 16) : value;
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemAppointmentBinding binding;
        Holder(ItemAppointmentBinding b) { super(b.getRoot()); binding = b; }
    }
}
