package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存调整单创建DTO
 */
@Schema(description = "库存调整单创建DTO")
public class InventoryAdjustCreateDTO {

    @NotBlank(message = "调整类型不能为空")
    @Schema(description = "调整类型（gain/loss/temp_loss/weight_diff/other）", example = "loss", required = true)
    private String adjustType;

    @NotBlank(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "WH001", required = true)
    private String warehouseId;

    @Schema(description = "关联盘点单号")
    private String referenceCheckCode;

    @Schema(description = "调整原因分类")
    private String adjustReason;

    @Schema(description = "关联单号")
    private String referenceNo;

    @Schema(description = "参与部门列表")
    private List<String> participatingDepts;

    @NotEmpty(message = "调整明细不能为空")
    @Valid
    @Schema(description = "调整明细列表", required = true)
    private List<AdjustItemDTO> items;

    @Schema(description = "备注")
    private String remark;

    public String getAdjustType() {
        return adjustType;
    }

    public void setAdjustType(String adjustType) {
        this.adjustType = adjustType;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getReferenceCheckCode() {
        return referenceCheckCode;
    }

    public void setReferenceCheckCode(String referenceCheckCode) {
        this.referenceCheckCode = referenceCheckCode;
    }

    public String getAdjustReason() {
        return adjustReason;
    }

    public void setAdjustReason(String adjustReason) {
        this.adjustReason = adjustReason;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public List<String> getParticipatingDepts() {
        return participatingDepts;
    }

    public void setParticipatingDepts(List<String> participatingDepts) {
        this.participatingDepts = participatingDepts;
    }

    public List<AdjustItemDTO> getItems() {
        return items;
    }

    public void setItems(List<AdjustItemDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 调整明细DTO
     */
    @Schema(description = "调整明细DTO")
    public static class AdjustItemDTO {
        @NotBlank(message = "物料ID不能为空")
        @Schema(description = "物料ID", required = true)
        private String materialId;

        @Schema(description = "物料名称")
        private String materialName;

        @Schema(description = "规格型号")
        private String specification;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "调整前数量")
        private BigDecimal beforeQuantity;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "单价（元）")
        private String unitCost;

        @jakarta.validation.constraints.NotNull(message = "调整数量不能为空")
        @Schema(description = "调整数量（正数为增加，负数为减少）", required = true)
        private BigDecimal adjustQuantity;

        @NotBlank(message = "调整原因不能为空")
        @Schema(description = "调整原因", required = true)
        private String reason;

        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public BigDecimal getBeforeQuantity() { return beforeQuantity; }
        public void setBeforeQuantity(BigDecimal beforeQuantity) { this.beforeQuantity = beforeQuantity; }
        public String getBatchNo() { return batchNo; }
        public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
        public String getUnitCost() { return unitCost; }
        public void setUnitCost(String unitCost) { this.unitCost = unitCost; }
        public BigDecimal getAdjustQuantity() { return adjustQuantity; }
        public void setAdjustQuantity(BigDecimal adjustQuantity) { this.adjustQuantity = adjustQuantity; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
