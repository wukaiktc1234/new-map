package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 物品领用申请创建DTO
 * 用于物品领用类型审批的详细表单数据
 * 包含物品名称、数量、用途、紧急度、预估费用等信息
 */
@Schema(description = "物品领用申请创建DTO")
public class RequisitionCreateDTO {

    /** 物品名称 */
    @NotBlank(message = "物品名称不能为空")
    @Size(max = 100, message = "物品名称长度不能超过100个字符")
    @Schema(description = "物品名称", example = "一次性手套",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemName;

    /** 分类ID（关联物料分类表） */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @Max(value = 9999, message = "数量不能超过9999")
    @Schema(description = "数量", example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    /** 单位（箱/包/个/套等） */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位长度不能超过20个字符")
    @Schema(description = "单位", example = "包",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;

    /** 用途说明 */
    @NotBlank(message = "用途不能为空")
    @Size(max = 500, message = "用途长度不能超过500个字符")
    @Schema(description = "用途", example = "厨房日常操作使用",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String purpose;

    /** 期望供应商 */
    @Size(max = 100, message = "期望供应商长度不能超过100个字符")
    @Schema(description = "期望供应商", example = "XX食品包装公司")
    private String supplierPreference;

    /**
     * 紧急程度
     * normal=普通, urgent=紧急, emergency=特急
     * 可选，默认为normal
     */
    @Pattern(regexp = "^(normal|urgent|emergency)?$", message = "紧急程度不合法")
    @Schema(description = "紧急程度", example = "normal",
            allowableValues = {"normal", "urgent", "emergency"})
    private String urgencyLevel;

    /** 期望到货日期 */
    @Schema(description = "期望到货日期", example = "2026-06-20")
    private LocalDate expectedDate;

    /**
     * 预估费用（单位：元）
     * 前端传入元，后端转换为分存储到数据库
     * 可选，系统可根据物料单价自动计算
     */
    @DecimalMin(value = "0.00", message = "预估费用不能小于0")
    @Schema(description = "预估费用（单位：元）", example = "250.00")
    private BigDecimal estimatedCost;

    // ==================== Getter & Setter 方法 ====================

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

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}
