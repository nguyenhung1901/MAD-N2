package com.example.contactmanagerprojava.ui.dashboard;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.AppointmentEntity;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.databinding.FragmentDashboardBinding;
import com.example.contactmanagerprojava.ui.MainActivity;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.ui.contacts.ContactDetailActivity;
import com.example.contactmanagerprojava.util.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Fragment tổng quan; hiển thị số liệu nhanh, lịch hẹn sắp tới, liên hệ yêu thích và liên hệ gần đây.
 */
public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private AppRepository repository;

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        repository = new AppRepository(requireContext());
        long userId = new SessionManager(requireContext()).getUserId();
        binding.tvHeroSubtitle.setText("Hôm nay là " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"))));
    /**
     * Tổng hợp dữ liệu từ nhiều DAO để phục vụ màn hình Dashboard và Thống kê.
     */
        AppRepository.DashboardStats stats = repository.dashboard(userId);
        binding.tvTotalContacts.setText(String.valueOf(stats.totalContacts));
        binding.tvFavorites.setText(String.valueOf(stats.favoriteContacts));
        binding.tvUpcoming.setText(String.valueOf(stats.upcomingAppointments));
        binding.tvInteractions.setText(String.valueOf(stats.totalInteractions));
        repository.observeAppointments(userId).observe(getViewLifecycleOwner(), this::bindUpcomingAppointments);
        repository.observeFavoriteContacts(userId).observe(getViewLifecycleOwner(), this::bindFavorites);
    /**
     * Quan sát danh sách liên hệ của người dùng thông qua Room LiveData.
     */
        repository.observeContacts(userId).observe(getViewLifecycleOwner(), this::bindRecentContacts);

        binding.btnViewAppointments.setOnClickListener(v -> navigateTo(R.id.nav_appointments));
        binding.btnViewFavorites.setOnClickListener(v -> navigateTo(R.id.nav_contacts));
        return binding.getRoot();
    }

    // Điều hướng MainActivity sang menu/Fragment tương ứng.
    private void navigateTo(int menuId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateTo(menuId);
        }
    }

    // Gắn dữ liệu vào giao diện tương ứng.
    private void bindUpcomingAppointments(List<AppointmentEntity> source) {
        binding.upcomingContainer.removeAllViews();
        List<AppointmentEntity> items = new ArrayList<>();
        for (AppointmentEntity item : source) if ("UPCOMING".equals(item.status)) items.add(item);
        items.sort(Comparator.comparing(a -> a.startAt));
        if (items.isEmpty()) {
            binding.tvUpcomingEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvUpcomingEmpty.setVisibility(View.GONE);
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            AppointmentEntity item = items.get(i);
            ContactEntity contact = repository.getContact(item.contactId);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(12), dp(8), dp(12));
            row.setOnClickListener(v -> navigateTo(R.id.nav_appointments));

            TextView icon = new TextView(requireContext());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(44), dp(44));
            icon.setLayoutParams(iconLp);
            icon.setGravity(Gravity.CENTER);
            icon.setBackgroundResource(R.drawable.bg_icon_purple);
            icon.setText("📅");
            icon.setTextSize(18f);
            row.addView(icon);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textLp.leftMargin = dp(12);
            textCol.setLayoutParams(textLp);

            TextView title = new TextView(requireContext());
            title.setText(item.title);
            title.setTextSize(18f);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
            title.setTextColor(requireContext().getColor(R.color.cm_text_primary));
            textCol.addView(title);

            if (contact != null && !TextUtils.isEmpty(contact.fullName)) {
                TextView contactTv = new TextView(requireContext());
                contactTv.setText(AppSettings.maskName(requireContext(), contact.fullName));
                contactTv.setTextColor(requireContext().getColor(R.color.cm_text_secondary));
                contactTv.setTextSize(14f);
                contactTv.setPadding(0, dp(2), 0, 0);
                textCol.addView(contactTv);
            }

            TextView sub = new TextView(requireContext());
            sub.setText(formatDateTime(item.startAt));
            sub.setTextColor(requireContext().getColor(R.color.cm_text_secondary));
            sub.setTextSize(14f);
            sub.setPadding(0, dp(4), 0, 0);
            textCol.addView(sub);
            row.addView(textCol);
            binding.upcomingContainer.addView(row);
            if (i < Math.min(3, items.size()) - 1) binding.upcomingContainer.addView(divider());
        }
    }

    // Gắn dữ liệu vào giao diện tương ứng.
    private void bindFavorites(List<ContactEntity> source) {
        binding.favoritesContainer.removeAllViews();
        if (source == null || source.isEmpty()) {
            binding.tvFavoritesEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvFavoritesEmpty.setVisibility(View.GONE);
        for (int i = 0; i < Math.min(3, source.size()); i++) {
            ContactEntity item = source.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(10), dp(6), dp(10));
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            row.setOnClickListener(v -> startActivity(new Intent(requireContext(), ContactDetailActivity.class).putExtra("contactId", item.id)));

            TextView avatar = new TextView(requireContext());
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(42), dp(42));
            avatar.setLayoutParams(avatarLp);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
            avatar.setText(item.fullName == null || item.fullName.isBlank() ? "?" : item.fullName.substring(0,1).toUpperCase());
            avatar.setTextColor(requireContext().getColor(android.R.color.white));
            avatar.setTextSize(16f);
            avatar.setTypeface(avatar.getTypeface(), Typeface.BOLD);
            row.addView(avatar);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textLp.leftMargin = dp(12);
            textCol.setLayoutParams(textLp);

            TextView name = new TextView(requireContext());
            name.setText(AppSettings.maskName(requireContext(), item.fullName));
            name.setTextSize(17f);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(requireContext().getColor(R.color.cm_text_primary));
            textCol.addView(name);

            TextView company = new TextView(requireContext());
            company.setText(AppSettings.isPrivacyMode(requireContext()) ? "••••" : (TextUtils.isEmpty(item.company) ? "—" : item.company));
            company.setTextColor(requireContext().getColor(R.color.cm_text_secondary));
            company.setTextSize(14f);
            company.setPadding(0, dp(2), 0, 0);
            textCol.addView(company);

            LinearLayout actions = new LinearLayout(requireContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(8), 0, 0);
            LinearLayout phone = createMiniAction("Gọi", R.drawable.bg_button_call_soft, R.color.cm_blue_icon);
            phone.setOnClickListener(v -> { repository.addInteraction(item.id, "CALL", "Gọi từ tổng quan"); dial(repository.getPhones(item.id).isEmpty() ? null : repository.getPhones(item.id).get(0).phoneNumber); });
            actions.addView(phone);
            if (!TextUtils.isEmpty(item.email)) {
                LinearLayout mail = createMiniAction("Email", R.drawable.bg_button_email_soft, R.color.cm_purple_icon);
                LinearLayout.LayoutParams mailLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mailLp.leftMargin = dp(10);
                mail.setLayoutParams(mailLp);
                mail.setOnClickListener(v -> { repository.addInteraction(item.id, "EMAIL", "Email từ tổng quan"); email(item.email); });
                actions.addView(mail);
            }
            textCol.addView(actions);
            row.addView(textCol);
            binding.favoritesContainer.addView(row);
            if (i < Math.min(3, source.size()) - 1) binding.favoritesContainer.addView(divider());
        }
    }

    // Gắn dữ liệu vào giao diện tương ứng.
    private void bindRecentContacts(List<ContactEntity> source) {
        binding.recentContactsContainer.removeAllViews();
        if (source == null || source.isEmpty()) {
            binding.tvRecentEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvRecentEmpty.setVisibility(View.GONE);
        source.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        for (int i = 0; i < Math.min(3, source.size()); i++) {
            ContactEntity item = source.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            row.setOnClickListener(v -> startActivity(new Intent(requireContext(), ContactDetailActivity.class).putExtra("contactId", item.id)));

            TextView avatar = new TextView(requireContext());
            avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
            avatar.setText(item.fullName == null || item.fullName.isBlank() ? "?" : item.fullName.substring(0,1).toUpperCase());
            avatar.setTextColor(requireContext().getColor(android.R.color.white));
            avatar.setTypeface(avatar.getTypeface(), Typeface.BOLD);
            row.addView(avatar);

            LinearLayout textCol = new LinearLayout(requireContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textLp.leftMargin = dp(12);
            textCol.setLayoutParams(textLp);

            TextView name = new TextView(requireContext());
            name.setText(AppSettings.maskName(requireContext(), item.fullName));
            name.setTextSize(17f);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(requireContext().getColor(R.color.cm_text_primary));
            textCol.addView(name);

            TextView role = new TextView(requireContext());
            String roleText = !TextUtils.isEmpty(item.position) ? item.position : (!TextUtils.isEmpty(item.department) ? item.department : "");
            role.setText(roleText);
            role.setTextColor(requireContext().getColor(R.color.cm_text_secondary));
            role.setTextSize(14f);
            textCol.addView(role);
            row.addView(textCol);

            TextView company = new TextView(requireContext());
            company.setText(AppSettings.isPrivacyMode(requireContext()) ? "••••" : (TextUtils.isEmpty(item.company) ? "—" : item.company));
            company.setTextColor(requireContext().getColor(R.color.cm_text_secondary));
            company.setTextSize(15f);
            row.addView(company);
            binding.recentContactsContainer.addView(row);
            if (i < Math.min(3, source.size()) - 1) binding.recentContactsContainer.addView(divider());
        }
    }


    // Tạo view/model phụ trợ phục vụ hiển thị giao diện.
    private LinearLayout createMiniAction(String label, int bgRes, int textColorRes) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundResource(bgRes);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        TextView txt = new TextView(requireContext());
        txt.setText(label);
        txt.setTextColor(requireContext().getColor(textColorRes));
        txt.setTextSize(13f);
        txt.setTypeface(txt.getTypeface(), Typeface.BOLD);
        box.addView(txt);
        return box;
    }

    // Mở ứng dụng gọi điện hệ thống bằng ACTION_DIAL.
    private void dial(String phone) {
        if (TextUtils.isEmpty(phone)) return;
        // API ngoài - Android Intent: mở ứng dụng gọi điện và để người dùng xác nhận cuộc gọi.
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    // Mở ứng dụng email bằng ACTION_SENDTO và ghi nhận tương tác email.
    private void email(String email) {
        if (TextUtils.isEmpty(email)) return;
        // API ngoài - Android Intent: mở ứng dụng email với địa chỉ người nhận.
        startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email)));
    }

    // Định dạng dữ liệu thành chuỗi dễ đọc cho giao diện.
    private String formatDateTime(String value) {
        if (TextUtils.isEmpty(value) || value.length() < 16) return value;
        return value.substring(0, 10).replace('-', '/') + "  ⏰ " + value.substring(11, 16);
    }

    private TextView link(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(requireContext().getColor(R.color.cm_primary));
        tv.setTextSize(14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View divider() {
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackgroundColor(requireContext().getColor(R.color.cm_outline));
        return divider;
    }

    // Chuyển đổi đơn vị dp sang pixel theo mật độ màn hình hiện tại.
    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }
}
