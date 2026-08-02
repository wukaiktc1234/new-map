package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 预算基本信息DTO（用于缓存和批量查询）
 * 仅包含跨模块共享的必要字段，避免传输完整实体
 */
@Schema(description = "预算基本信息")
public class BudgetBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算ID */
    @Schema(description = "预算ID", example = "1")
    private Long budgetId;

    /** 预算名称（实体暂无该字段，预留扩展） */
    @Schema(description = "预算名称")
    private String budgetName;

    /** 预算期间，格式 yyyy 或 yyyy-MM，由年份/月份派生 */
    @Schema(description = "预算期间", example = "2026-06")
    private String period;

    /** 预算金额（单位：分） */
    @Schema(description = "预算金额（分）", example = "10000000")
    private Long totalAmount;

    /** 已使用金额（单位：分） */
    @Schema(description = "已使用金额（分）", example = "3500000")
    private Long usedAmount;

    /** 责任部门ID */
    @Schema(description = "责任部门ID", example = "1")
    private Long departmentId;

    /** 预算状态（实体暂无该字段，预留扩展） */
    @Schema(description = "预算状态")
    private Integer status;

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(Long usedAmount) {
        this.usedAmount = usedAmount;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BudgetBasicInfo{" +
                "budgetId=" + budgetId +
                ", budgetName='" + budgetName + '\'' +
                ", period='" + period + '\'' +
                ", totalAmount=" + totalAmount +
                ", usedAmount=" + usedAmount +
                ", departmentId=" + departmentId +
                ", status=" + status +
                '}';
    }
}
