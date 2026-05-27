package com.example.contactmanagerprojava.ui.appointments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.AppointmentEntity;
import com.example.contactmanagerprojava.data.ContactEntity;
import com.example.contactmanagerprojava.databinding.ActivityAppointmentEditorBinding;
import com.example.contactmanagerprojava.util.AppSettings;
import com.example.contactmanagerprojava.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Màn hình thêm/sửa lịch hẹn, chọn liên hệ, thời gian, loại lịch và trạng thái.
 */
public class AppointmentEditorActivity extends AppCompatActivity {
    private ActivityAppointmentEditorBinding binding;
    private AppRepository repository;
    private long editingId;
    private final List<ContactEntity> contacts = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
    /**
     * Khởi tạo Activity, binding giao diện, Repository và các sự kiện cần thiết khi màn hình được mở.
     */
        super.onCreate(savedInstanceState);
        binding = ActivityAppointmentEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppSettings.applyPrivacy(this);
        repository = new AppRepository(this);
        editingId = getIntent().getLongExtra("appointment_id", 0L);
        long userId = new SessionManager(this).getUserId();
        contacts.clear();
        contacts.addAll(repository.getContactsSync(userId));
        setupContacts();
        setupTypes();
        String selectedDate = getIntent().getStringExtra("selected_date");
        binding.etDate.setText(selectedDate == null || selectedDate.isBlank() ? LocalDate.now().toString() : selectedDate);
        binding.etStart.setText("09:00");
        binding.etEnd.setText("10:00");
        if (editingId != 0L) bindEdit();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> save());
        binding.etDate.setOnClickListener(v -> openDatePicker());
        binding.etStart.setOnClickListener(v -> openTimePicker(binding.etStart));
        binding.etEnd.setOnClickListener(v -> openTimePicker(binding.etEnd));
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setupContacts() {
        List<String> names = new ArrayList<>();
        if (contacts.isEmpty()) names.add("Chưa có liên hệ");
        else for (ContactEntity c : contacts) names.add(c.fullName);
        binding.spinnerContact.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setupTypes() {
        binding.spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Arrays.asList("MEETING", "CALL", "TASK", "OTHER")));
    }

    // Đổ dữ liệu bản ghi đang sửa lên form để người dùng cập nhật.
    private void bindEdit() {
        long userId = new SessionManager(this).getUserId();
        AppointmentEntity a = repository.getAppointment(editingId, userId);
        if (a == null) return;
        binding.etTitle.setText(a.title);
        binding.etDescription.setText(a.description);
        binding.etLocation.setText(a.location);
        binding.etDate.setText(a.startAt.substring(0, 10));
        binding.etStart.setText(a.startAt.substring(11, 16));
        binding.etEnd.setText(a.endAt.substring(11, 16));
        setSpinnerValue(binding.spinnerType, a.type);
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).id == a.contactId) {
                binding.spinnerContact.setSelection(i);
                break;
            }
        }
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setSpinnerValue(android.widget.Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) if (String.valueOf(spinner.getItemAtPosition(i)).equalsIgnoreCase(value)) { spinner.setSelection(i); return; }
    }

    private void openDatePicker() {
        LocalDate current = LocalDate.parse(binding.etDate.getText().toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> binding.etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)), current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private void openTimePicker(android.widget.TextView target) {
        LocalTime current = LocalTime.parse(target.getText().toString(), DateTimeFormatter.ofPattern("HH:mm"));
        new TimePickerDialog(this, (view, hourOfDay, minute) -> target.setText(String.format("%02d:%02d", hourOfDay, minute)), current.getHour(), current.getMinute(), true).show();
    }

    // Đọc dữ liệu từ form, kiểm tra hợp lệ và lưu vào database thông qua repository.
    private void save() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Hãy tạo ít nhất 1 liên hệ trước khi thêm lịch hẹn", Toast.LENGTH_LONG).show();
            return;
        }
        String date = binding.etDate.getText().toString().trim();
        String start = binding.etStart.getText().toString().trim();
        String end = binding.etEnd.getText().toString().trim();
        if (binding.etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Nhập tiêu đề lịch hẹn", Toast.LENGTH_SHORT).show();
            return;
        }
        long contactId = contacts.get(binding.spinnerContact.getSelectedItemPosition()).id;
        long userId = new SessionManager(this).getUserId();
        AppointmentEntity a = editingId > 0L ? repository.getAppointment(editingId, userId) : null;
        if (a == null) {
            a = new AppointmentEntity(contactId, binding.etTitle.getText().toString().trim(), date + " " + start, date + " " + end, binding.spinnerType.getSelectedItem().toString(), "UPCOMING");
        } else {
            a.contactId = contactId;
            a.title = binding.etTitle.getText().toString().trim();
            a.startAt = date + " " + start;
            a.endAt = date + " " + end;
            a.type = binding.spinnerType.getSelectedItem().toString();
        }
        a.description = binding.etDescription.getText().toString().trim();
        a.location = binding.etLocation.getText().toString().trim();
    /**
     * Lưu lịch hẹn và ghi nhận tương tác nếu đây là lịch hẹn mới.
     */
        repository.saveAppointment(a);
        Toast.makeText(this, "Đã lưu lịch hẹn", Toast.LENGTH_SHORT).show();
        finish();
    }
}
