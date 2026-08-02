package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存出库单创建DTO
 */
@Schema(description = "库存出库单创建DTO")
public class InventoryOutboundCreateDTO {

    @NotBlank(message = "出库类型不能为空")
    @Schema(description = "出库类型（requisition/sale/return/other）", example = "requisition", required = true)
    private String outboundType;

    @NotBlank(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "WH001", required = true)
    private String warehouseId;

    @Schema(description = "目标门店/部门ID")
    private String targetId;

    @Schema(description = "目标门店/部门名称")
    private String targetName;

    @Schema(description = "关联单号")
    private String referenceNo;

    @NotBlank(message = "出库日期不能为空")
    @Schema(description = "出库日期（YYYY-MM-DD）", example = "2026-06-27", required = true)
    private String outboundDate;

    @NotEmpty(message = "出库明细不能为空")
    @Valid
    @Schema(description = "出库明细列表", required = true)
    private List<OutboundItemDTO> items;

    @Schema(description = "备注")
    private String remark;

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getOutboundDate() {
        return outboundDate;
    }

    public void setOutboundDate(String outboundDate) {
        this.outboundDate = outboundDate;
    }

    public List<OutboundItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OutboundItemDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 出库明细DTO
     */
    @Schema(description = "出库明细DTO")
    public static class OutboundItemDTO {
        @NotBlank(message = "物料ID不能为空")
        @Schema(description = "物料ID", required = true)
        private String materialId;

        @NotNull(message = "申请出库数量不能为空")
        @Schema(description = "申请出库数量", required = true)
        private BigDecimal requestQuantity;

        @Schema(description = "备注")
        private String remark;

        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public BigDecimal getRequestQuantity() { return requestQuantity; }
        public void setRequestQuantity(BigDecimal requestQuantity) { this.requestQuantity = requestQuantity; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
