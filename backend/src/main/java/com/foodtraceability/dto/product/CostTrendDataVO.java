package com.foodtraceability.dto.product;

import java.io.Serializable;

/**
 * 成本趋势数据 VO
 * 描述按日期聚合的成本变化趋势
 */
public class CostTrendDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日期，格式 yyyy-MM-dd */
    private String date;
    /** 平均成本（分） */
    private Long avgCost;
    /** 最大成本（分） */
    private Long maxCost;
    /** 最小成本（分） */
    private Long minCost;
    /** 菜品数量 */
    private Integer dishCount;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getAvgCost() { return avgCost; }
    public void setAvgCost(Long avgCost) { this.avgCost = avgCost; }
    public Long getMaxCost() { return maxCost; }
    public void setMaxCost(Long maxCost) { this.maxCost = maxCost; }
    public Long getMinCost() { return minCost; }
    public void setMinCost(Long minCost) { this.minCost = minCost; }
    public Integer getDishCount() { return dishCount; }
    public void setDishCount(Integer dishCount) { this.dishCount = dishCount; }
}
