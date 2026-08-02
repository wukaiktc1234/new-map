package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 审批统计VO
 * 用于展示审批相关的统计数据
 * 包括各状态数量、平均处理时长、近期通过率等指标
 * 主要用于管理后台的审批概览面板
 */
@Schema(description = "审批统计VO")
public class ApprovalStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 总量统计 ====================

    /** 总审批数量 */
    @Schema(description = "总审批数量", example = "156")
    private Integer totalCount;

    /** 待审批数量 */
    @Schema(description = "待审批数量", example = "23")
    private Integer pendingCount;

    /** 已通过数量 */
    @Schema(description = "已通过数量", example = "118")
    private Integer approvedCount;

    /** 已驳回数量 */
    @Schema(description = "已驳回数量", example = "12")
    private Integer rejectedCount;

    /** 已撤回数量 */
    @Schema(description = "已撤回数量", example = "3")
    private Integer withdrawnCount;

    // ==================== 效率指标 ====================

    /**
     * 平均处理时长（单位：小时）
     * 从提交到最终完成（通过/驳回）的平均耗时
     */
    @Schema(description = "平均处理时长（小时）", example = "18.5")
    private Double avgProcessHours;

    /** 紧急审批数量 */
    @Schema(description = "紧急审批数量", example = "8")
    private Integer urgentCount;

    // ==================== 时间维度统计 ====================

    /** 本月新增审批数量 */
    @Schema(description = "本月新增审批数量", example = "32")
    private Integer thisMonthCount;

    /** 超时未处理数量（超过SLA规定时限） */
    @Schema(description = "超时未处理数量", example = "5")
    private Integer overdueCount;

    // ==================== 个人视角统计 ====================

    /** 待我审批的数量（当前登录用户作为审批人的待办数） */
    @Schema(description = "待我审批的数量", example = "7")
    private Integer myPendingReviewCount;

    /**
     * 近期通过率（百分比形式，0-100）
     * 统计最近30天的审批通过率
     */
    @Schema(description = "近期通过率（%）", example = "92.5")
    private Double recentApprovedRate;

    // ==================== 个人视角统计（别名，兼容历史调用方） ====================

    /** 我发起的审批总数（别名，与 totalCount 语义区分） */
    @Schema(description = "我发起的审批总数", example = "45")
    private Integer myTotal;

    /** 我发起的待审批数量（别名，与 pendingCount 语义区分） */
    @Schema(description = "我发起的待审批数量", example = "8")
    private Integer myPending;

    /** 我发起的已通过数量（别名，与 approvedCount 语义区分） */
    @Schema(description = "我发起的已通过数量", example = "30")
    private Integer myApproved;

    /** 我发起的已驳回数量（别名，与 rejectedCount 语义区分） */
    @Schema(description = "我发起的已驳回数量", example = "5")
    private Integer myRejected;

    /** 待我审批的数量（别名，与 myPendingReviewCount 等价） */
    @Schema(description = "待我审批的数量", example = "7")
    private Integer pendingReview;

    // ==================== Getter & Setter 方法 ====================

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Integer getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Integer getWithdrawnCount() {
        return withdrawnCount;
    }

    public void setWithdrawnCount(Integer withdrawnCount) {
        this.withdrawnCount = withdrawnCount;
    }

    public Double getAvgProcessHours() {
        return avgProcessHours;
    }

    public void setAvgProcessHours(Double avgProcessHours) {
        this.avgProcessHours = avgProcessHours;
    }

    public Integer getUrgentCount() {
        return urgentCount;
    }

    public void setUrgentCount(Integer urgentCount) {
        this.urgentCount = urgentCount;
    }

    public Integer getThisMonthCount() {
        return thisMonthCount;
    }

    public void setThisMonthCount(Integer thisMonthCount) {
        this.thisMonthCount = thisMonthCount;
    }

    public Integer getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Integer overdueCount) {
        this.overdueCount = overdueCount;
    }

    public Integer getMyPendingReviewCount() {
        return myPendingReviewCount;
    }

    public void setMyPendingReviewCount(Integer myPendingReviewCount) {
        this.myPendingReviewCount = myPendingReviewCount;
    }

    public Double getRecentApprovedRate() {
        return recentApprovedRate;
    }

    public void setRecentApprovedRate(Double recentApprovedRate) {
        this.recentApprovedRate = recentApprovedRate;
    }

    public Integer getMyTotal() {
        return myTotal;
    }

    public void setMyTotal(Integer myTotal) {
        this.myTotal = myTotal;
    }

    public Integer getMyPending() {
        return myPending;
    }

    public void setMyPending(Integer myPending) {
        this.myPending = myPending;
    }

    public Integer getMyApproved() {
        return myApproved;
    }

    public void setMyApproved(Integer myApproved) {
        this.myApproved = myApproved;
    }

    public Integer getMyRejected() {
        return myRejected;
    }

    public void setMyRejected(Integer myRejected) {
        this.myRejected = myRejected;
    }

    public Integer getPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(Integer pendingReview) {
        this.pendingReview = pendingReview;
    }
}
