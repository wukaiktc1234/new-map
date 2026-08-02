package com.foodtraceability.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产主数据VO（视图对象）
 * 用于返回给前端的资产详细信息
 */
@Schema(description = "资产主数据视图对象")
public class AssetMasterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资产ID")
    private Long id;

    @Schema(description = "资产编号")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "所属分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "购置日期")
    private LocalDate purchaseDate;

    @Schema(description = "原值")
    private BigDecimal originalValue;

    @Schema(description = "净值")
    private BigDecimal netValue;

    @Schema(description = "累计折旧")
    private BigDecimal accumulatedDepreciation;

    @Schema(description = "残值")
    private BigDecimal residualValue;

    @Schema(description = "折旧方法")
    private String depreciationMethod;

    @Schema(description = "使用月数")
    private Integer usefulLifeMonths;

    @Schema(description = "状态：idle-闲置 in_use-使用中 repairing-维修中 damaged-已损毁")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "二维码")
    private String qrCode;

    @Schema(description = "使用次数")
    private Integer useCount;

    @Schema(description = "当前关联订单ID")
    private String currentOrderId;

    @Schema(description = "当前关联厨房订单ID")
    private Long currentKitchenOrderId;

    @Schema(description = "责任人姓名")
    private String custodianName;

    @Schema(description = "资产照片URL")
    private String imageUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public BigDecimal getNetValue() {
        return netValue;
    }

    public void setNetValue(BigDecimal netValue) {
        this.netValue = netValue;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public BigDecimal getResidualValue() {
        return residualValue;
    }

    public void setResidualValue(BigDecimal residualValue) {
        this.residualValue = residualValue;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public Integer getUsefulLifeMonths() {
        return usefulLifeMonths;
    }

    public void setUsefulLifeMonths(Integer usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public String getCurrentOrderId() {
        return currentOrderId;
    }

    public void setCurrentOrderId(String currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public Long getCurrentKitchenOrderId() {
        return currentKitchenOrderId;
    }

    public void setCurrentKitchenOrderId(Long currentKitchenOrderId) {
        this.currentKitchenOrderId = currentKitchenOrderId;
    }

    public String getCustodianName() {
        return custodianName;
    }

    public void setCustodianName(String custodianName) {
        this.custodianName = custodianName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
}
