package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 菜品分类实体类
 * 支持二级分类结构，用于组织菜品
 */
@TableName("food_categories")
@Schema(description = "菜品分类实体")
public class FoodCategoryNew {

    /** 分类ID，主键自增 */
    @TableId(value = "category_id", type = IdType.AUTO)
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /** 分类名称 */
    @TableField("category_name")
    @Schema(description = "分类名称", example = "热菜")
    private String categoryName;

    /** 父级分类ID，0表示顶级分类 */
    @TableField("parent_id")
    @Schema(description = "父级分类ID", example = "0")
    private Long parentId;

    /** 分类图标URL */
    @TableField("icon_url")
    @Schema(description = "分类图标URL")
    private String iconUrl;

    /** 排序权重 */
    @TableField("sort_order")
    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    /** 状态：1启用 0停用（遵循项目统一状态标准） */
    @TableField("status")
    @Schema(description = "状态: 1启用 0停用", example = "1")
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;

    // Getter方法
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Long getParentId() { return parentId; }
    public String getIconUrl() { return iconUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getStatus() { return status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }
    public Integer getVersion() { return version; }

    // Setter方法
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setStatus(Integer status) { this.status = status; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setVersion(Integer version) { this.version = version; }
}
