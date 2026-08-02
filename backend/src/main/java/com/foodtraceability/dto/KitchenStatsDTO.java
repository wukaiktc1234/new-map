package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 后厨统计面板DTO
 * 用于返回厨房工作量的统计数据
 */
@Schema(description = "后厨统计面板DTO")
public class KitchenStatsDTO {
    @Schema(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "待处理数量")
    private Integer pendingCount;

    @Schema(description = "制作中数量")
    private Integer makingCount;

    @Schema(description = "已完成数量")
    private Integer completedCount;

    @Schema(description = "完成率")
    private BigDecimal completionRate;

    @Schema(description = "平均等待时间(分钟)")
    private BigDecimal avgWaitMinutes;

    @Schema(description = "平均制作时间(分钟)")
    private BigDecimal avgMakingMinutes;

    @Schema(description = "超时订单数")
    private Integer overdueCount;

    @Schema(description = "高峰时段")
    private String peakHour;

    public KitchenStatsDTO() {
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getMakingCount() {
        return makingCount;
    }

    public void setMakingCount(Integer makingCount) {
        this.makingCount = makingCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public BigDecimal getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(BigDecimal completionRate) {
        this.completionRate = completionRate;
    }

    public BigDecimal getAvgWaitMinutes() {
        return avgWaitMinutes;
    }

    public void setAvgWaitMinutes(BigDecimal avgWaitMinutes) {
        this.avgWaitMinutes = avgWaitMinutes;
    }

    public BigDecimal getAvgMakingMinutes() {
        return avgMakingMinutes;
    }

    public void setAvgMakingMinutes(BigDecimal avgMakingMinutes) {
        this.avgMakingMinutes = avgMakingMinutes;
    }

    public Integer getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Integer overdueCount) {
        this.overdueCount = overdueCount;
    }

    public String getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(String peakHour) {
        this.peakHour = peakHour;
    }
}
