package com.foodtraceability.dto;

import java.time.LocalDateTime;

/**
 * 死信视图对象（VO）
 * 用于死信详情和列表展示，包含处理人用户名（关联查询）
 * 对应 plan.md 第 5.1.3 节
 */
public class DeadLetterVO {

    private Long deadLetterId;
    private Long originalRecordId;
    private Long templateId;
    private String templateCode;
    private String recipient;
    private Integer recipientType;
    private String subject;
    private String content;
    private String channel;
    private Integer sendStatus;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetry;
    private String originalMessageId;
    private String bizType;
    private String bizId;
    private String deadLetterReason;
    private LocalDateTime deadLetterTime;
    private Integer resolved;
    private Long resolvedBy;
    /** 处理人用户名（关联 user 表查询） */
    private String resolvedByUsername;
    private LocalDateTime resolvedTime;
    private String resolveRemark;
    private LocalDateTime createTime;

    public DeadLetterVO() {
    }

    public Long getDeadLetterId() {
        return this.deadLetterId;
    }

    public Long getOriginalRecordId() {
        return this.originalRecordId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public Integer getRecipientType() {
        return this.recipientType;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getContent() {
        return this.content;
    }

    public String getChannel() {
        return this.channel;
    }

    public Integer getSendStatus() {
        return this.sendStatus;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Integer getRetryCount() {
        return this.retryCount;
    }

    public Integer getMaxRetry() {
        return this.maxRetry;
    }

    public String getOriginalMessageId() {
        return this.originalMessageId;
    }

    public String getBizType() {
        return this.bizType;
    }

    public String getBizId() {
        return this.bizId;
    }

    public String getDeadLetterReason() {
        return this.deadLetterReason;
    }

    public LocalDateTime getDeadLetterTime() {
        return this.deadLetterTime;
    }

    public Integer getResolved() {
        return this.resolved;
    }

    public Long getResolvedBy() {
        return this.resolvedBy;
    }

    public String getResolvedByUsername() {
        return this.resolvedByUsername;
    }

    public LocalDateTime getResolvedTime() {
        return this.resolvedTime;
    }

    public String getResolveRemark() {
        return this.resolveRemark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setDeadLetterId(Long deadLetterId) {
        this.deadLetterId = deadLetterId;
    }

    public void setOriginalRecordId(Long originalRecordId) {
        this.originalRecordId = originalRecordId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setRecipientType(Integer recipientType) {
        this.recipientType = recipientType;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setSendStatus(Integer sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public void setDeadLetterReason(String deadLetterReason) {
        this.deadLetterReason = deadLetterReason;
    }

    public void setDeadLetterTime(LocalDateTime deadLetterTime) {
        this.deadLetterTime = deadLetterTime;
    }

    public void setResolved(Integer resolved) {
        this.resolved = resolved;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public void setResolvedByUsername(String resolvedByUsername) {
        this.resolvedByUsername = resolvedByUsername;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public void setResolveRemark(String resolveRemark) {
        this.resolveRemark = resolveRemark;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
