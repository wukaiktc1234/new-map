package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("combo_inventory")
@Schema(description = "套餐库存产品关联实体")
public class ComboInventory {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关联ID")
    private Long id;
    @Schema(description = "套餐ID")
    @TableField("combo_id")
    private Long comboId;
    @Schema(description = "库存产品ID")
    @TableField("inventory_id")
    private Long inventoryId;
    @Schema(description = "库存产品名称")
    @TableField("inventory_name")
    private String inventoryName;
    @Schema(description = "使用数量")
    @TableField("quantity")
    private BigDecimal quantity;
    @Schema(description = "计量单位")
    @TableField("unit")
    private String unit;
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;
    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;
    @TableLogic
    @Schema(description = "删除标记（0：未删除，1：已删除）")
    private Integer deleted;

    // Getter methods
    public Long getId() {
        return id;
    }

    public Long getComboId() {
        return comboId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public String getInventoryName() {
        return inventoryName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public void setComboId(Long comboId) {
        this.comboId = comboId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public ComboInventory() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ComboInventory)) return false;
        final ComboInventory other = (ComboInventory) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$comboId = this.getComboId();
        final java.lang.Object other$comboId = other.getComboId();
        if (this$comboId == null ? other$comboId != null : !this$comboId.equals(other$comboId)) return false;
        final java.lang.Object this$inventoryId = this.getInventoryId();
        final java.lang.Object other$inventoryId = other.getInventoryId();
        if (this$inventoryId == null ? other$inventoryId != null : !this$inventoryId.equals(other$inventoryId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$inventoryName = this.getInventoryName();
        final java.lang.Object other$inventoryName = other.getInventoryName();
        if (this$inventoryName == null ? other$inventoryName != null : !this$inventoryName.equals(other$inventoryName)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ComboInventory;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $comboId = this.getComboId();
        result = result * PRIME + ($comboId == null ? 43 : $comboId.hashCode());
        final java.lang.Object $inventoryId = this.getInventoryId();
        result = result * PRIME + ($inventoryId == null ? 43 : $inventoryId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $inventoryName = this.getInventoryName();
        result = result * PRIME + ($inventoryName == null ? 43 : $inventoryName.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ComboInventory(id=" + this.getId() + ", comboId=" + this.getComboId() + ", inventoryId=" + this.getInventoryId() + ", inventoryName=" + this.getInventoryName() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
