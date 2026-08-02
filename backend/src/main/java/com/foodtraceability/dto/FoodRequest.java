package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "菜品请求DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodRequest {

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100个字符")
    @Schema(description = "菜品名称", example = "宫保鸡丁")
    private String foodName;

    @NotBlank(message = "菜品分类不能为空")
    @Size(max = 50, message = "菜品分类长度不能超过50个字符")
    @Schema(description = "菜品分类", example = "热菜")
    private String foodCategory;

    @Size(max = 500, message = "菜品描述长度不能超过500个字符")
    @Schema(description = "菜品描述", example = "经典川菜，麻辣鲜香")
    private String foodDescription;

    @NotNull(message = "菜品状态不能为空")
    @Min(value = 0, message = "菜品状态不能小于0")
    @Max(value = 1, message = "菜品状态不能超过1")
    @Schema(description = "菜品状态（0-下架，1-上架）", example = "1")
    private Integer foodStatus;

    @Schema(description = "菜品价格（可选，在产品定价中设置）", example = "38.00")
    private BigDecimal price;

    @Schema(description = "成本价（可选，在产品定价中设置）", example = "20.00")
    private BigDecimal costPrice;

    @Size(max = 255, message = "图片URL长度不能超过255个字符")
    @Schema(description = "菜品图片URL", example = "/uploads/food/gongbao.jpg")
    private String foodImageUrl;

    @Size(max = 50, message = "批次号长度不能超过50个字符")
    @Schema(description = "批次号", example = "B20240101")
    private String batchNumber;

    @Schema(description = "生产日期", example = "2024-01-01T00:00:00")
    private LocalDateTime productionDate;

    @Min(value = 1, message = "保质期必须大于0")
    @Max(value = 3650, message = "保质期不能超过3650天")
    @Schema(description = "保质期（天）", example = "30")
    private Integer shelfLifeDays;

    @Size(max = 100, message = "生产厂家长度不能超过100个字符")
    @Schema(description = "生产厂家", example = "XX食品厂")
    private String manufacturer;

    @DecimalMin(value = "0.01", message = "重量必须大于0")
    @Schema(description = "重量（克）", example = "500.00")
    private BigDecimal weight;

    @Size(max = 200, message = "生产地址长度不能超过200个字符")
    @Schema(description = "生产地址", example = "XX省XX市XX区")
    private String productionAddress;

    @Schema(description = "营养成分", example = "蛋白质:20g,脂肪:10g")
    private String nutritionInfo;

    @Size(max = 200, message = "存储条件长度不能超过200个字符")
    @Schema(description = "存储条件", example = "冷藏保存")
    private String storageConditions;

    @Schema(description = "追溯码", example = "TC202401010001")
    private String traceCode;

    @Schema(description = "过期日期", example = "2024-02-01T00:00:00")
    private LocalDateTime expirationDate;

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public void setFoodDescription(String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public Integer getFoodStatus() {
        return foodStatus;
    }

    public void setFoodStatus(Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getFoodImageUrl() {
        return foodImageUrl;
    }

    public void setFoodImageUrl(String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDateTime getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDateTime productionDate) {
        this.productionDate = productionDate;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getProductionAddress() {
        return productionAddress;
    }

    public void setProductionAddress(String productionAddress) {
        this.productionAddress = productionAddress;
    }

    public String getNutritionInfo() {
        return nutritionInfo;
    }

    public void setNutritionInfo(String nutritionInfo) {
        this.nutritionInfo = nutritionInfo;
    }

    public String getStorageConditions() {
        return storageConditions;
    }

    public void setStorageConditions(String storageConditions) {
        this.storageConditions = storageConditions;
    }

    public String getTraceCode() {
        return traceCode;
    }

    public void setTraceCode(String traceCode) {
        this.traceCode = traceCode;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
}