package com.example.contactmanagerprojava.ui.contacts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.databinding.FragmentContactsBinding;
import com.example.contactmanagerprojava.ui.adapters.ContactAdapter;
import com.example.contactmanagerprojava.util.AppExecutors;
import com.example.contactmanagerprojava.util.DeviceContactImporter;
import com.example.contactmanagerprojava.util.SessionManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fragment danh bạ; xử lý danh sách liên hệ, lọc, sắp xếp, yêu thích và nhập danh bạ từ thiết bị.
 */
public class ContactsFragment extends Fragment {
    private static final String CONTACT_SYNC_PREFS = "contact_sync_prefs";
    private static final String KEY_SYNC_COMPLETED_PREFIX = "device_contacts_sync_completed_";
    private static final String KEY_SYNC_DEFER_UNTIL_PREFIX = "device_contacts_sync_defer_until_";
    private static final long DEFER_ONE_DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long DEFER_ONE_WEEK_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String LEGACY_DEVICE_IMPORT_TAG = "Nh\u1eadp t\u1eeb m\u00e1y";
    private static final String NO_LABEL_TEXT = "Kh\u00f4ng c\u00f3 nh\u00e3n";

    private FragmentContactsBinding binding;
    private AppRepository repository;
    private boolean favoritesOnly;
    private String selectedLabel = "";
    private final List<ContactEntity> allContacts = new ArrayList<>();
    private ContactAdapter adapter;
    private boolean sortAscending = true;
    private long userId;
    private boolean syncPromptCheckedThisView;
    private boolean isImportingDeviceContacts;
    private ActivityResultLauncher<String> readContactsPermissionLauncher;

    public ContactsFragment() { this(false); }
    public ContactsFragment(boolean favoritesOnly) { this.favoritesOnly = favoritesOnly; }

    @Override
    // Lifecycle method được gọi khi màn hình/lớp được tạo; dùng để khởi tạo binding, repository, sự kiện và dữ liệu ban đầu.
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
    public void onCreate(@Nullable Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        // API AndroidX: đăng ký callback nhận kết quả quyền hoặc Activity Result theo lifecycle.
        readContactsPermissionLauncher = registerForActivityResult(
                // API AndroidX: contract xin runtime permission như READ_CONTACTS.
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        importDeviceContacts();
                    } else {
                        deferContactSyncPrompt(DEFER_ONE_WEEK_MS);
                        Toast.makeText(requireContext(), "B\u1ea1n c\u1ea7n c\u1ea5p quy\u1ec1n danh b\u1ea1 \u0111\u1ec3 \u0111\u1ed3ng b\u1ed9 li\u00ean h\u1ec7.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Nullable @Override
    // Lifecycle method của Fragment; inflate layout, gắn sự kiện UI và bắt đầu quan sát dữ liệu.
    /**
     * Tạo giao diện cho Fragment, khởi tạo binding, Adapter và các sự kiện thao tác của người dùng.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        repository = new AppRepository(requireContext());
        adapter = new ContactAdapter(requireContext());
        binding.recyclerContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerContacts.setAdapter(adapter);
        userId = new SessionManager(requireContext()).getUserId();

        AppExecutors.db().execute(() -> repository.clearLegacyDeviceImportTag(userId));

        repository.observeContacts(userId).observe(getViewLifecycleOwner(), list -> {
            allContacts.clear();
            if (list != null) allContacts.addAll(list);
            buildLabelChips();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
            maybeShowContactSyncPrompt();
        });

        // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
        binding.btnCreateContact.setOnClickListener(v -> startActivity(new Intent(requireContext(), ContactEditorActivity.class)));
        binding.btnFavorites.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            updateFavoriteButton();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
        });
        binding.chipAllLabels.setOnClickListener(v -> {
            selectedLabel = "";
            updateLabelChipStyles();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
        });
        binding.btnSortAlpha.setOnClickListener(v -> {
            sortAscending = !sortAscending;
            updateSortButton();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
            applyFilters();
        });
        updateFavoriteButton();
        updateSortButton();

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        });
        adapter.setListener(new ContactAdapter.Listener() {
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            @Override public void onClick(long contactId) { startActivity(new Intent(requireContext(), ContactDetailActivity.class).putExtra("contactId", contactId)); }
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            @Override public void onEdit(long contactId) { startActivity(new Intent(requireContext(), ContactEditorActivity.class).putExtra("contactId", contactId)); }
            @Override public void onDelete(ContactEntity item) { repository.deleteContact(item); }
            @Override public void onToggleFavorite(ContactEntity item) { item.favorite = !item.favorite; repository.saveContact(item, repository.getPhones(item.id)); }
        });
        return binding.getRoot();
    }

    @Override
    // Lifecycle method của Fragment; giải phóng binding để tránh rò rỉ bộ nhớ.
    /**
     * Giải phóng binding của Fragment để tránh rò rỉ bộ nhớ.
     */
    public void onDestroyView() {
    /**
     * Giải phóng binding của Fragment để tránh rò rỉ bộ nhớ.
     */
        super.onDestroyView();
        binding = null;
        syncPromptCheckedThisView = false;
    }

    private void maybeShowContactSyncPrompt() {
        if (binding == null || !isAdded() || syncPromptCheckedThisView || isImportingDeviceContacts) return;
        syncPromptCheckedThisView = true;
        if (isDeviceContactSyncCompleted()) return;
        if (System.currentTimeMillis() < getContactSyncDeferUntil()) return;

        binding.getRoot().postDelayed(() -> {
            if (binding == null || !isAdded() || isImportingDeviceContacts) return;
            if (isDeviceContactSyncCompleted()) return;
            if (System.currentTimeMillis() < getContactSyncDeferUntil()) return;
            showContactSyncDialog();
        }, 350L);
    }

    // Hiển thị hộp thoại, thông báo hoặc một phần giao diện cho người dùng.
    private void showContactSyncDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("\u0110\u1ed3ng b\u1ed9 danh b\u1ea1?")
                .setMessage("\u1ee8ng d\u1ee5ng c\u00f3 th\u1ec3 nh\u1eadp li\u00ean h\u1ec7 t\u1eeb danh b\u1ea1 \u0111i\u1ec7n tho\u1ea1i \u0111\u1ec3 b\u1ea1n kh\u00f4ng ph\u1ea3i th\u00eam th\u1ee7 c\u00f4ng. D\u1eef li\u1ec7u ch\u1ec9 \u0111\u01b0\u1ee3c l\u01b0u c\u1ee5c b\u1ed9 tr\u00ean m\u00e1y.")
                .setPositiveButton("\u0110\u1ed3ng b\u1ed9", (dialog, which) -> requestOrImportDeviceContacts())
                .setNegativeButton("\u0110\u1ec3 sau", (dialog, which) -> deferContactSyncPrompt(DEFER_ONE_DAY_MS))
                .show();
    }

    // Kiểm tra quyền READ_CONTACTS rồi xin quyền hoặc bắt đầu import danh bạ.
    private void requestOrImportDeviceContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            importDeviceContacts();
            return;
        }
        readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
    }

