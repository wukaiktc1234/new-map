package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 库存日志导出DTO
 */
@Schema(description = "库存日志导出DTO")
public class InventoryLogExportDTO {

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "操作类型（in/out/check/transfer/loss）")
    private String operationType;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "开始时间（yyyy-MM-dd HH:mm:ss）")
    private String startTime;

    @Schema(description = "结束时间（yyyy-MM-dd HH:mm:ss）")
    private String endTime;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
