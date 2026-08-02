package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品档案查询 DTO
 */
@Schema(description = "商品档案查询请求")
public class MaterialArchiveQueryDTO {

    /** 当前页码（从1开始） */
    @Schema(description = "当前页码", example = "1", defaultValue = "1")
    private Integer current = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer size = 10;

    /** 搜索关键词（名称或编码模糊匹配） */
    @Schema(description = "搜索关键词（名称或编码）")
    private String keyword;

    /** 分类ID（精确匹配） */
    @Schema(description = "分类ID")
    private Long categoryId;

    /** 使用部门ID（匹配指定部门或通用物料） */
    @Schema(description = "使用部门ID（NULL=通用物料）")
    private Long departmentId;

    /** 分类名称（模糊匹配，需 JOIN material_categories） */
    @Schema(description = "分类名称")
    private String categoryName;

    /** 状态：1启用 0停用 null全部 */
    @Schema(description = "状态: 1启用 0停用")
    private Integer status;

    // ===== Getter / Setter =====

    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
