package com.foodtraceability.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 库存锁定请求DTO
 * 用于订单预留库存
 */
public class InventoryLockDTO {
    /**
     * 库存ID
     */
    @NotNull(message = "库存ID不能为空")
    private Long inventoryId;

    /**
     * 锁定数量
     */
    @NotNull(message = "锁定数量不能为空")
    @DecimalMin(value = "0.01", message = "锁定数量必须大于0")
    private BigDecimal quantity;

    /**
     * 关联单据号
     */
    private String referenceNo;

    /**
     * 关联单据类型
     */
    private String referenceType;

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
}
