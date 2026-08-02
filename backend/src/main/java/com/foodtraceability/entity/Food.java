package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("food")
public class Food {
    @TableId(value = "food_code", type = IdType.INPUT)
    @JsonProperty("foodCode")
    private String foodCode;
    @TableField("food_name")
    private String foodName;
    @TableField("food_category")
    private String foodCategory;
    @TableField("food_price")
    private BigDecimal foodPrice;
    @TableField("cost_price")
    private BigDecimal costPrice;
    @TableField("food_desc")
    private String foodDesc;
    @TableField("food_image")
    private String foodImage;
    @TableField("food_status")
    private String foodStatus;
    @TableField("batch_number")
    private String batchNumber;
    @TableField("trace_code")
    private String traceCode;
    @TableField("manufacturer")
    private String manufacturer;
    @TableField("production_date")
    private LocalDateTime productionDate;
    @TableField("expiration_date")
    private LocalDateTime expirationDate;
    @TableField("stock")
    private Integer stock;
    @TableField("food_description")
    private String foodDescription;
    @TableField("shelf_life_days")
    private Integer shelfLifeDays;
    @TableField("production_address")
    private String productionAddress;
    @TableField("nutrition_info")
    private String nutritionInfo;
    @TableField("storage_conditions")
    private String storageConditions;
    @TableField("price")
    private BigDecimal price;
    @TableField("weight")
    private BigDecimal weight;
    @TableField("quality_grade")
    private String qualityGrade;
    @TableField("quality_report_no")
    private String qualityReportNo;
    @TableField("quality_check_date")
    private LocalDateTime qualityCheckDate;
    @TableField("certification_info")
    private String certificationInfo;
    @TableField("food_image_url")
    private String foodImageUrl;
    @TableField("trace_code_image_url")
    private String traceCodeImageUrl;
    @TableField("trace_status")
    private Integer traceStatus;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("create_by")
    private String createBy;
    @TableField("update_by")
    private String updateBy;
    @TableField("remarks")
    private String remarks;
    @TableField("device_id")
    private String deviceId;
    @TableField("sensor_data")
    private String sensorData;
    @TableField("rfid_tag")
    private String rfidTag;
    @TableField("gps_location")
    private String gpsLocation;
    @TableField("collect_time")
    private LocalDateTime collectTime;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    public Food() {
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

    public BigDecimal getFoodPrice() {
        return this.foodPrice;
    }

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public String getFoodDesc() {
        return this.foodDesc;
    }

    public String getFoodImage() {
        return this.foodImage;
    }

    public String getFoodStatus() {
        return this.foodStatus;
    }

    public String getBatchNumber() {
        return this.batchNumber;
    }

    public String getTraceCode() {
        return this.traceCode;
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

    public Integer getStock() {
        return this.stock;
    }

    public String getFoodDescription() {
        return this.foodDescription;
    }

    public Integer getShelfLifeDays() {
        return this.shelfLifeDays;
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

    public BigDecimal getWeight() {
        return this.weight;
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

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getSensorData() {
        return this.sensorData;
    }

    public String getRfidTag() {
        return this.rfidTag;
    }

    public String getGpsLocation() {
        return this.gpsLocation;
    }

    public LocalDateTime getCollectTime() {
        return this.collectTime;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    @JsonProperty("foodCode")
    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setFoodCategory(final String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public void setFoodPrice(final BigDecimal foodPrice) {
        this.foodPrice = foodPrice;
    }

    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setFoodDesc(final String foodDesc) {
        this.foodDesc = foodDesc;
    }

    public void setFoodImage(final String foodImage) {
        this.foodImage = foodImage;
    }

    public void setFoodStatus(final String foodStatus) {
        this.foodStatus = foodStatus;
    }

    public void setBatchNumber(final String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setProductionDate(final LocalDateTime productionDate) {
        this.productionDate = productionDate;
    }

    public void setExpirationDate(final LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setStock(final Integer stock) {
        this.stock = stock;
    }

    public void setFoodDescription(final String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public void setShelfLifeDays(final Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
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

    public void setWeight(final BigDecimal weight) {
        this.weight = weight;
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

    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    public void setSensorData(final String sensorData) {
        this.sensorData = sensorData;
    }

    public void setRfidTag(final String rfidTag) {
        this.rfidTag = rfidTag;
    }

    public void setGpsLocation(final String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public void setCollectTime(final LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Food)) return false;
        final Food other = (Food) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stock = this.getStock();
        final java.lang.Object other$stock = other.getStock();
        if (this$stock == null ? other$stock != null : !this$stock.equals(other$stock)) return false;
        final java.lang.Object this$shelfLifeDays = this.getShelfLifeDays();
        final java.lang.Object other$shelfLifeDays = other.getShelfLifeDays();
        if (this$shelfLifeDays == null ? other$shelfLifeDays != null : !this$shelfLifeDays.equals(other$shelfLifeDays)) return false;
        final java.lang.Object this$traceStatus = this.getTraceStatus();
        final java.lang.Object other$traceStatus = other.getTraceStatus();
        if (this$traceStatus == null ? other$traceStatus != null : !this$traceStatus.equals(other$traceStatus)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$foodCode = this.getFoodCode();
        final java.lang.Object other$foodCode = other.getFoodCode();
        if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$foodCategory = this.getFoodCategory();
        final java.lang.Object other$foodCategory = other.getFoodCategory();
        if (this$foodCategory == null ? other$foodCategory != null : !this$foodCategory.equals(other$foodCategory)) return false;
        final java.lang.Object this$foodPrice = this.getFoodPrice();
        final java.lang.Object other$foodPrice = other.getFoodPrice();
        if (this$foodPrice == null ? other$foodPrice != null : !this$foodPrice.equals(other$foodPrice)) return false;
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$foodDesc = this.getFoodDesc();
        final java.lang.Object other$foodDesc = other.getFoodDesc();
        if (this$foodDesc == null ? other$foodDesc != null : !this$foodDesc.equals(other$foodDesc)) return false;
        final java.lang.Object this$foodImage = this.getFoodImage();
        final java.lang.Object other$foodImage = other.getFoodImage();
        if (this$foodImage == null ? other$foodImage != null : !this$foodImage.equals(other$foodImage)) return false;
        final java.lang.Object this$foodStatus = this.getFoodStatus();
        final java.lang.Object other$foodStatus = other.getFoodStatus();
        if (this$foodStatus == null ? other$foodStatus != null : !this$foodStatus.equals(other$foodStatus)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$manufacturer = this.getManufacturer();
        final java.lang.Object other$manufacturer = other.getManufacturer();
        if (this$manufacturer == null ? other$manufacturer != null : !this$manufacturer.equals(other$manufacturer)) return false;
        final java.lang.Object this$productionDate = this.getProductionDate();
        final java.lang.Object other$productionDate = other.getProductionDate();
        if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
        final java.lang.Object this$expirationDate = this.getExpirationDate();
        final java.lang.Object other$expirationDate = other.getExpirationDate();
        if (this$expirationDate == null ? other$expirationDate != null : !this$expirationDate.equals(other$expirationDate)) return false;
        final java.lang.Object this$foodDescription = this.getFoodDescription();
        final java.lang.Object other$foodDescription = other.getFoodDescription();
        if (this$foodDescription == null ? other$foodDescription != null : !this$foodDescription.equals(other$foodDescription)) return false;
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
        final java.lang.Object this$deviceId = this.getDeviceId();
        final java.lang.Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) return false;
        final java.lang.Object this$sensorData = this.getSensorData();
        final java.lang.Object other$sensorData = other.getSensorData();
        if (this$sensorData == null ? other$sensorData != null : !this$sensorData.equals(other$sensorData)) return false;
        final java.lang.Object this$rfidTag = this.getRfidTag();
        final java.lang.Object other$rfidTag = other.getRfidTag();
        if (this$rfidTag == null ? other$rfidTag != null : !this$rfidTag.equals(other$rfidTag)) return false;
        final java.lang.Object this$gpsLocation = this.getGpsLocation();
        final java.lang.Object other$gpsLocation = other.getGpsLocation();
        if (this$gpsLocation == null ? other$gpsLocation != null : !this$gpsLocation.equals(other$gpsLocation)) return false;
        final java.lang.Object this$collectTime = this.getCollectTime();
        final java.lang.Object other$collectTime = other.getCollectTime();
        if (this$collectTime == null ? other$collectTime != null : !this$collectTime.equals(other$collectTime)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Food;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stock = this.getStock();
        result = result * PRIME + ($stock == null ? 43 : $stock.hashCode());
        final java.lang.Object $shelfLifeDays = this.getShelfLifeDays();
        result = result * PRIME + ($shelfLifeDays == null ? 43 : $shelfLifeDays.hashCode());
        final java.lang.Object $traceStatus = this.getTraceStatus();
        result = result * PRIME + ($traceStatus == null ? 43 : $traceStatus.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $foodCode = this.getFoodCode();
        result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $foodCategory = this.getFoodCategory();
        result = result * PRIME + ($foodCategory == null ? 43 : $foodCategory.hashCode());
        final java.lang.Object $foodPrice = this.getFoodPrice();
        result = result * PRIME + ($foodPrice == null ? 43 : $foodPrice.hashCode());
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $foodDesc = this.getFoodDesc();
        result = result * PRIME + ($foodDesc == null ? 43 : $foodDesc.hashCode());
        final java.lang.Object $foodImage = this.getFoodImage();
        result = result * PRIME + ($foodImage == null ? 43 : $foodImage.hashCode());
        final java.lang.Object $foodStatus = this.getFoodStatus();
        result = result * PRIME + ($foodStatus == null ? 43 : $foodStatus.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $manufacturer = this.getManufacturer();
        result = result * PRIME + ($manufacturer == null ? 43 : $manufacturer.hashCode());
        final java.lang.Object $productionDate = this.getProductionDate();
        result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
        final java.lang.Object $expirationDate = this.getExpirationDate();
        result = result * PRIME + ($expirationDate == null ? 43 : $expirationDate.hashCode());
        final java.lang.Object $foodDescription = this.getFoodDescription();
        result = result * PRIME + ($foodDescription == null ? 43 : $foodDescription.hashCode());
        final java.lang.Object $productionAddress = this.getProductionAddress();
        result = result * PRIME + ($productionAddress == null ? 43 : $productionAddress.hashCode());
        final java.lang.Object $nutritionInfo = this.getNutritionInfo();
        result = result * PRIME + ($nutritionInfo == null ? 43 : $nutritionInfo.hashCode());
        final java.lang.Object $storageConditions = this.getStorageConditions();
        result = result * PRIME + ($storageConditions == null ? 43 : $storageConditions.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
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
        final java.lang.Object $deviceId = this.getDeviceId();
        result = result * PRIME + ($deviceId == null ? 43 : $deviceId.hashCode());
        final java.lang.Object $sensorData = this.getSensorData();
        result = result * PRIME + ($sensorData == null ? 43 : $sensorData.hashCode());
        final java.lang.Object $rfidTag = this.getRfidTag();
        result = result * PRIME + ($rfidTag == null ? 43 : $rfidTag.hashCode());
        final java.lang.Object $gpsLocation = this.getGpsLocation();
        result = result * PRIME + ($gpsLocation == null ? 43 : $gpsLocation.hashCode());
        final java.lang.Object $collectTime = this.getCollectTime();
        result = result * PRIME + ($collectTime == null ? 43 : $collectTime.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Food(foodCode=" + this.getFoodCode() + ", foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodPrice=" + this.getFoodPrice() + ", costPrice=" + this.getCostPrice() + ", foodDesc=" + this.getFoodDesc() + ", foodImage=" + this.getFoodImage() + ", foodStatus=" + this.getFoodStatus() + ", batchNumber=" + this.getBatchNumber() + ", traceCode=" + this.getTraceCode() + ", manufacturer=" + this.getManufacturer() + ", productionDate=" + this.getProductionDate() + ", expirationDate=" + this.getExpirationDate() + ", stock=" + this.getStock() + ", foodDescription=" + this.getFoodDescription() + ", shelfLifeDays=" + this.getShelfLifeDays() + ", productionAddress=" + this.getProductionAddress() + ", nutritionInfo=" + this.getNutritionInfo() + ", storageConditions=" + this.getStorageConditions() + ", price=" + this.getPrice() + ", weight=" + this.getWeight() + ", qualityGrade=" + this.getQualityGrade() + ", qualityReportNo=" + this.getQualityReportNo() + ", qualityCheckDate=" + this.getQualityCheckDate() + ", certificationInfo=" + this.getCertificationInfo() + ", foodImageUrl=" + this.getFoodImageUrl() + ", traceCodeImageUrl=" + this.getTraceCodeImageUrl() + ", traceStatus=" + this.getTraceStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", remarks=" + this.getRemarks() + ", deviceId=" + this.getDeviceId() + ", sensorData=" + this.getSensorData() + ", rfidTag=" + this.getRfidTag() + ", gpsLocation=" + this.getGpsLocation() + ", collectTime=" + this.getCollectTime() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
