package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 排班方案创建DTO
 * 用于创建新的排班方案（草稿状态）
 */
@Schema(description = "排班方案创建DTO")
public class SchedulePlanCreateDTO {

    @NotBlank(message = "方案名称不能为空")
    @Schema(description = "方案名称", example = "2026年5月第3周排班方案", required = true)
    private String planName;

    @NotNull(message = "周期起始日期不能为空")
    @Schema(description = "周期起始日期", example = "2026-05-12", required = true)
    private LocalDate startDate;

    @NotNull(message = "周期结束日期不能为空")
    @Schema(description = "周期结束日期", example = "2026-05-18", required = true)
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
