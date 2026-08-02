package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 配方/BOM查询条件DTO
 */
@Schema(description = "配方查询条件")
public class RecipeQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID")
    private Long foodId;

    @Schema(description = "原料名称（模糊搜索）")
    private String materialName;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "recipe_id")
    private String sortField = "recipe_id";

    @Schema(description = "排序方向: asc/desc", example = "asc")
    private String sortOrder = "asc";

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}
