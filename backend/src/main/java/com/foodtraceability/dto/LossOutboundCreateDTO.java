package com.foodtraceability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LossOutboundCreateDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    @NotNull(message = "报损数量不能为空")
    @Min(value = 1, message = "报损数量必须大于0")
    private Integer lossQuantity;
    @NotBlank(message = "报损原因不能为空")
    private String lossReason;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotBlank(message = "报损日期不能为空")
    private String lossDate;
    private String remark;

    public LossOutboundCreateDTO() {
    }

    public Long getProductId() {
        return this.productId;
    }

    public Integer getLossQuantity() {
        return this.lossQuantity;
    }

    public String getLossReason() {
        return this.lossReason;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public String getLossDate() {
        return this.lossDate;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setLossQuantity(final Integer lossQuantity) {
        this.lossQuantity = lossQuantity;
    }

    public void setLossReason(final String lossReason) {
        this.lossReason = lossReason;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setLossDate(final String lossDate) {
        this.lossDate = lossDate;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LossOutboundCreateDTO)) return false;
        final LossOutboundCreateDTO other = (LossOutboundCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$lossQuantity = this.getLossQuantity();
        final java.lang.Object other$lossQuantity = other.getLossQuantity();
        if (this$lossQuantity == null ? other$lossQuantity != null : !this$lossQuantity.equals(other$lossQuantity)) return false;
        final java.lang.Object this$warehouseId = this.getWarehouseId();
        final java.lang.Object other$warehouseId = other.getWarehouseId();
        if (this$warehouseId == null ? other$warehouseId != null : !this$warehouseId.equals(other$warehouseId)) return false;
        final java.lang.Object this$lossReason = this.getLossReason();
        final java.lang.Object other$lossReason = other.getLossReason();
        if (this$lossReason == null ? other$lossReason != null : !this$lossReason.equals(other$lossReason)) return false;
        final java.lang.Object this$lossDate = this.getLossDate();
        final java.lang.Object other$lossDate = other.getLossDate();
        if (this$lossDate == null ? other$lossDate != null : !this$lossDate.equals(other$lossDate)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LossOutboundCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $lossQuantity = this.getLossQuantity();
        result = result * PRIME + ($lossQuantity == null ? 43 : $lossQuantity.hashCode());
        final java.lang.Object $warehouseId = this.getWarehouseId();
        result = result * PRIME + ($warehouseId == null ? 43 : $warehouseId.hashCode());
        final java.lang.Object $lossReason = this.getLossReason();
        result = result * PRIME + ($lossReason == null ? 43 : $lossReason.hashCode());
        final java.lang.Object $lossDate = this.getLossDate();
        result = result * PRIME + ($lossDate == null ? 43 : $lossDate.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LossOutboundCreateDTO(productId=" + this.getProductId() + ", lossQuantity=" + this.getLossQuantity() + ", lossReason=" + this.getLossReason() + ", warehouseId=" + this.getWarehouseId() + ", lossDate=" + this.getLossDate() + ", remark=" + this.getRemark() + ")";
    }
}
