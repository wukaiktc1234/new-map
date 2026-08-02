package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则实体类
 * 用于定义库存预警规则，如低库存、高库存、临期、过期等
 */
@TableName("inventory_warning_rules")
public class InventoryWarningRule {
    /**
     * 规则ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 仓库ID（NULL表示全局规则）
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 预警类型（1:低库存 2:高库存 3:临期 4:过期）
     */
    @TableField("warning_type")
    private Integer warningType;

    /**
     * 条件字段
     */
    @TableField("condition_field")
    private String conditionField;

    /**
     * 条件运算符（<, >, =, <=, >=）
     */
    @TableField("condition_operator")
    private String conditionOperator;

    /**
     * 阈值
     */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /**
     * 通知方式（1:邮件 2:站内信 3:短信 4:全部）
     */
    @TableField("notify_method")
    private Integer notifyMethod;

    /**
     * 是否启用
     */
    @TableField("is_enabled")
    private Boolean isEnabled;

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

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public String getConditionField() {
        return conditionField;
    }

    public void setConditionField(String conditionField) {
        this.conditionField = conditionField;
    }

    public String getConditionOperator() {
        return conditionOperator;
    }

    public void setConditionOperator(String conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Integer getNotifyMethod() {
        return notifyMethod;
    }

    public void setNotifyMethod(Integer notifyMethod) {
        this.notifyMethod = notifyMethod;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
