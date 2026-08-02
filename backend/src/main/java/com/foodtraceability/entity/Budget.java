package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算实体类
 * 用于管理采购预算信息
 */
@TableName("budget")
@Schema(description = "预算实体")
public class Budget {
    
    /**
     * 预算ID，主键
     */
    @TableId(value = "budget_id", type = IdType.ASSIGN_ID)
    @Schema(description = "预算ID", example = "BUD20251201")
    private String budgetId;
    
    /**
     * 预算名称
     */
    @TableField("budget_name")
    @Schema(description = "预算名称", example = "2025年12月食材采购预算")
    private String budgetName;
    
    /**
     * 预算金额
     */
    @TableField("budget_amount")
    @Schema(description = "预算金额", example = "50000.00")
    private BigDecimal budgetAmount;
    
    /**
     * 已使用金额
     */
    @TableField("used_amount")
    @Schema(description = "已使用金额", example = "12340.00")
    private BigDecimal usedAmount;
    
    /**
     * 剩余金额
     */
    @TableField("remaining_amount")
    @Schema(description = "剩余金额", example = "37660.00")
    private BigDecimal remainingAmount;
    
    /**
     * 预算期间（如202512表示2025年12月）
     */
    @TableField("budget_period")
    @Schema(description = "预算期间", example = "202512")
    private String budgetPeriod;
    
    /**
     * 预算状态（active-生效中，expired-已过期，closed-已关闭）
     */
    @TableField("status")
    @Schema(description = "预算状态", example = "active")
    private String status;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "system")
    private String createBy;
    
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;

    // Getter and Setter methods
    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(String budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getBudgetPeriod() {
        return budgetPeriod;
    }

    public void setBudgetPeriod(String budgetPeriod) {
        this.budgetPeriod = budgetPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}