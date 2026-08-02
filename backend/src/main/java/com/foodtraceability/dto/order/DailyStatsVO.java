package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 按日期分组统计VO
 * 用于展示按日期分组的营业额、成本、利润数据
 */
@Schema(description = "按日期分组统计数据")
public class DailyStatsVO {

    /** 日期（yyyy-MM-dd） */
    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    /** 门店名称 */
    @Schema(description = "门店名称")
    private String storeName;

    /** 订单数 */
    @Schema(description = "订单数")
    private Long orderCount;

    /** 营业额（分） */
    @Schema(description = "营业额（分）")
    private Long revenue;

    /** 成本（分） */
    @Schema(description = "成本（分）")
    private Long cost;

    /** 利润（分） */
    @Schema(description = "利润（分）")
    private Long profit;

    /** 利润率（百分比） */
    @Schema(description = "利润率（百分比）")
    private Double profitRate;

    // Getter和Setter方法
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    public Long getRevenue() { return revenue; }
    public void setRevenue(Long revenue) { this.revenue = revenue; }
    public Long getCost() { return cost; }
    public void setCost(Long cost) { this.cost = cost; }
    public Long getProfit() { return profit; }
    public void setProfit(Long profit) { this.profit = profit; }
    public Double getProfitRate() { return profitRate; }
    public void setProfitRate(Double profitRate) { this.profitRate = profitRate; }
}
