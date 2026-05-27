package com.example.contactmanagerprojava.ui.appointments;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.AppointmentEntity;
import com.example.contactmanagerprojava.databinding.FragmentAppointmentsBinding;
import com.example.contactmanagerprojava.ui.adapters.AppointmentAdapter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment quản lý lịch hẹn; hỗ trợ các chế độ xem danh sách, tuần, tháng và cập nhật trạng thái.
 */
public class AppointmentsFragment extends Fragment {
    private FragmentAppointmentsBinding binding;
    private AppRepository repository;
    private AppointmentAdapter adapter;
    private final List<AppointmentEntity> allItems = new ArrayList<>();
    private String selectedDate;
    private YearMonth visibleMonth;
    private Mode mode = Mode.MONTH;
    private String selectedType = "";

    private enum Mode { LIST, WEEK, MONTH }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppointmentsBinding.inflate(inflater, container, false);
        repository = new AppRepository(requireContext());
        adapter = new AppointmentAdapter(requireContext(), new AppointmentAdapter.Listener() {
            @Override public void onEdit(AppointmentEntity item) {
                Intent intent = new Intent(requireContext(), AppointmentEditorActivity.class);
                intent.putExtra("appointment_id", item.id);
                intent.putExtra("selected_date", selectedDate);
                // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
                startActivity(intent);
            }
            @Override public void onDelete(AppointmentEntity item) { repository.deleteAppointment(item); }
            @Override public void onDone(AppointmentEntity item) { repository.updateAppointmentStatus(item, "DONE"); }
            @Override public void onCancel(AppointmentEntity item) { repository.updateAppointmentStatus(item, "CANCELLED"); }
        });
        binding.recyclerAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAppointments.setAdapter(adapter);

        selectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        visibleMonth = YearMonth.from(LocalDate.now());
        updateSelectedDateText();

        long userId = new com.example.contactmanagerprojava.util.SessionManager(requireContext()).getUserId();
        repository.observeAppointments(userId).observe(getViewLifecycleOwner(), items -> {
            allItems.clear();
            allItems.addAll(items);
            renderMonthGrid();
            buildWeekStrip();
            buildTypeChips();
            renderAppointments();
        });

