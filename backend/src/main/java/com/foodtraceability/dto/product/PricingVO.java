package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产品定价视图对象VO
 */
@Schema(description = "产品定价视图对象")
public class PricingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "定价记录ID", example = "1")
    private Long pricingId;

    @Schema(description = "产品类型: FOOD-菜品, COMBO-套餐", example = "FOOD")
    private String productType;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品ID", example = "1")
    private Long productId;

    @Schema(description = "原售价（分）", example = "3500")
    private Long oldSalePrice;

    @Schema(description = "新售价（分）", example = "3800")
    private Long newSalePrice;

    @Schema(description = "价格变动（分）", example = "+300")
    private String priceChange;

    @Schema(description = "价格变动百分比", example = "8.57")
    private Double changePercent;

    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    @Schema(description = "毛利（分）", example = "2300")
    private Long profit;

    @Schema(description = "毛利率(%)", example = "60.53")
    private Double profitRate;

    @Schema(description = "定价策略: MANUAL-手动调整, AUTO-自动计算, BATCH-批量调价")
    private String pricingStrategy;

    @Schema(description = "定价策略名称")
    private String pricingStrategyName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getter和Setter方法
    public Long getPricingId() { return pricingId; }
    public void setPricingId(Long pricingId) { this.pricingId = pricingId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getOldSalePrice() { return oldSalePrice; }
    public void setOldSalePrice(Long oldSalePrice) { this.oldSalePrice = oldSalePrice; }

    public Long getNewSalePrice() { return newSalePrice; }
    public void setNewSalePrice(Long newSalePrice) { this.newSalePrice = newSalePrice; }

    public String getPriceChange() { return priceChange; }
    public void setPriceChange(String priceChange) { this.priceChange = priceChange; }

    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double changePercent) { this.changePercent = changePercent; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public Long getProfit() { return profit; }
    public void setProfit(Long profit) { this.profit = profit; }

    public Double getProfitRate() { return profitRate; }
    public void setProfitRate(Double profitRate) { this.profitRate = profitRate; }

    public String getPricingStrategy() { return pricingStrategy; }
    public void setPricingStrategy(String pricingStrategy) { this.pricingStrategy = pricingStrategy; }

    public String getPricingStrategyName() { return pricingStrategyName; }
    public void setPricingStrategyName(String pricingStrategyName) { this.pricingStrategyName = pricingStrategyName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
