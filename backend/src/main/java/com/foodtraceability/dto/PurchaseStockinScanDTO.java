package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 采购入库扫描DTO
 */
public class PurchaseStockinScanDTO {
    private Long stockinId;
    private String traceCode;
    private BigDecimal scannedQuantity;
    private String scanOperator;
    private Long scanOperatorId;
    private String scanDevice;
    private String storageLocation;
    private String remark;

    public PurchaseStockinScanDTO() {
    }

    public Long getStockinId() {
        return this.stockinId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public BigDecimal getScannedQuantity() {
        return this.scannedQuantity;
    }

    public String getScanOperator() {
        return this.scanOperator;
    }

    public Long getScanOperatorId() {
        return this.scanOperatorId;
    }

    public String getScanDevice() {
        return this.scanDevice;
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setStockinId(final Long stockinId) {
        this.stockinId = stockinId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setScannedQuantity(final BigDecimal scannedQuantity) {
        this.scannedQuantity = scannedQuantity;
    }

    public void setScanOperator(final String scanOperator) {
        this.scanOperator = scanOperator;
    }

    public void setScanOperatorId(final Long scanOperatorId) {
        this.scanOperatorId = scanOperatorId;
    }

    public void setScanDevice(final String scanDevice) {
        this.scanDevice = scanDevice;
    }

    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseStockinScanDTO)) return false;
        final PurchaseStockinScanDTO other = (PurchaseStockinScanDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stockinId = this.getStockinId();
        final java.lang.Object other$stockinId = other.getStockinId();
        if (this$stockinId == null ? other$stockinId != null : !this$stockinId.equals(other$stockinId)) return false;
        final java.lang.Object this$scanOperatorId = this.getScanOperatorId();
        final java.lang.Object other$scanOperatorId = other.getScanOperatorId();
        if (this$scanOperatorId == null ? other$scanOperatorId != null : !this$scanOperatorId.equals(other$scanOperatorId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$scannedQuantity = this.getScannedQuantity();
        final java.lang.Object other$scannedQuantity = other.getScannedQuantity();
        if (this$scannedQuantity == null ? other$scannedQuantity != null : !this$scannedQuantity.equals(other$scannedQuantity)) return false;
        final java.lang.Object this$scanOperator = this.getScanOperator();
        final java.lang.Object other$scanOperator = other.getScanOperator();
        if (this$scanOperator == null ? other$scanOperator != null : !this$scanOperator.equals(other$scanOperator)) return false;
        final java.lang.Object this$scanDevice = this.getScanDevice();
        final java.lang.Object other$scanDevice = other.getScanDevice();
        if (this$scanDevice == null ? other$scanDevice != null : !this$scanDevice.equals(other$scanDevice)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseStockinScanDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stockinId = this.getStockinId();
        result = result * PRIME + ($stockinId == null ? 43 : $stockinId.hashCode());
        final java.lang.Object $scanOperatorId = this.getScanOperatorId();
        result = result * PRIME + ($scanOperatorId == null ? 43 : $scanOperatorId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $scannedQuantity = this.getScannedQuantity();
        result = result * PRIME + ($scannedQuantity == null ? 43 : $scannedQuantity.hashCode());
        final java.lang.Object $scanOperator = this.getScanOperator();
        result = result * PRIME + ($scanOperator == null ? 43 : $scanOperator.hashCode());
        final java.lang.Object $scanDevice = this.getScanDevice();
        result = result * PRIME + ($scanDevice == null ? 43 : $scanDevice.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseStockinScanDTO(stockinId=" + this.getStockinId() + ", traceCode=" + this.getTraceCode() + ", scannedQuantity=" + this.getScannedQuantity() + ", scanOperator=" + this.getScanOperator() + ", scanOperatorId=" + this.getScanOperatorId() + ", scanDevice=" + this.getScanDevice() + ", storageLocation=" + this.getStorageLocation() + ", remark=" + this.getRemark() + ")";
    }
}
