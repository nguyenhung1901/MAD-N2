package com.example.contactmanagerprojava.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.data.ContactPhoneEntity;
import com.example.contactmanagerprojava.databinding.ItemContactBinding;
import com.example.contactmanagerprojava.util.AppSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter hiển thị danh sách liên hệ, nhãn, số điện thoại và các thao tác nhanh.
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.Holder> {
    public interface Listener {
        void onClick(long contactId);
        void onEdit(long contactId);
        void onDelete(ContactEntity item);
        void onToggleFavorite(ContactEntity item);
    }

    private final LayoutInflater inflater;
    private final AppRepository repository;
    private final List<ContactEntity> items = new ArrayList<>();
    private Listener listener;
    private static final String LEGACY_DEVICE_IMPORT_TAG = "Nh\u1eadp t\u1eeb m\u00e1y";
    private static final String NO_LABEL_TEXT = "Kh\u00f4ng c\u00f3 nh\u00e3n";

    /**
     * Khởi tạo đối tượng ContactAdapter với các thông tin cần thiết trước khi lưu hoặc hiển thị.
     */
    public ContactAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.repository = new AppRepository(context);
    }
    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    public void setListener(Listener listener) { this.listener = listener; }
    public void submitList(List<ContactEntity> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }
    public void filter(String q, List<ContactEntity> original) {
        items.clear();
        String lower = q.toLowerCase(Locale.ROOT);
        for (ContactEntity item : original) if (item.fullName.toLowerCase(Locale.ROOT).contains(lower)) items.add(item);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return new Holder(ItemContactBinding.inflate(inflater, parent, false)); }
    @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
        ContactEntity item = items.get(pos);
        h.binding.tvAvatar.setText(item.fullName.substring(0,1).toUpperCase(Locale.ROOT));
        h.binding.tvName.setText(AppSettings.maskName(h.binding.getRoot().getContext(), item.fullName));

        bindTextOrGone(h.binding.tvSubtitle, safe(item.position));
        bindTextOrGone(h.binding.tvCompanyLine, safe(item.company));

        List<ContactPhoneEntity> phones = repository.getPhones(item.id);
        if (!phones.isEmpty() && phones.get(0).phoneNumber != null && !phones.get(0).phoneNumber.isBlank()) {
            h.binding.layoutPhoneRow.setVisibility(android.view.View.VISIBLE);
            h.binding.tvPhone.setText(AppSettings.maskPhone(h.binding.getRoot().getContext(), phones.get(0).phoneNumber));
        } else {
            h.binding.layoutPhoneRow.setVisibility(android.view.View.GONE);
        }

        if (item.email != null && !item.email.isBlank()) {
            h.binding.layoutEmailRow.setVisibility(android.view.View.VISIBLE);
            h.binding.tvEmail.setText(AppSettings.maskEmail(h.binding.getRoot().getContext(), item.email));
        } else {
            h.binding.layoutEmailRow.setVisibility(android.view.View.GONE);
        }

        bindTags(h, item.tags);

        h.binding.tvUpdated.setText("Cập nhật: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(item.updatedAt)));
        h.binding.btnFavorite.setImageResource(item.favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        h.binding.getRoot().setOnClickListener(v -> { if (listener != null) listener.onClick(item.id); });
        h.binding.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(item); });
        h.binding.btnFavorite.setOnClickListener(v -> { if (listener != null) listener.onToggleFavorite(item); });
    }
    @Override public int getItemCount() { return items.size(); }

    // Gắn dữ liệu vào giao diện tương ứng.
    private void bindTextOrGone(TextView view, String value) {
        if (value.isEmpty()) {
            view.setVisibility(android.view.View.GONE);
        } else {
            view.setVisibility(android.view.View.VISIBLE);
            view.setText(value);
        }
    }

    // Gắn dữ liệu vào giao diện tương ứng.
    private void bindTags(Holder h, String rawTags) {
        h.binding.layoutTags.removeAllViews();
        List<String> tags = parseTags(rawTags);
        if (tags.isEmpty()) {
            h.binding.layoutTags.setVisibility(android.view.View.GONE);
            h.binding.tvPrimaryTag.setVisibility(android.view.View.GONE);
            return;
        }
        h.binding.tvPrimaryTag.setVisibility(android.view.View.VISIBLE);
        h.binding.tvPrimaryTag.setText(tags.get(0));
        if (tags.size() == 1) {
            h.binding.layoutTags.setVisibility(android.view.View.GONE);
            return;
        }
        h.binding.layoutTags.setVisibility(android.view.View.VISIBLE);
        for (int i = 1; i < tags.size(); i++) {
            TextView chip = new TextView(h.binding.getRoot().getContext());
            chip.setText(tags.get(i));
            chip.setTextSize(11);
            chip.setTextColor(ContextCompat.getColor(h.binding.getRoot().getContext(), colorForTagText(tags.get(i))));
            chip.setBackgroundResource(R.drawable.bg_contact_label);
            chip.setPadding(dp(h, 8), dp(h, 4), dp(h, 8), dp(h, 4));
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(h, 8);
            chip.setLayoutParams(lp);
            h.binding.layoutTags.addView(chip);
        }
    }

    private List<String> parseTags(String value) {
        List<String> out = new ArrayList<>();
        if (value == null) return out;
        for (String part : value.split("[,;#]")) {
            String t = part.trim();
            if (!t.isEmpty() && !NO_LABEL_TEXT.equalsIgnoreCase(t) && !LEGACY_DEVICE_IMPORT_TAG.equalsIgnoreCase(t)) out.add(t);
        }
        return out;
    }


    private int colorForTagText(String tag) {
        String t = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "gia đình": return R.color.cm_tag_family;
            case "bạn bè": return R.color.cm_tag_friends;
            case "công việc": return R.color.cm_tag_work;
            case "khách hàng": return R.color.cm_tag_customer;
            case "đối tác": return R.color.cm_tag_partner;
            case "khẩn cấp": return R.color.cm_tag_emergency;
            default: return R.color.cm_primary;
        }
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // Chuyển đổi đơn vị dp sang pixel theo mật độ màn hình hiện tại.
    private int dp(Holder h, int value) {
        float density = h.binding.getRoot().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemContactBinding binding;
        Holder(ItemContactBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
