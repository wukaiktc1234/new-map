package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产主数据实体类（增强版）
 * 用于管理固定资产的全生命周期信息
 */
@TableName("asset_masters_enhanced")
public class AssetMasterNew {

    /** 资产ID */
    @TableId(type = IdType.AUTO)
    private Long assetId;

    /** 资产编号（唯一） */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 所属分类ID */
    private Long categoryId;

    /** 规格型号 */
    private String specification;

    /** 品牌 */
    private String brand;

    /** 购置日期 */
    private LocalDate purchaseDate;

    /**
     * 原值（分）
     * 存储单位为分，避免浮点精度问题
     */
    private Long originalCost;

    /**
     * 累计折旧（分）
     */
    private Long accumulatedDepreciation;

    /**
     * 净值（分）
     * 计算公式：原值 - 累计折旧
     */
    private Long netBookValue;

    /** 使用月数 */
    private Integer usefulLifeMonths;

    /** 剩余使用月数 */
    private Integer remainingMonths;

    /**
     * 折旧方法
     * 1-直线法 2-年数总和法 3-工作量法
     */
    private Integer depreciationMethod;

    /**
     * 资产状态
     * 1-在用 2-闲置 3-维修中 4-已报废 5-已处置
     */
    private Integer status;

    /** 存放位置 */
    private String location;

    /** 责任人ID */
    private Long responsibleUserId;

    /** 所属门店ID */
    private Long storeId;

    /** 供应商信息（JSON格式） */
    private String supplierInfo;

    /** 保修到期日 */
    private LocalDate warrantyExpiry;

    /** 下次保养日期 */
    private LocalDate nextMaintenanceDate;

    /** 二维码（用于盘点扫码） */
    private String qrCode;

    /** 资产照片URL */
    private String imageUrl;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Long getOriginalCost() {
        return originalCost;
    }

    public void setOriginalCost(Long originalCost) {
        this.originalCost = originalCost;
    }

    public Long getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(Long accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public Long getNetBookValue() {
        return netBookValue;
    }

    public void setNetBookValue(Long netBookValue) {
        this.netBookValue = netBookValue;
    }

    public Integer getUsefulLifeMonths() {
        return usefulLifeMonths;
    }

    public void setUsefulLifeMonths(Integer usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }

    public Integer getRemainingMonths() {
        return remainingMonths;
    }

    public void setRemainingMonths(Integer remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    public Integer getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(Integer depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getSupplierInfo() {
        return supplierInfo;
    }

    public void setSupplierInfo(String supplierInfo) {
        this.supplierInfo = supplierInfo;
    }

    public LocalDate getWarrantyExpiry() {
        return warrantyExpiry;
    }

    public void setWarrantyExpiry(LocalDate warrantyExpiry) {
        this.warrantyExpiry = warrantyExpiry;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
