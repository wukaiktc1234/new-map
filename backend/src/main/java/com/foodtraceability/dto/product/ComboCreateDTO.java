package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 套餐创建请求DTO
 */
@Schema(description = "套餐创建请求")
public class ComboCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "套餐名称不能为空")
    @Schema(description = "套餐名称", example = "超值双人餐", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comboName;

    @Schema(description = "套餐编码（可选，系统自动生成）", example = "CB001")
    private String comboCode;

    @Positive(message = "套餐价格必须大于0")
    @Schema(description = "套餐价格（分）", example = "8800")
    private Long comboPrice;

    @Schema(description = "原价（分，各单品之和）", example = "12000")
    private Long originalPrice;

    @Schema(description = "优惠金额（分）", example = "3200")
    private Long discountAmount;

    @Schema(description = "套餐图片URL")
    private String imageUrl;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "有效期开始日期")
    private LocalDate validStartDate;

    @Schema(description = "有效期结束日期")
    private LocalDate validEndDate;

    @Schema(description = "每日限量（NULL表示不限量）", example = "50")
    private Integer dailyLimit;

    @Schema(description = "状态: 1在售 2停售", example = "1")
    private Integer status;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder = 0;

    @Schema(description = "套餐明细列表")
    private List<ComboIngredientDTO> ingredients;

    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public String getComboCode() {
        return comboCode;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public Long getComboPrice() {
        return comboPrice;
    }

    public void setComboPrice(Long comboPrice) {
        this.comboPrice = comboPrice;
    }

    public Long getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Long originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getValidStartDate() {
        return validStartDate;
    }

    public void setValidStartDate(LocalDate validStartDate) {
        this.validStartDate = validStartDate;
    }

    public LocalDate getValidEndDate() {
        return validEndDate;
    }

    public void setValidEndDate(LocalDate validEndDate) {
        this.validEndDate = validEndDate;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ComboIngredientDTO> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<ComboIngredientDTO> ingredients) {
        this.ingredients = ingredients;
    }
}
