package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 退款统计VO
 * 用于退款汇总统计数据展示
 */
@Schema(description = "退款统计数据")
public class OrderRefundStatsVO {

    /** 待处理退款数 */
    @Schema(description = "待处理退款数")
    private Long pendingCount;

    /** 已退款金额（分） */
    @Schema(description = "已退款金额（分）")
    private Long refundedAmount;

    /** 退款率（百分比） */
    @Schema(description = "退款率（百分比）")
    private Double refundRate;

    /** 平均处理时长（小时） */
    @Schema(description = "平均处理时长（小时）")
    private Double avgProcessHours;

    // Getter和Setter方法
    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }
    public Long getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(Long refundedAmount) { this.refundedAmount = refundedAmount; }
    public Double getRefundRate() { return refundRate; }
    public void setRefundRate(Double refundRate) { this.refundRate = refundRate; }
    public Double getAvgProcessHours() { return avgProcessHours; }
    public void setAvgProcessHours(Double avgProcessHours) { this.avgProcessHours = avgProcessHours; }
}
