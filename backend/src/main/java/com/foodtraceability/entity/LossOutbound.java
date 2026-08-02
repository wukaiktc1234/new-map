package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("loss_outbound")
public class LossOutbound {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("loss_no")
    private String lossNo;
    @TableField("product_id")
    private Long productId;
    @TableField("product_name")
    private String productName;
    @TableField("loss_quantity")
    private Integer lossQuantity;
    @TableField("unit")
    private String unit;
    @TableField("loss_reason")
    private String lossReason;
    @TableField("warehouse_id")
    private Long warehouseId;
    @TableField("warehouse_name")
    private String warehouseName;
    @TableField("loss_date")
    private String lossDate;
    @TableField("loss_amount")
    private BigDecimal lossAmount;
    @TableField("status")
    private String status;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("operator_name")
    private String operatorName;
    @TableField("approver_id")
    private Long approverId;
    @TableField("approver_name")
    private String approverName;
    @TableField("approve_time")
    private LocalDateTime approveTime;
    @TableField("remark")
    private String remark;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public LossOutbound() {
    }

    public Long getId() {
        return this.id;
    }

    public String getLossNo() {
        return this.lossNo;
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public Integer getLossQuantity() {
        return this.lossQuantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public String getLossReason() {
        return this.lossReason;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public String getWarehouseName() {
        return this.warehouseName;
    }

    public String getLossDate() {
        return this.lossDate;
    }

    public BigDecimal getLossAmount() {
        return this.lossAmount;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public Long getApproverId() {
        return this.approverId;
    }

    public String getApproverName() {
        return this.approverName;
    }

    public LocalDateTime getApproveTime() {
        return this.approveTime;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setLossNo(final String lossNo) {
        this.lossNo = lossNo;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setLossQuantity(final Integer lossQuantity) {
        this.lossQuantity = lossQuantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setLossReason(final String lossReason) {
        this.lossReason = lossReason;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setWarehouseName(final String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public void setLossDate(final String lossDate) {
        this.lossDate = lossDate;
    }

    public void setLossAmount(final BigDecimal lossAmount) {
        this.lossAmount = lossAmount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setOperatorId(final Long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setApproverId(final Long approverId) {
        this.approverId = approverId;
    }

    public void setApproverName(final String approverName) {
        this.approverName = approverName;
    }

    public void setApproveTime(final LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LossOutbound)) return false;
        final LossOutbound other = (LossOutbound) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$lossQuantity = this.getLossQuantity();
        final java.lang.Object other$lossQuantity = other.getLossQuantity();
        if (this$lossQuantity == null ? other$lossQuantity != null : !this$lossQuantity.equals(other$lossQuantity)) return false;
        final java.lang.Object this$warehouseId = this.getWarehouseId();
        final java.lang.Object other$warehouseId = other.getWarehouseId();
        if (this$warehouseId == null ? other$warehouseId != null : !this$warehouseId.equals(other$warehouseId)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$approverId = this.getApproverId();
        final java.lang.Object other$approverId = other.getApproverId();
        if (this$approverId == null ? other$approverId != null : !this$approverId.equals(other$approverId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$lossNo = this.getLossNo();
        final java.lang.Object other$lossNo = other.getLossNo();
        if (this$lossNo == null ? other$lossNo != null : !this$lossNo.equals(other$lossNo)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$lossReason = this.getLossReason();
        final java.lang.Object other$lossReason = other.getLossReason();
        if (this$lossReason == null ? other$lossReason != null : !this$lossReason.equals(other$lossReason)) return false;
        final java.lang.Object this$warehouseName = this.getWarehouseName();
        final java.lang.Object other$warehouseName = other.getWarehouseName();
        if (this$warehouseName == null ? other$warehouseName != null : !this$warehouseName.equals(other$warehouseName)) return false;
        final java.lang.Object this$lossDate = this.getLossDate();
        final java.lang.Object other$lossDate = other.getLossDate();
        if (this$lossDate == null ? other$lossDate != null : !this$lossDate.equals(other$lossDate)) return false;
        final java.lang.Object this$lossAmount = this.getLossAmount();
        final java.lang.Object other$lossAmount = other.getLossAmount();
        if (this$lossAmount == null ? other$lossAmount != null : !this$lossAmount.equals(other$lossAmount)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$approverName = this.getApproverName();
        final java.lang.Object other$approverName = other.getApproverName();
        if (this$approverName == null ? other$approverName != null : !this$approverName.equals(other$approverName)) return false;
        final java.lang.Object this$approveTime = this.getApproveTime();
        final java.lang.Object other$approveTime = other.getApproveTime();
        if (this$approveTime == null ? other$approveTime != null : !this$approveTime.equals(other$approveTime)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LossOutbound;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $lossQuantity = this.getLossQuantity();
        result = result * PRIME + ($lossQuantity == null ? 43 : $lossQuantity.hashCode());
        final java.lang.Object $warehouseId = this.getWarehouseId();
        result = result * PRIME + ($warehouseId == null ? 43 : $warehouseId.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $approverId = this.getApproverId();
        result = result * PRIME + ($approverId == null ? 43 : $approverId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $lossNo = this.getLossNo();
        result = result * PRIME + ($lossNo == null ? 43 : $lossNo.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $lossReason = this.getLossReason();
        result = result * PRIME + ($lossReason == null ? 43 : $lossReason.hashCode());
        final java.lang.Object $warehouseName = this.getWarehouseName();
        result = result * PRIME + ($warehouseName == null ? 43 : $warehouseName.hashCode());
        final java.lang.Object $lossDate = this.getLossDate();
        result = result * PRIME + ($lossDate == null ? 43 : $lossDate.hashCode());
        final java.lang.Object $lossAmount = this.getLossAmount();
        result = result * PRIME + ($lossAmount == null ? 43 : $lossAmount.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $approverName = this.getApproverName();
        result = result * PRIME + ($approverName == null ? 43 : $approverName.hashCode());
        final java.lang.Object $approveTime = this.getApproveTime();
        result = result * PRIME + ($approveTime == null ? 43 : $approveTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LossOutbound(id=" + this.getId() + ", lossNo=" + this.getLossNo() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", lossQuantity=" + this.getLossQuantity() + ", unit=" + this.getUnit() + ", lossReason=" + this.getLossReason() + ", warehouseId=" + this.getWarehouseId() + ", warehouseName=" + this.getWarehouseName() + ", lossDate=" + this.getLossDate() + ", lossAmount=" + this.getLossAmount() + ", status=" + this.getStatus() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", approverId=" + this.getApproverId() + ", approverName=" + this.getApproverName() + ", approveTime=" + this.getApproveTime() + ", remark=" + this.getRemark() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
