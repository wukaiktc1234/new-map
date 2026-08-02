package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 批量状态更新请求DTO
 */
@Schema(description = "批量状态更新请求")
public class BatchStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> foodIds;

    @Schema(description = "目标状态: 1在售 2停售 3售罄", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    public List<Long> getFoodIds() { return foodIds; }
    public void setFoodIds(List<Long> foodIds) { this.foodIds = foodIds; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
