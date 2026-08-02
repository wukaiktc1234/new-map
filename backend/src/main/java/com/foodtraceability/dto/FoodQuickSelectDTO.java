package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品快速选择响应DTO
 * 用于采购订单商品快速选择
 */
@Schema(description = "商品快速选择响应DTO")
public class FoodQuickSelectDTO {
    @Schema(description = "商品编码", example = "FOOD001")
    private String foodCode;
    @Schema(description = "商品名称", example = "优质大米")
    private String foodName;
    @Schema(description = "商品分类ID", example = "CAT001")
    private String categoryId;
    @Schema(description = "商品分类名称", example = "粮油类")
    private String categoryName;
    @Schema(description = "销售价格", example = "5.50")
    private BigDecimal foodPrice;
    @Schema(description = "成本价格", example = "4.00")
    private BigDecimal costPrice;
    @Schema(description = "规格", example = "25kg/袋")
    private String specification;
    @Schema(description = "库存数量", example = "1000")
    private Integer stock;
    @Schema(description = "商品状态（1-上架，0-下架）", example = "1")
    private Integer status;
    @Schema(description = "商品图片URL", example = "http://example.com/image.jpg")
    private String foodImageUrl;
    @Schema(description = "生产厂家", example = "XX粮油有限公司")
    private String manufacturer;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期", example = "2024-01-01")
    private LocalDateTime productionDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期", example = "2024-12-31")
    private LocalDateTime expirationDate;
    @Schema(description = "保质期天数", example = "365")
    private Integer shelfLifeDays;
    @Schema(description = "存储条件", example = "阴凉干燥处保存")
    private String storageConditions;
    @Schema(description = "质量等级", example = "一级")
    private String qualityGrade;
    @Schema(description = "备注", example = "优质东北大米")
    private String remarks;

    public FoodQuickSelectDTO() {
    }

    public String getFoodCode() {
        return this.foodCode;
    }

    public String getFoodName() {
        return this.foodName;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public BigDecimal getFoodPrice() {
        return this.foodPrice;
    }

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public String getSpecification() {
        return this.specification;
    }

    public Integer getStock() {
        return this.stock;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getFoodImageUrl() {
        return this.foodImageUrl;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public LocalDateTime getProductionDate() {
        return this.productionDate;
    }

    public LocalDateTime getExpirationDate() {
        return this.expirationDate;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
    }

    public String getStorageConditions() {
        return this.storageConditions;
    }

    public String getQualityGrade() {
        return this.qualityGrade;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public void setFoodPrice(final BigDecimal foodPrice) {
        this.foodPrice = foodPrice;
    }

    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    public void setStock(final Integer stock) {
        this.stock = stock;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setFoodImageUrl(final String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public void setProductionDate(final LocalDateTime productionDate) {
        this.productionDate = productionDate;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public void setExpirationDate(final LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setStorageConditions(final String storageConditions) {
        this.storageConditions = storageConditions;
    }

    public void setQualityGrade(final String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FoodQuickSelectDTO)) return false;
        final FoodQuickSelectDTO other = (FoodQuickSelectDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stock = this.getStock();
        final java.lang.Object other$stock = other.getStock();
        if (this$stock == null ? other$stock != null : !this$stock.equals(other$stock)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$foodCode = this.getFoodCode();
        final java.lang.Object other$foodCode = other.getFoodCode();
        if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$categoryId = this.getCategoryId();
        final java.lang.Object other$categoryId = other.getCategoryId();
        if (this$categoryId == null ? other$categoryId != null : !this$categoryId.equals(other$categoryId)) return false;
        final java.lang.Object this$categoryName = this.getCategoryName();
        final java.lang.Object other$categoryName = other.getCategoryName();
        if (this$categoryName == null ? other$categoryName != null : !this$categoryName.equals(other$categoryName)) return false;
        final java.lang.Object this$foodPrice = this.getFoodPrice();
        final java.lang.Object other$foodPrice = other.getFoodPrice();
        if (this$foodPrice == null ? other$foodPrice != null : !this$foodPrice.equals(other$foodPrice)) return false;
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
        final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
        if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
        final java.lang.Object this$manufacturer = this.getManufacturer();
        final java.lang.Object other$manufacturer = other.getManufacturer();
        if (this$manufacturer == null ? other$manufacturer != null : !this$manufacturer.equals(other$manufacturer)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expirationDate = this.getExpirationDate();
        final java.lang.Object other$expirationDate = other.getExpirationDate();
        if (this$expirationDate == null ? other$expirationDate != null : !this$expirationDate.equals(other$expirationDate)) return false;
        final java.lang.Object this$storageConditions = this.getStorageConditions();
        final java.lang.Object other$storageConditions = other.getStorageConditions();
        if (this$storageConditions == null ? other$storageConditions != null : !this$storageConditions.equals(other$storageConditions)) return false;
        final java.lang.Object this$qualityGrade = this.getQualityGrade();
        final java.lang.Object other$qualityGrade = other.getQualityGrade();
        if (this$qualityGrade == null ? other$qualityGrade != null : !this$qualityGrade.equals(other$qualityGrade)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoodQuickSelectDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stock = this.getStock();
        result = result * PRIME + ($stock == null ? 43 : $stock.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $foodCode = this.getFoodCode();
        result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $categoryId = this.getCategoryId();
        result = result * PRIME + ($categoryId == null ? 43 : $categoryId.hashCode());
        final java.lang.Object $categoryName = this.getCategoryName();
        result = result * PRIME + ($categoryName == null ? 43 : $categoryName.hashCode());
        final java.lang.Object $foodPrice = this.getFoodPrice();
        result = result * PRIME + ($foodPrice == null ? 43 : $foodPrice.hashCode());
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
        result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
        final java.lang.Object $manufacturer = this.getManufacturer();
        result = result * PRIME + ($manufacturer == null ? 43 : $manufacturer.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $expirationDate = this.getExpirationDate();
        result = result * PRIME + ($expirationDate == null ? 43 : $expirationDate.hashCode());
        final java.lang.Object $storageConditions = this.getStorageConditions();
        result = result * PRIME + ($storageConditions == null ? 43 : $storageConditions.hashCode());
        final java.lang.Object $qualityGrade = this.getQualityGrade();
        result = result * PRIME + ($qualityGrade == null ? 43 : $qualityGrade.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FoodQuickSelectDTO(foodCode=" + this.getFoodCode() + ", foodName=" + this.getFoodName() + ", categoryId=" + this.getCategoryId() + ", categoryName=" + this.getCategoryName() + ", foodPrice=" + this.getFoodPrice() + ", costPrice=" + this.getCostPrice() + ", specification=" + this.getSpecification() + ", stock=" + this.getStock() + ", status=" + this.getStatus() + ", foodImageUrl=" + this.getFoodImageUrl() + ", manufacturer=" + this.getManufacturer() + ", productionDate=" + this.getProductionDate() + ", expirationDate=" + this.getExpirationDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", storageConditions=" + this.getStorageConditions() + ", qualityGrade=" + this.getQualityGrade() + ", remarks=" + this.getRemarks() + ")";
    }
}
