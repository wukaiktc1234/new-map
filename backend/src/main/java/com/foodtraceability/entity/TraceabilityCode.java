package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("traceability_code")
public class TraceabilityCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String codeType;
    private String uniqueCode;
    private String qrCode;
    private String productName;
    private Long inventoryId;
    private String batchNo;
    private String orderNo;
    private Integer status;
    private String productionTime;
    private String expiryTime;
    private Long storeId;
    private String createdBy;
    private String updatedBy;

    public TraceabilityCode() {
    }

    public Long getId() {
        return this.id;
    }

    public String getCodeType() {
        return this.codeType;
    }

    public String getUniqueCode() {
        return this.uniqueCode;
    }

    public String getQrCode() {
        return this.qrCode;
    }

    public String getProductName() {
        return this.productName;
    }

    public Long getInventoryId() {
        return this.inventoryId;
    }

    public String getBatchNo() {
        return this.batchNo;
    }

    public String getOrderNo() {
        return this.orderNo;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getProductionTime() {
        return this.productionTime;
    }

    public String getExpiryTime() {
        return this.expiryTime;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setCodeType(final String codeType) {
        this.codeType = codeType;
    }

    public void setUniqueCode(final String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    public void setQrCode(final String qrCode) {
        this.qrCode = qrCode;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setInventoryId(final Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public void setBatchNo(final String batchNo) {
        this.batchNo = batchNo;
    }

    public void setOrderNo(final String orderNo) {
        this.orderNo = orderNo;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setProductionTime(final String productionTime) {
        this.productionTime = productionTime;
    }

    public void setExpiryTime(final String expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TraceabilityCode)) return false;
        final TraceabilityCode other = (TraceabilityCode) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$inventoryId = this.getInventoryId();
        final java.lang.Object other$inventoryId = other.getInventoryId();
        if (this$inventoryId == null ? other$inventoryId != null : !this$inventoryId.equals(other$inventoryId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$codeType = this.getCodeType();
        final java.lang.Object other$codeType = other.getCodeType();
        if (this$codeType == null ? other$codeType != null : !this$codeType.equals(other$codeType)) return false;
        final java.lang.Object this$uniqueCode = this.getUniqueCode();
        final java.lang.Object other$uniqueCode = other.getUniqueCode();
        if (this$uniqueCode == null ? other$uniqueCode != null : !this$uniqueCode.equals(other$uniqueCode)) return false;
        final java.lang.Object this$qrCode = this.getQrCode();
        final java.lang.Object other$qrCode = other.getQrCode();
        if (this$qrCode == null ? other$qrCode != null : !this$qrCode.equals(other$qrCode)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$batchNo = this.getBatchNo();
        final java.lang.Object other$batchNo = other.getBatchNo();
        if (this$batchNo == null ? other$batchNo != null : !this$batchNo.equals(other$batchNo)) return false;
        final java.lang.Object this$orderNo = this.getOrderNo();
        final java.lang.Object other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) return false;
        final java.lang.Object this$productionTime = this.getProductionTime();
        final java.lang.Object other$productionTime = other.getProductionTime();
        if (this$productionTime == null ? other$productionTime != null : !this$productionTime.equals(other$productionTime)) return false;
        final java.lang.Object this$expiryTime = this.getExpiryTime();
        final java.lang.Object other$expiryTime = other.getExpiryTime();
        if (this$expiryTime == null ? other$expiryTime != null : !this$expiryTime.equals(other$expiryTime)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TraceabilityCode;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $inventoryId = this.getInventoryId();
        result = result * PRIME + ($inventoryId == null ? 43 : $inventoryId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $codeType = this.getCodeType();
        result = result * PRIME + ($codeType == null ? 43 : $codeType.hashCode());
        final java.lang.Object $uniqueCode = this.getUniqueCode();
        result = result * PRIME + ($uniqueCode == null ? 43 : $uniqueCode.hashCode());
        final java.lang.Object $qrCode = this.getQrCode();
        result = result * PRIME + ($qrCode == null ? 43 : $qrCode.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $batchNo = this.getBatchNo();
        result = result * PRIME + ($batchNo == null ? 43 : $batchNo.hashCode());
        final java.lang.Object $orderNo = this.getOrderNo();
        result = result * PRIME + ($orderNo == null ? 43 : $orderNo.hashCode());
        final java.lang.Object $productionTime = this.getProductionTime();
        result = result * PRIME + ($productionTime == null ? 43 : $productionTime.hashCode());
        final java.lang.Object $expiryTime = this.getExpiryTime();
        result = result * PRIME + ($expiryTime == null ? 43 : $expiryTime.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TraceabilityCode(id=" + this.getId() + ", codeType=" + this.getCodeType() + ", uniqueCode=" + this.getUniqueCode() + ", qrCode=" + this.getQrCode() + ", productName=" + this.getProductName() + ", inventoryId=" + this.getInventoryId() + ", batchNo=" + this.getBatchNo() + ", orderNo=" + this.getOrderNo() + ", status=" + this.getStatus() + ", productionTime=" + this.getProductionTime() + ", expiryTime=" + this.getExpiryTime() + ", storeId=" + this.getStoreId() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
