package com.foodtraceability.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购入库完成事件
 * 触发：追溯码生成、库存更新、财务应付记录
 */
public class PurchaseStockInEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private Long stockinId;
    private String stockinNo;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private Long productId;
    private String productName;
    private String productCode;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String batchNo;
    private String productionDate;
    private Integer shelfLifeDays;
    private Long warehouseId;
    private String warehouseName;
    private Long storeId;
    private String storeName;
    private LocalDateTime stockinTime;
    private String operatorName;
    private LocalDateTime eventTime;

    public PurchaseStockInEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() {
        return this.eventId;
    }

    public Long getStockinId() {
        return this.stockinId;
    }

    public String getStockinNo() {
        return this.stockinNo;
    }

    public String getOrderNo() {
        return this.orderNo;
    }

    public Long getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getBatchNo() {
        return this.batchNo;
    }

    public String getProductionDate() {
        return this.productionDate;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public String getWarehouseName() {
        return this.warehouseName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public LocalDateTime getStockinTime() {
        return this.stockinTime;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getEventTime() {
        return this.eventTime;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public void setStockinId(final Long stockinId) {
        this.stockinId = stockinId;
    }

    public void setStockinNo(final String stockinNo) {
        this.stockinNo = stockinNo;
    }

    public void setOrderNo(final String orderNo) {
        this.orderNo = orderNo;
    }

    public void setSupplierId(final Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setBatchNo(final String batchNo) {
        this.batchNo = batchNo;
    }

    public void setProductionDate(final String productionDate) {
        this.productionDate = productionDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setWarehouseName(final String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setStockinTime(final LocalDateTime stockinTime) {
        this.stockinTime = stockinTime;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setEventTime(final LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseStockInEvent)) return false;
        final PurchaseStockInEvent other = (PurchaseStockInEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stockinId = this.getStockinId();
        final java.lang.Object other$stockinId = other.getStockinId();
        if (this$stockinId == null ? other$stockinId != null : !this$stockinId.equals(other$stockinId)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$warehouseId = this.getWarehouseId();
        final java.lang.Object other$warehouseId = other.getWarehouseId();
        if (this$warehouseId == null ? other$warehouseId != null : !this$warehouseId.equals(other$warehouseId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$stockinNo = this.getStockinNo();
        final java.lang.Object other$stockinNo = other.getStockinNo();
        if (this$stockinNo == null ? other$stockinNo != null : !this$stockinNo.equals(other$stockinNo)) return false;
        final java.lang.Object this$orderNo = this.getOrderNo();
        final java.lang.Object other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$productCode = this.getProductCode();
        final java.lang.Object other$productCode = other.getProductCode();
        if (this$productCode == null ? other$productCode != null : !this$productCode.equals(other$productCode)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$batchNo = this.getBatchNo();
        final java.lang.Object other$batchNo = other.getBatchNo();
        if (this$batchNo == null ? other$batchNo != null : !this$batchNo.equals(other$batchNo)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$warehouseName = this.getWarehouseName();
        final java.lang.Object other$warehouseName = other.getWarehouseName();
        if (this$warehouseName == null ? other$warehouseName != null : !this$warehouseName.equals(other$warehouseName)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$stockinTime = this.getStockinTime();
        final java.lang.Object other$stockinTime = other.getStockinTime();
        if (this$stockinTime == null ? other$stockinTime != null : !this$stockinTime.equals(other$stockinTime)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$eventTime = this.getEventTime();
        final java.lang.Object other$eventTime = other.getEventTime();
        if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseStockInEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stockinId = this.getStockinId();
        result = result * PRIME + ($stockinId == null ? 43 : $stockinId.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $warehouseId = this.getWarehouseId();
        result = result * PRIME + ($warehouseId == null ? 43 : $warehouseId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $stockinNo = this.getStockinNo();
        result = result * PRIME + ($stockinNo == null ? 43 : $stockinNo.hashCode());
        final java.lang.Object $orderNo = this.getOrderNo();
        result = result * PRIME + ($orderNo == null ? 43 : $orderNo.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $productCode = this.getProductCode();
        result = result * PRIME + ($productCode == null ? 43 : $productCode.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $batchNo = this.getBatchNo();
        result = result * PRIME + ($batchNo == null ? 43 : $batchNo.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $warehouseName = this.getWarehouseName();
        result = result * PRIME + ($warehouseName == null ? 43 : $warehouseName.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $stockinTime = this.getStockinTime();
        result = result * PRIME + ($stockinTime == null ? 43 : $stockinTime.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $eventTime = this.getEventTime();
        result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseStockInEvent(eventId=" + this.getEventId() + ", stockinId=" + this.getStockinId() + ", stockinNo=" + this.getStockinNo() + ", orderNo=" + this.getOrderNo() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", productCode=" + this.getProductCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", unitPrice=" + this.getUnitPrice() + ", totalAmount=" + this.getTotalAmount() + ", batchNo=" + this.getBatchNo() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", warehouseId=" + this.getWarehouseId() + ", warehouseName=" + this.getWarehouseName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", stockinTime=" + this.getStockinTime() + ", operatorName=" + this.getOperatorName() + ", eventTime=" + this.getEventTime() + ")";
    }
}
