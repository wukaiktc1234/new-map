package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 套餐查询条件DTO
 */
@Schema(description = "套餐查询条件")
public class ComboQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐名称（模糊搜索）")
    private String comboName;

    @Schema(description = "套餐编码")
    private String comboCode;

    @Schema(description = "状态: 1在售 2停售")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "排序字段", example = "sort_order")
    private String sortField;

    @Schema(description = "排序方向: asc/desc", example = "asc")
    private String sortOrder = "asc";

    public String getComboName() { return comboName; }
    public void setComboName(String comboName) { this.comboName = comboName; }

    public String getComboCode() { return comboCode; }
    public void setComboCode(String comboCode) { this.comboCode = comboCode; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}
