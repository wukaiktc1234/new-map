package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存日志表实体类
 * 记录库存变动的操作流水
 */
@TableName("inventory_log")
public class InventoryLog {
    /**
     * 日志ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产品/物料ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 操作类型（in/out/check/transfer/loss）
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作前库存数量
     */
    @TableField("before_stock")
    private BigDecimal beforeStock;

    /**
     * 操作后库存数量
     */
    @TableField("after_stock")
    private BigDecimal afterStock;

    /**
     * 变化数量
     */
    @TableField("change_amount")
    private BigDecimal changeAmount;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public InventoryLog() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getOperationType() {
        return this.operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getBeforeStock() {
        return this.beforeStock;
    }

    public void setBeforeStock(BigDecimal beforeStock) {
        this.beforeStock = beforeStock;
    }

    public BigDecimal getAfterStock() {
        return this.afterStock;
    }

    public void setAfterStock(BigDecimal afterStock) {
        this.afterStock = afterStock;
    }

    public BigDecimal getChangeAmount() {
        return this.changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 兼容性方法：旧版字段名为 createdAt（java.util.Date）
     * 保留以兼容现有调用代码，内部转换为 LocalDateTime
     */
    public java.util.Date getCreatedAt() {
        return this.createTime != null
            ? java.util.Date.from(this.createTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }

    /**
     * 兼容性方法：旧版字段名为 createdAt（java.util.Date）
     * 保留以兼容现有调用代码，内部转换为 LocalDateTime
     */
    public void setCreatedAt(java.util.Date createdAt) {
        this.createTime = createdAt != null
            ? createdAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }
}
