package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 报损单创建DTO
 */
@Schema(description = "报损单创建DTO")
public class InventoryLossCreateDTO {

    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "1", required = true)
    private Long warehouseId;

    @NotNull(message = "报损类型不能为空")
    @Min(value = 1, message = "报损类型无效")
    @Max(value = 4, message = "报损类型无效")
    @Schema(description = "报损类型（1:过期 2:损坏 3:丢失 4:其他）", example = "2", required = true)
    private Integer lossType;

    @Schema(description = "总数量")
    @DecimalMin(value = "0", message = "总数量不能为负数")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额（分）")
    @Min(value = 0, message = "总金额不能为负数")
    private Long totalAmount;

    @Schema(description = "报损原因")
    @Size(max = 500, message = "报损原因长度不能超过500个字符")
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
