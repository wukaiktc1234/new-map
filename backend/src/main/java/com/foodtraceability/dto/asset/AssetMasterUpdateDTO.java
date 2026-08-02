package com.foodtraceability.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产主数据更新DTO
 * 用于接收更新资产的请求参数，所有字段可选
 */
@Schema(description = "资产主数据更新请求")
public class AssetMasterUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资产名称")
    @Size(min = 2, max = 100, message = "资产名称长度必须在2-100个字符之间")
    private String assetName;

    @Schema(description = "所属分类ID")
    private Long categoryId;

    @Schema(description = "规格型号")
    @Size(max = 200, message = "规格型号长度不能超过200个字符")
    private String specification;

    @Schema(description = "品牌")
    @Size(max = 100, message = "品牌长度不能超过100个字符")
    private String brand;

    @Schema(description = "购置日期")
    private LocalDate purchaseDate;

    @Schema(description = "原值（元）")
    private BigDecimal originalValue;

    @Schema(description = "使用月数")
    private Integer usefulLifeMonths;

    @Schema(description = "折旧方法：1-直线法 2-年数总和法 3-工作量法")
    private Integer depreciationMethod;

    @Schema(description = "存放位置")
    @Size(max = 200, message = "存放位置长度不能超过200个字符")
    private String location;

    @Schema(description = "责任人ID")
    private Long responsibleUserId;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "供应商信息（JSON格式）")
    @Size(max = 2000, message = "供应商信息长度不能超过2000个字符")
    private String supplierInfo;

    @Schema(description = "保修到期日")
    private LocalDate warrantyExpiry;

    @Schema(description = "下次保养日期")
    private LocalDate nextMaintenanceDate;

    @Schema(description = "资产照片URL")
    @Size(max = 500, message = "图片URL长度不能超过500个字符")
    private String imageUrl;

    @Schema(description = "状态：idle-闲置 in_use-使用中 repairing-维修中 damaged-已损毁")
    private String status;

    @Schema(description = "备注")
    @Size(max = 1000, message = "备注长度不能超过1000个字符")
    private String remark;

    // ==================== Getter & Setter ====================

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

    public BigDecimal getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(BigDecimal originalValue) {
        this.originalValue = originalValue;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
