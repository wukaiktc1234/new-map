package com.foodtraceability.dto;

import java.time.LocalDateTime;

/**
 * 死信基本信息（用于列表展示的精简版）
 * 对应 plan.md 第 5.1.4 节
 */
public class DeadLetterBasicInfo {

    private Long deadLetterId;
    private String templateCode;
    private String recipient;
    private String channel;
    private Integer sendStatus;
    private Integer retryCount;
    private Integer resolved;
    private LocalDateTime deadLetterTime;

    public DeadLetterBasicInfo() {
    }

    public Long getDeadLetterId() {
        return this.deadLetterId;
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public String getChannel() {
        return this.channel;
    }

    public Integer getSendStatus() {
        return this.sendStatus;
    }

    public Integer getRetryCount() {
        return this.retryCount;
    }

    public Integer getResolved() {
        return this.resolved;
    }

    public LocalDateTime getDeadLetterTime() {
        return this.deadLetterTime;
    }

    public void setDeadLetterId(Long deadLetterId) {
        this.deadLetterId = deadLetterId;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setSendStatus(Integer sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setResolved(Integer resolved) {
        this.resolved = resolved;
    }

    public void setDeadLetterTime(LocalDateTime deadLetterTime) {
        this.deadLetterTime = deadLetterTime;
    }
}
