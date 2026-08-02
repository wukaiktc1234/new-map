package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("material_template")
@Schema(description = "原料模板实体")
public class MaterialTemplate {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("template_code")
    @Schema(description = "模板编码")
    private String templateCode;
    @TableField("material_name")
    @Schema(description = "原料名称")
    private String materialName;
    @TableField("category")
    @Schema(description = "分类")
    private String category;
    @TableField("brand")
    @Schema(description = "品牌")
    private String brand;
    @TableField("specification")
    @Schema(description = "规格")
    private String specification;
    @TableField("default_shelf_life")
    @Schema(description = "默认保质期(天)")
    private Integer defaultShelfLife;
    @TableField("storage_condition")
    @Schema(description = "存储条件")
    private String storageCondition;
    @TableField("weight_unit")
    @Schema(description = "默认重量单位")
    private String weightUnit;
    @TableField("barcode")
    @Schema(description = "商品条码")
    private String barcode;
    @TableField("barcode_unique")
    @Schema(description = "条码是否唯一标识")
    private Integer barcodeUnique;
    @TableField("supplier_id")
    @Schema(description = "默认供应商ID")
    private String supplierId;
    @TableField("supplier_name")
    @Schema(description = "默认供应商名称")
    private String supplierName;
    @TableField("description")
    @Schema(description = "描述")
    private String description;
    @TableField("version")
    @Schema(description = "版本号")
    private Integer version;
    @TableField("parent_id")
    @Schema(description = "父档案ID（版本继承）")
    private Long parentId;
    @TableField("status")
    @Schema(description = "状态")
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public Integer getDefaultShelfLife() {
        return defaultShelfLife;
    }

    public String getStorageCondition() {
        return storageCondition;
    }

    public MaterialTemplate() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public String getCategory() {
        return this.category;
    }

    public String getBrand() {
        return this.brand;
    }

    public String getSpecification() {
        return this.specification;
    }

    public String getWeightUnit() {
        return this.weightUnit;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public Integer getBarcodeUnique() {
        return this.barcodeUnique;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public void setBrand(final String brand) {
        this.brand = brand;
    }

    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    public void setDefaultShelfLife(final Integer defaultShelfLife) {
        this.defaultShelfLife = defaultShelfLife;
    }

    public void setStorageCondition(final String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public void setWeightUnit(final String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public void setBarcodeUnique(final Integer barcodeUnique) {
        this.barcodeUnique = barcodeUnique;
    }

    public void setSupplierId(final String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MaterialTemplate)) return false;
        final MaterialTemplate other = (MaterialTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$defaultShelfLife = this.getDefaultShelfLife();
        final java.lang.Object other$defaultShelfLife = other.getDefaultShelfLife();
        if (this$defaultShelfLife == null ? other$defaultShelfLife != null : !this$defaultShelfLife.equals(other$defaultShelfLife)) return false;
        final java.lang.Object this$barcodeUnique = this.getBarcodeUnique();
        final java.lang.Object other$barcodeUnique = other.getBarcodeUnique();
        if (this$barcodeUnique == null ? other$barcodeUnique != null : !this$barcodeUnique.equals(other$barcodeUnique)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$category = this.getCategory();
        final java.lang.Object other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) return false;
        final java.lang.Object this$brand = this.getBrand();
        final java.lang.Object other$brand = other.getBrand();
        if (this$brand == null ? other$brand != null : !this$brand.equals(other$brand)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        final java.lang.Object this$storageCondition = this.getStorageCondition();
        final java.lang.Object other$storageCondition = other.getStorageCondition();
        if (this$storageCondition == null ? other$storageCondition != null : !this$storageCondition.equals(other$storageCondition)) return false;
        final java.lang.Object this$weightUnit = this.getWeightUnit();
        final java.lang.Object other$weightUnit = other.getWeightUnit();
        if (this$weightUnit == null ? other$weightUnit != null : !this$weightUnit.equals(other$weightUnit)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MaterialTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $defaultShelfLife = this.getDefaultShelfLife();
        result = result * PRIME + ($defaultShelfLife == null ? 43 : $defaultShelfLife.hashCode());
        final java.lang.Object $barcodeUnique = this.getBarcodeUnique();
        result = result * PRIME + ($barcodeUnique == null ? 43 : $barcodeUnique.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $category = this.getCategory();
        result = result * PRIME + ($category == null ? 43 : $category.hashCode());
        final java.lang.Object $brand = this.getBrand();
        result = result * PRIME + ($brand == null ? 43 : $brand.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        final java.lang.Object $storageCondition = this.getStorageCondition();
        result = result * PRIME + ($storageCondition == null ? 43 : $storageCondition.hashCode());
        final java.lang.Object $weightUnit = this.getWeightUnit();
        result = result * PRIME + ($weightUnit == null ? 43 : $weightUnit.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MaterialTemplate(id=" + this.getId() + ", templateCode=" + this.getTemplateCode() + ", materialName=" + this.getMaterialName() + ", category=" + this.getCategory() + ", brand=" + this.getBrand() + ", specification=" + this.getSpecification() + ", defaultShelfLife=" + this.getDefaultShelfLife() + ", storageCondition=" + this.getStorageCondition() + ", weightUnit=" + this.getWeightUnit() + ", barcode=" + this.getBarcode() + ", barcodeUnique=" + this.getBarcodeUnique() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", description=" + this.getDescription() + ", version=" + this.getVersion() + ", parentId=" + this.getParentId() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
