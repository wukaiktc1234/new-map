package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 创建采购单DTO
 */
@Schema(description = "创建采购单DTO")
public class PurchaseOrderCreateFromWarningDTO {

    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "1", required = true)
    private Long warehouseId;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @NotNull(message = "采购明细不能为空")
    @Schema(description = "采购明细列表", required = true)
    private List<PurchaseOrderItemFromWarningDTO> items;

    @Schema(description = "备注")
    private String remark;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public List<PurchaseOrderItemFromWarningDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemFromWarningDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 采购明细项DTO
     */
    @Schema(description = "采购明细项DTO")
    public static class PurchaseOrderItemFromWarningDTO {

        @NotNull(message = "产品ID不能为空")
        @Schema(description = "产品ID", required = true)
        private Long productId;

        @NotNull(message = "采购数量不能为空")
        @Schema(description = "采购数量", required = true)
        private Integer quantity;

        @Schema(description = "单价（分）")
        private Long unitPrice;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(Long unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
