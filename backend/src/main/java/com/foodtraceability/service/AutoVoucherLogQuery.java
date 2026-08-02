package com.foodtraceability.service;

import java.time.LocalDateTime;

/**
 * 自动凭证日志查询条件DTO
 * 用于封装分页查询凭证处理日志的条件参数
 * @author example
 * @since 2026-04-04
 */
public class AutoVoucherLogQuery {
    /**
     * 当前页码（从1开始）
     */
    private Long current = 1L;
    /**
     * 每页大小
     */
    private Long size = 10L;
    /**
     * 事件类型（可选）
     */
    private String eventType;
    /**
     * 处理状态（可选）：SUCCESS/FAILED/PENDING_REVIEW/SKIPPED/DUPLICATE
     */
    private String status;
    /**
     * 源业务单据ID（可选，支持模糊查询）
     */
    private String sourceBusinessId;
    /**
     * 源业务单据编号（可选，支持模糊查询）
     */
    private String sourceBusinessNo;
    /**
     * 规则ID（可选）
     */
    private Long ruleId;
    /**
     * 凭证ID（可选）
     */
    private Long voucherId;
    /**
     * 开始时间（可选）
     */
    private LocalDateTime startTime;
    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;
    /**
     * 操作人（可选）
     */
    private String operator;

    public AutoVoucherLogQuery() {
    }

    /**
     * 当前页码（从1开始）
     */
    public Long getCurrent() {
        return this.current;
    }

    /**
     * 每页大小
     */
    public Long getSize() {
        return this.size;
    }

    /**
     * 事件类型（可选）
     */
    public String getEventType() {
        return this.eventType;
    }

    /**
     * 处理状态（可选）：SUCCESS/FAILED/PENDING_REVIEW/SKIPPED/DUPLICATE
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * 源业务单据ID（可选，支持模糊查询）
     */
    public String getSourceBusinessId() {
        return this.sourceBusinessId;
    }

    /**
     * 源业务单据编号（可选，支持模糊查询）
     */
    public String getSourceBusinessNo() {
        return this.sourceBusinessNo;
    }

    /**
     * 规则ID（可选）
     */
    public Long getRuleId() {
        return this.ruleId;
    }

    /**
     * 凭证ID（可选）
     */
    public Long getVoucherId() {
        return this.voucherId;
    }

    /**
     * 开始时间（可选）
     */
    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    /**
     * 结束时间（可选）
     */
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    /**
     * 操作人（可选）
     */
    public String getOperator() {
        return this.operator;
    }

    /**
     * 当前页码（从1开始）
     */
    public void setCurrent(final Long current) {
        this.current = current;
    }

    /**
     * 每页大小
     */
    public void setSize(final Long size) {
        this.size = size;
    }

    /**
     * 事件类型（可选）
     */
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    /**
     * 处理状态（可选）：SUCCESS/FAILED/PENDING_REVIEW/SKIPPED/DUPLICATE
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 源业务单据ID（可选，支持模糊查询）
     */
    public void setSourceBusinessId(final String sourceBusinessId) {
        this.sourceBusinessId = sourceBusinessId;
    }

    /**
     * 源业务单据编号（可选，支持模糊查询）
     */
    public void setSourceBusinessNo(final String sourceBusinessNo) {
        this.sourceBusinessNo = sourceBusinessNo;
    }

    /**
     * 规则ID（可选）
     */
    public void setRuleId(final Long ruleId) {
        this.ruleId = ruleId;
    }

    /**
     * 凭证ID（可选）
     */
    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    /**
     * 开始时间（可选）
     */
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 结束时间（可选）
     */
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 操作人（可选）
     */
    public void setOperator(final String operator) {
        this.operator = operator;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AutoVoucherLogQuery)) return false;
        final AutoVoucherLogQuery other = (AutoVoucherLogQuery) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$current = this.getCurrent();
        final java.lang.Object other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !this$current.equals(other$current)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$ruleId = this.getRuleId();
        final java.lang.Object other$ruleId = other.getRuleId();
        if (this$ruleId == null ? other$ruleId != null : !this$ruleId.equals(other$ruleId)) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$eventType = this.getEventType();
        final java.lang.Object other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$sourceBusinessId = this.getSourceBusinessId();
        final java.lang.Object other$sourceBusinessId = other.getSourceBusinessId();
        if (this$sourceBusinessId == null ? other$sourceBusinessId != null : !this$sourceBusinessId.equals(other$sourceBusinessId)) return false;
        final java.lang.Object this$sourceBusinessNo = this.getSourceBusinessNo();
        final java.lang.Object other$sourceBusinessNo = other.getSourceBusinessNo();
        if (this$sourceBusinessNo == null ? other$sourceBusinessNo != null : !this$sourceBusinessNo.equals(other$sourceBusinessNo)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AutoVoucherLogQuery;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $current = this.getCurrent();
        result = result * PRIME + ($current == null ? 43 : $current.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $ruleId = this.getRuleId();
        result = result * PRIME + ($ruleId == null ? 43 : $ruleId.hashCode());
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $eventType = this.getEventType();
        result = result * PRIME + ($eventType == null ? 43 : $eventType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $sourceBusinessId = this.getSourceBusinessId();
        result = result * PRIME + ($sourceBusinessId == null ? 43 : $sourceBusinessId.hashCode());
        final java.lang.Object $sourceBusinessNo = this.getSourceBusinessNo();
        result = result * PRIME + ($sourceBusinessNo == null ? 43 : $sourceBusinessNo.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "AutoVoucherLogQuery(current=" + this.getCurrent() + ", size=" + this.getSize() + ", eventType=" + this.getEventType() + ", status=" + this.getStatus() + ", sourceBusinessId=" + this.getSourceBusinessId() + ", sourceBusinessNo=" + this.getSourceBusinessNo() + ", ruleId=" + this.getRuleId() + ", voucherId=" + this.getVoucherId() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", operator=" + this.getOperator() + ")";
    }
}
