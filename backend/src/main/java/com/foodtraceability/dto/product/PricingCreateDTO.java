package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产品定价创建请求DTO
 */
@Schema(description = "产品定价请求")
public class PricingCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "产品类型不能为空")
    @Schema(description = "产品类型: FOOD-菜品, COMBO-套餐", example = "FOOD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productType;

    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID（菜品ID或套餐ID）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "产品名称（仅用于批量调价的审计/日志展示，不参与业务逻辑）", example = "宫保鸡丁")
    private String productName;

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    @Schema(description = "售价（分）", example = "3800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long salePrice;

    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    @Schema(description = "定价原因/备注")
    private String remark;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "定价策略: MANUAL-手动调整, AUTO-自动计算, BATCH-批量调价")
    private String pricingStrategy = "MANUAL";

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Long salePrice) {
        this.salePrice = salePrice;
    }

    public Long getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Long costPrice) {
        this.costPrice = costPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getPricingStrategy() {
        return pricingStrategy;
    }

    public void setPricingStrategy(String pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }
}
