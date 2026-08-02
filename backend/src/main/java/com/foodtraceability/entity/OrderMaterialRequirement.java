package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_material_requirement")
@Schema(description = "订单原料需求实体")
public class OrderMaterialRequirement {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("requirement_id")
    @Schema(description = "需求ID")
    private String requirementId;
    @TableField("order_id")
    @Schema(description = "订单ID")
    private String orderId;
    @TableField("kitchen_order_id")
    @Schema(description = "后厨订单ID")
    private String kitchenOrderId;
    @TableField("order_number")
    @Schema(description = "订单编号")
    private String orderNumber;
    @TableField("dish_id")
    @Schema(description = "菜品ID")
    private String dishId;
    @TableField("dish_name")
    @Schema(description = "菜品名称")
    private String dishName;
    @TableField("dish_quantity")
    @Schema(description = "菜品数量")
    private Integer dishQuantity;
    @TableField("is_combo")
    @Schema(description = "是否套餐：0-单品，1-套餐")
    private Integer isCombo;
    @TableField("combo_id")
    @Schema(description = "套餐ID")
    private String comboId;
    @TableField("material_id")
    @Schema(description = "原料ID")
    private String materialId;
    @TableField("material_name")
    @Schema(description = "原料名称")
    private String materialName;
    @TableField("required_quantity")
    @Schema(description = "需要数量")
    private BigDecimal requiredQuantity;
    @TableField("unit")
    @Schema(description = "单位")
    private String unit;
    @TableField("locked_quantity")
    @Schema(description = "已锁定数量")
    private BigDecimal lockedQuantity;
    @TableField("used_quantity")
    @Schema(description = "已使用数量")
    private BigDecimal usedQuantity;
    @TableField("status")
    @Schema(description = "状态：pending-待使用，partial-部分使用，completed-已完成")
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public OrderMaterialRequirement() {
    }

    public Long getId() {
        return this.id;
    }

    public String getRequirementId() {
        return this.requirementId;
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

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public Integer getDishQuantity() {
        return this.dishQuantity;
    }

    public Integer getIsCombo() {
        return this.isCombo;
    }

    public String getComboId() {
        return this.comboId;
    }

    public String getMaterialId() {
        return this.materialId;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public BigDecimal getRequiredQuantity() {
        return this.requiredQuantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getLockedQuantity() {
        return this.lockedQuantity;
    }

    public BigDecimal getUsedQuantity() {
        return this.usedQuantity;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRequirementId(final String requirementId) {
        this.requirementId = requirementId;
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

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setDishQuantity(final Integer dishQuantity) {
        this.dishQuantity = dishQuantity;
    }

    public void setIsCombo(final Integer isCombo) {
        this.isCombo = isCombo;
    }

    public void setComboId(final String comboId) {
        this.comboId = comboId;
    }

    public void setMaterialId(final String materialId) {
        this.materialId = materialId;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setRequiredQuantity(final BigDecimal requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setLockedQuantity(final BigDecimal lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public void setUsedQuantity(final BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderMaterialRequirement)) return false;
        final OrderMaterialRequirement other = (OrderMaterialRequirement) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$dishQuantity = this.getDishQuantity();
        final java.lang.Object other$dishQuantity = other.getDishQuantity();
        if (this$dishQuantity == null ? other$dishQuantity != null : !this$dishQuantity.equals(other$dishQuantity)) return false;
        final java.lang.Object this$isCombo = this.getIsCombo();
        final java.lang.Object other$isCombo = other.getIsCombo();
        if (this$isCombo == null ? other$isCombo != null : !this$isCombo.equals(other$isCombo)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$requirementId = this.getRequirementId();
        final java.lang.Object other$requirementId = other.getRequirementId();
        if (this$requirementId == null ? other$requirementId != null : !this$requirementId.equals(other$requirementId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$comboId = this.getComboId();
        final java.lang.Object other$comboId = other.getComboId();
        if (this$comboId == null ? other$comboId != null : !this$comboId.equals(other$comboId)) return false;
        final java.lang.Object this$materialId = this.getMaterialId();
        final java.lang.Object other$materialId = other.getMaterialId();
        if (this$materialId == null ? other$materialId != null : !this$materialId.equals(other$materialId)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$requiredQuantity = this.getRequiredQuantity();
        final java.lang.Object other$requiredQuantity = other.getRequiredQuantity();
        if (this$requiredQuantity == null ? other$requiredQuantity != null : !this$requiredQuantity.equals(other$requiredQuantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$lockedQuantity = this.getLockedQuantity();
        final java.lang.Object other$lockedQuantity = other.getLockedQuantity();
        if (this$lockedQuantity == null ? other$lockedQuantity != null : !this$lockedQuantity.equals(other$lockedQuantity)) return false;
        final java.lang.Object this$usedQuantity = this.getUsedQuantity();
        final java.lang.Object other$usedQuantity = other.getUsedQuantity();
        if (this$usedQuantity == null ? other$usedQuantity != null : !this$usedQuantity.equals(other$usedQuantity)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderMaterialRequirement;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $dishQuantity = this.getDishQuantity();
        result = result * PRIME + ($dishQuantity == null ? 43 : $dishQuantity.hashCode());
        final java.lang.Object $isCombo = this.getIsCombo();
        result = result * PRIME + ($isCombo == null ? 43 : $isCombo.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $requirementId = this.getRequirementId();
        result = result * PRIME + ($requirementId == null ? 43 : $requirementId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $comboId = this.getComboId();
        result = result * PRIME + ($comboId == null ? 43 : $comboId.hashCode());
        final java.lang.Object $materialId = this.getMaterialId();
        result = result * PRIME + ($materialId == null ? 43 : $materialId.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $requiredQuantity = this.getRequiredQuantity();
        result = result * PRIME + ($requiredQuantity == null ? 43 : $requiredQuantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $lockedQuantity = this.getLockedQuantity();
        result = result * PRIME + ($lockedQuantity == null ? 43 : $lockedQuantity.hashCode());
        final java.lang.Object $usedQuantity = this.getUsedQuantity();
        result = result * PRIME + ($usedQuantity == null ? 43 : $usedQuantity.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderMaterialRequirement(id=" + this.getId() + ", requirementId=" + this.getRequirementId() + ", orderId=" + this.getOrderId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderNumber=" + this.getOrderNumber() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", dishQuantity=" + this.getDishQuantity() + ", isCombo=" + this.getIsCombo() + ", comboId=" + this.getComboId() + ", materialId=" + this.getMaterialId() + ", materialName=" + this.getMaterialName() + ", requiredQuantity=" + this.getRequiredQuantity() + ", unit=" + this.getUnit() + ", lockedQuantity=" + this.getLockedQuantity() + ", usedQuantity=" + this.getUsedQuantity() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
