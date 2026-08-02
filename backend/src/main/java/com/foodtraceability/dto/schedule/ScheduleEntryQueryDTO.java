package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 排班条目查询DTO
 * 用于排班条目的多条件筛选和分页查询
 */
@Schema(description = "排班条目查询DTO")
public class ScheduleEntryQueryDTO {

    @Schema(description = "方案ID", example = "")
    private String planId;

    @Schema(description = "员工ID(按员工筛选)")
    private Long employeeId;

    @Schema(description = "起始日期(含)", example = "2026-05-01")
    private LocalDate startDate;

    @Schema(description = "结束日期(含)", example = "2026-05-31")
    private LocalDate endDate;

    /**
     * 班次类型筛选
     * morning/noon/evening/night_off
     */
    @Schema(description = "班次类型筛选",
            allowableValues = {"morning", "noon", "evening", "night_off"})
    private String shiftType;

    @Schema(description = "页码，默认1", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，默认20", example = "20")
    private Integer size = 20;

    // ==================== Getter & Setter ====================

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
