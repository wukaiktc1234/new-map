package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 人工介入处理DTO
 */
public class ManualInterventionDTO {
    private Long stockinId;
    private String traceCode;
    private String interventionType;
    private BigDecimal expectedQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal difference;
    private String reason;
    private String handler;
    private Long handlerId;
    private String handlingAction;
    private String handlingRemark;
    private BigDecimal adjustedQuantity;
    public static final String TYPE_QUANTITY_MISMATCH = "QUANTITY_MISMATCH";
    public static final String TYPE_DUPLICATE_SCAN = "DUPLICATE_SCAN";
    public static final String TYPE_INVALID_CODE = "INVALID_CODE";
    public static final String TYPE_EXPIRED = "EXPIRED";
    public static final String TYPE_OTHER = "OTHER";
    public static final String ACTION_ACCEPT_DIFFERENCE = "ACCEPT_DIFFERENCE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_REPRINT = "REPRINT";
    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ACTION_MANUAL_OVERRIDE = "MANUAL_OVERRIDE";

    public ManualInterventionDTO() {
    }

    public Long getStockinId() {
        return this.stockinId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getInterventionType() {
        return this.interventionType;
    }

    public BigDecimal getExpectedQuantity() {
        return this.expectedQuantity;
    }

    public BigDecimal getActualQuantity() {
        return this.actualQuantity;
    }

    public BigDecimal getDifference() {
        return this.difference;
    }

    public String getReason() {
        return this.reason;
    }

    public String getHandler() {
        return this.handler;
    }

    public Long getHandlerId() {
        return this.handlerId;
    }

    public String getHandlingAction() {
        return this.handlingAction;
    }

    public String getHandlingRemark() {
        return this.handlingRemark;
    }

    public BigDecimal getAdjustedQuantity() {
        return this.adjustedQuantity;
    }

    public void setStockinId(final Long stockinId) {
        this.stockinId = stockinId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setInterventionType(final String interventionType) {
        this.interventionType = interventionType;
    }

    public void setExpectedQuantity(final BigDecimal expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public void setActualQuantity(final BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public void setDifference(final BigDecimal difference) {
        this.difference = difference;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public void setHandler(final String handler) {
        this.handler = handler;
    }

    public void setHandlerId(final Long handlerId) {
        this.handlerId = handlerId;
    }

    public void setHandlingAction(final String handlingAction) {
        this.handlingAction = handlingAction;
    }

    public void setHandlingRemark(final String handlingRemark) {
        this.handlingRemark = handlingRemark;
    }

    public void setAdjustedQuantity(final BigDecimal adjustedQuantity) {
        this.adjustedQuantity = adjustedQuantity;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ManualInterventionDTO)) return false;
        final ManualInterventionDTO other = (ManualInterventionDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stockinId = this.getStockinId();
        final java.lang.Object other$stockinId = other.getStockinId();
        if (this$stockinId == null ? other$stockinId != null : !this$stockinId.equals(other$stockinId)) return false;
        final java.lang.Object this$handlerId = this.getHandlerId();
        final java.lang.Object other$handlerId = other.getHandlerId();
        if (this$handlerId == null ? other$handlerId != null : !this$handlerId.equals(other$handlerId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$interventionType = this.getInterventionType();
        final java.lang.Object other$interventionType = other.getInterventionType();
        if (this$interventionType == null ? other$interventionType != null : !this$interventionType.equals(other$interventionType)) return false;
        final java.lang.Object this$expectedQuantity = this.getExpectedQuantity();
        final java.lang.Object other$expectedQuantity = other.getExpectedQuantity();
        if (this$expectedQuantity == null ? other$expectedQuantity != null : !this$expectedQuantity.equals(other$expectedQuantity)) return false;
        final java.lang.Object this$actualQuantity = this.getActualQuantity();
        final java.lang.Object other$actualQuantity = other.getActualQuantity();
        if (this$actualQuantity == null ? other$actualQuantity != null : !this$actualQuantity.equals(other$actualQuantity)) return false;
        final java.lang.Object this$difference = this.getDifference();
        final java.lang.Object other$difference = other.getDifference();
        if (this$difference == null ? other$difference != null : !this$difference.equals(other$difference)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final java.lang.Object this$handler = this.getHandler();
        final java.lang.Object other$handler = other.getHandler();
        if (this$handler == null ? other$handler != null : !this$handler.equals(other$handler)) return false;
        final java.lang.Object this$handlingAction = this.getHandlingAction();
        final java.lang.Object other$handlingAction = other.getHandlingAction();
        if (this$handlingAction == null ? other$handlingAction != null : !this$handlingAction.equals(other$handlingAction)) return false;
        final java.lang.Object this$handlingRemark = this.getHandlingRemark();
        final java.lang.Object other$handlingRemark = other.getHandlingRemark();
        if (this$handlingRemark == null ? other$handlingRemark != null : !this$handlingRemark.equals(other$handlingRemark)) return false;
        final java.lang.Object this$adjustedQuantity = this.getAdjustedQuantity();
        final java.lang.Object other$adjustedQuantity = other.getAdjustedQuantity();
        if (this$adjustedQuantity == null ? other$adjustedQuantity != null : !this$adjustedQuantity.equals(other$adjustedQuantity)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ManualInterventionDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stockinId = this.getStockinId();
        result = result * PRIME + ($stockinId == null ? 43 : $stockinId.hashCode());
        final java.lang.Object $handlerId = this.getHandlerId();
        result = result * PRIME + ($handlerId == null ? 43 : $handlerId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $interventionType = this.getInterventionType();
        result = result * PRIME + ($interventionType == null ? 43 : $interventionType.hashCode());
        final java.lang.Object $expectedQuantity = this.getExpectedQuantity();
        result = result * PRIME + ($expectedQuantity == null ? 43 : $expectedQuantity.hashCode());
        final java.lang.Object $actualQuantity = this.getActualQuantity();
        result = result * PRIME + ($actualQuantity == null ? 43 : $actualQuantity.hashCode());
        final java.lang.Object $difference = this.getDifference();
        result = result * PRIME + ($difference == null ? 43 : $difference.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final java.lang.Object $handler = this.getHandler();
        result = result * PRIME + ($handler == null ? 43 : $handler.hashCode());
        final java.lang.Object $handlingAction = this.getHandlingAction();
        result = result * PRIME + ($handlingAction == null ? 43 : $handlingAction.hashCode());
        final java.lang.Object $handlingRemark = this.getHandlingRemark();
        result = result * PRIME + ($handlingRemark == null ? 43 : $handlingRemark.hashCode());
        final java.lang.Object $adjustedQuantity = this.getAdjustedQuantity();
        result = result * PRIME + ($adjustedQuantity == null ? 43 : $adjustedQuantity.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ManualInterventionDTO(stockinId=" + this.getStockinId() + ", traceCode=" + this.getTraceCode() + ", interventionType=" + this.getInterventionType() + ", expectedQuantity=" + this.getExpectedQuantity() + ", actualQuantity=" + this.getActualQuantity() + ", difference=" + this.getDifference() + ", reason=" + this.getReason() + ", handler=" + this.getHandler() + ", handlerId=" + this.getHandlerId() + ", handlingAction=" + this.getHandlingAction() + ", handlingRemark=" + this.getHandlingRemark() + ", adjustedQuantity=" + this.getAdjustedQuantity() + ")";
    }
}
