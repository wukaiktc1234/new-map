package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 菜品查询条件DTO
 */
@Schema(description = "菜品查询条件")
public class FoodQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品名称（模糊搜索）")
    private String foodName;

    @Schema(description = "菜品编码")
    private String foodCode;

    @Schema(description = "关键字（按菜品名称或编码模糊搜索，二选一匹配）")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态: 1在售 2停售 3售罄")
    private Integer status;

    @Schema(description = "是否推荐")
    private Boolean isRecommend;

    @Schema(description = "是否辣")
    private Boolean isSpicy;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "排序字段", example = "sort_order")
    private String sortField;

    @Schema(description = "排序方向: asc/desc", example = "asc")
    private String sortOrder = "asc";

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodCode() {
        return foodCode;
    }

    public void setFoodCode(String foodCode) {
        this.foodCode = foodCode;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Boolean isRecommend) {
        this.isRecommend = isRecommend;
    }

    public Boolean getIsSpicy() {
        return isSpicy;
    }

    public void setIsSpicy(Boolean isSpicy) {
        this.isSpicy = isSpicy;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
