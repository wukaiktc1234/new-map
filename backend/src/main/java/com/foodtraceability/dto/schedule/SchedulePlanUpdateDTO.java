package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 排班方案更新DTO
 * 用于编辑排班方案的基本信息（仅草稿状态可编辑）
 */
@Schema(description = "排班方案更新DTO")
public class SchedulePlanUpdateDTO {

    @Schema(description = "方案名称", example = "2026年5月第3周排班方案(修订)")
    private String planName;

    @Schema(description = "周期起始日期", example = "2026-05-12")
    private LocalDate startDate;

    @Schema(description = "周期结束日期", example = "2026-05-18")
    private LocalDate endDate;

    @Schema(description = "使用的模板ID（可选）", example = "")
    private Long templateId;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }
}
