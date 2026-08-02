package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 报损单更新DTO
 */
@Schema(description = "报损单更新DTO")
public class InventoryLossUpdateDTO {

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "报损类型（1:过期 2:损坏 3:丢失 4:其他）")
    private Integer lossType;

    @Schema(description = "总数量")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额（分）")
    private Long totalAmount;

    @Schema(description = "报损原因")
    private String reason;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getLossType() {
        return lossType;
    }

    public void setLossType(Integer lossType) {
        this.lossType = lossType;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
