package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "库存扣减DTO")
public class InventoryDeductionDTO {
    @Schema(description = "模式A：按追溯码扣减的列表")
    private List<DeductionItem> modeAItems;
    @Schema(description = "模式B：按配方扣减的列表")
    private List<DeductionItem> modeBItems;


    @Schema(description = "扣减项目")
    public static class DeductionItem {
        @Schema(description = "原料ID")
        private String materialId;
        @Schema(description = "原料名称")
        private String materialName;
        @Schema(description = "追溯码（模式A使用）")
        private String traceCode;
        @Schema(description = "批次码（模式B-批次使用）")
        private String batchCode;
        @Schema(description = "扣减数量")
        private BigDecimal quantity;
        @Schema(description = "单位")
        private String unit;

        public DeductionItem() {
        }

        public String getMaterialId() {
            return this.materialId;
        }

        public String getMaterialName() {
            return this.materialName;
        }

        public String getTraceCode() {
            return this.traceCode;
        }

        public String getBatchCode() {
            return this.batchCode;
        }

        public BigDecimal getQuantity() {
            return this.quantity;
        }

        public String getUnit() {
            return this.unit;
        }

        public void setMaterialId(final String materialId) {
            this.materialId = materialId;
        }

        public void setMaterialName(final String materialName) {
            this.materialName = materialName;
        }

        public void setTraceCode(final String traceCode) {
            this.traceCode = traceCode;
        }

        public void setBatchCode(final String batchCode) {
            this.batchCode = batchCode;
        }

        public void setQuantity(final BigDecimal quantity) {
            this.quantity = quantity;
        }

        public void setUnit(final String unit) {
            this.unit = unit;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof InventoryDeductionDTO.DeductionItem)) return false;
            final InventoryDeductionDTO.DeductionItem other = (InventoryDeductionDTO.DeductionItem) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$materialId = this.getMaterialId();
            final java.lang.Object other$materialId = other.getMaterialId();
            if (this$materialId == null ? other$materialId != null : !this$materialId.equals(other$materialId)) return false;
            final java.lang.Object this$materialName = this.getMaterialName();
            final java.lang.Object other$materialName = other.getMaterialName();
            if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
            final java.lang.Object this$traceCode = this.getTraceCode();
            final java.lang.Object other$traceCode = other.getTraceCode();
            if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
            final java.lang.Object this$batchCode = this.getBatchCode();
            final java.lang.Object other$batchCode = other.getBatchCode();
            if (this$batchCode == null ? other$batchCode != null : !this$batchCode.equals(other$batchCode)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$unit = this.getUnit();
            final java.lang.Object other$unit = other.getUnit();
            if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof InventoryDeductionDTO.DeductionItem;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $materialId = this.getMaterialId();
            result = result * PRIME + ($materialId == null ? 43 : $materialId.hashCode());
            final java.lang.Object $materialName = this.getMaterialName();
            result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
            final java.lang.Object $traceCode = this.getTraceCode();
            result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
            final java.lang.Object $batchCode = this.getBatchCode();
            result = result * PRIME + ($batchCode == null ? 43 : $batchCode.hashCode());
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $unit = this.getUnit();
            result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InventoryDeductionDTO.DeductionItem(materialId=" + this.getMaterialId() + ", materialName=" + this.getMaterialName() + ", traceCode=" + this.getTraceCode() + ", batchCode=" + this.getBatchCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ")";
        }
    }

    public InventoryDeductionDTO() {
    }

    public List<DeductionItem> getModeAItems() {
        return this.modeAItems;
    }

    public List<DeductionItem> getModeBItems() {
        return this.modeBItems;
    }

    public void setModeAItems(final List<DeductionItem> modeAItems) {
        this.modeAItems = modeAItems;
    }

    public void setModeBItems(final List<DeductionItem> modeBItems) {
        this.modeBItems = modeBItems;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InventoryDeductionDTO)) return false;
        final InventoryDeductionDTO other = (InventoryDeductionDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$modeAItems = this.getModeAItems();
        final java.lang.Object other$modeAItems = other.getModeAItems();
        if (this$modeAItems == null ? other$modeAItems != null : !this$modeAItems.equals(other$modeAItems)) return false;
        final java.lang.Object this$modeBItems = this.getModeBItems();
        final java.lang.Object other$modeBItems = other.getModeBItems();
        if (this$modeBItems == null ? other$modeBItems != null : !this$modeBItems.equals(other$modeBItems)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InventoryDeductionDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $modeAItems = this.getModeAItems();
        result = result * PRIME + ($modeAItems == null ? 43 : $modeAItems.hashCode());
        final java.lang.Object $modeBItems = this.getModeBItems();
        result = result * PRIME + ($modeBItems == null ? 43 : $modeBItems.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InventoryDeductionDTO(modeAItems=" + this.getModeAItems() + ", modeBItems=" + this.getModeBItems() + ")";
    }
}
