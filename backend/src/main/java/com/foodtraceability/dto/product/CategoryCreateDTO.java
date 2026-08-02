package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 分类创建请求DTO
 */
@Schema(description = "分类创建请求")
public class CategoryCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", example = "热菜", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;

    @Schema(description = "父级分类ID，0表示顶级分类", example = "0")
    private Long parentId = 0L;

    @Schema(description = "分类图标URL")
    private String iconUrl;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder = 0;

    @Schema(description = "状态: 1启用 2停用", example = "1")
    private Integer status = 1;

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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
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
