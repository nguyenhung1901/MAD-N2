package com.example.contactmanagerprojava.ui.contacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.AppointmentEntity;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.data.ContactPhoneEntity;
import com.example.contactmanagerprojava.databinding.ActivityContactDetailBinding;
import com.example.contactmanagerprojava.ui.MainActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.example.contactmanagerprojava.util.AppSettings;

/**
 * Màn hình chi tiết liên hệ; hiển thị thông tin, số điện thoại, ghi chú, lịch hẹn và lịch sử tương tác.
 */
public class ContactDetailActivity extends AppCompatActivity {
    private ActivityContactDetailBinding binding;
    private AppRepository repository;
    private long contactId;

    @Override protected void onCreate(Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        binding = ActivityContactDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        repository = new AppRepository(this);
        contactId = getIntent().getLongExtra("contactId", 0L);
        bind();
        observeAppointments();
        binding.btnBack.setOnClickListener(v -> finish());
        // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
        binding.btnEdit.setOnClickListener(v -> startActivity(new Intent(this, ContactEditorActivity.class).putExtra("contactId", contactId)));
        binding.btnViewAllAppointments.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("openMenuId", R.id.nav_appointments);
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            startActivity(intent);
        });
    }

    // Gắn dữ liệu từ model vào các thành phần giao diện.
    private void bind() {
        ContactEntity c = repository.getContact(contactId);
        if (c == null || c.fullName == null || c.fullName.isBlank()) return;
        List<ContactPhoneEntity> phones = repository.getPhones(contactId);
        binding.tvAvatar.setText(c.fullName.substring(0, 1).toUpperCase(Locale.ROOT));
        binding.tvName.setText(AppSettings.maskName(this, c.fullName));
        setTextOrGone(binding.tvPosition, safe(c.position));
        setTextOrGone(binding.tvCompany, safe(c.company));
        renderTags(parseTags(c.tags));

        String primaryPhone = phones.isEmpty() ? "" : safe(phones.get(0).phoneNumber);
        if (!primaryPhone.isEmpty()) {
            binding.layoutPrimaryPhone.setVisibility(View.VISIBLE);
            binding.tvPrimaryPhone.setText(AppSettings.maskPhone(this, primaryPhone));
            binding.layoutPrimaryPhone.setOnClickListener(v -> dial(primaryPhone));
            binding.btnCallPrimary.setOnClickListener(v -> dial(primaryPhone));
        } else binding.layoutPrimaryPhone.setVisibility(View.GONE);

        String secondaryPhone = phones.size() > 1 ? safe(phones.get(1).phoneNumber) : "";
        if (!secondaryPhone.isEmpty()) {
            binding.layoutSecondaryPhone.setVisibility(View.VISIBLE);
            binding.tvSecondaryPhone.setText(AppSettings.maskPhone(this, secondaryPhone));
            binding.layoutSecondaryPhone.setOnClickListener(v -> dial(secondaryPhone));
            binding.btnCallSecondary.setOnClickListener(v -> dial(secondaryPhone));
        } else binding.layoutSecondaryPhone.setVisibility(View.GONE);

        if (c.email != null && !c.email.isBlank()) {
            binding.layoutEmailSection.setVisibility(View.VISIBLE);
            binding.tvEmail.setText(AppSettings.maskEmail(this, c.email));
            binding.layoutEmailSection.setOnClickListener(v -> email(c.email));
            binding.btnEmail.setOnClickListener(v -> email(c.email));
        } else binding.layoutEmailSection.setVisibility(View.GONE);

        setRow(binding.layoutDepartment, binding.tvDepartment, safe(c.department));
        setRow(binding.layoutAddress, binding.tvAddress, safe(c.address));
        setRow(binding.layoutBirthday, binding.tvBirthday, safe(c.birthday));
        setRow(binding.layoutNote, binding.tvNote, cleanAutoImportNote(c.note));
    }

    // Trả về LiveData để UI tự động cập nhật khi dữ liệu Room thay đổi.
    private void observeAppointments() {
        long userId = new com.example.contactmanagerprojava.util.SessionManager(this).getUserId();
        repository.observeAppointmentsForContact(userId, contactId).observe(this, list -> renderAppointments(list == null ? new ArrayList<>() : list));
    }

    private void renderAppointments(List<AppointmentEntity> list) {
        binding.appointmentsContainer.removeAllViews();
        binding.tvAppointmentsTitle.setText("Lịch hẹn (" + list.size() + ")");
        if (list.isEmpty()) {
            binding.layoutAppointmentsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.layoutAppointmentsEmpty.setVisibility(View.GONE);
        int count = Math.min(3, list.size());
        for (int i = 0; i < count; i++) {
            AppointmentEntity a = list.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_alt);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(10);
            card.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText(safe(a.title).isEmpty() ? "Lịch hẹn" : a.title);
            title.setTextSize(15f);
            title.setTextColor(getColor(R.color.cm_text_primary));
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title);

            if (!safe(a.description).isEmpty()) {
                TextView desc = new TextView(this);
                desc.setText(a.description);
                desc.setTextSize(13f);
                desc.setTextColor(getColor(R.color.cm_text_secondary));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                dlp.topMargin = dp(4);
                desc.setLayoutParams(dlp);
                card.addView(desc);
            }

            TextView dt = new TextView(this);
            dt.setText(formatDateTimeRange(a.startAt, a.endAt));
            dt.setTextSize(13f);
            dt.setTextColor(getColor(R.color.cm_primary));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(8);
            dt.setLayoutParams(tlp);
            card.addView(dt);

            if (!safe(a.location).isEmpty()) {
                TextView loc = new TextView(this);
                loc.setText(a.location);
                loc.setTextSize(13f);
                loc.setTextColor(getColor(R.color.cm_text_secondary));
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                llp.topMargin = dp(4);
                loc.setLayoutParams(llp);
                card.addView(loc);
            }
            binding.appointmentsContainer.addView(card);
        }
    }

    // Định dạng dữ liệu thành chuỗi dễ đọc cho giao diện.
    private String formatDateTimeRange(String startAt, String endAt) {
        try {
            LocalDateTime start = LocalDateTime.parse(startAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))) + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm", new Locale("vi", "VN")));
        } catch (Exception e) {
            return safe(startAt);
        }
    }

    private void renderTags(List<String> tags) {
        binding.tagsContainer.removeAllViews();
        if (tags.isEmpty()) {
            binding.layoutTagsSection.setVisibility(View.GONE);
            return;
        }
        binding.layoutTagsSection.setVisibility(View.VISIBLE);
        for (String tag : tags) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextSize(12f);
            chip.setTextColor(getColor(R.color.cm_primary));
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8); lp.bottomMargin = dp(8);
            chip.setLayoutParams(lp);
            binding.tagsContainer.addView(chip);
        }
    }

    // Chuyển đổi đơn vị dp sang pixel theo mật độ màn hình hiện tại.
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
    // API ngoài - Android Intent: mở ứng dụng gọi điện và để người dùng xác nhận cuộc gọi.
    private void dial(String phone) { if (phone == null || phone.isBlank()) return; repository.addInteraction(contactId, "CALL", "Gọi điện"); startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); }
    // API ngoài - Android Intent: mở ứng dụng email với địa chỉ người nhận.
    private void email(String email) { if (email == null || email.isBlank()) return; repository.addInteraction(contactId, "EMAIL", "Gửi email"); startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email))); }

    private List<String> parseTags(String value) {
        List<String> out = new ArrayList<>();
        if (value == null) return out;
        for (String part : value.split("[,;#]")) {
            String t = part.trim();
            if (!t.isEmpty() && !"Không có nhãn".equalsIgnoreCase(t)) out.add(t);
        }
        return out;
    }

    private String cleanAutoImportNote(String value) {
        String note = safe(value);
        if (note.equalsIgnoreCase("Liên hệ được nhập từ danh bạ điện thoại")
                || note.equalsIgnoreCase("Liên hệ được nhập từ danh bạ điện thoại.")) {
            return "";
        }
        return note;
    }

    // Chuẩn hóa giá trị đầu vào để tránh null hoặc chuỗi rỗng không mong muốn.
    private String safe(String value) { return value == null ? "" : value.trim(); }
    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setTextOrGone(TextView view, String value) { if (value == null || value.isBlank()) view.setVisibility(View.GONE); else { view.setVisibility(View.VISIBLE); view.setText(value);} }
    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setRow(View container, TextView view, String value) {
        if (value == null || value.isBlank()) {
            container.setVisibility(View.GONE);
        } else {
            container.setVisibility(View.VISIBLE);
            view.setText(AppSettings.maskGeneric(this, value));
        }
    }
}
