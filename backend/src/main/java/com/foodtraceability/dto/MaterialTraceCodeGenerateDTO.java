package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "原料追溯码生成DTO")
public class MaterialTraceCodeGenerateDTO {
    @Schema(description = "物料ID")
    private String materialId;
    @Schema(description = "物料名称")
    private String materialName;
    @Schema(description = "条形码")
    private String barcode;
    @Schema(description = "商品编码")
    private String productCode;
    @Schema(description = "商品分类")
    private String productCategory;
    @Schema(description = "供应商ID")
    private String supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "采购入库单ID")
    private String purchaseStockinId;
    @Schema(description = "采购订单号")
    private String purchaseOrderNo;
    @Schema(description = "批次号")
    private String batchNumber;
    @Schema(description = "生产日期")
    private LocalDate productionDate;
    @Schema(description = "保质期（天）")
    private Integer shelfLifeDays;
    @Schema(description = "过期日期")
    private LocalDate expiryDate;
    @Schema(description = "入库时间")
    private String inboundTime;
    @Schema(description = "数量")
    private BigDecimal quantity;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "重量")
    private BigDecimal weight;
    @Schema(description = "重量单位")
    private String weightUnit;
    @Schema(description = "入库单价")
    private BigDecimal unitPrice;
    @Schema(description = "仓库ID")
    private String warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "门店ID")
    private String storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "存储位置")
    private String storageLocation;
    @Schema(description = "存储条件")
    private String storageCondition;
    @Schema(description = "入库类型：quick-快速入库，supplier-供应商直送，order-采购订单")
    private String entryType;
    @Schema(description = "生成数量")
    private Integer generateCount;
    @Schema(description = "备注")
    private String remark;

    public MaterialTraceCodeGenerateDTO() {
    }

    public String getMaterialId() {
        return this.materialId;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public String getProductCategory() {
        return this.productCategory;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getPurchaseStockinId() {
        return this.purchaseStockinId;
    }

    public String getPurchaseOrderNo() {
        return this.purchaseOrderNo;
    }

    public String getBatchNumber() {
        return this.batchNumber;
    }

    public LocalDate getProductionDate() {
        return this.productionDate;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public String getInboundTime() {
        return this.inboundTime;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getWeight() {
        return this.weight;
    }

    public String getWeightUnit() {
        return this.weightUnit;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public String getWarehouseId() {
        return this.warehouseId;
    }

    public String getWarehouseName() {
        return this.warehouseName;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }

    public String getStorageCondition() {
        return this.storageCondition;
    }

    public String getEntryType() {
        return this.entryType;
    }

    public Integer getGenerateCount() {
        return this.generateCount;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setMaterialId(final String materialId) {
        this.materialId = materialId;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    public void setProductCategory(final String productCategory) {
        this.productCategory = productCategory;
    }

    public void setSupplierId(final String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setPurchaseStockinId(final String purchaseStockinId) {
        this.purchaseStockinId = purchaseStockinId;
    }

    public void setPurchaseOrderNo(final String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public void setBatchNumber(final String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setProductionDate(final LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setInboundTime(final String inboundTime) {
        this.inboundTime = inboundTime;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setWeight(final BigDecimal weight) {
        this.weight = weight;
    }

    public void setWeightUnit(final String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setWarehouseId(final String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setWarehouseName(final String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public void setStorageCondition(final String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public void setEntryType(final String entryType) {
        this.entryType = entryType;
    }

    public void setGenerateCount(final Integer generateCount) {
        this.generateCount = generateCount;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MaterialTraceCodeGenerateDTO)) return false;
        final MaterialTraceCodeGenerateDTO other = (MaterialTraceCodeGenerateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$generateCount = this.getGenerateCount();
        final java.lang.Object other$generateCount = other.getGenerateCount();
        if (this$generateCount == null ? other$generateCount != null : !this$generateCount.equals(other$generateCount)) return false;
        final java.lang.Object this$materialId = this.getMaterialId();
        final java.lang.Object other$materialId = other.getMaterialId();
        if (this$materialId == null ? other$materialId != null : !this$materialId.equals(other$materialId)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$productCode = this.getProductCode();
        final java.lang.Object other$productCode = other.getProductCode();
        if (this$productCode == null ? other$productCode != null : !this$productCode.equals(other$productCode)) return false;
        final java.lang.Object this$productCategory = this.getProductCategory();
        final java.lang.Object other$productCategory = other.getProductCategory();
        if (this$productCategory == null ? other$productCategory != null : !this$productCategory.equals(other$productCategory)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$purchaseStockinId = this.getPurchaseStockinId();
        final java.lang.Object other$purchaseStockinId = other.getPurchaseStockinId();
        if (this$purchaseStockinId == null ? other$purchaseStockinId != null : !this$purchaseStockinId.equals(other$purchaseStockinId)) return false;
        final java.lang.Object this$purchaseOrderNo = this.getPurchaseOrderNo();
        final java.lang.Object other$purchaseOrderNo = other.getPurchaseOrderNo();
        if (this$purchaseOrderNo == null ? other$purchaseOrderNo != null : !this$purchaseOrderNo.equals(other$purchaseOrderNo)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expiryDate = this.getExpiryDate();
        final java.lang.Object other$expiryDate = other.getExpiryDate();
        if (this$expiryDate == null ? other$expiryDate != null : !this$expiryDate.equals(other$expiryDate)) return false;
        final java.lang.Object this$inboundTime = this.getInboundTime();
        final java.lang.Object other$inboundTime = other.getInboundTime();
        if (this$inboundTime == null ? other$inboundTime != null : !this$inboundTime.equals(other$inboundTime)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$weight = this.getWeight();
        final java.lang.Object other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !this$weight.equals(other$weight)) return false;
        final java.lang.Object this$weightUnit = this.getWeightUnit();
        final java.lang.Object other$weightUnit = other.getWeightUnit();
        if (this$weightUnit == null ? other$weightUnit != null : !this$weightUnit.equals(other$weightUnit)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$warehouseId = this.getWarehouseId();
        final java.lang.Object other$warehouseId = other.getWarehouseId();
        if (this$warehouseId == null ? other$warehouseId != null : !this$warehouseId.equals(other$warehouseId)) return false;
        final java.lang.Object this$warehouseName = this.getWarehouseName();
        final java.lang.Object other$warehouseName = other.getWarehouseName();
        if (this$warehouseName == null ? other$warehouseName != null : !this$warehouseName.equals(other$warehouseName)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$storageCondition = this.getStorageCondition();
        final java.lang.Object other$storageCondition = other.getStorageCondition();
        if (this$storageCondition == null ? other$storageCondition != null : !this$storageCondition.equals(other$storageCondition)) return false;
        final java.lang.Object this$entryType = this.getEntryType();
        final java.lang.Object other$entryType = other.getEntryType();
        if (this$entryType == null ? other$entryType != null : !this$entryType.equals(other$entryType)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MaterialTraceCodeGenerateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $generateCount = this.getGenerateCount();
        result = result * PRIME + ($generateCount == null ? 43 : $generateCount.hashCode());
        final java.lang.Object $materialId = this.getMaterialId();
        result = result * PRIME + ($materialId == null ? 43 : $materialId.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $productCode = this.getProductCode();
        result = result * PRIME + ($productCode == null ? 43 : $productCode.hashCode());
        final java.lang.Object $productCategory = this.getProductCategory();
        result = result * PRIME + ($productCategory == null ? 43 : $productCategory.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $purchaseStockinId = this.getPurchaseStockinId();
        result = result * PRIME + ($purchaseStockinId == null ? 43 : $purchaseStockinId.hashCode());
        final java.lang.Object $purchaseOrderNo = this.getPurchaseOrderNo();
        result = result * PRIME + ($purchaseOrderNo == null ? 43 : $purchaseOrderNo.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $expiryDate = this.getExpiryDate();
        result = result * PRIME + ($expiryDate == null ? 43 : $expiryDate.hashCode());
        final java.lang.Object $inboundTime = this.getInboundTime();
        result = result * PRIME + ($inboundTime == null ? 43 : $inboundTime.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $weight = this.getWeight();
        result = result * PRIME + ($weight == null ? 43 : $weight.hashCode());
        final java.lang.Object $weightUnit = this.getWeightUnit();
        result = result * PRIME + ($weightUnit == null ? 43 : $weightUnit.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $warehouseId = this.getWarehouseId();
        result = result * PRIME + ($warehouseId == null ? 43 : $warehouseId.hashCode());
        final java.lang.Object $warehouseName = this.getWarehouseName();
        result = result * PRIME + ($warehouseName == null ? 43 : $warehouseName.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $storageCondition = this.getStorageCondition();
        result = result * PRIME + ($storageCondition == null ? 43 : $storageCondition.hashCode());
        final java.lang.Object $entryType = this.getEntryType();
        result = result * PRIME + ($entryType == null ? 43 : $entryType.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MaterialTraceCodeGenerateDTO(materialId=" + this.getMaterialId() + ", materialName=" + this.getMaterialName() + ", barcode=" + this.getBarcode() + ", productCode=" + this.getProductCode() + ", productCategory=" + this.getProductCategory() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", purchaseStockinId=" + this.getPurchaseStockinId() + ", purchaseOrderNo=" + this.getPurchaseOrderNo() + ", batchNumber=" + this.getBatchNumber() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", expiryDate=" + this.getExpiryDate() + ", inboundTime=" + this.getInboundTime() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", weight=" + this.getWeight() + ", weightUnit=" + this.getWeightUnit() + ", unitPrice=" + this.getUnitPrice() + ", warehouseId=" + this.getWarehouseId() + ", warehouseName=" + this.getWarehouseName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", storageLocation=" + this.getStorageLocation() + ", storageCondition=" + this.getStorageCondition() + ", entryType=" + this.getEntryType() + ", generateCount=" + this.getGenerateCount() + ", remark=" + this.getRemark() + ")";
    }
}
