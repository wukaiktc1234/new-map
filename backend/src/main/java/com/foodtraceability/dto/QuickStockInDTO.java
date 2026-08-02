package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "快速入库DTO")
public class QuickStockInDTO {
    @Schema(description = "商品条码")
    private String barcode;
    @Schema(description = "模板ID（条码匹配失败时使用）")
    private Long templateId;
    @NotBlank(message = "原料名称不能为空")
    @Schema(description = "原料名称")
    private String materialName;
    @NotNull(message = "重量不能为空")
    @Schema(description = "重量")
    private BigDecimal weight;
    @NotBlank(message = "重量单位不能为空")
    @Schema(description = "重量单位")
    private String weightUnit;
    @NotNull(message = "生产日期不能为空")
    @Schema(description = "生产日期")
    private LocalDate productionDate;
    @NotNull(message = "保质期天数不能为空")
    @Schema(description = "保质期天数")
    private Integer shelfLifeDays;
    @Schema(description = "过期日期（自动计算）")
    private LocalDate expiryDate;
    @NotBlank(message = "存储条件不能为空")
    @Schema(description = "存储条件")
    private String storageCondition;
    @Schema(description = "供应商ID")
    private String supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "批次号")
    private String batchNumber;
    @Schema(description = "入库门店ID")
    private String storeId;
    @Schema(description = "入库门店名称")
    private String storeName;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "生成数量")
    private Integer generateCount = 1;

    public QuickStockInDTO() {
    }

    public String getBarcode() {
        return this.barcode;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public BigDecimal getWeight() {
        return this.weight;
    }

    public String getWeightUnit() {
        return this.weightUnit;
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

    public String getStorageCondition() {
        return this.storageCondition;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getBatchNumber() {
        return this.batchNumber;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getRemark() {
        return this.remark;
    }

    public Integer getGenerateCount() {
        return this.generateCount;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setWeight(final BigDecimal weight) {
        this.weight = weight;
    }

    public void setWeightUnit(final String weightUnit) {
        this.weightUnit = weightUnit;
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

    public void setStorageCondition(final String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public void setSupplierId(final String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setBatchNumber(final String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setGenerateCount(final Integer generateCount) {
        this.generateCount = generateCount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof QuickStockInDTO)) return false;
        final QuickStockInDTO other = (QuickStockInDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$templateId = this.getTemplateId();
        final java.lang.Object other$templateId = other.getTemplateId();
        if (this$templateId == null ? other$templateId != null : !this$templateId.equals(other$templateId)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$generateCount = this.getGenerateCount();
        final java.lang.Object other$generateCount = other.getGenerateCount();
        if (this$generateCount == null ? other$generateCount != null : !this$generateCount.equals(other$generateCount)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$weight = this.getWeight();
        final java.lang.Object other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !this$weight.equals(other$weight)) return false;
        final java.lang.Object this$weightUnit = this.getWeightUnit();
        final java.lang.Object other$weightUnit = other.getWeightUnit();
        if (this$weightUnit == null ? other$weightUnit != null : !this$weightUnit.equals(other$weightUnit)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expiryDate = this.getExpiryDate();
        final java.lang.Object other$expiryDate = other.getExpiryDate();
        if (this$expiryDate == null ? other$expiryDate != null : !this$expiryDate.equals(other$expiryDate)) return false;
        final java.lang.Object this$storageCondition = this.getStorageCondition();
        final java.lang.Object other$storageCondition = other.getStorageCondition();
        if (this$storageCondition == null ? other$storageCondition != null : !this$storageCondition.equals(other$storageCondition)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof QuickStockInDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $templateId = this.getTemplateId();
        result = result * PRIME + ($templateId == null ? 43 : $templateId.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $generateCount = this.getGenerateCount();
        result = result * PRIME + ($generateCount == null ? 43 : $generateCount.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $weight = this.getWeight();
        result = result * PRIME + ($weight == null ? 43 : $weight.hashCode());
        final java.lang.Object $weightUnit = this.getWeightUnit();
        result = result * PRIME + ($weightUnit == null ? 43 : $weightUnit.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $expiryDate = this.getExpiryDate();
        result = result * PRIME + ($expiryDate == null ? 43 : $expiryDate.hashCode());
        final java.lang.Object $storageCondition = this.getStorageCondition();
        result = result * PRIME + ($storageCondition == null ? 43 : $storageCondition.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "QuickStockInDTO(barcode=" + this.getBarcode() + ", templateId=" + this.getTemplateId() + ", materialName=" + this.getMaterialName() + ", weight=" + this.getWeight() + ", weightUnit=" + this.getWeightUnit() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", expiryDate=" + this.getExpiryDate() + ", storageCondition=" + this.getStorageCondition() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", batchNumber=" + this.getBatchNumber() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", remark=" + this.getRemark() + ", generateCount=" + this.getGenerateCount() + ")";
    }
}
