package com.foodtraceability.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 追溯码生成事件
 * 触发：追溯码打印、状态记录
 */
public class TraceCodeGeneratedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private String traceCodeType;
    private String traceCodeId;
    private String traceCode;
    private String qrCodeUrl;
    private String productId;
    private String productName;
    private String sourceType;
    private String sourceId;
    private Long storeId;
    private String storeName;
    private LocalDateTime generateTime;
    private String operatorName;
    private LocalDateTime eventTime;

    public TraceCodeGeneratedEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() {
        return this.eventId;
    }

    public String getTraceCodeType() {
        return this.traceCodeType;
    }

    public String getTraceCodeId() {
        return this.traceCodeId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getQrCodeUrl() {
        return this.qrCodeUrl;
    }

    public String getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getSourceType() {
        return this.sourceType;
    }

    public String getSourceId() {
        return this.sourceId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public LocalDateTime getGenerateTime() {
        return this.generateTime;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getEventTime() {
        return this.eventTime;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public void setTraceCodeType(final String traceCodeType) {
        this.traceCodeType = traceCodeType;
    }

    public void setTraceCodeId(final String traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setQrCodeUrl(final String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public void setProductId(final String productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setSourceType(final String sourceType) {
        this.sourceType = sourceType;
    }

    public void setSourceId(final String sourceId) {
        this.sourceId = sourceId;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setGenerateTime(final LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setEventTime(final LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TraceCodeGeneratedEvent)) return false;
        final TraceCodeGeneratedEvent other = (TraceCodeGeneratedEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$traceCodeType = this.getTraceCodeType();
        final java.lang.Object other$traceCodeType = other.getTraceCodeType();
        if (this$traceCodeType == null ? other$traceCodeType != null : !this$traceCodeType.equals(other$traceCodeType)) return false;
        final java.lang.Object this$traceCodeId = this.getTraceCodeId();
        final java.lang.Object other$traceCodeId = other.getTraceCodeId();
        if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$qrCodeUrl = this.getQrCodeUrl();
        final java.lang.Object other$qrCodeUrl = other.getQrCodeUrl();
        if (this$qrCodeUrl == null ? other$qrCodeUrl != null : !this$qrCodeUrl.equals(other$qrCodeUrl)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$sourceType = this.getSourceType();
        final java.lang.Object other$sourceType = other.getSourceType();
        if (this$sourceType == null ? other$sourceType != null : !this$sourceType.equals(other$sourceType)) return false;
        final java.lang.Object this$sourceId = this.getSourceId();
        final java.lang.Object other$sourceId = other.getSourceId();
        if (this$sourceId == null ? other$sourceId != null : !this$sourceId.equals(other$sourceId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$generateTime = this.getGenerateTime();
        final java.lang.Object other$generateTime = other.getGenerateTime();
        if (this$generateTime == null ? other$generateTime != null : !this$generateTime.equals(other$generateTime)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$eventTime = this.getEventTime();
        final java.lang.Object other$eventTime = other.getEventTime();
        if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TraceCodeGeneratedEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $traceCodeType = this.getTraceCodeType();
        result = result * PRIME + ($traceCodeType == null ? 43 : $traceCodeType.hashCode());
        final java.lang.Object $traceCodeId = this.getTraceCodeId();
        result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $qrCodeUrl = this.getQrCodeUrl();
        result = result * PRIME + ($qrCodeUrl == null ? 43 : $qrCodeUrl.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $sourceType = this.getSourceType();
        result = result * PRIME + ($sourceType == null ? 43 : $sourceType.hashCode());
        final java.lang.Object $sourceId = this.getSourceId();
        result = result * PRIME + ($sourceId == null ? 43 : $sourceId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $generateTime = this.getGenerateTime();
        result = result * PRIME + ($generateTime == null ? 43 : $generateTime.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $eventTime = this.getEventTime();
        result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TraceCodeGeneratedEvent(eventId=" + this.getEventId() + ", traceCodeType=" + this.getTraceCodeType() + ", traceCodeId=" + this.getTraceCodeId() + ", traceCode=" + this.getTraceCode() + ", qrCodeUrl=" + this.getQrCodeUrl() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", sourceType=" + this.getSourceType() + ", sourceId=" + this.getSourceId() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", generateTime=" + this.getGenerateTime() + ", operatorName=" + this.getOperatorName() + ", eventTime=" + this.getEventTime() + ")";
    }
}
