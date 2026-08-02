package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("asset_masters_enhanced")
public class AssetMaster {
    @TableId(value = "asset_id", type = IdType.AUTO)
    private Long id;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    @TableField(exist = false)
    private String categoryName;
    private String specification;
    @TableField(exist = false)
    private String unit;
    @TableField("purchase_order_id")
    private Long purchaseOrderId;
    @TableField("purchase_order_no")
    private String purchaseOrderNo;
    @TableField(exist = false)
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    @TableField("supplier_id")
    private Long supplierId;
    @TableField("supplier_name")
    private String supplierName;
    @TableField("request_id")
    private String requestId;
    @TableField("request_no")
    private String requestNo;
    @TableField(exist = false)
    private String invoiceNumber;
    @TableField("original_cost")
    private BigDecimal originalValue;
    @TableField("net_book_value")
    private BigDecimal netValue;
    @TableField("accumulated_depreciation")
    private BigDecimal accumulatedDepreciation;
    @TableField(exist = false)
    private BigDecimal residualValue;
    private Integer depreciationMethod;
    private Integer usefulLifeMonths;
    @TableField(exist = false)
    private LocalDate depreciationStartDate;
    @TableField(exist = false)
    private LocalDate lastDepreciationDate;
    @TableField(exist = false)
    private String status;
    private Long storeId;
    @TableField(exist = false)
    private String storeName;
    @TableField("department_id")
    private Long departmentId;
    @TableField("department_name")
    private String departmentName;
    @TableField(exist = false)
    private Long custodianId;
    @TableField(exist = false)
    private String custodianName;
    private String location;
    @TableField(exist = false)
    private String assetType;
    private String qrCode;
    @TableField(exist = false)
    private Integer useCount;
    @TableField(exist = false)
    private String currentOrderId;
    @TableField(exist = false)
    private Long currentKitchenOrderId;
    @TableField(exist = false)
    private LocalDateTime bindTime;
    @TableField(exist = false)
    private LocalDateTime lastUseTime;
    private String imageUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private String updateBy;
    @TableLogic
    private Integer deleted;

    public AssetMaster() {
    }

    public Long getId() {
        return this.id;
    }

    public String getAssetCode() {
        return this.assetCode;
    }

