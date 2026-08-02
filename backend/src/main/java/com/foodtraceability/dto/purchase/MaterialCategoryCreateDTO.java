package com.foodtraceability.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 商品分类创建 DTO
 */
public class MaterialCategoryCreateDTO {

    /** 分类编码（业务唯一） */
    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过50")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "分类编码仅支持字母、数字、下划线、短横线")
    private String categoryCode;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 200, message = "分类名称长度不能超过200")
    private String categoryName;

    /** 父分类ID（可选，null 表示顶级分类） */
    private Long parentId;

    /** 分类描述 */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    /** 排序值（默认 0） */
    private Integer sortOrder;

    // ==================== Getter / Setter ====================

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

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
}
