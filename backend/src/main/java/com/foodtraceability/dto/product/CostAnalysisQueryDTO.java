package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 成本分析查询条件DTO
 */
@Schema(description = "成本分析查询条件")
public class CostAnalysisQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "产品类型: FOOD-菜品, COMBO-套餐, ALL-全部")
    private String productType = "ALL";

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态: 1在售 2停售")
    private Integer status;

    @Schema(description = "关键字: 模糊搜索菜品/套餐名称或编码")
    private String keyword;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "最低毛利率(%)")
    private Double minProfitRate;

    @Schema(description = "最高毛利率(%)")
    private Double maxProfitRate;

    @Schema(description = "排序字段: profitRate/salePrice/costPrice/foodName")
    private String sortField = "profitRate";

    @Schema(description = "排序方向: asc/desc")
    private String sortOrder = "asc";

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getMinProfitRate() { return minProfitRate; }
    public void setMinProfitRate(Double minProfitRate) { this.minProfitRate = minProfitRate; }

    public Double getMaxProfitRate() { return maxProfitRate; }
    public void setMaxProfitRate(Double maxProfitRate) { this.maxProfitRate = maxProfitRate; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