    public String getAssetName() {
        return this.assetName;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public String getSpecification() {
        return this.specification;
    }

    public String getUnit() {
        return this.unit;
    }

    public Long getPurchaseOrderId() {
        return this.purchaseOrderId;
    }

    public String getPurchaseOrderNo() {
        return this.purchaseOrderNo;
    }

    public BigDecimal getPurchasePrice() {
        return this.purchasePrice;
    }

    public LocalDate getPurchaseDate() {
        return this.purchaseDate;
    }

    public Long getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getRequestNo() {
        return this.requestNo;
    }

    public String getInvoiceNumber() {
        return this.invoiceNumber;
    }

    public BigDecimal getOriginalValue() {
        return this.originalValue;
    }

    public BigDecimal getNetValue() {
        return this.netValue;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return this.accumulatedDepreciation;
    }

    public BigDecimal getResidualValue() {
        return this.residualValue;
    }

    public Integer getDepreciationMethod() {
        return this.depreciationMethod;
    }

    public Integer getUsefulLifeMonths() {
        return this.usefulLifeMonths;
    }

    public LocalDate getDepreciationStartDate() {
        return this.depreciationStartDate;
    }

    public LocalDate getLastDepreciationDate() {
        return this.lastDepreciationDate;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public Long getCustodianId() {
        return this.custodianId;
    }

    public String getCustodianName() {
        return this.custodianName;
    }

    public String getLocation() {
        return this.location;
    }

    public String getAssetType() {
        return this.assetType;
    }

    public String getQrCode() {
        return this.qrCode;
    }

    public Integer getUseCount() {
        return this.useCount;
    }

    public String getCurrentOrderId() {
        return this.currentOrderId;
    }

    public Long getCurrentKitchenOrderId() {
        return this.currentKitchenOrderId;
    }

    public LocalDateTime getBindTime() {
        return this.bindTime;
    }

    public LocalDateTime getLastUseTime() {
        return this.lastUseTime;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setAssetCode(final String assetCode) {
        this.assetCode = assetCode;
    }

    public void setAssetName(final String assetName) {
        this.assetName = assetName;
    }

    public void setCategoryId(final Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setPurchaseOrderId(final Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public void setPurchaseOrderNo(final String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public void setPurchasePrice(final BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setPurchaseDate(final LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setSupplierId(final Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public void setRequestNo(final String requestNo) {
        this.requestNo = requestNo;
    }

    public void setInvoiceNumber(final String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setOriginalValue(final BigDecimal originalValue) {
        this.originalValue = originalValue;
    }

    public void setNetValue(final BigDecimal netValue) {
        this.netValue = netValue;
    }

    public void setAccumulatedDepreciation(final BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public void setResidualValue(final BigDecimal residualValue) {
        this.residualValue = residualValue;
    }

    public void setDepreciationMethod(final Integer depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public void setUsefulLifeMonths(final Integer usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }

    public void setDepreciationStartDate(final LocalDate depreciationStartDate) {
        this.depreciationStartDate = depreciationStartDate;
    }

    public void setLastDepreciationDate(final LocalDate lastDepreciationDate) {
        this.lastDepreciationDate = lastDepreciationDate;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setCustodianId(final Long custodianId) {
        this.custodianId = custodianId;
    }

    public void setCustodianName(final String custodianName) {
        this.custodianName = custodianName;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setAssetType(final String assetType) {
        this.assetType = assetType;
    }

    public void setQrCode(final String qrCode) {
        this.qrCode = qrCode;
    }

    public void setUseCount(final Integer useCount) {
        this.useCount = useCount;
    }

    public void setCurrentOrderId(final String currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public void setCurrentKitchenOrderId(final Long currentKitchenOrderId) {
        this.currentKitchenOrderId = currentKitchenOrderId;
    }

    public void setBindTime(final LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }

    public void setLastUseTime(final LocalDateTime lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AssetMaster)) return false;
        final AssetMaster other = (AssetMaster) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$categoryId = this.getCategoryId();
        final java.lang.Object other$categoryId = other.getCategoryId();
        if (this$categoryId == null ? other$categoryId != null : !this$categoryId.equals(other$categoryId)) return false;
        final java.lang.Object this$purchaseOrderId = this.getPurchaseOrderId();
        final java.lang.Object other$purchaseOrderId = other.getPurchaseOrderId();
        if (this$purchaseOrderId == null ? other$purchaseOrderId != null : !this$purchaseOrderId.equals(other$purchaseOrderId)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$usefulLifeMonths = this.getUsefulLifeMonths();
        final java.lang.Object other$usefulLifeMonths = other.getUsefulLifeMonths();
        if (this$usefulLifeMonths == null ? other$usefulLifeMonths != null : !this$usefulLifeMonths.equals(other$usefulLifeMonths)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$custodianId = this.getCustodianId();
        final java.lang.Object other$custodianId = other.getCustodianId();
        if (this$custodianId == null ? other$custodianId != null : !this$custodianId.equals(other$custodianId)) return false;
        final java.lang.Object this$useCount = this.getUseCount();
        final java.lang.Object other$useCount = other.getUseCount();
        if (this$useCount == null ? other$useCount != null : !this$useCount.equals(other$useCount)) return false;
        final java.lang.Object this$currentKitchenOrderId = this.getCurrentKitchenOrderId();
        final java.lang.Object other$currentKitchenOrderId = other.getCurrentKitchenOrderId();
        if (this$currentKitchenOrderId == null ? other$currentKitchenOrderId != null : !this$currentKitchenOrderId.equals(other$currentKitchenOrderId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$assetCode = this.getAssetCode();
        final java.lang.Object other$assetCode = other.getAssetCode();
        if (this$assetCode == null ? other$assetCode != null : !this$assetCode.equals(other$assetCode)) return false;
        final java.lang.Object this$assetName = this.getAssetName();
        final java.lang.Object other$assetName = other.getAssetName();
        if (this$assetName == null ? other$assetName != null : !this$assetName.equals(other$assetName)) return false;
        final java.lang.Object this$categoryName = this.getCategoryName();
        final java.lang.Object other$categoryName = other.getCategoryName();
        if (this$categoryName == null ? other$categoryName != null : !this$categoryName.equals(other$categoryName)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$purchasePrice = this.getPurchasePrice();
        final java.lang.Object other$purchasePrice = other.getPurchasePrice();
        if (this$purchasePrice == null ? other$purchasePrice != null : !this$purchasePrice.equals(other$purchasePrice)) return false;
        final java.lang.Object this$purchaseDate = this.getPurchaseDate();
        final java.lang.Object other$purchaseDate = other.getPurchaseDate();
        if (this$purchaseDate == null ? other$purchaseDate != null : !this$purchaseDate.equals(other$purchaseDate)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$invoiceNumber = this.getInvoiceNumber();
        final java.lang.Object other$invoiceNumber = other.getInvoiceNumber();
        if (this$invoiceNumber == null ? other$invoiceNumber != null : !this$invoiceNumber.equals(other$invoiceNumber)) return false;
        final java.lang.Object this$originalValue = this.getOriginalValue();
        final java.lang.Object other$originalValue = other.getOriginalValue();
        if (this$originalValue == null ? other$originalValue != null : !this$originalValue.equals(other$originalValue)) return false;
        final java.lang.Object this$netValue = this.getNetValue();
        final java.lang.Object other$netValue = other.getNetValue();
        if (this$netValue == null ? other$netValue != null : !this$netValue.equals(other$netValue)) return false;
        final java.lang.Object this$accumulatedDepreciation = this.getAccumulatedDepreciation();
        final java.lang.Object other$accumulatedDepreciation = other.getAccumulatedDepreciation();
        if (this$accumulatedDepreciation == null ? other$accumulatedDepreciation != null : !this$accumulatedDepreciation.equals(other$accumulatedDepreciation)) return false;
        final java.lang.Object this$residualValue = this.getResidualValue();
        final java.lang.Object other$residualValue = other.getResidualValue();
        if (this$residualValue == null ? other$residualValue != null : !this$residualValue.equals(other$residualValue)) return false;
        final java.lang.Object this$depreciationMethod = this.getDepreciationMethod();
        final java.lang.Object other$depreciationMethod = other.getDepreciationMethod();
        if (this$depreciationMethod == null ? other$depreciationMethod != null : !this$depreciationMethod.equals(other$depreciationMethod)) return false;
        final java.lang.Object this$depreciationStartDate = this.getDepreciationStartDate();
        final java.lang.Object other$depreciationStartDate = other.getDepreciationStartDate();
        if (this$depreciationStartDate == null ? other$depreciationStartDate != null : !this$depreciationStartDate.equals(other$depreciationStartDate)) return false;
        final java.lang.Object this$lastDepreciationDate = this.getLastDepreciationDate();
        final java.lang.Object other$lastDepreciationDate = other.getLastDepreciationDate();
        if (this$lastDepreciationDate == null ? other$lastDepreciationDate != null : !this$lastDepreciationDate.equals(other$lastDepreciationDate)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$custodianName = this.getCustodianName();
        final java.lang.Object other$custodianName = other.getCustodianName();
        if (this$custodianName == null ? other$custodianName != null : !this$custodianName.equals(other$custodianName)) return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final java.lang.Object this$assetType = this.getAssetType();
        final java.lang.Object other$assetType = other.getAssetType();
        if (this$assetType == null ? other$assetType != null : !this$assetType.equals(other$assetType)) return false;
        final java.lang.Object this$qrCode = this.getQrCode();
        final java.lang.Object other$qrCode = other.getQrCode();
        if (this$qrCode == null ? other$qrCode != null : !this$qrCode.equals(other$qrCode)) return false;
        final java.lang.Object this$currentOrderId = this.getCurrentOrderId();
        final java.lang.Object other$currentOrderId = other.getCurrentOrderId();
        if (this$currentOrderId == null ? other$currentOrderId != null : !this$currentOrderId.equals(other$currentOrderId)) return false;
        final java.lang.Object this$bindTime = this.getBindTime();
        final java.lang.Object other$bindTime = other.getBindTime();
        if (this$bindTime == null ? other$bindTime != null : !this$bindTime.equals(other$bindTime)) return false;
        final java.lang.Object this$lastUseTime = this.getLastUseTime();
        final java.lang.Object other$lastUseTime = other.getLastUseTime();
        if (this$lastUseTime == null ? other$lastUseTime != null : !this$lastUseTime.equals(other$lastUseTime)) return false;
        final java.lang.Object this$imageUrl = this.getImageUrl();
        final java.lang.Object other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AssetMaster;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $categoryId = this.getCategoryId();
        result = result * PRIME + ($categoryId == null ? 43 : $categoryId.hashCode());
        final java.lang.Object $purchaseOrderId = this.getPurchaseOrderId();
        result = result * PRIME + ($purchaseOrderId == null ? 43 : $purchaseOrderId.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $usefulLifeMonths = this.getUsefulLifeMonths();
        result = result * PRIME + ($usefulLifeMonths == null ? 43 : $usefulLifeMonths.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $custodianId = this.getCustodianId();
        result = result * PRIME + ($custodianId == null ? 43 : $custodianId.hashCode());
        final java.lang.Object $useCount = this.getUseCount();
        result = result * PRIME + ($useCount == null ? 43 : $useCount.hashCode());
        final java.lang.Object $currentKitchenOrderId = this.getCurrentKitchenOrderId();
        result = result * PRIME + ($currentKitchenOrderId == null ? 43 : $currentKitchenOrderId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $assetCode = this.getAssetCode();
        result = result * PRIME + ($assetCode == null ? 43 : $assetCode.hashCode());
        final java.lang.Object $assetName = this.getAssetName();
        result = result * PRIME + ($assetName == null ? 43 : $assetName.hashCode());
        final java.lang.Object $categoryName = this.getCategoryName();
        result = result * PRIME + ($categoryName == null ? 43 : $categoryName.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $purchasePrice = this.getPurchasePrice();
        result = result * PRIME + ($purchasePrice == null ? 43 : $purchasePrice.hashCode());
        final java.lang.Object $purchaseDate = this.getPurchaseDate();
        result = result * PRIME + ($purchaseDate == null ? 43 : $purchaseDate.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $invoiceNumber = this.getInvoiceNumber();
        result = result * PRIME + ($invoiceNumber == null ? 43 : $invoiceNumber.hashCode());
        final java.lang.Object $originalValue = this.getOriginalValue();
        result = result * PRIME + ($originalValue == null ? 43 : $originalValue.hashCode());
        final java.lang.Object $netValue = this.getNetValue();
        result = result * PRIME + ($netValue == null ? 43 : $netValue.hashCode());
        final java.lang.Object $accumulatedDepreciation = this.getAccumulatedDepreciation();
        result = result * PRIME + ($accumulatedDepreciation == null ? 43 : $accumulatedDepreciation.hashCode());
        final java.lang.Object $residualValue = this.getResidualValue();
        result = result * PRIME + ($residualValue == null ? 43 : $residualValue.hashCode());
        final java.lang.Object $depreciationMethod = this.getDepreciationMethod();
        result = result * PRIME + ($depreciationMethod == null ? 43 : $depreciationMethod.hashCode());
        final java.lang.Object $depreciationStartDate = this.getDepreciationStartDate();
        result = result * PRIME + ($depreciationStartDate == null ? 43 : $depreciationStartDate.hashCode());
        final java.lang.Object $lastDepreciationDate = this.getLastDepreciationDate();
        result = result * PRIME + ($lastDepreciationDate == null ? 43 : $lastDepreciationDate.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $custodianName = this.getCustodianName();
        result = result * PRIME + ($custodianName == null ? 43 : $custodianName.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final java.lang.Object $assetType = this.getAssetType();
        result = result * PRIME + ($assetType == null ? 43 : $assetType.hashCode());
        final java.lang.Object $qrCode = this.getQrCode();
        result = result * PRIME + ($qrCode == null ? 43 : $qrCode.hashCode());
        final java.lang.Object $currentOrderId = this.getCurrentOrderId();
        result = result * PRIME + ($currentOrderId == null ? 43 : $currentOrderId.hashCode());
        final java.lang.Object $bindTime = this.getBindTime();
        result = result * PRIME + ($bindTime == null ? 43 : $bindTime.hashCode());
        final java.lang.Object $lastUseTime = this.getLastUseTime();
        result = result * PRIME + ($lastUseTime == null ? 43 : $lastUseTime.hashCode());
        final java.lang.Object $imageUrl = this.getImageUrl();
        result = result * PRIME + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "AssetMaster(id=" + this.getId() + ", assetCode=" + this.getAssetCode() + ", assetName=" + this.getAssetName() + ", categoryId=" + this.getCategoryId() + ", categoryName=" + this.getCategoryName() + ", specification=" + this.getSpecification() + ", unit=" + this.getUnit() + ", purchaseOrderId=" + this.getPurchaseOrderId() + ", purchasePrice=" + this.getPurchasePrice() + ", purchaseDate=" + this.getPurchaseDate() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", invoiceNumber=" + this.getInvoiceNumber() + ", originalValue=" + this.getOriginalValue() + ", netValue=" + this.getNetValue() + ", accumulatedDepreciation=" + this.getAccumulatedDepreciation() + ", residualValue=" + this.getResidualValue() + ", depreciationMethod=" + this.getDepreciationMethod() + ", usefulLifeMonths=" + this.getUsefulLifeMonths() + ", depreciationStartDate=" + this.getDepreciationStartDate() + ", lastDepreciationDate=" + this.getLastDepreciationDate() + ", status=" + this.getStatus() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", departmentId=" + this.getDepartmentId() + ", custodianId=" + this.getCustodianId() + ", custodianName=" + this.getCustodianName() + ", location=" + this.getLocation() + ", assetType=" + this.getAssetType() + ", qrCode=" + this.getQrCode() + ", useCount=" + this.getUseCount() + ", currentOrderId=" + this.getCurrentOrderId() + ", currentKitchenOrderId=" + this.getCurrentKitchenOrderId() + ", bindTime=" + this.getBindTime() + ", lastUseTime=" + this.getLastUseTime() + ", imageUrl=" + this.getImageUrl() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
