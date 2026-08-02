package com.foodtraceability.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存入库DTO
 * 用于库存入库操作的请求参数
 */
public class InventoryIncreaseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 物料ID */
    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 入库数量 */
    @NotNull(message = "入库数量不能为空")
    private BigDecimal quantity;

    /** 单位成本（分） */
    private Long unitCost;

    /** 变动类型：1-采购入库，2-调拨入库，3-盘盈，4-退货入库，5-其他 */
    private Integer transactionType;

    /** 批次号 */
    private String batchNo;

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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Long unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
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
