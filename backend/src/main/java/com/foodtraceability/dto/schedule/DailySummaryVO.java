package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 每日汇总VO
 * 表示某一天的排班汇总统计（需求vs实际vs缺员）
 */
@Schema(description = "每日汇总VO")
public class DailySummaryVO {

    /**
     * 日期(YYYY-MM-DD)
     */
    @Schema(description = "日期", pattern = "yyyy-MM-dd")
    private String date;

    /**
     * 星期几(中文)
     * 如: 周一、周二、...、周日
     */
    @Schema(description = "星期几", example = "周一")
    private String dayOfWeek;

    /**
     * 是否周末(周六或周日)
     */
    @Schema(description = "是否周末")
    private Boolean isWeekend;

    /**
     * 是否法定节假日
     * 后续可通过节假日服务获取
     */
    @Schema(description = "是否法定节假日")
    private Boolean isHoliday;

    /**
     * 各时段需求人数
     * key: morning/noon/evening, value: 需求人数
     */
    @Schema(description = "各时段需求人数")
    private Map<String, Integer> demand;

    /**
     * 各时段实际排班人数
     * key: morning/noon/evening, value: 实际人数
     */
    @Schema(description = "各时段实际排班人数")
    private Map<String, Integer> actual;

    /**
     * 各时段缺员人数(demand - actual, 负数表示超编)
     * key: morning/noon/evening, value: 缺员人数
     */
    @Schema(description = "各时段缺员人数")
    private Map<String, Integer> shortage;

    // ==================== Getter & Setter ====================

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getIsWeekend() {
        return isWeekend;
    }

    public void setIsWeekend(Boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public Boolean getIsHoliday() {
        return isHoliday;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public Map<String, Integer> getDemand() {
        return demand;
    }

    public void setDemand(Map<String, Integer> demand) {
        this.demand = demand;
    }

    public Map<String, Integer> getActual() {
        return actual;
    }

    public void setActual(Map<String, Integer> actual) {
        this.actual = actual;
    }

    public Map<String, Integer> getShortage() {
        return shortage;
    }

    public void setShortage(Map<String, Integer> shortage) {
        this.shortage = shortage;
    }
}
