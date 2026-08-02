package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 采购退货明细创建DTO
 */
@Schema(description = "采购退货明细创建请求")
public class PurchaseReturnItemCreateDTO {

    @NotNull(message = "原入库明细ID不能为空")
    @Schema(description = "原入库明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long stockinItemId;

    @NotNull(message = "物料ID不能为空")
    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long materialId;

    @NotNull(message = "退货数量不能为空")
    @DecimalMin(value = "0.001", message = "退货数量必须大于0")
    @Schema(description = "退货数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;

    @Schema(description = "退货原因")
    private String returnReason;

    @Schema(description = "入库批次号（辅助）")
    private String batchNo;

    public Long getStockinItemId() {
        return stockinItemId;
    }

    public void setStockinItemId(Long stockinItemId) {
        this.stockinItemId = stockinItemId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
}
