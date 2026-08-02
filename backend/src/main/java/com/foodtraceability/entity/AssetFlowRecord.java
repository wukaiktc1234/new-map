package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("asset_flow_records")
public class AssetFlowRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String flowType;
    private Long fromStoreId;
    private String fromStoreName;
    private String fromStatus;
    private Long toStoreId;
    private String toStoreName;
    private String toStatus;
    private String relatedOrderId;
    private Long relatedKitchenOrderId;
    private Integer quantity;
    private String reason;
    private String remark;
    private Long operatorId;
    private String operatorName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime flowTime;

    public AssetFlowRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getAssetId() {
        return this.assetId;
    }

    public String getAssetCode() {
        return this.assetCode;
    }

    public String getAssetName() {
        return this.assetName;
    }

    public String getFlowType() {
        return this.flowType;
    }

    public Long getFromStoreId() {
        return this.fromStoreId;
    }

    public String getFromStoreName() {
        return this.fromStoreName;
    }

    public String getFromStatus() {
        return this.fromStatus;
    }

    public Long getToStoreId() {
        return this.toStoreId;
    }

    public String getToStoreName() {
        return this.toStoreName;
    }

    public String getToStatus() {
        return this.toStatus;
    }

    public String getRelatedOrderId() {
        return this.relatedOrderId;
    }

    public Long getRelatedKitchenOrderId() {
        return this.relatedKitchenOrderId;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public String getReason() {
        return this.reason;
    }

    public String getRemark() {
        return this.remark;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getFlowTime() {
        return this.flowTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setAssetId(final Long assetId) {
        this.assetId = assetId;
    }

    public void setAssetCode(final String assetCode) {
        this.assetCode = assetCode;
    }

    public void setAssetName(final String assetName) {
        this.assetName = assetName;
    }

    public void setFlowType(final String flowType) {
        this.flowType = flowType;
    }

    public void setFromStoreId(final Long fromStoreId) {
        this.fromStoreId = fromStoreId;
    }

    public void setFromStoreName(final String fromStoreName) {
        this.fromStoreName = fromStoreName;
    }

    public void setFromStatus(final String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public void setToStoreId(final Long toStoreId) {
        this.toStoreId = toStoreId;
    }

    public void setToStoreName(final String toStoreName) {
        this.toStoreName = toStoreName;
    }

    public void setToStatus(final String toStatus) {
        this.toStatus = toStatus;
    }

    public void setRelatedOrderId(final String relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public void setRelatedKitchenOrderId(final Long relatedKitchenOrderId) {
        this.relatedKitchenOrderId = relatedKitchenOrderId;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setOperatorId(final Long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setFlowTime(final LocalDateTime flowTime) {
        this.flowTime = flowTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AssetFlowRecord)) return false;
        final AssetFlowRecord other = (AssetFlowRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$assetId = this.getAssetId();
        final java.lang.Object other$assetId = other.getAssetId();
        if (this$assetId == null ? other$assetId != null : !this$assetId.equals(other$assetId)) return false;
        final java.lang.Object this$fromStoreId = this.getFromStoreId();
        final java.lang.Object other$fromStoreId = other.getFromStoreId();
        if (this$fromStoreId == null ? other$fromStoreId != null : !this$fromStoreId.equals(other$fromStoreId)) return false;
        final java.lang.Object this$toStoreId = this.getToStoreId();
        final java.lang.Object other$toStoreId = other.getToStoreId();
        if (this$toStoreId == null ? other$toStoreId != null : !this$toStoreId.equals(other$toStoreId)) return false;
        final java.lang.Object this$relatedKitchenOrderId = this.getRelatedKitchenOrderId();
        final java.lang.Object other$relatedKitchenOrderId = other.getRelatedKitchenOrderId();
        if (this$relatedKitchenOrderId == null ? other$relatedKitchenOrderId != null : !this$relatedKitchenOrderId.equals(other$relatedKitchenOrderId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$assetCode = this.getAssetCode();
        final java.lang.Object other$assetCode = other.getAssetCode();
        if (this$assetCode == null ? other$assetCode != null : !this$assetCode.equals(other$assetCode)) return false;
        final java.lang.Object this$assetName = this.getAssetName();
        final java.lang.Object other$assetName = other.getAssetName();
        if (this$assetName == null ? other$assetName != null : !this$assetName.equals(other$assetName)) return false;
        final java.lang.Object this$flowType = this.getFlowType();
        final java.lang.Object other$flowType = other.getFlowType();
        if (this$flowType == null ? other$flowType != null : !this$flowType.equals(other$flowType)) return false;
        final java.lang.Object this$fromStoreName = this.getFromStoreName();
        final java.lang.Object other$fromStoreName = other.getFromStoreName();
        if (this$fromStoreName == null ? other$fromStoreName != null : !this$fromStoreName.equals(other$fromStoreName)) return false;
        final java.lang.Object this$fromStatus = this.getFromStatus();
        final java.lang.Object other$fromStatus = other.getFromStatus();
        if (this$fromStatus == null ? other$fromStatus != null : !this$fromStatus.equals(other$fromStatus)) return false;
        final java.lang.Object this$toStoreName = this.getToStoreName();
        final java.lang.Object other$toStoreName = other.getToStoreName();
        if (this$toStoreName == null ? other$toStoreName != null : !this$toStoreName.equals(other$toStoreName)) return false;
        final java.lang.Object this$toStatus = this.getToStatus();
        final java.lang.Object other$toStatus = other.getToStatus();
        if (this$toStatus == null ? other$toStatus != null : !this$toStatus.equals(other$toStatus)) return false;
        final java.lang.Object this$relatedOrderId = this.getRelatedOrderId();
        final java.lang.Object other$relatedOrderId = other.getRelatedOrderId();
        if (this$relatedOrderId == null ? other$relatedOrderId != null : !this$relatedOrderId.equals(other$relatedOrderId)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$flowTime = this.getFlowTime();
        final java.lang.Object other$flowTime = other.getFlowTime();
        if (this$flowTime == null ? other$flowTime != null : !this$flowTime.equals(other$flowTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AssetFlowRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $assetId = this.getAssetId();
        result = result * PRIME + ($assetId == null ? 43 : $assetId.hashCode());
        final java.lang.Object $fromStoreId = this.getFromStoreId();
        result = result * PRIME + ($fromStoreId == null ? 43 : $fromStoreId.hashCode());
        final java.lang.Object $toStoreId = this.getToStoreId();
        result = result * PRIME + ($toStoreId == null ? 43 : $toStoreId.hashCode());
        final java.lang.Object $relatedKitchenOrderId = this.getRelatedKitchenOrderId();
        result = result * PRIME + ($relatedKitchenOrderId == null ? 43 : $relatedKitchenOrderId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $assetCode = this.getAssetCode();
        result = result * PRIME + ($assetCode == null ? 43 : $assetCode.hashCode());
        final java.lang.Object $assetName = this.getAssetName();
        result = result * PRIME + ($assetName == null ? 43 : $assetName.hashCode());
        final java.lang.Object $flowType = this.getFlowType();
        result = result * PRIME + ($flowType == null ? 43 : $flowType.hashCode());
        final java.lang.Object $fromStoreName = this.getFromStoreName();
        result = result * PRIME + ($fromStoreName == null ? 43 : $fromStoreName.hashCode());
        final java.lang.Object $fromStatus = this.getFromStatus();
        result = result * PRIME + ($fromStatus == null ? 43 : $fromStatus.hashCode());
        final java.lang.Object $toStoreName = this.getToStoreName();
        result = result * PRIME + ($toStoreName == null ? 43 : $toStoreName.hashCode());
        final java.lang.Object $toStatus = this.getToStatus();
        result = result * PRIME + ($toStatus == null ? 43 : $toStatus.hashCode());
        final java.lang.Object $relatedOrderId = this.getRelatedOrderId();
        result = result * PRIME + ($relatedOrderId == null ? 43 : $relatedOrderId.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $flowTime = this.getFlowTime();
        result = result * PRIME + ($flowTime == null ? 43 : $flowTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "AssetFlowRecord(id=" + this.getId() + ", assetId=" + this.getAssetId() + ", assetCode=" + this.getAssetCode() + ", assetName=" + this.getAssetName() + ", flowType=" + this.getFlowType() + ", fromStoreId=" + this.getFromStoreId() + ", fromStoreName=" + this.getFromStoreName() + ", fromStatus=" + this.getFromStatus() + ", toStoreId=" + this.getToStoreId() + ", toStoreName=" + this.getToStoreName() + ", toStatus=" + this.getToStatus() + ", relatedOrderId=" + this.getRelatedOrderId() + ", relatedKitchenOrderId=" + this.getRelatedKitchenOrderId() + ", quantity=" + this.getQuantity() + ", reason=" + this.getReason() + ", remark=" + this.getRemark() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", flowTime=" + this.getFlowTime() + ")";
    }
}
