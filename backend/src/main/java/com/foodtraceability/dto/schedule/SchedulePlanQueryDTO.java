package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 排班方案查询DTO
 * 用于排班方案列表的筛选和分页
 */
@Schema(description = "排班方案查询DTO")
public class SchedulePlanQueryDTO {

    @Schema(description = "状态筛选(draft/published/executing/archived)", example = "draft")
    private String status;

    @Schema(description = "周期起始日期（从该日期开始）", example = "2026-05-01")
    private LocalDate startDate;

    @Schema(description = "周期结束日期（到该日期结束）", example = "2026-05-31")
    private LocalDate endDate;

    @Schema(description = "使用的模板ID筛选", example = "")
    private Long templateId;

    @Schema(description = "页码，默认1", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，默认20", example = "20")
    private Integer size = 20;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
