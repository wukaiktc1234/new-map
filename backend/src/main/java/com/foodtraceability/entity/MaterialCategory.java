package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 商品分类实体类
 * 对应表: material_categories（支持树形结构，自关联 parent_id）
 */
@TableName("material_categories")
public class MaterialCategory {

    /** 分类ID（主键，自增） */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /** 分类编码（业务唯一，如 VGE/MEAT） */
    @TableField("category_code")
    private String categoryCode;

    /** 分类名称 */
    @TableField("category_name")
    private String categoryName;

    /** 父分类ID（NULL 表示顶级分类） */
    @TableField("parent_id")
    private Long parentId;

    /** 分类描述 */
    @TableField("description")
    private String description;

    /** 排序值（升序，越小越靠前） */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态：1启用 0停用 */
    @TableField("status")
    private Integer status;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== Getter / Setter ====================

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
