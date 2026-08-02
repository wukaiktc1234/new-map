package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐视图对象VO
 */
@Schema(description = "套餐视图对象")
public class ComboVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐ID", example = "1")
    private Long comboId;

    @Schema(description = "套餐编码", example = "CB001")
    private String comboCode;

    @Schema(description = "套餐名称", example = "超值双人餐")
    private String comboName;

    @Schema(description = "套餐价格（分）", example = "8800")
    private Long comboPrice;

    @Schema(description = "原价（分）", example = "12000")
    private Long originalPrice;

    @Schema(description = "优惠金额（分）", example = "3200")
    private Long discountAmount;

    @Schema(description = "优惠率(%)", example = "26.67")
    private Double discountRate;

    @Schema(description = "成本价（分）", example = "4500")
    private Long costPrice;

    @Schema(description = "套餐总成本（分，从 combo_ingredients 关联 foods.cost_price 计算）", example = "4500")
    private Long totalCost;

    @Schema(description = "毛利（分）", example = "4300")
    private Long profit;

    @Schema(description = "毛利率(%)", example = "48.86")
    private Double profitRate;

    @Schema(description = "套餐图片URL")
    private String imageUrl;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "有效期开始日期")
    private LocalDate validStartDate;

    @Schema(description = "有效期结束日期")
    private LocalDate validEndDate;

    @Schema(description = "每日限量", example = "50")
    private Integer dailyLimit;

    @Schema(description = "今日已售", example = "10")
    private Integer soldToday;

    @Schema(description = "状态: 1在售 2停售", example = "1")
    private Integer status;

    @Schema(description = "状态名称", example = "在售")
    private String statusName;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    @Schema(description = "套餐明细列表")
    private List<ComboIngredientVO> ingredients;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter和Setter方法
    public Long getComboId() { return comboId; }
    public void setComboId(Long comboId) { this.comboId = comboId; }

    public String getComboCode() { return comboCode; }
    public void setComboCode(String comboCode) { this.comboCode = comboCode; }

    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }

    public Long getComboPrice() { return comboPrice; }
    public void setComboPrice(Long comboPrice) { this.comboPrice = comboPrice; }

    public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Long originalPrice) { this.originalPrice = originalPrice; }

    public Long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }

    public Double getDiscountRate() { return discountRate; }
    public void setDiscountRate(Double discountRate) { this.discountRate = discountRate; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public Long getTotalCost() { return totalCost; }
    public void setTotalCost(Long totalCost) { this.totalCost = totalCost; }

    public Long getProfit() { return profit; }
    public void setProfit(Long profit) { this.profit = profit; }

    public Double getProfitRate() { return profitRate; }
    public void setProfitRate(Double profitRate) { this.profitRate = profitRate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getValidStartDate() { return validStartDate; }
    public void setValidStartDate(LocalDate validStartDate) { this.validStartDate = validStartDate; }

    public LocalDate getValidEndDate() { return validEndDate; }
    public void setValidEndDate(LocalDate validEndDate) { this.validEndDate = validEndDate; }

    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }

    public Integer getSoldToday() { return soldToday; }
    public void setSoldToday(Integer soldToday) { this.soldToday = soldToday; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<ComboIngredientVO> getIngredients() { return ingredients; }
    public void setIngredients(List<ComboIngredientVO> ingredients) { this.ingredients = ingredients; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
