package com.foodtraceability.dto.product;

import java.io.Serializable;

/**
 * 成本预警规则 VO
 * 描述菜品成本的预警阈值和触发条件
 */
public class CostAlertRuleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private String id;
    /** 规则名称 */
    private String name;
    /** 规则类型：margin_below/cost_increase/absolute_cost */
    private String ruleType;
    /** 阈值（百分比或绝对值） */
    private Double threshold;
    /** 是否启用 */
    private Boolean enabled;
    /** 适用分类ID（null 表示全部） */
    private String categoryId;
    /** 适用分类名称 */
    private String categoryName;
    /** 创建时间（ISO 8601） */
    private String createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
