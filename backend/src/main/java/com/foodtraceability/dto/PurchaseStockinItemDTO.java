package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入库明细DTO
 * 用于接收入库单中的商品明细信息
 */
@Schema(description = "入库明细")
public class PurchaseStockinItemDTO {

    /**
     * 关联的订单明细ID
     */
    @NotNull(message = "订单明细ID不能为空")
    @Schema(description = "订单明细ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderItemId;

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
     * 批次号
     */
    @Size(max = 50, message = "批次号不能超过50个字符")
    @Schema(description = "批次号", example = "B20260425001")
    private String batchNo;

    /**
     * 生产日期
     */
    @Schema(description = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    @Schema(description = "有效期至")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    /**
     * 实收数量
     */
    @NotNull(message = "实收数量不能为空")
    @DecimalMin(value = "0.001", message = "实收数量必须大于0")
    @Schema(description = "实收数量", example = "50.000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal actualQuantity;

    /**
     * 单位
     */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位不能超过20个字符")
    @Schema(description = "单位", example = "斤", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;

    /**
     * 入库单价（单位：分）
     */
    @Min(value = 0, message = "单价不能为负数")
    @Schema(description = "入库单价（分）", example = "300")
    private Long unitPrice;

    /**
     * 库位ID
     */
    @Schema(description = "库位ID", example = "5")
    private Long locationId;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 计算明细金额（单价 * 实收数量）
     * @return 金额（分）
     */
    public Long calculateAmount() {
        if (actualQuantity == null || unitPrice == null) {
            return 0L;
        }
        return Math.round(actualQuantity.doubleValue() * unitPrice);
    }
}
