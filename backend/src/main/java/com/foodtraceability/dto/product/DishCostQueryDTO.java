package com.foodtraceability.dto.product;

import java.io.Serializable;

/**
 * 菜品成本查询 DTO
 */
public class DishCostQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页码（从1开始） */
    private Integer page = 1;
    /** 每页条数 */
    private Integer size = 20;
    /** 分类ID */
    private String categoryId;
    /** 状态：normal/warning/danger */
    private String status;
    /** 关键字（菜品名称） */
    private String keyword;
    /** 排序字段：cost_change/margin_rate/name */
    private String sortBy;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
