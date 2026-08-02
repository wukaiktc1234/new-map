package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品响应DTO
 * 用于返回产品信息
 */
@Schema(description = "产品响应")
public class ProductResponse {
    @Schema(description = "产品ID", example = "F001")
    private String foodId;
    @Schema(description = "食品编码", example = "D00001")
    private String foodCode;
    @Schema(description = "产品名称", example = "有机苹果")
    private String foodName;
    @Schema(description = "产品类别", example = "水果")
    private String foodCategory;
    @Schema(description = "产品描述", example = "来自山东烟台的优质有机苹果")
    private String foodDescription;
    @Schema(description = "批次号", example = "B20241201001")
    private String batchNumber;
    @Schema(description = "生产日期", example = "2024-12-01 08:00:00")
    private LocalDateTime productionDate;
    @Schema(description = "保质期（天）", example = "30")
    private Integer shelfLifeDays;
    @Schema(description = "过期日期", example = "2024-12-31 23:59:59")
    private LocalDateTime expirationDate;
    @Schema(description = "生产厂家", example = "绿色农场有限公司")
    private String manufacturer;
    @Schema(description = "生产地址", example = "山东省烟台市栖霞市桃村镇")
    private String productionAddress;
    @Schema(description = "营养成分信息", example = "{\"protein\":\"0.3g\",\"fat\":\"0.2g\",\"carbs\":\"14g\"}")
    private String nutritionInfo;
    @Schema(description = "存储条件", example = "常温避光保存")
    private String storageConditions;
    @Schema(description = "价格（元）", example = "12.50")
    private BigDecimal price;
    @Schema(description = "成本价（元）", example = "8.00")
    private BigDecimal costPrice;
    @Schema(description = "重量（克）", example = "200")
    private BigDecimal weight;
    @Schema(description = "产品状态（0-下架，1-上架）", example = "1")
    private Integer foodStatus;
    @Schema(description = "质量等级（A-优，B-良，C-合格）", example = "A")
    private String qualityGrade;
    @Schema(description = "质检报告编号", example = "Q20241201001")
    private String qualityReportNo;
    @Schema(description = "质检日期", example = "2024-12-01 10:00:00")
    private LocalDateTime qualityCheckDate;
    @Schema(description = "认证信息", example = "有机认证，认证编号：ORG2024001")
    private String certificationInfo;
    @Schema(description = "产品图片URL", example = "/uploads/food/apple001.jpg")
    private String foodImageUrl;
    @Schema(description = "溯源码", example = "TRACE20241201001")
    private String traceCode;
    @Schema(description = "溯源码图片URL", example = "/uploads/qrcode/trace001.png")
    private String traceCodeImageUrl;
    @Schema(description = "溯源状态（0-未溯源，1-已溯源，2-溯源中）", example = "1")
    private Integer traceStatus;
    @Schema(description = "创建时间", example = "2024-12-01 09:00:00")
    private LocalDateTime createTime;
    @Schema(description = "更新时间", example = "2024-12-01 10:00:00")
    private LocalDateTime updateTime;
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    @Schema(description = "备注信息", example = "特殊注意事项")
    private String remarks;

    public ProductResponse() {
    }

    public String getFoodId() {
        return this.foodId;
    }

    public String getFoodCode() {
        return this.foodCode;
    }

    public String getFoodName() {
        return this.foodName;
    }

    public String getFoodCategory() {
        return this.foodCategory;
    }

    public String getFoodDescription() {
        return this.foodDescription;
    }

    public String getBatchNumber() {
        return this.batchNumber;
    }

    public LocalDateTime getProductionDate() {
        return this.productionDate;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
    }

    public LocalDateTime getExpirationDate() {
        return this.expirationDate;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public String getProductionAddress() {
        return this.productionAddress;
    }

    public String getNutritionInfo() {
        return this.nutritionInfo;
    }

    public String getStorageConditions() {
        return this.storageConditions;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public BigDecimal getWeight() {
        return this.weight;
    }

    public Integer getFoodStatus() {
        return this.foodStatus;
    }

    public String getQualityGrade() {
        return this.qualityGrade;
    }

    public String getQualityReportNo() {
        return this.qualityReportNo;
    }

    public LocalDateTime getQualityCheckDate() {
        return this.qualityCheckDate;
    }

    public String getCertificationInfo() {
        return this.certificationInfo;
    }

    public String getFoodImageUrl() {
        return this.foodImageUrl;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getTraceCodeImageUrl() {
        return this.traceCodeImageUrl;
    }

    public Integer getTraceStatus() {
        return this.traceStatus;
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

    public String getRemarks() {
        return this.remarks;
    }

    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
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

    public void setExpirationDate(final LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
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

    public void setTraceCodeImageUrl(final String traceCodeImageUrl) {
        this.traceCodeImageUrl = traceCodeImageUrl;
    }

    public void setTraceStatus(final Integer traceStatus) {
        this.traceStatus = traceStatus;
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

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ProductResponse)) return false;
        final ProductResponse other = (ProductResponse) o;
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
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodCode = this.getFoodCode();
        final java.lang.Object other$foodCode = other.getFoodCode();
        if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
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
        final java.lang.Object this$expirationDate = this.getExpirationDate();
        final java.lang.Object other$expirationDate = other.getExpirationDate();
        if (this$expirationDate == null ? other$expirationDate != null : !this$expirationDate.equals(other$expirationDate)) return false;
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
        final java.lang.Object this$traceCodeImageUrl = this.getTraceCodeImageUrl();
        final java.lang.Object other$traceCodeImageUrl = other.getTraceCodeImageUrl();
        if (this$traceCodeImageUrl == null ? other$traceCodeImageUrl != null : !this$traceCodeImageUrl.equals(other$traceCodeImageUrl)) return false;
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
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ProductResponse;
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
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodCode = this.getFoodCode();
        result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
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
        final java.lang.Object $expirationDate = this.getExpirationDate();
        result = result * PRIME + ($expirationDate == null ? 43 : $expirationDate.hashCode());
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
        final java.lang.Object $traceCodeImageUrl = this.getTraceCodeImageUrl();
        result = result * PRIME + ($traceCodeImageUrl == null ? 43 : $traceCodeImageUrl.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ProductResponse(foodId=" + this.getFoodId() + ", foodCode=" + this.getFoodCode() + ", foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodDescription=" + this.getFoodDescription() + ", batchNumber=" + this.getBatchNumber() + ", productionDate=" + this.getProductionDate() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", expirationDate=" + this.getExpirationDate() + ", manufacturer=" + this.getManufacturer() + ", productionAddress=" + this.getProductionAddress() + ", nutritionInfo=" + this.getNutritionInfo() + ", storageConditions=" + this.getStorageConditions() + ", price=" + this.getPrice() + ", costPrice=" + this.getCostPrice() + ", weight=" + this.getWeight() + ", foodStatus=" + this.getFoodStatus() + ", qualityGrade=" + this.getQualityGrade() + ", qualityReportNo=" + this.getQualityReportNo() + ", qualityCheckDate=" + this.getQualityCheckDate() + ", certificationInfo=" + this.getCertificationInfo() + ", foodImageUrl=" + this.getFoodImageUrl() + ", traceCode=" + this.getTraceCode() + ", traceCodeImageUrl=" + this.getTraceCodeImageUrl() + ", traceStatus=" + this.getTraceStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", remarks=" + this.getRemarks() + ")";
    }
}
