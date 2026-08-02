package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 定价查询条件DTO
 */
@Schema(description = "定价查询条件")
public class PricingQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "产品类型: FOOD-菜品, COMBO-套餐, ALL-全部")
    private String productType = "ALL";

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "定价策略: MANUAL-手动调整, AUTO-自动计算, BATCH-批量调价")
    private String pricingStrategy;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "排序方向: asc/desc", example = "desc")
    private String sortOrder = "desc";

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getPricingStrategy() { return pricingStrategy; }
    public void setPricingStrategy(String pricingStrategy) { this.pricingStrategy = pricingStrategy; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}
