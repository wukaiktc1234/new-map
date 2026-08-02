package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品请求DTO
 * 用于创建和更新产品信息
 */
@Schema(description = "产品请求")
public class ProductRequest {
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Schema(description = "产品名称", example = "有机苹果")
    private String foodName;
    @NotBlank(message = "产品类别不能为空")
    @Size(max = 50, message = "产品类别长度不能超过50个字符")
    @Schema(description = "产品类别", example = "水果")
    private String foodCategory;
    @Size(max = 500, message = "产品描述长度不能超过500个字符")
    @Schema(description = "产品描述", example = "来自山东烟台的优质有机苹果")
    private String foodDescription;
    @NotBlank(message = "批次号不能为空")
    @Size(max = 50, message = "批次号长度不能超过50个字符")
    @Schema(description = "批次号", example = "B20241201001")
    private String batchNumber;
    @NotNull(message = "生产日期不能为空")
    @Schema(description = "生产日期", example = "2024-12-01 08:00:00")
    private LocalDateTime productionDate;
    @NotNull(message = "保质期不能为空")
    @Min(value = 1, message = "保质期不能小于1天")
    @Max(value = 3650, message = "保质期不能超过3650天")
    @Schema(description = "保质期（天）", example = "30")
    private Integer shelfLifeDays;
    @NotBlank(message = "生产厂家不能为空")
    @Size(max = 100, message = "生产厂家长度不能超过100个字符")
    @Schema(description = "生产厂家", example = "绿色农场有限公司")
    private String manufacturer;
    @Size(max = 200, message = "生产地址长度不能超过200个字符")
    @Schema(description = "生产地址", example = "山东省烟台市栖霞市桃村镇")
    private String productionAddress;
    @Size(max = 1000, message = "营养成分信息长度不能超过1000个字符")
    @Schema(description = "营养成分信息", example = "{\"protein\":\"0.3g\",\"fat\":\"0.2g\",\"carbs\":\"14g\"}")
    private String nutritionInfo;
    @Size(max = 200, message = "存储条件长度不能超过200个字符")
    @Schema(description = "存储条件", example = "常温避光保存")
    private String storageConditions;
    @Schema(description = "价格（元）", example = "12.50")
    private BigDecimal price;
    @Schema(description = "成本价（元）", example = "8.00")
    private BigDecimal costPrice;
    @Schema(description = "重量（克）", example = "200")
    private BigDecimal weight;
    @NotNull(message = "产品状态不能为空")
    @Min(value = 0, message = "产品状态不能小于0")
    @Max(value = 1, message = "产品状态不能超过1")
    @Schema(description = "产品状态（0-下架，1-上架）", example = "1")
    private Integer foodStatus;
    @Pattern(regexp = "^[A-C]$", message = "质量等级只能是A、B或C")
    @Schema(description = "质量等级（A-优，B-良，C-合格）", example = "A")
    private String qualityGrade;
    @Size(max = 50, message = "质检报告编号长度不能超过50个字符")
    @Schema(description = "质检报告编号", example = "Q20241201001")
    private String qualityReportNo;
    @Schema(description = "质检日期", example = "2024-12-01 10:00:00")
    private LocalDateTime qualityCheckDate;
    @Size(max = 200, message = "认证信息长度不能超过200个字符")
    @Schema(description = "认证信息", example = "有机认证，认证编号：ORG2024001")
    private String certificationInfo;
    @Size(max = 200, message = "产品图片URL长度不能超过200个字符")
    @Schema(description = "产品图片URL", example = "/uploads/food/apple001.jpg")
    private String foodImageUrl;
    @Size(max = 50, message = "溯源码长度不能超过50个字符")
    @Schema(description = "溯源码", example = "TRACE20241201001")
    private String traceCode;
    @Min(value = 0, message = "溯源状态不能小于0")
    @Max(value = 2, message = "溯源状态不能超过2")
    @Schema(description = "溯源状态（0-未溯源，1-已溯源，2-溯源中）", example = "1")
    private Integer traceStatus;
    @Size(max = 500, message = "备注信息长度不能超过500个字符")
    @Schema(description = "备注信息", example = "特殊注意事项")
    private String remarks;

