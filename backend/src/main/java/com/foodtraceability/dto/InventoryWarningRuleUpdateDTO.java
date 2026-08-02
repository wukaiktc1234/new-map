package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 预警规则更新DTO
 */
@Schema(description = "预警规则更新DTO")
public class InventoryWarningRuleUpdateDTO {

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "仓库ID（NULL表示全局规则）")
    private Long warehouseId;

    @Schema(description = "预警类型（1:低库存 2:高库存 3:临期 4:过期）")
    private Integer warningType;

    @Schema(description = "条件字段")
    private String conditionField;

    @Schema(description = "条件运算符（<, >, =, <=, >=）")
    private String conditionOperator;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "通知方式（1:邮件 2:站内信 3:短信 4:全部）")
    private Integer notifyMethod;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

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
}