        binding.btnAddForDate.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AppointmentEditorActivity.class);
            intent.putExtra("selected_date", selectedDate);
            // API ngoài - Android Activity: mở màn hình/ứng dụng khác thông qua Intent.
            startActivity(intent);
        });
        binding.tabList.setOnClickListener(v -> switchMode(Mode.LIST));
        binding.tabWeek.setOnClickListener(v -> switchMode(Mode.WEEK));
        binding.tabMonth.setOnClickListener(v -> switchMode(Mode.MONTH));
        binding.chipAllTypes.setOnClickListener(v -> { selectedType = ""; updateTypeChipStyles(); renderAppointments(); });
        binding.btnPrevMonth.setOnClickListener(v -> { visibleMonth = visibleMonth.minusMonths(1); renderMonthGrid(); });
        binding.btnNextMonth.setOnClickListener(v -> { visibleMonth = visibleMonth.plusMonths(1); renderMonthGrid(); });
        binding.btnToday.setOnClickListener(v -> {
            LocalDate today = LocalDate.now();
            visibleMonth = YearMonth.from(today);
            selectedDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            updateSelectedDateText();
            renderMonthGrid();
            renderAppointments();
            buildWeekStrip();
        });
        switchMode(Mode.MONTH);
        return binding.getRoot();
    }

    private void switchMode(Mode newMode) {
        mode = newMode;
        binding.weekScroll.setVisibility(mode == Mode.WEEK ? View.VISIBLE : View.GONE);
        binding.calendarCard.setVisibility(mode == Mode.MONTH ? View.VISIBLE : View.GONE);
        updateTab(binding.tabList, mode == Mode.LIST);
        updateTab(binding.tabWeek, mode == Mode.WEEK);
        updateTab(binding.tabMonth, mode == Mode.MONTH);
        if (mode == Mode.WEEK) buildWeekStrip();
        if (mode == Mode.MONTH) renderMonthGrid();
        renderAppointments();
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateTab(TextView view, boolean selected) {
        view.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
        view.setTextColor(requireContext().getColor(selected ? R.color.cm_primary : R.color.cm_text_secondary));
    }

    private void buildWeekStrip() {
        binding.weekContainer.removeAllViews();
        LocalDate date = LocalDate.parse(selectedDate);
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            TextView chip = new TextView(requireContext());
            chip.setText(day.format(DateTimeFormatter.ofPattern("E dd", new Locale("vi", "VN"))));
            chip.setGravity(Gravity.CENTER);
            chip.setMinWidth(dp(68));
            chip.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = dp(10);
            chip.setLayoutParams(lp);
            boolean selected = day.format(DateTimeFormatter.ISO_LOCAL_DATE).equals(selectedDate);
            boolean marked = hasAppointmentOn(day);
            chip.setBackgroundResource(selected ? (marked ? R.drawable.bg_calendar_day_selected_marked : R.drawable.bg_calendar_day_selected)
                    : (marked ? R.drawable.bg_calendar_day_marked : R.drawable.bg_input_soft));
            chip.setTextColor(requireContext().getColor(selected ? android.R.color.white : R.color.cm_text_primary));
            chip.setOnClickListener(v -> {
                selectedDate = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
                visibleMonth = YearMonth.from(day);
                updateSelectedDateText();
                buildWeekStrip();
                renderMonthGrid();
                renderAppointments();
            });
            binding.weekContainer.addView(chip);
        }
    }

    private void renderMonthGrid() {
        binding.tvMonthTitle.setText("Tháng " + String.format(Locale.getDefault(), "%02d/%d", visibleMonth.getMonthValue(), visibleMonth.getYear()));
        binding.monthGrid.removeAllViews();
        LocalDate firstDay = visibleMonth.atDay(1);
        int offset = firstDay.getDayOfWeek().getValue() % 7;
        LocalDate gridStart = firstDay.minusDays(offset);

        for (int i = 0; i < 42; i++) {
            LocalDate day = gridStart.plusDays(i);
            boolean currentMonth = day.getMonthValue() == visibleMonth.getMonthValue();
            boolean selected = day.format(DateTimeFormatter.ISO_LOCAL_DATE).equals(selectedDate);
            boolean marked = hasAppointmentOn(day);

            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(6), dp(8), dp(6), dp(8));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(54);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            cell.setLayoutParams(lp);
            cell.setBackgroundResource(selected ? (marked ? R.drawable.bg_calendar_day_selected_marked : R.drawable.bg_calendar_day_selected)
                    : (marked ? R.drawable.bg_calendar_day_marked : R.drawable.bg_calendar_day_default));

            TextView dayText = new TextView(requireContext());
            dayText.setText(String.valueOf(day.getDayOfMonth()));
            dayText.setTextSize(15f);
            dayText.setTextColor(requireContext().getColor(selected ? android.R.color.white : (currentMonth ? R.color.cm_text_primary : R.color.cm_text_secondary)));
            dayText.setTypeface(dayText.getTypeface(), selected || marked ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            cell.addView(dayText);

            if (marked) {
                TextView dot = new TextView(requireContext());
                dot.setText(selected ? "●" : "•");
                dot.setTextSize(selected ? 12f : 16f);
                dot.setTextColor(requireContext().getColor(selected ? android.R.color.white : R.color.cm_primary));
                cell.addView(dot);
            }

            cell.setOnClickListener(v -> {
                selectedDate = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
                visibleMonth = YearMonth.from(day);
                updateSelectedDateText();
                renderMonthGrid();
                buildWeekStrip();
                renderAppointments();
                binding.scrollAppointments.post(() -> binding.scrollAppointments.smoothScrollTo(0, binding.tvSectionTitle.getTop()));
            });
            binding.monthGrid.addView(cell);
        }
    }


    private void buildTypeChips() {
        binding.typeFilterContainer.removeAllViews();
        binding.typeFilterContainer.addView(binding.chipAllTypes);
        java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>();
        for (AppointmentEntity item : allItems) {
            if (item.type != null && !item.type.trim().isEmpty()) types.add(item.type.trim());
        }
        for (String type : types) {
            TextView chip = new TextView(requireContext());
            chip.setText(typeLabel(type));
            chip.setTextSize(13f);
            chip.setPadding(dp(14), dp(10), dp(14), dp(10));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setBackgroundResource(type.equals(selectedType) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
            chip.setTextColor(requireContext().getColor(type.equals(selectedType) ? R.color.cm_primary : R.color.cm_text_primary));
            chip.setOnClickListener(v -> { selectedType = type; updateTypeChipStyles(); renderAppointments(); });
            binding.typeFilterContainer.addView(chip);
        }
        updateTypeChipStyles();
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateTypeChipStyles() {
        binding.chipAllTypes.setBackgroundResource(selectedType.isEmpty() ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
        binding.chipAllTypes.setTextColor(requireContext().getColor(selectedType.isEmpty() ? R.color.cm_primary : R.color.cm_text_primary));
        for (int i = 1; i < binding.typeFilterContainer.getChildCount(); i++) {
            View child = binding.typeFilterContainer.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            TextView chip = (TextView) child;
            boolean selected = chip.getText().toString().equals(typeLabel(selectedType));
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_idle);
            chip.setTextColor(requireContext().getColor(selected ? R.color.cm_primary : R.color.cm_text_primary));
        }
    }

    private String typeLabel(String type) {
        if (type == null) return "Khác";
        switch (type.toUpperCase(java.util.Locale.ROOT)) {
            case "MEETING": return "Cuộc họp";
            case "CALL": return "Cuộc gọi";
            case "TASK": return "Công việc";
            case "OTHER": return "Khác";
            default: return type;
        }
    }

    // Kiểm tra điều kiện boolean để quyết định luồng xử lý.
    private boolean hasAppointmentOn(LocalDate day) {
        String key = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
        for (AppointmentEntity item : allItems) {
            if (item.startAt != null && item.startAt.startsWith(key)) return true;
        }
        return false;
    }

    private void renderAppointments() {
        List<AppointmentEntity> filtered = new ArrayList<>();
        if (mode == Mode.LIST) {
            filtered.addAll(allItems);
            filtered.sort((a, b) -> {
                String sa = a.startAt == null ? "" : a.startAt;
                String sb = b.startAt == null ? "" : b.startAt;
                return sa.compareTo(sb);
            });
            binding.tvSectionTitle.setText("Tất cả lịch hẹn");
        } else if (mode == Mode.WEEK) {
            for (AppointmentEntity item : allItems) {
                if (item.startAt != null && item.startAt.startsWith(selectedDate)) filtered.add(item);
            }
            binding.tvSectionTitle.setText("Lịch hẹn ngày " + formatDate(selectedDate));
        } else {
            for (AppointmentEntity item : allItems) {
                if (item.startAt != null && item.startAt.startsWith(selectedDate)) filtered.add(item);
            }
            binding.tvSectionTitle.setText("Lịch hẹn ngày " + formatDate(selectedDate));
        }
        if (!selectedType.isEmpty()) {
            java.util.List<AppointmentEntity> typeFiltered = new java.util.ArrayList<>();
            for (AppointmentEntity item : filtered) {
                if (selectedType.equalsIgnoreCase(item.type)) typeFiltered.add(item);
            }
            filtered = typeFiltered;
        }
    /**
     * Cập nhật danh sách dữ liệu mới cho Adapter và yêu cầu RecyclerView vẽ lại danh sách.
     */
        adapter.submitList(new java.util.ArrayList<>(filtered));
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerAppointments.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // Cập nhật bản ghi hoặc trạng thái hiện tại.
    private void updateSelectedDateText() {
        binding.btnAddForDate.setText("+ Thêm lịch hẹn");
    }

    // Định dạng dữ liệu thành chuỗi dễ đọc cho giao diện.
    private String formatDate(String date) {
        try { return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception e) { return date; }
    }

    // Chuyển đổi đơn vị dp sang pixel theo mật độ màn hình hiện tại.
    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }
}
