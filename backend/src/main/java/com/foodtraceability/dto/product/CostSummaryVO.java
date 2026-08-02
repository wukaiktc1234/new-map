package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * 菜品成本汇总 VO
 * 描述所有菜品成本的汇总统计信息
 */
public class CostSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 菜品总数 */
    private Integer totalDishes;
    /** 正常状态数量 */
    private Integer normalCount;
    /** 预警状态数量 */
    private Integer warningCount;
    /** 危险状态数量 */
    private Integer dangerCount;
    /** 平均毛利率(%) */
    private Double avgMarginRate;
    /** 总成本变化百分比(%) */
    private Double totalCostChange;
    /** 成本上涨 TOP 列表 */
    private List<DishCostChange> topCostIncrease;
    /** 负毛利 TOP 列表 */
    private List<DishNegativeProfit> topNegativeProfit;

    /**
     * 成本变化项
     */
    public static class DishCostChange implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 菜品名称 */
        private String dishName;
        /** 成本变化百分比(%) */
        private Double changePercent;

        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }
        public Double getChangePercent() { return changePercent; }
        public void setChangePercent(Double changePercent) { this.changePercent = changePercent; }
    }

    /**
     * 负毛利项
     */
    public static class DishNegativeProfit implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 菜品名称 */
        private String dishName;
        /** 毛利率(%) */
        private Double marginRate;

        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }
        public Double getMarginRate() { return marginRate; }
        public void setMarginRate(Double marginRate) { this.marginRate = marginRate; }
    }

    public Integer getTotalDishes() { return totalDishes; }
    public void setTotalDishes(Integer totalDishes) { this.totalDishes = totalDishes; }
    public Integer getNormalCount() { return normalCount; }
    public void setNormalCount(Integer normalCount) { this.normalCount = normalCount; }
    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
    public Integer getDangerCount() { return dangerCount; }
    public void setDangerCount(Integer dangerCount) { this.dangerCount = dangerCount; }
    public Double getAvgMarginRate() { return avgMarginRate; }
    public void setAvgMarginRate(Double avgMarginRate) { this.avgMarginRate = avgMarginRate; }
    public Double getTotalCostChange() { return totalCostChange; }
    public void setTotalCostChange(Double totalCostChange) { this.totalCostChange = totalCostChange; }
    public List<DishCostChange> getTopCostIncrease() { return topCostIncrease; }
    public void setTopCostIncrease(List<DishCostChange> topCostIncrease) { this.topCostIncrease = topCostIncrease; }
    public List<DishNegativeProfit> getTopNegativeProfit() { return topNegativeProfit; }
    public void setTopNegativeProfit(List<DishNegativeProfit> topNegativeProfit) { this.topNegativeProfit = topNegativeProfit; }
}
