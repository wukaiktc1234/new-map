package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POS菜品分类DTO
 * 用于收银终端菜品分类数据传输
 */
@Schema(description = "POS菜品分类DTO")
public class PosCategoryDTO {

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    public PosCategoryDTO() {
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
