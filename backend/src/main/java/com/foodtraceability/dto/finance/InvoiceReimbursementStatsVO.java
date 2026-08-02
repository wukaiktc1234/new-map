package com.foodtraceability.dto.finance;

/**
 * 发票报销统计VO
 *
 * <p>Sprint 3.1 P0 F-005：用于 InvoiceReimbursementController.getStats 返回，
 * 与 FinanceStatisticsVO（财务概览）解耦。按查询条件聚合统计报销单数量与金额。</p>
 *
 * <p>字段语义：</p>
 * <ul>
 *   <li>数量字段：按状态分类统计报销单数量</li>
 *   <li>金额字段：单位均为分（Long），通过前端 converter 转元展示</li>
 *   <li>totalAmount：有效报销单（非取消/非拒绝）的 totalAmount 之和</li>
 *   <li>approvedAmount：已审批+已付款的 approvedAmount 之和</li>
 *   <li>paidAmount：已付款的 approvedAmount 之和</li>
 * </ul>
 */
public class InvoiceReimbursementStatsVO {

    /** 总数（含所有状态） */
    private Long totalCount;

    /** 待审核数量（status=0 草稿） */
    private Long pendingCount;

    /** 已审批数量（status=1） */
    private Long approvedCount;

    /** 已付款数量（status=2） */
    private Long paidCount;

    /** 已取消数量（status=3） */
    private Long cancelledCount;

    /** 已拒绝数量（status=4） */
    private Long rejectedCount;

    /** 报销总金额（单位：分，有效单据 totalAmount 之和） */
    private Long totalAmount;

    /** 已审批金额（单位：分，已审批+已付款 approvedAmount 之和） */
    private Long approvedAmount;

    /** 已付款金额（单位：分，已付款 approvedAmount 之和） */
    private Long paidAmount;

    /** 统计周期（如 "2026-06"） */
    private String period;

    public InvoiceReimbursementStatsVO() {
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Long getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(Long paidCount) {
        this.paidCount = paidCount;
    }

    public Long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public Long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Long approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
