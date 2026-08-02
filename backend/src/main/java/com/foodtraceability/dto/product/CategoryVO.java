package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类视图对象VO（支持树形结构）
 */
@Schema(description = "分类视图对象")
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "分类名称", example = "热菜")
    private String categoryName;

    @Schema(description = "父级分类ID", example = "0")
    private Long parentId;

    @Schema(description = "父级分类名称")
    private String parentName;

    @Schema(description = "分类图标URL")
    private String iconUrl;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    @Schema(description = "状态: 1启用 2停用", example = "1")
    private Integer status;

    @Schema(description = "状态名称", example = "启用")
    private String statusName;

    @Schema(description = "该分类下的菜品数量")
    private Integer foodCount;

    @Schema(description = "子分类列表")
    private List<CategoryVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter和Setter方法
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Integer getFoodCount() { return foodCount; }
    public void setFoodCount(Integer foodCount) { this.foodCount = foodCount; }

    public List<CategoryVO> getChildren() { return children; }
    public void setChildren(List<CategoryVO> children) { this.children = children; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
