package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 预约统计VO
 * 用于预约汇总统计数据展示
 */
@Schema(description = "预约统计数据")
public class ReservationStatsVO {

    /** 今日预约数 */
    @Schema(description = "今日预约数")
    private Long todayReservations;

    /** 待确认数 */
    @Schema(description = "待确认数")
    private Long pendingConfirm;

    /** 已到店数 */
    @Schema(description = "已到店数")
    private Long arrived;

    /** 取消率（百分比） */
    @Schema(description = "取消率（百分比）")
    private Double cancelRate;

    /** 今日已确认数 */
    @Schema(description = "今日已确认数")
    private Long confirmed;

    /** 今日已取消数 */
    @Schema(description = "今日已取消数")
    private Long cancelled;

    // Getter和Setter方法
    public Long getTodayReservations() { return todayReservations; }
    public void setTodayReservations(Long todayReservations) { this.todayReservations = todayReservations; }
    public Long getPendingConfirm() { return pendingConfirm; }
    public void setPendingConfirm(Long pendingConfirm) { this.pendingConfirm = pendingConfirm; }
    public Long getArrived() { return arrived; }
    public void setArrived(Long arrived) { this.arrived = arrived; }
    public Double getCancelRate() { return cancelRate; }
    public void setCancelRate(Double cancelRate) { this.cancelRate = cancelRate; }
    public Long getConfirmed() { return confirmed; }
    public void setConfirmed(Long confirmed) { this.confirmed = confirmed; }
    public Long getCancelled() { return cancelled; }
    public void setCancelled(Long cancelled) { this.cancelled = cancelled; }
}
