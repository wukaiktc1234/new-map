package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一库存流水实体（P1-LOCATION-MODEL-001 M3-M4：T3+T4a+T4b 三表合并）。
 * 追加型流水，禁止 update/delete（宪法 §三.7）。
 * source_type + source_ref 为强制来源引用（R-04 教训固化），取值示例：
 *   PURCHASE_STOCKIN / PURCHASE_ARRIVAL / RECEIPT_CONFIRM / SALE_DEDUCT / KDS_DEDUCT /
 *   REFUND_RESTOCK / TRANSFER_OUT / TRANSFER_IN / ADJUST / LOSS / CHECK /
 *   QUICK_STOCKIN / MIGRATED_WAREHOUSE_LEGACY
 */
@TableName("inventory_movement")
public class InventoryMovement {

    @TableId(type = IdType.AUTO)
    private Long movementId;

    /** 位置ID（STORE/CENTRAL/DEPOT 统一维度） */
    private Long locationId;

    /** 物料ID */
    private Long materialId;

    /** 变动数量（正=入 负=出） */
    private BigDecimal changeQty;

    /** 变动后结存 */
    private BigDecimal balanceAfter;

    /** 动作类型：IN / OUT / TRANSFER_OUT / TRANSFER_IN / ADJUST */
    private String movementType;

    /** 来源业务类型（强制） */
    private String sourceType;

    /** 来源单据号（强制） */
    private String sourceRef;

    /** 操作人 */
    private Long operatorId;

    /** 单位成本（分） */
    private Long unitCost;

    /** 总成本（分） */
    private Long totalCost;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public BigDecimal getChangeQty() { return changeQty; }
    public void setChangeQty(BigDecimal changeQty) { this.changeQty = changeQty; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public Long getUnitCost() { return unitCost; }
    public void setUnitCost(Long unitCost) { this.unitCost = unitCost; }

    public Long getTotalCost() { return totalCost; }
    public void setTotalCost(Long totalCost) { this.totalCost = totalCost; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