    // Chạy import danh bạ thiết bị ở background thread để không làm treo giao diện.
    private void importDeviceContacts() {
        if (binding == null || isImportingDeviceContacts) return;
        Context appContext = requireContext().getApplicationContext();
        isImportingDeviceContacts = true;
        Toast.makeText(requireContext(), "\u0110ang \u0111\u1ed3ng b\u1ed9 danh b\u1ea1...", Toast.LENGTH_SHORT).show();

        AppExecutors.db().execute(() -> {
            DeviceContactImporter.ImportResult result;
            try {
    /**
     * Đọc danh bạ từ Android Contacts Provider, chuẩn hóa dữ liệu và lưu các liên hệ chưa bị trùng vào Room Database.
     */
                result = DeviceContactImporter.importContacts(appContext, repository, userId);
            } catch (SecurityException e) {
                result = new DeviceContactImporter.ImportResult();
                result.failed = true;
                result.message = "\u1ee8ng d\u1ee5ng ch\u01b0a c\u00f3 quy\u1ec1n \u0111\u1ecdc danh b\u1ea1.";
            } catch (Exception e) {
                result = new DeviceContactImporter.ImportResult();
                result.failed = true;
                result.message = "Kh\u00f4ng th\u1ec3 \u0111\u1ed3ng b\u1ed9 danh b\u1ea1: " + e.getMessage();
            }

            DeviceContactImporter.ImportResult finalResult = result;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                isImportingDeviceContacts = false;
                if (binding == null) return;
                if (finalResult.failed) {
                    deferContactSyncPrompt(DEFER_ONE_WEEK_MS);
                } else {
                    markDeviceContactSyncCompleted();
                }
                Toast.makeText(requireContext(), finalResult.toUserMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    private boolean isDeviceContactSyncCompleted() {
        return contactSyncPrefs().getBoolean(KEY_SYNC_COMPLETED_PREFIX + userId, false);
    }

    private void markDeviceContactSyncCompleted() {
        contactSyncPrefs().edit()
                .putBoolean(KEY_SYNC_COMPLETED_PREFIX + userId, true)
                .remove(KEY_SYNC_DEFER_UNTIL_PREFIX + userId)
                .apply();
    }

    private long getContactSyncDeferUntil() {
        return contactSyncPrefs().getLong(KEY_SYNC_DEFER_UNTIL_PREFIX + userId, 0L);
    }

    private void deferContactSyncPrompt(long delayMs) {
        contactSyncPrefs().edit()
                .putLong(KEY_SYNC_DEFER_UNTIL_PREFIX + userId, System.currentTimeMillis() + delayMs)
                .apply();
    }

    private SharedPreferences contactSyncPrefs() {
        // API Android SharedPreferences: lưu cài đặt/phiên đăng nhập dạng key-value cục bộ.
        return requireContext().getSharedPreferences(CONTACT_SYNC_PREFS, Context.MODE_PRIVATE);
    }

    // Áp dụng tìm kiếm/lọc/sắp xếp lên danh sách dữ liệu hiển thị.
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
    private void applyFilters() {
        if (binding == null) return;
        String query = binding.etSearch.getText() == null ? "" : binding.etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<ContactEntity> filtered = new ArrayList<>();
        for (ContactEntity item : allContacts) {
            if (favoritesOnly && !item.favorite) continue;
            if (!selectedLabel.isEmpty() && !hasLabel(item.tags, selectedLabel)) continue;
            if (!query.isEmpty()) {
                String hay = ((item.fullName == null ? "" : item.fullName) + " " + (item.company == null ? "" : item.company) + " " + (item.position == null ? "" : item.position)).toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }
            filtered.add(item);
        }
        filtered.sort((a, b) -> {
            String an = a.fullName == null ? "" : a.fullName.trim().toLowerCase(Locale.ROOT);
            String bn = b.fullName == null ? "" : b.fullName.trim().toLowerCase(Locale.ROOT);
            return sortAscending ? an.compareTo(bn) : bn.compareTo(an);
        });
    /**
     * Cập nhật danh sách dữ liệu mới cho Adapter và yêu cầu RecyclerView vẽ lại danh sách.
     */
        adapter.submitList(filtered);
    }

    // Tạo các chip lọc nhãn dựa trên dữ liệu hiện có.
    private void buildLabelChips() {
        if (binding == null) return;
        binding.labelChipContainer.removeAllViews();
        Set<String> labels = new LinkedHashSet<>();
        for (ContactEntity item : allContacts) {
            for (String tag : splitTags(item.tags)) labels.add(tag);
        }
        for (String label : labels) {
            TextView chip = new TextView(requireContext());
            chip.setText(label);
            chip.setTextSize(13f);
            chip.setTextColor(ContextCompat.getColor(requireContext(), label.equals(selectedLabel) ? R.color.cm_primary : R.color.cm_text_primary));
            chip.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
            chip.setPadding(dp(14), dp(10), dp(14), dp(10));
            chip.setBackgroundResource(label.equals(selectedLabel) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                selectedLabel = label;
                updateLabelChipStyles();
    /**
     * Lọc danh sách đang hiển thị theo từ khóa, nhãn hoặc danh mục mà người dùng đã chọn.
     */
                applyFilters();
            });
            binding.labelChipContainer.addView(chip);
        }
        binding.chipAllLabels.setVisibility(labels.isEmpty() ? View.GONE : View.VISIBLE);
        updateLabelChipStyles();
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateLabelChipStyles() {
        if (binding == null) return;
        binding.chipAllLabels.setBackgroundResource(selectedLabel.isEmpty() ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
        binding.chipAllLabels.setTextColor(ContextCompat.getColor(requireContext(), selectedLabel.isEmpty() ? R.color.cm_primary : R.color.cm_text_primary));
        for (int i = 0; i < binding.labelChipContainer.getChildCount(); i++) {
            View child = binding.labelChipContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                boolean selected = chip.getText().toString().equals(selectedLabel);
                chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
                chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.cm_primary : R.color.cm_text_primary));
            }
        }
    }

    private List<String> splitTags(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split("[,;#]")) {
            String t = part.trim();
            if (!t.isEmpty() && !NO_LABEL_TEXT.equalsIgnoreCase(t) && !LEGACY_DEVICE_IMPORT_TAG.equalsIgnoreCase(t)) out.add(t);
        }
        return out;
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    private boolean hasLabel(String raw, String label) {
        for (String t : splitTags(raw)) if (t.equalsIgnoreCase(label)) return true;
        return false;
    }

    // Chuyển đổi đơn vị dp sang pixel theo mật độ màn hình hiện tại.
    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateFavoriteButton() {
        if (binding == null) return;
        binding.btnFavorites.setBackgroundResource(favoritesOnly ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
        binding.btnFavorites.setTextColor(ContextCompat.getColor(requireContext(), favoritesOnly ? R.color.cm_primary : R.color.cm_text_primary));
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateSortButton() {
        if (binding == null) return;
        binding.btnSortAlpha.setText(sortAscending ? "A-Z" : "Z-A");
        binding.btnSortAlpha.setBackgroundResource(R.drawable.bg_chip_idle);
        binding.btnSortAlpha.setTextColor(ContextCompat.getColor(requireContext(), R.color.cm_text_primary));
    }
}
