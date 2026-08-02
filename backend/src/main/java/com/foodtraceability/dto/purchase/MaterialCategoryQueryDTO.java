package com.foodtraceability.dto.purchase;

/**
 * 商品分类分页查询 DTO
 */
public class MaterialCategoryQueryDTO {

    /** 当前页码（默认 1） */
    private Integer current = 1;

    /** 每页大小（默认 10） */
    private Integer size = 10;

    /** 关键字（按 category_name 或 category_code 模糊匹配） */
    private String keyword;

    /** 状态：1启用 0停用（null 表示不过滤） */
    private Integer status;

    /** 父分类ID（null 表示不过滤） */
    private Long parentId;

    /** 是否包含子分类（true: 递归查询; false: 仅查当前层级） */
    private Boolean includeChildren = false;

    // ==================== Getter / Setter ====================

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getIncludeChildren() {
        return includeChildren;
    }

    public void setIncludeChildren(Boolean includeChildren) {
        this.includeChildren = includeChildren;
    }
}
