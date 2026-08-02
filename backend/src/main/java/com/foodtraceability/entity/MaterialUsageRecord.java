package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("material_usage_record")
@Schema(description = "原料使用记录实体")
public class MaterialUsageRecord {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("record_id")
    @Schema(description = "记录ID")
    private String recordId;
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID")
    private String traceCodeId;
    @TableField("trace_code")
    @Schema(description = "追溯码")
    private String traceCode;
    @TableField("material_id")
    @Schema(description = "原料ID")
    private String materialId;
    @TableField("material_name")
    @Schema(description = "原料名称")
    private String materialName;
    @TableField("used_quantity")
    @Schema(description = "使用数量")
    private BigDecimal usedQuantity;
    @TableField("unit")
    @Schema(description = "单位")
    private String unit;
    @TableField("order_id")
    @Schema(description = "关联订单ID")
    private String orderId;
    @TableField("kitchen_order_id")
    @Schema(description = "关联后厨订单ID")
    private String kitchenOrderId;
    @TableField("order_number")
    @Schema(description = "订单编号")
    private String orderNumber;
    @TableField("dish_name")
    @Schema(description = "关联菜品名称")
    private String dishName;
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private String operatorId;
    @TableField("operator_name")
    @Schema(description = "操作人姓名")
    private String operatorName;
    @TableField("usage_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "使用时间")
    private LocalDateTime usageTime;
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public MaterialUsageRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public String getRecordId() {
        return this.recordId;
    }

    public String getTraceCodeId() {
        return this.traceCodeId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getMaterialId() {
        return this.materialId;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public BigDecimal getUsedQuantity() {
        return this.usedQuantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getDishName() {
        return this.dishName;
    }

    public String getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getUsageTime() {
        return this.usageTime;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public void setTraceCodeId(final String traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setMaterialId(final String materialId) {
        this.materialId = materialId;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setUsedQuantity(final BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setOperatorId(final String operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUsageTime(final LocalDateTime usageTime) {
        this.usageTime = usageTime;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MaterialUsageRecord)) return false;
        final MaterialUsageRecord other = (MaterialUsageRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$recordId = this.getRecordId();
        final java.lang.Object other$recordId = other.getRecordId();
        if (this$recordId == null ? other$recordId != null : !this$recordId.equals(other$recordId)) return false;
        final java.lang.Object this$traceCodeId = this.getTraceCodeId();
        final java.lang.Object other$traceCodeId = other.getTraceCodeId();
        if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$materialId = this.getMaterialId();
        final java.lang.Object other$materialId = other.getMaterialId();
        if (this$materialId == null ? other$materialId != null : !this$materialId.equals(other$materialId)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$usedQuantity = this.getUsedQuantity();
        final java.lang.Object other$usedQuantity = other.getUsedQuantity();
        if (this$usedQuantity == null ? other$usedQuantity != null : !this$usedQuantity.equals(other$usedQuantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$usageTime = this.getUsageTime();
        final java.lang.Object other$usageTime = other.getUsageTime();
        if (this$usageTime == null ? other$usageTime != null : !this$usageTime.equals(other$usageTime)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MaterialUsageRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $recordId = this.getRecordId();
        result = result * PRIME + ($recordId == null ? 43 : $recordId.hashCode());
        final java.lang.Object $traceCodeId = this.getTraceCodeId();
        result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $materialId = this.getMaterialId();
        result = result * PRIME + ($materialId == null ? 43 : $materialId.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $usedQuantity = this.getUsedQuantity();
        result = result * PRIME + ($usedQuantity == null ? 43 : $usedQuantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $usageTime = this.getUsageTime();
        result = result * PRIME + ($usageTime == null ? 43 : $usageTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MaterialUsageRecord(id=" + this.getId() + ", recordId=" + this.getRecordId() + ", traceCodeId=" + this.getTraceCodeId() + ", traceCode=" + this.getTraceCode() + ", materialId=" + this.getMaterialId() + ", materialName=" + this.getMaterialName() + ", usedQuantity=" + this.getUsedQuantity() + ", unit=" + this.getUnit() + ", orderId=" + this.getOrderId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderNumber=" + this.getOrderNumber() + ", dishName=" + this.getDishName() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", usageTime=" + this.getUsageTime() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
