package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警记录实体类
 * 用于存储系统自动生成的库存预警记录
 */
@TableName("inventory_warning_records")
public class InventoryWarningRecord {
    /**
     * 记录ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /**
     * 规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 库存ID
     */
    @TableField("inventory_id")
    private Long inventoryId;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 预警类型（1:低库存 2:高库存 3:临期 4:过期）
     */
    @TableField("warning_type")
    private Integer warningType;

    /**
     * 当前值
     */
    @TableField("current_value")
    private BigDecimal currentValue;

    /**
     * 阈值
     */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /**
     * 预警消息
     */
    @TableField("message")
    private String message;

    /**
     * 是否已处理
     */
    @TableField("is_handled")
    private Boolean isHandled;

    /**
     * 处理时间
     */
    @TableField("handle_time")
    private LocalDateTime handleTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getWarningType() {
        return warningType;
    }

    public void setWarningType(Integer warningType) {
        this.warningType = warningType;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsHandled() {
        return isHandled;
    }

    public void setIsHandled(Boolean isHandled) {
        this.isHandled = isHandled;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
