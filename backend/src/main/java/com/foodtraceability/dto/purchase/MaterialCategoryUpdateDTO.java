package com.foodtraceability.dto.purchase;

import jakarta.validation.constraints.Size;

/**
 * 商品分类更新 DTO（部分更新，所有字段可选）
 */
public class MaterialCategoryUpdateDTO {

    /** 分类名称 */
    @Size(max = 200, message = "分类名称长度不能超过200")
    private String categoryName;

    /** 父分类ID（可选） */
    private Long parentId;

    /** 分类描述 */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    /** 排序值 */
    private Integer sortOrder;

    /** 状态：1启用 0停用（仅状态切换端点使用，普通更新不传） */
    private Integer status;

    // ==================== Getter / Setter ====================

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