    public String getFoodName() {
        return foodName;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public LocalDateTime getProductionDate() {
        return productionDate;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getProductionAddress() {
        return productionAddress;
    }

    public String getNutritionInfo() {
        return nutritionInfo;
    }

    public String getStorageConditions() {
        return storageConditions;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public Integer getFoodStatus() {
        return foodStatus;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public String getQualityReportNo() {
        return qualityReportNo;
    }

    public LocalDateTime getQualityCheckDate() {
        return qualityCheckDate;
    }

    public String getCertificationInfo() {
        return certificationInfo;
    }

    public String getFoodImageUrl() {
        return foodImageUrl;
    }

    public String getTraceCode() {
        return traceCode;
    }

    public Integer getTraceStatus() {
        return traceStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public ProductRequest() {
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setFoodCategory(final String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public void setFoodDescription(final String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public void setBatchNumber(final String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setProductionDate(final LocalDateTime productionDate) {
        this.productionDate = productionDate;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setProductionAddress(final String productionAddress) {
        this.productionAddress = productionAddress;
    }

    public void setNutritionInfo(final String nutritionInfo) {
        this.nutritionInfo = nutritionInfo;
    }

    public void setStorageConditions(final String storageConditions) {
        this.storageConditions = storageConditions;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setWeight(final BigDecimal weight) {
        this.weight = weight;
    }

    public void setFoodStatus(final Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    public void setQualityGrade(final String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public void setQualityReportNo(final String qualityReportNo) {
        this.qualityReportNo = qualityReportNo;
    }

    public void setQualityCheckDate(final LocalDateTime qualityCheckDate) {
        this.qualityCheckDate = qualityCheckDate;
    }

    public void setCertificationInfo(final String certificationInfo) {
        this.certificationInfo = certificationInfo;
    }

    public void setFoodImageUrl(final String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setTraceStatus(final Integer traceStatus) {
        this.traceStatus = traceStatus;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ProductRequest)) return false;
        final ProductRequest other = (ProductRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$foodStatus = this.getFoodStatus();
        final java.lang.Object other$foodStatus = other.getFoodStatus();
        if (this$foodStatus == null ? other$foodStatus != null : !this$foodStatus.equals(other$foodStatus)) return false;
        final java.lang.Object this$traceStatus = this.getTraceStatus();
        final java.lang.Object other$traceStatus = other.getTraceStatus();
        if (this$traceStatus == null ? other$traceStatus != null : !this$traceStatus.equals(other$traceStatus)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$foodCategory = this.getFoodCategory();
        final java.lang.Object other$foodCategory = other.getFoodCategory();
        if (this$foodCategory == null ? other$foodCategory != null : !this$foodCategory.equals(other$foodCategory)) return false;
        final java.lang.Object this$foodDescription = this.getFoodDescription();
        final java.lang.Object other$foodDescription = other.getFoodDescription();
        if (this$foodDescription == null ? other$foodDescription != null : !this$foodDescription.equals(other$foodDescription)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$manufacturer = this.getManufacturer();
        final java.lang.Object other$manufacturer = other.getManufacturer();
        if (this$manufacturer == null ? other$manufacturer != null : !this$manufacturer.equals(other$manufacturer)) return false;
        final java.lang.Object this$productionAddress = this.getProductionAddress();
        final java.lang.Object other$productionAddress = other.getProductionAddress();
        if (this$productionAddress == null ? other$productionAddress != null : !this$productionAddress.equals(other$productionAddress)) return false;
        final java.lang.Object this$nutritionInfo = this.getNutritionInfo();
        final java.lang.Object other$nutritionInfo = other.getNutritionInfo();
        if (this$nutritionInfo == null ? other$nutritionInfo != null : !this$nutritionInfo.equals(other$nutritionInfo)) return false;
        final java.lang.Object this$storageConditions = this.getStorageConditions();
        final java.lang.Object other$storageConditions = other.getStorageConditions();
        if (this$storageConditions == null ? other$storageConditions != null : !this$storageConditions.equals(other$storageConditions)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$weight = this.getWeight();
        final java.lang.Object other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !this$weight.equals(other$weight)) return false;
        final java.lang.Object this$qualityGrade = this.getQualityGrade();
        final java.lang.Object other$qualityGrade = other.getQualityGrade();
        if (this$qualityGrade == null ? other$qualityGrade != null : !this$qualityGrade.equals(other$qualityGrade)) return false;
        final java.lang.Object this$qualityReportNo = this.getQualityReportNo();
        final java.lang.Object other$qualityReportNo = other.getQualityReportNo();
        if (this$qualityReportNo == null ? other$qualityReportNo != null : !this$qualityReportNo.equals(other$qualityReportNo)) return false;
        final java.lang.Object this$qualityCheckDate = this.getQualityCheckDate();
        final java.lang.Object other$qualityCheckDate = other.getQualityCheckDate();
        if (this$qualityCheckDate == null ? other$qualityCheckDate != null : !this$qualityCheckDate.equals(other$qualityCheckDate)) return false;
        final java.lang.Object this$certificationInfo = this.getCertificationInfo();
        final java.lang.Object other$certificationInfo = other.getCertificationInfo();
        if (this$certificationInfo == null ? other$certificationInfo != null : !this$certificationInfo.equals(other$certificationInfo)) return false;
        final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
        final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
        if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ProductRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $foodStatus = this.getFoodStatus();
        result = result * PRIME + ($foodStatus == null ? 43 : $foodStatus.hashCode());
        final java.lang.Object $traceStatus = this.getTraceStatus();
        result = result * PRIME + ($traceStatus == null ? 43 : $traceStatus.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $foodCategory = this.getFoodCategory();
        result = result * PRIME + ($foodCategory == null ? 43 : $foodCategory.hashCode());
        final java.lang.Object $foodDescription = this.getFoodDescription();
        result = result * PRIME + ($foodDescription == null ? 43 : $foodDescription.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $manufacturer = this.getManufacturer();
        result = result * PRIME + ($manufacturer == null ? 43 : $manufacturer.hashCode());
        final java.lang.Object $productionAddress = this.getProductionAddress();
        result = result * PRIME + ($productionAddress == null ? 43 : $productionAddress.hashCode());
        final java.lang.Object $nutritionInfo = this.getNutritionInfo();
        result = result * PRIME + ($nutritionInfo == null ? 43 : $nutritionInfo.hashCode());
        final java.lang.Object $storageConditions = this.getStorageConditions();
        result = result * PRIME + ($storageConditions == null ? 43 : $storageConditions.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $weight = this.getWeight();
        result = result * PRIME + ($weight == null ? 43 : $weight.hashCode());
        final java.lang.Object $qualityGrade = this.getQualityGrade();
        result = result * PRIME + ($qualityGrade == null ? 43 : $qualityGrade.hashCode());
        final java.lang.Object $qualityReportNo = this.getQualityReportNo();
        result = result * PRIME + ($qualityReportNo == null ? 43 : $qualityReportNo.hashCode());
        final java.lang.Object $qualityCheckDate = this.getQualityCheckDate();
        result = result * PRIME + ($qualityCheckDate == null ? 43 : $qualityCheckDate.hashCode());
        final java.lang.Object $certificationInfo = this.getCertificationInfo();
        result = result * PRIME + ($certificationInfo == null ? 43 : $certificationInfo.hashCode());
        final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
        result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ProductRequest(foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodDescription=" + this.getFoodDescription() + ", batchNumber=" + this.getBatchNumber() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", manufacturer=" + this.getManufacturer() + ", productionAddress=" + this.getProductionAddress() + ", nutritionInfo=" + this.getNutritionInfo() + ", storageConditions=" + this.getStorageConditions() + ", price=" + this.getPrice() + ", costPrice=" + this.getCostPrice() + ", weight=" + this.getWeight() + ", foodStatus=" + this.getFoodStatus() + ", qualityGrade=" + this.getQualityGrade() + ", qualityReportNo=" + this.getQualityReportNo() + ", qualityCheckDate=" + this.getQualityCheckDate() + ", certificationInfo=" + this.getCertificationInfo() + ", foodImageUrl=" + this.getFoodImageUrl() + ", traceCode=" + this.getTraceCode() + ", traceStatus=" + this.getTraceStatus() + ", remarks=" + this.getRemarks() + ")";
    }
}
