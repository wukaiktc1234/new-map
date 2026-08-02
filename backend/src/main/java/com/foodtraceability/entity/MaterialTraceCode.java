package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("material_trace_code")
@Schema(description = "原料追溯码实体")
public class MaterialTraceCode {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID")
    private String traceCodeId;
    @TableField("trace_code")
    @Schema(description = "追溯码（唯一编码）")
    private String traceCode;
    @TableField("material_id")
    @Schema(description = "物料ID")
    private String materialId;
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;
    @TableField("batch_no")
    @Schema(description = "批次号")
    private String batchNumber;
    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private String supplierId;
    @TableField("supplier_name")
    @Schema(description = "供应商名称")
    private String supplierName;
    @TableField("purchase_order_id")
    @Schema(description = "采购订单ID")
    private String purchaseOrderId;
    @TableField("purchase_order_no")
    @Schema(description = "采购订单号")
    private String purchaseOrderNo;
    @TableField("quantity")
    @Schema(description = "数量")
    private BigDecimal quantity;
    @TableField("unit")
    @Schema(description = "单位")
    private String unit;
    @TableField("weight")
    @Schema(description = "重量")
    private BigDecimal weight;
    @TableField("weight_unit")
    @Schema(description = "重量单位")
    private String weightUnit;
    @TableField("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期")
    private LocalDate expiryDate;
    @TableField("shelf_life_days")
    @Schema(description = "保质期天数")
    private Integer shelfLifeDays;
    @TableField("storage_condition")
    @Schema(description = "存储条件（冷藏/常温/冷冻）")
    private String storageCondition;
    @TableField("storage_location")
    @Schema(description = "存储位置")
    private String storageLocation;
    @TableField("store_id")
    @Schema(description = "入库门店ID")
    private String storeId;
    @TableField("store_name")
    @Schema(description = "入库门店名称")
    private String storeName;
    @TableField("entry_type")
    @Schema(description = "入库类型：quick-快速入库，supplier-供应商直送，order-采购订单")
    private String entryType;
    @TableField("status")
    @Schema(description = "状态")
    private String status;
    @TableField("available_quantity")
    @Schema(description = "可用数量")
    private BigDecimal availableQuantity;
    @TableField("locked_quantity")
    @Schema(description = "锁定数量")
    private BigDecimal lockedQuantity;
    @TableField("trace_type")
    @Schema(description = "追溯码类型：batch-批次码，unit-单品码，container-容器码")
    private String traceType;
    @TableField("bind_dish_id")
    @Schema(description = "绑定菜品ID（单品码专用）")
    private String bindDishId;
    @TableField("bind_dish_name")
    @Schema(description = "绑定菜品名称（单品码专用）")
    private String bindDishName;
    @TableField("generate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生成时间")
    private LocalDateTime generateTime;
    @TableField("scan_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扫描时间")
    private LocalDateTime scanTime;
    @TableField("use_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "使用时间")
    private LocalDateTime useTime;
    @TableField("used_quantity")
    @Schema(description = "已使用数量")
    private BigDecimal usedQuantity;
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

    public MaterialTraceCode() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTraceCodeId() {
        return this.traceCodeId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getMaterialId() {
        return this.materialId;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public String getBatchNumber() {
        return this.batchNumber;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getPurchaseOrderId() {
        return this.purchaseOrderId;
    }

    public String getPurchaseOrderNo() {
        return this.purchaseOrderNo;
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

    public LocalDate getProductionDate() {
        return this.productionDate;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
    }

    public String getStorageCondition() {
        return this.storageCondition;
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getEntryType() {
        return this.entryType;
    }

    public String getStatus() {
        return this.status;
    }

    public BigDecimal getAvailableQuantity() {
        return this.availableQuantity;
    }

    public BigDecimal getLockedQuantity() {
        return this.lockedQuantity;
    }

    public String getTraceType() {
        return this.traceType;
    }

    public String getBindDishId() {
        return this.bindDishId;
    }

    public String getBindDishName() {
        return this.bindDishName;
    }

    public LocalDateTime getGenerateTime() {
        return this.generateTime;
    }

    public LocalDateTime getScanTime() {
        return this.scanTime;
    }

    public LocalDateTime getUseTime() {
        return this.useTime;
    }

    public BigDecimal getUsedQuantity() {
        return this.usedQuantity;
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

    public void setTraceCodeId(final String traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setMaterialId(final String materialId) {
        this.materialId = materialId;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setBatchNumber(final String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setSupplierId(final String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setPurchaseOrderId(final String purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public void setPurchaseOrderNo(final String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    public void setProductionDate(final LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public void setExpiryDate(final LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setStorageCondition(final String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setEntryType(final String entryType) {
        this.entryType = entryType;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setAvailableQuantity(final BigDecimal availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void setLockedQuantity(final BigDecimal lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public void setTraceType(final String traceType) {
        this.traceType = traceType;
    }

    public void setBindDishId(final String bindDishId) {
        this.bindDishId = bindDishId;
    }

    public void setBindDishName(final String bindDishName) {
        this.bindDishName = bindDishName;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setGenerateTime(final LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setScanTime(final LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUseTime(final LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public void setUsedQuantity(final BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
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
        if (!(o instanceof MaterialTraceCode)) return false;
        final MaterialTraceCode other = (MaterialTraceCode) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$traceCodeId = this.getTraceCodeId();
        final java.lang.Object other$traceCodeId = other.getTraceCodeId();
        if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$materialId = this.getMaterialId();
        final java.lang.Object other$materialId = other.getMaterialId();
        if (this$materialId == null ? other$materialId != null : !this$materialId.equals(other$materialId)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$purchaseOrderId = this.getPurchaseOrderId();
        final java.lang.Object other$purchaseOrderId = other.getPurchaseOrderId();
        if (this$purchaseOrderId == null ? other$purchaseOrderId != null : !this$purchaseOrderId.equals(other$purchaseOrderId)) return false;
        final java.lang.Object this$purchaseOrderNo = this.getPurchaseOrderNo();
        final java.lang.Object other$purchaseOrderNo = other.getPurchaseOrderNo();
        if (this$purchaseOrderNo == null ? other$purchaseOrderNo != null : !this$purchaseOrderNo.equals(other$purchaseOrderNo)) return false;
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
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expiryDate = this.getExpiryDate();
        final java.lang.Object other$expiryDate = other.getExpiryDate();
        if (this$expiryDate == null ? other$expiryDate != null : !this$expiryDate.equals(other$expiryDate)) return false;
        final java.lang.Object this$storageCondition = this.getStorageCondition();
        final java.lang.Object other$storageCondition = other.getStorageCondition();
        if (this$storageCondition == null ? other$storageCondition != null : !this$storageCondition.equals(other$storageCondition)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$entryType = this.getEntryType();
        final java.lang.Object other$entryType = other.getEntryType();
        if (this$entryType == null ? other$entryType != null : !this$entryType.equals(other$entryType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$availableQuantity = this.getAvailableQuantity();
        final java.lang.Object other$availableQuantity = other.getAvailableQuantity();
        if (this$availableQuantity == null ? other$availableQuantity != null : !this$availableQuantity.equals(other$availableQuantity)) return false;
        final java.lang.Object this$lockedQuantity = this.getLockedQuantity();
        final java.lang.Object other$lockedQuantity = other.getLockedQuantity();
        if (this$lockedQuantity == null ? other$lockedQuantity != null : !this$lockedQuantity.equals(other$lockedQuantity)) return false;
        final java.lang.Object this$traceType = this.getTraceType();
        final java.lang.Object other$traceType = other.getTraceType();
        if (this$traceType == null ? other$traceType != null : !this$traceType.equals(other$traceType)) return false;
        final java.lang.Object this$bindDishId = this.getBindDishId();
        final java.lang.Object other$bindDishId = other.getBindDishId();
        if (this$bindDishId == null ? other$bindDishId != null : !this$bindDishId.equals(other$bindDishId)) return false;
        final java.lang.Object this$bindDishName = this.getBindDishName();
        final java.lang.Object other$bindDishName = other.getBindDishName();
        if (this$bindDishName == null ? other$bindDishName != null : !this$bindDishName.equals(other$bindDishName)) return false;
        final java.lang.Object this$generateTime = this.getGenerateTime();
        final java.lang.Object other$generateTime = other.getGenerateTime();
        if (this$generateTime == null ? other$generateTime != null : !this$generateTime.equals(other$generateTime)) return false;
        final java.lang.Object this$scanTime = this.getScanTime();
        final java.lang.Object other$scanTime = other.getScanTime();
        if (this$scanTime == null ? other$scanTime != null : !this$scanTime.equals(other$scanTime)) return false;
        final java.lang.Object this$useTime = this.getUseTime();
        final java.lang.Object other$useTime = other.getUseTime();
        if (this$useTime == null ? other$useTime != null : !this$useTime.equals(other$useTime)) return false;
        final java.lang.Object this$usedQuantity = this.getUsedQuantity();
        final java.lang.Object other$usedQuantity = other.getUsedQuantity();
        if (this$usedQuantity == null ? other$usedQuantity != null : !this$usedQuantity.equals(other$usedQuantity)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MaterialTraceCode;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $traceCodeId = this.getTraceCodeId();
        result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $materialId = this.getMaterialId();
        result = result * PRIME + ($materialId == null ? 43 : $materialId.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $purchaseOrderId = this.getPurchaseOrderId();
        result = result * PRIME + ($purchaseOrderId == null ? 43 : $purchaseOrderId.hashCode());
        final java.lang.Object $purchaseOrderNo = this.getPurchaseOrderNo();
        result = result * PRIME + ($purchaseOrderNo == null ? 43 : $purchaseOrderNo.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
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
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $entryType = this.getEntryType();
        result = result * PRIME + ($entryType == null ? 43 : $entryType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $availableQuantity = this.getAvailableQuantity();
        result = result * PRIME + ($availableQuantity == null ? 43 : $availableQuantity.hashCode());
        final java.lang.Object $lockedQuantity = this.getLockedQuantity();
        result = result * PRIME + ($lockedQuantity == null ? 43 : $lockedQuantity.hashCode());
        final java.lang.Object $traceType = this.getTraceType();
        result = result * PRIME + ($traceType == null ? 43 : $traceType.hashCode());
        final java.lang.Object $bindDishId = this.getBindDishId();
        result = result * PRIME + ($bindDishId == null ? 43 : $bindDishId.hashCode());
        final java.lang.Object $bindDishName = this.getBindDishName();
        result = result * PRIME + ($bindDishName == null ? 43 : $bindDishName.hashCode());
        final java.lang.Object $generateTime = this.getGenerateTime();
        result = result * PRIME + ($generateTime == null ? 43 : $generateTime.hashCode());
        final java.lang.Object $scanTime = this.getScanTime();
        result = result * PRIME + ($scanTime == null ? 43 : $scanTime.hashCode());
        final java.lang.Object $useTime = this.getUseTime();
        result = result * PRIME + ($useTime == null ? 43 : $useTime.hashCode());
        final java.lang.Object $usedQuantity = this.getUsedQuantity();
        result = result * PRIME + ($usedQuantity == null ? 43 : $usedQuantity.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MaterialTraceCode(id=" + this.getId() + ", traceCodeId=" + this.getTraceCodeId() + ", traceCode=" + this.getTraceCode() + ", materialId=" + this.getMaterialId() + ", materialName=" + this.getMaterialName() + ", batchNumber=" + this.getBatchNumber() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", purchaseOrderId=" + this.getPurchaseOrderId() + ", purchaseOrderNo=" + this.getPurchaseOrderNo() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", weight=" + this.getWeight() + ", weightUnit=" + this.getWeightUnit() + ", productionDate=" + this.getProductionDate() + ", expiryDate=" + this.getExpiryDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", storageCondition=" + this.getStorageCondition() + ", storageLocation=" + this.getStorageLocation() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", entryType=" + this.getEntryType() + ", status=" + this.getStatus() + ", availableQuantity=" + this.getAvailableQuantity() + ", lockedQuantity=" + this.getLockedQuantity() + ", traceType=" + this.getTraceType() + ", bindDishId=" + this.getBindDishId() + ", bindDishName=" + this.getBindDishName() + ", generateTime=" + this.getGenerateTime() + ", scanTime=" + this.getScanTime() + ", useTime=" + this.getUseTime() + ", usedQuantity=" + this.getUsedQuantity() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
