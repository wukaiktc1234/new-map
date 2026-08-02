package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 采购订单明细DTO
 * 用于接收采购订单中的商品明细信息
 */
@Schema(description = "采购订单明细")
public class PurchaseOrderItemDTO {

    /**
     * 物料ID
     */
    @Schema(description = "物料ID", example = "100")
    private Long materialId;

    /**
     * 物料名称
     */
    @NotBlank(message = "物料名称不能为空")
    @Size(max = 200, message = "物料名称不能超过200个字符")
    @Schema(description = "物料名称", example = "土豆", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialName;

    /**
     * 规格
     */
    @Size(max = 200, message = "规格不能超过200个字符")
    @Schema(description = "规格", example = "大号/5kg装")
    private String specification;

    /**
     * 单位
     */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位不能超过20个字符")
    @Schema(description = "单位", example = "斤", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;

    /**
     * 采购数量
     */
    @NotNull(message = "采购数量不能为空")
    @DecimalMin(value = "0.001", message = "采购数量必须大于0")
    @Schema(description = "采购数量", example = "100.000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;

    /**
     * 单价（单位：分）
     */
    @NotNull(message = "单价不能为空")
    @Min(value = 0, message = "单价不能为负数")
    @Schema(description = "单价（分）", example = "300", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long unitPrice;

    /**
     * 税率
     */
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Schema(description = "税率", example = "0.13")
    private BigDecimal taxRate;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    /**
     * 计划收货方类型：STORE / WAREHOUSE
     */
    @Size(max = 20, message = "计划收货方类型不能超过20个字符")
    @Schema(description = "计划收货方类型")
    private String plannedReceiverType;

    /**
     * 计划收货门店ID
     */
    @Size(max = 50, message = "计划收货门店ID不能超过50个字符")
    @Schema(description = "计划收货门店ID")
    private String plannedStoreId;

    /**
     * 计划收货仓库ID
     */
    @Schema(description = "计划收货仓库ID")
    private Long plannedWarehouseId;

    // ==================== Getter & Setter ====================

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPlannedReceiverType() {
        return plannedReceiverType;
    }

    public void setPlannedReceiverType(String plannedReceiverType) {
        this.plannedReceiverType = plannedReceiverType;
    }

    public String getPlannedStoreId() {
        return plannedStoreId;
    }

    public void setPlannedStoreId(String plannedStoreId) {
        this.plannedStoreId = plannedStoreId;
    }

    public Long getPlannedWarehouseId() {
        return plannedWarehouseId;
    }

    public void setPlannedWarehouseId(Long plannedWarehouseId) {
        this.plannedWarehouseId = plannedWarehouseId;
    }

    /**
     * 计算明细金额（单价 * 数量）
     * @return 金额（分）
     */
    public Long calculateAmount() {
        if (quantity == null || unitPrice == null) {
            return 0L;
        }
        return Math.round(quantity.doubleValue() * unitPrice);
    }
}
