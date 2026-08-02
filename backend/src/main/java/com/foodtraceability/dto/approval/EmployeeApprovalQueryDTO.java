package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * 员工日常审批查询DTO
 * 用于审批列表的筛选和分页查询条件
 * 支持按类型、状态、关键词、优先级、日期范围等多维度查询
 */
@Schema(description = "员工日常审批查询DTO")
public class EmployeeApprovalQueryDTO {

    /** 审批类型筛选（可选） */
    @Pattern(regexp = "^(leave|overtime|swap|travel|reimbursement|requisition)?$", message = "审批类型不合法")
    @Schema(description = "审批类型筛选", example = "leave")
    private String type;

    /** 审批状态筛选（可选） */
    @Pattern(regexp = "^(pending|approved|rejected|withdrawn)?$", message = "审批状态不合法")
    @Schema(description = "审批状态筛选", example = "pending")
    private String status;

    /** 关键词搜索（模糊匹配标题） */
    @Schema(description = "关键词（标题模糊搜索）", example = "年假")
    private String keyword;

    /** 优先级筛选（可选） */
    @Pattern(regexp = "^(normal|urgent|critical)?$", message = "优先级不合法")
    @Schema(description = "优先级筛选", example = "urgent")
    private String priority;

    /** 起始日期（创建时间范围起始） */
    @Schema(description = "起始日期", example = "2026-06-01")
    private LocalDate dateFrom;

    /** 结束日期（创建时间范围结束） */
    @Schema(description = "结束日期", example = "2026-06-30")
    private LocalDate dateTo;

    /** 当前页码（默认1） */
    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "当前页码（默认1）", example = "1")
    private Integer current = 1;

    /** 每页条数（默认10，最大100） */
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer size = 10;

    // ==================== Getter & Setter 方法 ====================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
