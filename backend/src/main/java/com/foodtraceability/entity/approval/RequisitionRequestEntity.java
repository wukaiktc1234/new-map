package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物品领用申请详情实体
 * 对应数据库表 requisition_requests
 * 存储领用物品名称、数量、用途、预估费用等详细信息
 */
@TableName("requisition_requests")
@Schema(description = "物品领用申请详情实体")
public class RequisitionRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 领用记录ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "request_id")
    @Schema(description = "领用记录ID", example = "q1w2e3r4t5y6...")
    private String id;

    /** 关联审批主表ID */
    @TableField("approval_id")
    @Schema(description = "关联审批主表ID", example = "approval001")
    private String approvalId;

    /** 申请人ID */
    @TableField("employee_id")
    @Schema(description = "申请人ID", example = "emp001")
    private String employeeId;

    /** 物品名称 */
    @TableField("item_name")
    @Schema(description = "物品名称", example = "一次性手套")
    private String itemName;

    /** 分类ID（关联物料分类表） */
    @TableField("category_id")
    @Schema(description = "分类ID", example = "1001")
    private Long categoryId;

    /** 数量 */
    @TableField("quantity")
    @Schema(description = "数量", example = "100")
    private Integer quantity;

    /** 单位（箱/包/个/套等） */
    @TableField("unit")
    @Schema(description = "单位", example = "包")
    private String unit;

    /** 用途说明 */
    @TableField("purpose")
    @Schema(description = "用途", example = "厨房日常操作使用")
    private String purpose;

    /** 期望供应商 */
    @TableField("supplier_preference")
    @Schema(description = "期望供应商", example = "XX食品包装公司")
    private String supplierPreference;

    /**
     * 紧急程度
     * normal=普通, urgent=紧急, emergency=特急
     */
    @TableField("urgency_level")
    @Schema(description = "紧急程度", example = "normal")
    private String urgencyLevel;

    /** 期望到货日期 */
    @TableField("expected_date")
    @Schema(description = "期望到货日期", example = "2026-06-20")
    private LocalDate expectedDate;

    /**
     * 部门预算（单位：分）
     * 当前部门剩余可用预算
     */
    @TableField("department_budget")
    @Schema(description = "部门预算（分）", example = "500000")
    private BigDecimal departmentBudget;

    /**
     * 预估费用（单位：分）
     * 本次领用的预估总价
     */
    @TableField("estimated_cost")
    @Schema(description = "预估费用（分）", example = "25000")
    private BigDecimal estimatedCost;

    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建人ID */
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 更新人ID */
    @TableField("updated_by")
    @Schema(description = "更新人ID")
    private String updatedBy;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取请求ID（别名方法，委托给 getId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @return 请求ID
     */
    public String getRequestId() {
        return this.id;
    }

    /**
     * 设置请求ID（别名方法，委托给 setId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @param requestId 请求ID
     */
    public void setRequestId(String requestId) {
        this.id = requestId;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSupplierPreference() {
        return supplierPreference;
    }

    public void setSupplierPreference(String supplierPreference) {
        this.supplierPreference = supplierPreference;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public BigDecimal getDepartmentBudget() {
        return departmentBudget;
    }

    public void setDepartmentBudget(BigDecimal departmentBudget) {
        this.departmentBudget = departmentBudget;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
