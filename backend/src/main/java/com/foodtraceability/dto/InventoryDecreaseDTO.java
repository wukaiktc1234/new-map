package com.foodtraceability.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存扣减DTO
 * 用于库存扣减操作的请求参数
 */
public class InventoryDecreaseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 物料ID */
    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 扣减数量 */
    @NotNull(message = "扣减数量不能为空")
    private BigDecimal quantity;

    /** 变动类型：1-销售出库，2-领料出库，3-调拨出库，4-盘亏，5-损耗，6-其他 */
    private Integer transactionType;

    /** 关联单号 */
    private String referenceNo;

    /** 关联单据类型 */
    private String referenceType;

    /** 备注 */
    private String remark;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
