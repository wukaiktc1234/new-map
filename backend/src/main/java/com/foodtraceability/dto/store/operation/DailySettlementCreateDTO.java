package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * 日结对账创建DTO
 * 用于创建对账记录
 * 注意：通常由系统自动生成，此DTO仅用于手动补录场景
 */
@Schema(description = "日结对账创建DTO")
public class DailySettlementCreateDTO {

    /** 门店ID */
    @NotBlank(message = "门店ID不能为空")
    @Schema(description = "门店ID", example = "store001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeId;

    /** 结算日期 */
    @NotNull(message = "结算日期不能为空")
    @Schema(description = "结算日期", example = "2026-05-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate settlementDate;

    /**
     * 总营业收入（单位：元，前端传入）
     * 后端会转换为分存储到数据库
     */
    @Positive(message = "总营业收入必须为正数")
    @Schema(description = "总营业收入（单位：元）", example = "15800.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private String totalRevenue;

    /** 订单总数 */
    @Schema(description = "订单总数", example = "156")
    private Integer orderCount;

    /** 总成本（单位：元） */
    @Schema(description = "总成本（单位：元）", example = "8000.00")
    private String totalCost;

    /** 净利润（单位：元） */
    @Schema(description = "净利润（单位：元）", example = "7800.50")
    private String netProfit;

    /** 毛利率（百分比，如 45.5 表示 45.5%） */
    @Schema(description = "毛利率（百分比）", example = "45.5")
    private String grossProfitRate;

    /** 客单价（单位：元） */
    @Schema(description = "客单价（单位：元）", example = "101.28")
    private String avgOrderValue;

    /** 餐桌使用率（百分比，如 85.0 表示 85%） */
    @Schema(description = "餐桌使用率（百分比）", example = "85.0")
    private String tableUsageRate;

    /** 差额（单位：元） */
    @Schema(description = "差额（单位：元）", example = "0.00")
    private String differenceAmount;

    /** 备注 */
    @Schema(description = "备注", example = "今日营业正常")
    private String remark;

    // ==================== Getter & Setter ====================

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(String totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public String getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(String totalCost) {
        this.totalCost = totalCost;
    }

    public String getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(String netProfit) {
        this.netProfit = netProfit;
    }

    public String getGrossProfitRate() {
        return grossProfitRate;
    }

    public void setGrossProfitRate(String grossProfitRate) {
        this.grossProfitRate = grossProfitRate;
    }

    public String getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(String avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public String getTableUsageRate() {
        return tableUsageRate;
    }

    public void setTableUsageRate(String tableUsageRate) {
        this.tableUsageRate = tableUsageRate;
    }

    public String getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(String differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
