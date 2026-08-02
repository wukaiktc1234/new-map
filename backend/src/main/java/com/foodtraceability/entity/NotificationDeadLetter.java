package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 通知死信实体类
 * 持久化 RabbitMQ 死信队列中的消息，便于查询和重试
 * 对应 spec NC-006（死信队列处理）和 plan.md ADR-003（死信处理策略）
 */
@TableName("notification_dead_letter")
@Schema(description = "通知死信实体")
public class NotificationDeadLetter {

    @TableId(value = "dead_letter_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long deadLetterId;

    @TableField("original_record_id")
    @Schema(description = "原始msg_send_record.record_id")
    private Long originalRecordId;

    @TableField("template_id")
    @Schema(description = "关联模板ID")
    private Long templateId;

    @TableField("template_code")
    @Schema(description = "关联模板编码")
    private String templateCode;

    @TableField("recipient")
    @Schema(description = "收件人(邮箱/用户ID/手机号)")
    private String recipient;

    @TableField("recipient_type")
    @Schema(description = "收件人类型:1=EMAIL 2=USER_ID 3=PHONE")
    private Integer recipientType;

    @TableField("subject")
    @Schema(description = "发送主题")
    private String subject;

    @TableField("content")
    @Schema(description = "发送内容")
    private String content;

    @TableField("channel")
    @Schema(description = "发送渠道:EMAIL/SITE_MSG/SMS/WEBHOOK")
    private String channel;

    @TableField("send_status")
    @Schema(description = "发送状态:3=FAILED")
    private Integer sendStatus;

    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    @TableField("retry_count")
    @Schema(description = "已重试次数")
    private Integer retryCount;

    @TableField("max_retry")
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @TableField("original_message_id")
    @Schema(description = "RabbitMQ原始消息ID")
    private String originalMessageId;

    @TableField("biz_type")
    @Schema(description = "业务类型标识")
    private String bizType;

    @TableField("biz_id")
    @Schema(description = "业务关联ID")
    private String bizId;

    @TableField("dead_letter_reason")
    @Schema(description = "死信原因(超过最大重试次数等)")
    private String deadLetterReason;

    @TableField("dead_letter_time")
    @Schema(description = "进入死信时间")
    private LocalDateTime deadLetterTime;

    @TableField("resolved")
    @Schema(description = "是否已处理:0=未处理 1=已处理")
    private Integer resolved;

    @TableField("resolved_by")
    @Schema(description = "处理人用户ID")
    private Long resolvedBy;

    @TableField("resolved_time")
    @Schema(description = "处理时间")
    private LocalDateTime resolvedTime;

    @TableField("resolve_remark")
    @Schema(description = "处理备注")
    private String resolveRemark;

    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记:0=未删除 1=已删除")
    private Integer deleted;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public NotificationDeadLetter() {
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

    public LocalDateTime getResolvedTime() {
        return this.resolvedTime;
    }

    public String getResolveRemark() {
        return this.resolveRemark;
    }

    public Long getVersion() {
        return this.version;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
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

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public void setResolveRemark(String resolveRemark) {
        this.resolveRemark = resolveRemark;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "NotificationDeadLetter(deadLetterId=" + this.deadLetterId
                + ", originalRecordId=" + this.originalRecordId
                + ", templateCode=" + this.templateCode
                + ", recipient=" + this.recipient
                + ", channel=" + this.channel
                + ", sendStatus=" + this.sendStatus
                + ", retryCount=" + this.retryCount
                + ", deadLetterReason=" + this.deadLetterReason
                + ", resolved=" + this.resolved
                + ", deadLetterTime=" + this.deadLetterTime + ")";
    }
}
