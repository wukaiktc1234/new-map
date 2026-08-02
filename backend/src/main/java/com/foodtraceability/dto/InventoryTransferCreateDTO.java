package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 调拨单创建DTO
 */
@Schema(description = "调拨单创建DTO")
public class InventoryTransferCreateDTO {

    @NotNull(message = "源仓库ID不能为空")
    @Schema(description = "源仓库ID", example = "1", required = true)
    private Long fromWarehouseId;

    @NotNull(message = "目标仓库ID不能为空")
    @Schema(description = "目标仓库ID", example = "2", required = true)
    private Long toWarehouseId;

    @Schema(description = "产品ID", example = "1")
    private Long productId;

    @Schema(description = "产品名称", example = "食用油")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    private String productName;

    @Schema(description = "调拨数量")
    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0.001", message = "调拨数量必须大于0")
    private BigDecimal transferQuantity;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public Long getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(Long fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public Long getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(Long toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getTransferQuantity() {
        return transferQuantity;
    }

    public void setTransferQuantity(BigDecimal transferQuantity) {
        this.transferQuantity = transferQuantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
