package com.example.contactmanagerprojava.ui.contacts;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.data.ContactPhoneEntity;
import com.example.contactmanagerprojava.databinding.ActivityContactEditorBinding;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.SessionManager;
import com.example.contactmanagerprojava.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình thêm/sửa liên hệ; xử lý thông tin cá nhân và nhiều số điện thoại.
 */
public class ContactEditorActivity extends AppCompatActivity {
    private ActivityContactEditorBinding binding;
    private AppRepository repository;
    private ContactEntity editing;

    @Override protected void onCreate(Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        binding = ActivityContactEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        repository = new AppRepository(this);
        long id = getIntent().getLongExtra("contactId", 0L);
        if (id != 0L) {
            editing = repository.getContact(id);
            bindEdit();
        }
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnAddSecondaryPhone.setOnClickListener(v -> addSecondaryPhoneRow(null));
    }

    // Đổ dữ liệu bản ghi đang sửa lên form để người dùng cập nhật.
    private void bindEdit() {
        if (editing == null) return;
        binding.etName.setText(editing.fullName);
        binding.etCompany.setText(editing.company);
        binding.etPosition.setText(editing.position);
        binding.etDepartment.setText(editing.department);
        binding.etEmail.setText(editing.email);
        binding.etAddress.setText(editing.address);
        binding.etBirthday.setText(editing.birthday);
        String[] labels = getResources().getStringArray(R.array.contact_labels);
        int selectedIndex = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(editing.tags)) { selectedIndex = i; break; }
        }
        binding.spinnerTag.setSelection(selectedIndex);
        binding.etNote.setText(editing.note);
        List<ContactPhoneEntity> phones = repository.getPhones(editing.id);
        if (!phones.isEmpty()) binding.etPhonePrimary.setText(phones.get(0).phoneNumber);
        binding.secondaryPhonesContainer.removeAllViews();
        for (int i = 1; i < phones.size(); i++) {
            addSecondaryPhoneRow(phones.get(i).phoneNumber);
        }
    }

    private void addSecondaryPhoneRow(String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_phone_secondary_editor, binding.secondaryPhonesContainer, false);
        EditText editText = row.findViewById(R.id.etSecondaryPhone);
        TextView btnRemove = row.findViewById(R.id.btnRemoveSecondaryPhone);
        if (value != null) editText.setText(value);
        btnRemove.setOnClickListener(v -> binding.secondaryPhonesContainer.removeView(row));
        binding.secondaryPhonesContainer.addView(row);
    }

    // Đọc dữ liệu từ form, kiểm tra hợp lệ và lưu vào database thông qua repository.
    private void save() {
        String name = binding.etName.getText().toString().trim();
        String primary = binding.etPhonePrimary.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Tên bắt buộc");
            binding.etName.requestFocus();
            return;
        }
        if (!isValidPhone(primary)) {
            binding.etPhonePrimary.setError("Số chính phải từ 9 đến 11 số");
            binding.etPhonePrimary.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email không hợp lệ");
            binding.etEmail.requestFocus();
            return;
        }
        ContactEntity c = editing != null ? editing : new ContactEntity(new SessionManager(this).getUserId(), name);
        c.fullName = name;
        c.company = binding.etCompany.getText().toString().trim();
        c.position = binding.etPosition.getText().toString().trim();
        c.department = binding.etDepartment.getText().toString().trim();
        c.email = email;
        c.address = binding.etAddress.getText().toString().trim();
        c.birthday = binding.etBirthday.getText().toString().trim();
        String selectedTag = String.valueOf(binding.spinnerTag.getSelectedItem());
        c.tags = "Không có nhãn".equals(selectedTag) ? "" : selectedTag;
        c.note = binding.etNote.getText().toString().trim();

        List<ContactPhoneEntity> phones = new ArrayList<>();
        phones.add(new ContactPhoneEntity(c.id, sanitizePhone(primary), "Chính", true));
        for (int i = 0; i < binding.secondaryPhonesContainer.getChildCount(); i++) {
            View child = binding.secondaryPhonesContainer.getChildAt(i);
            EditText et = child.findViewById(R.id.etSecondaryPhone);
            String secondary = et.getText().toString().trim();
            if (secondary.isEmpty()) continue;
            if (!isValidPhone(secondary)) {
                et.setError("Số phụ phải từ 9 đến 11 số");
                et.requestFocus();
                return;
            }
            phones.add(new ContactPhoneEntity(c.id, sanitizePhone(secondary), "Phụ " + phones.size(), false));
        }
    /**
     * Lưu thông tin liên hệ và danh sách số điện thoại tương ứng vào Room Database.
     */
        repository.saveContact(c, phones);
        Toast.makeText(this, "Đã lưu liên hệ", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    private boolean isValidPhone(String phone) {
        String normalized = sanitizePhone(phone);
        return normalized.matches("^\\+?\\d{9,11}$");
    }

    private String sanitizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s.-]", "");
    }
}
