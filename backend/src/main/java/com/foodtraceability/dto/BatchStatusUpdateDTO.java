package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量状态更新请求DTO
 */
@Schema(description = "批量状态更新请求DTO")
public class BatchStatusUpdateDTO {
    @Schema(description = "后厨订单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> orderIds;

    @Schema(description = "目标状态: pending/received/making/completed/served", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetStatus;

    public BatchStatusUpdateDTO() {
    }

    public List<String> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }
}
