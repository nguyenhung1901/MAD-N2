package com.example.contactmanagerprojava.ui.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.contactmanagerprojava.R;
import com.example.contactmanagerprojava.data.AppRepository;
import com.example.contactmanagerprojava.data.CategoryCountRow;
import com.example.contactmanagerprojava.data.TopContactRow;
import com.example.contactmanagerprojava.databinding.FragmentStatsBinding;
import com.example.contactmanagerprojava.util.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment thống kê; tổng hợp dữ liệu và vẽ biểu đồ bằng MPAndroidChart.
 */
public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;

    private static final ValueFormatter INTEGER_VALUE_FORMATTER = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            return String.valueOf(Math.round(value));
        }
    };

    private static final ValueFormatter INTEGER_AXIS_FORMATTER = new ValueFormatter() {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            int rounded = Math.round(value);
            if (Math.abs(value - rounded) < 0.001f) {
                return String.valueOf(rounded);
            }
            return "";
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        AppRepository.DashboardStats stats = new AppRepository(requireContext())
    /**
     * Tổng hợp dữ liệu từ nhiều DAO để phục vụ màn hình Dashboard và Thống kê.
     */
                .dashboard(new SessionManager(requireContext()).getUserId());

        setText(R.id.tvKpiContacts, stats.totalContacts);
        setText(R.id.tvKpiFavorites, stats.favoriteContacts);
        setText(R.id.tvKpiAppointments, stats.totalAppointments);
        setText(R.id.tvKpiUpcoming, stats.upcomingAppointments);
        setText(R.id.tvKpiNotes, stats.totalNotes);
        setText(R.id.tvKpiInteractions, stats.totalInteractions);
        setText(R.id.tvKpiOverdue, stats.overdueAppointments);
        setText(R.id.tvKpiDone, stats.doneAppointments);

    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ trạng thái lịch hẹn.
     */
        setupAppointmentStatusChart(stats);
    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ phân loại lịch hẹn.
     */
        setupAppointmentTypeChart(stats);
    /**
     * Hiển thị biểu đồ cột cho các liên hệ có số lượt tương tác cao nhất.
     */
        setupTopContactsChart(stats);
    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ phân loại ghi chú.
     */
        setupNoteCategoryChart(stats);
        return binding.getRoot();
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    private void setText(int viewId, int value) {
        TextView tv = binding.getRoot().findViewById(viewId);
        if (tv != null) tv.setText(String.valueOf(value));
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ trạng thái lịch hẹn.
     */
    private void setupAppointmentStatusChart(AppRepository.DashboardStats stats) {
        List<PieEntry> entries = new ArrayList<>();
        if (stats.upcomingAppointments > 0) entries.add(new PieEntry(stats.upcomingAppointments, "Sắp tới"));
        if (stats.doneAppointments > 0) entries.add(new PieEntry(stats.doneAppointments, "Hoàn thành"));
        if (stats.cancelledAppointments > 0) entries.add(new PieEntry(stats.cancelledAppointments, "Đã hủy"));
        if (stats.overdueAppointments > 0) entries.add(new PieEntry(stats.overdueAppointments, "Quá hạn"));
        configurePieChart(binding.pieAppointments, entries,
                new int[]{
                        Color.parseColor("#3B82F6"),
                        Color.parseColor("#22C55E"),
                        Color.parseColor("#F97316"),
                        Color.parseColor("#EF4444")
                },
                "Lịch hẹn",
                "Chưa có dữ liệu lịch hẹn");
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ phân loại lịch hẹn.
     */
    private void setupAppointmentTypeChart(AppRepository.DashboardStats stats) {
        List<PieEntry> entries = new ArrayList<>();
        for (CategoryCountRow row : stats.appointmentTypes) {
            if (row == null || row.total <= 0) continue;
            entries.add(new PieEntry(row.total, normalizeAppointmentType(row.label)));
        }
        configurePieChart(binding.pieAppointmentTypes, entries,
                new int[]{
                        Color.parseColor("#8B5CF6"),
                        Color.parseColor("#06B6D4"),
                        Color.parseColor("#F59E0B"),
                        Color.parseColor("#10B981")
                },
                "Loại hẹn",
                "Chưa có dữ liệu loại lịch");
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Chuẩn bị dữ liệu và hiển thị biểu đồ phân loại ghi chú.
     */
    private void setupNoteCategoryChart(AppRepository.DashboardStats stats) {
        List<PieEntry> entries = new ArrayList<>();
        for (CategoryCountRow row : stats.noteCategories) {
            if (row == null || row.total <= 0) continue;
            entries.add(new PieEntry(row.total, normalizeCategory(row.label)));
        }
        configurePieChart(binding.pieNotes, entries,
                new int[]{
                        Color.parseColor("#4F46E5"),
                        Color.parseColor("#16A34A"),
                        Color.parseColor("#D97706"),
                        Color.parseColor("#9333EA"),
                        Color.parseColor("#DC2626")
                },
                "Ghi chú",
                "Chưa có dữ liệu ghi chú");
    }

    /**
     * Cấu hình chung cho biểu đồ vòng như dữ liệu, màu sắc, chú thích và thông báo khi không có dữ liệu.
     */
    private void configurePieChart(PieChart chart, List<PieEntry> entries, int[] colors, String centerText, String noDataText) {
        if (entries == null || entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText(noDataText);
            chart.invalidate();
            return;
        }

        // API ngoài - MPAndroidChart: tạo dataset cho biểu đồ tròn/vòng dùng trong thống kê.
        PieDataSet set = new PieDataSet(entries, "");
        List<Integer> palette = new ArrayList<>();
        for (int color : colors) palette.add(color);
        set.setColors(palette);
        set.setSliceSpace(4f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(11f);
        set.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(set);
        data.setValueFormatter(INTEGER_VALUE_FORMATTER);
        chart.setData(data);
        chart.setUsePercentValues(false);
        chart.setDrawRoundedSlices(true);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setCenterText(centerText);
        chart.setCenterTextSize(15f);
        chart.setCenterTextColor(Color.parseColor("#111827"));
        chart.setEntryLabelColor(Color.parseColor("#111827"));
        chart.setEntryLabelTextSize(11f);
        chart.setNoDataText(noDataText);

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        Legend legend = chart.getLegend();
        legend.setTextColor(Color.parseColor("#6B7280"));
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.animateY(500);
        chart.invalidate();
    }

    // Thiết lập giá trị/trạng thái cho UI hoặc cấu hình cục bộ.
    /**
     * Hiển thị biểu đồ cột cho các liên hệ có số lượt tương tác cao nhất.
     */
    private void setupTopContactsChart(AppRepository.DashboardStats stats) {
        BarChart chart = binding.barTopContacts;
        List<BarEntry> bars = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        int maxInteractionCount = 0;

        for (TopContactRow row : stats.topContacts) {
            bars.add(new BarEntry(index, row.total));
            if (row.total > maxInteractionCount) {
                maxInteractionCount = row.total;
            }
            String label = row.contactName == null || row.contactName.trim().isEmpty()
                    ? "Liên hệ #" + row.contactId
                    : row.contactName.trim();
            if (label.length() > 10) label = label.substring(0, 10) + "…";
            labels.add(label);
            index++;
        }

        if (bars.isEmpty()) {
            chart.clear();
            chart.setNoDataText("Chưa có dữ liệu tương tác");
            chart.invalidate();
            return;
        }

        // API ngoài - MPAndroidChart: tạo dataset cho biểu đồ cột top liên hệ tương tác.
        BarDataSet set = new BarDataSet(bars, "Lượt tương tác");
        set.setColor(Color.parseColor("#8B5CF6"));
        set.setValueTextColor(Color.parseColor("#6B7280"));
        set.setValueTextSize(11f);

        BarData data = new BarData(set);
        data.setBarWidth(0.52f);
        data.setValueFormatter(INTEGER_VALUE_FORMATTER);
        chart.setData(data);
        chart.setFitBars(true);
        chart.setDrawGridBackground(false);
        chart.setNoDataText("Chưa có dữ liệu tương tác");

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.getLegend().setTextColor(Color.parseColor("#6B7280"));
        chart.getLegend().setTextSize(12f);
        chart.getAxisRight().setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#6B7280"));
        leftAxis.setGridColor(Color.parseColor("#E5E7EB"));
    /**
     * Cấu hình trục Y của biểu đồ dạng đếm để chỉ hiển thị số nguyên.
     */
        configureIntegerCountAxis(leftAxis, maxInteractionCount);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#6B7280"));
        xAxis.setLabelCount(labels.size());
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.animateY(600);
        chart.invalidate();
    }

    // Đếm số lượng bản ghi phục vụ hiển thị dashboard/thống kê.
    /**
     * Cấu hình trục Y của biểu đồ dạng đếm để chỉ hiển thị số nguyên.
     */
    private void configureIntegerCountAxis(YAxis axis, int maxValue) {
        int safeMax = Math.max(1, maxValue);
    /**
     * Chọn khoảng chia hợp lý cho trục số nguyên của biểu đồ cột.
     */
        int step = chooseNiceIntegerStep(safeMax);
        int axisMax = ((safeMax + step - 1) / step) * step;
        int labelCount = Math.max(2, axisMax / step + 1);

        axis.setAxisMinimum(0f);
        axis.setAxisMaximum(axisMax);
        axis.setGranularity(1f);
        axis.setGranularityEnabled(true);
        axis.setLabelCount(labelCount, true);
        axis.setValueFormatter(INTEGER_AXIS_FORMATTER);
    }

    /**
     * Chọn khoảng chia hợp lý cho trục số nguyên của biểu đồ cột.
     */
    private int chooseNiceIntegerStep(int maxValue) {
        if (maxValue <= 5) return 1;
        if (maxValue <= 10) return 2;
        if (maxValue <= 25) return 5;
        if (maxValue <= 50) return 10;
        if (maxValue <= 100) return 20;

        int magnitude = 1;
        while (maxValue / magnitude > 10) {
            magnitude *= 10;
        }

        int roughStep = (int) Math.ceil(maxValue / 5.0);
        return Math.max(1, ((roughStep + magnitude - 1) / magnitude) * magnitude);
    }

    /**
     * Chuẩn hóa tên danh mục ghi chú trước khi hiển thị trên biểu đồ.
     */
    private String normalizeCategory(String raw) {
        if (raw == null) return "Khác";
        String value = raw.trim();
        switch (value) {
            case "CALL": return "Cuộc gọi";
            case "MEETING": return "Cuộc họp";
            case "IDEA": return "Ý tưởng";
            case "TASK": return "Việc cần làm";
            case "GENERAL": return "Chung";
            default: return value;
        }
    }

    /**
     * Chuẩn hóa tên loại lịch hẹn trước khi hiển thị trên biểu đồ.
     */
    private String normalizeAppointmentType(String raw) {
        if (raw == null) return "Khác";
        String value = raw.trim();
        switch (value) {
            case "MEETING": return "Cuộc họp";
            case "CALL": return "Cuộc gọi";
            case "TASK": return "Công việc";
            default: return "Khác";
        }
    }
}
