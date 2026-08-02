package com.foodtraceability.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 资产主数据创建DTO
 * 用于接收新增资产的请求参数
 */
@Schema(description = "资产主数据创建请求")
public class AssetMasterCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产编号（可选，系统可自动生成） */
    @Size(min = 2, max = 32, message = "资产编号长度必须在2-32个字符之间")
    private String assetCode;

    /** 资产名称（必填） */
    @NotBlank(message = "资产名称不能为空")
    @Size(min = 2, max = 100, message = "资产名称长度必须在2-100个字符之间")
    private String assetName;

    /** 所属分类ID（必填） */
    @NotNull(message = "所属分类不能为空")
    private Long categoryId;

    /** 规格型号 */
    @Size(max = 200, message = "规格型号长度不能超过200个字符")
    private String specification;

    /** 品牌 */
    @Size(max = 100, message = "品牌长度不能超过100个字符")
    private String brand;

    /** 购置日期 */
    private java.time.LocalDate purchaseDate;

    /**
     * 原值（元，前端传入）
     * 后端会转换为分存储
     */
    @NotNull(message = "原值不能为空")
    @DecimalMin(value = "0.01", message = "原值必须大于0")
    private java.math.BigDecimal originalCostYuan;

    /** 使用月数（根据分类自动计算或手动填写） */
    @Min(value = 1, message = "使用月数至少1个月")
    private Integer usefulLifeMonths;

    /**
     * 折旧方法（可选，默认使用分类设置）
     * 1-直线法 2-年数总和法 3-工作量法
     */
    @Min(value = 1, message = "折旧方法值无效")
    @Max(value = 3, message = "折旧方法值无效")
    private Integer depreciationMethod;

    /** 存放位置 */
    @Size(max = 200, message = "存放位置长度不能超过200个字符")
    private String location;

    /** 责任人ID */
    private Long responsibleUserId;

    /** 所属门店ID */
    private Long storeId;

    /** 供应商信息（JSON格式） */
    @Size(max = 2000, message = "供应商信息长度不能超过2000个字符")
    private String supplierInfo;

    /** 保修到期日 */
    private java.time.LocalDate warrantyExpiry;

    /** 下次保养日期 */
    private java.time.LocalDate nextMaintenanceDate;

    /** 二维码（可选，系统可根据编号自动生成） */
    @Size(max = 100, message = "二维码长度不能超过100个字符")
    private String qrCode;

    /** 资产照片URL */
    @Size(max = 500, message = "图片URL长度不能超过500个字符")
    private String imageUrl;

    /** 备注 */
    @Size(max = 1000, message = "备注长度不能超过1000个字符")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

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

    public java.time.LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(java.time.LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public java.math.BigDecimal getOriginalCostYuan() {
        return originalCostYuan;
    }

    public void setOriginalCostYuan(java.math.BigDecimal originalCostYuan) {
        this.originalCostYuan = originalCostYuan;
    }

    public Integer getUsefulLifeMonths() {
        return usefulLifeMonths;
    }

    public void setUsefulLifeMonths(Integer usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }

    public Integer getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(Integer depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
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

    public java.time.LocalDate getWarrantyExpiry() {
        return warrantyExpiry;
    }

    public void setWarrantyExpiry(java.time.LocalDate warrantyExpiry) {
        this.warrantyExpiry = warrantyExpiry;
    }

    public java.time.LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(java.time.LocalDate nextMaintenanceDate) {
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
}
