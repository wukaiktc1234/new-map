package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 分类基本信息DTO（用于缓存和批量查询）
 */
@Schema(description = "分类基本信息")
public class CategoryBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "分类名称", example = "热菜")
    private String categoryName;

    @Schema(description = "父级分类ID", example = "0")
    private Long parentId;

    @Schema(description = "状态: 1启用 2停用", example = "1")
    private Integer status;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
