package com.foodtraceability.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 库存扣减请求DTO
 * 用于销售出库等场景
 */
public class InventoryDeductDTO {
    /**
     * 库存ID
     */
    @NotNull(message = "库存ID不能为空")
    private Long inventoryId;

    /**
     * 扣减数量
     */
    @NotNull(message = "扣减数量不能为空")
    @DecimalMin(value = "0.01", message = "扣减数量必须大于0")
    private BigDecimal quantity;

    /**
     * 关联单据号
     */
    private String referenceNo;

    /**
     * 关联单据类型
     */
    private String referenceType;

    /**
     * 变动类型（1入库 2出库 3盘点 4调拨等）
     */
    private Integer transactionType;

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * 业务来源（统一流水 source_type，P0-A 卡收编）。
     * 取值限 -001 §1.4 词表；**必填**：缺省即显式拒绝（宪法 §III.4 / §IV.4）。
     */
    private String sourceType;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
