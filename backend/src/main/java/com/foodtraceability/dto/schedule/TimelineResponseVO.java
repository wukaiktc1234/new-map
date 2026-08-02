package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 时间线日历响应VO
 * 包含一周7天的完整排班数据（核心数据结构）
 *
 * <p>数据结构说明：
 * <ul>
 *   <li>dates: 7天日期数组，固定长度为7</li>
 *   <li>employees: 员工列表，每人包含7天的排班条目</li>
 *   <li>dailySummary: 每日汇总，包含需求vs实际排班对比</li>
 * </ul>
 */
@Schema(description = "时间线日历响应VO")
public class TimelineResponseVO {

    @Schema(description = "周一日期", example = "2026-05-11")
    private String weekStart;

    @Schema(description = "周日日期", example = "2026-05-17")
    private String weekEnd;

    /**
     * 7天日期数组(周一~周日)
     * 固定长度为7，格式YYYY-MM-DD
     */
    @Schema(description = "7天日期数组(周一~周日)")
    private List<String> dates;

    /**
     * 员工列表
     * 每人包含7天的排班条目(entries)和统计信息
     */
    @Schema(description = "员工排班列表")
    private List<TimelineEmployeeVO> employees;

    /**
     * 每日汇总
     * 包含各时段的需求、实际、缺员情况
     */
    @Schema(description = "每日汇总统计")
    private List<DailySummaryVO> dailySummary;

    // ==================== Getter & Setter ====================

    public String getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(String weekStart) {
        this.weekStart = weekStart;
    }

    public String getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(String weekEnd) {
        this.weekEnd = weekEnd;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<TimelineEmployeeVO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<TimelineEmployeeVO> employees) {
        this.employees = employees;
    }

    public List<DailySummaryVO> getDailySummary() {
        return dailySummary;
    }

    public void setDailySummary(List<DailySummaryVO> dailySummary) {
        this.dailySummary = dailySummary;
    }
}
