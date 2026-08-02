package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 消息发送记录实体类
 * 记录所有消息的发送状态、重试信息和审计追踪
 * 支持多通道发送：EMAIL、SITE_MSG、SMS、WEBHOOK
 */
@TableName("msg_send_record")
@Schema(description = "消息发送记录实体")
public class MsgSendRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long recordId;
    @TableField("template_id")
    @Schema(description = "关联模板ID")
    private Long templateId;
    @TableField("template_code")
    @Schema(description = "关联模板编码")
    private String templateCode;
    @TableField("template_name")
    @Schema(description = "关联模板名称")
    private String templateName;
    /**
     * 收件人地址/ID（邮箱/用户ID/手机号）
     */
    @TableField("recipient")
    @Schema(description = "收件人(邮箱/用户ID/手机号)")
    private String recipient;
    /**
     * 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
     */
    @TableField("recipient_type")
    @Schema(description = "收件人类型:1=EMAIL 2=USER_ID 3=PHONE")
    private Integer recipientType;
    @TableField("subject")
    @Schema(description = "实际发送主题")
    private String subject;
    @TableField("content")
    @Schema(description = "实际发送内容")
    private String content;
    @TableField("channel")
    @Schema(description = "发送渠道")
    private String channel;
    /**
     * 发送状态: 0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL
     */
    @TableField("send_status")
    @Schema(description = "发送状态:0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL")
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
    @TableField("send_time")
    @Schema(description = "开始发送时间")
    private LocalDateTime sendTime;
    @TableField("finish_time")
    @Schema(description = "完成时间")
    private LocalDateTime finishTime;
    /**
     * 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
     */
    @TableField("trigger_type")
    @Schema(description = "触发类型:1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT")
    private Integer triggerType;
    @TableField("biz_type")
    @Schema(description = "业务类型标识")
    private String bizType;
    @TableField("biz_id")
    @Schema(description = "业务关联ID")
    private String bizId;
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
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private Long createUserId;
    @TableField("create_username")
    @Schema(description = "创建用户名")
    private String createUsername;
    /**
     * 关联 notification 表 ID（SITE_MSG 渠道专用，消除双系统割裂）
     * spec F-003: SITE_MSG 渠道发送后回填此字段建立关联
     */
    @TableField("notification_id")
    @Schema(description = "关联notification表ID(SITE_MSG渠道专用)")
    private Long notificationId;

    public MsgSendRecord() {
    }

    public Long getRecordId() {
        return this.recordId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    /**
     * 收件人地址/ID（邮箱/用户ID/手机号）
     */
    public String getRecipient() {
        return this.recipient;
    }

    /**
     * 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
     */
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

    /**
     * 发送状态: 0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL
     */
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

    public LocalDateTime getSendTime() {
        return this.sendTime;
    }

    public LocalDateTime getFinishTime() {
        return this.finishTime;
    }

    /**
     * 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
     */
    public Integer getTriggerType() {
        return this.triggerType;
    }

    public String getBizType() {
        return this.bizType;
    }

    public String getBizId() {
        return this.bizId;
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

    public Long getCreateUserId() {
        return this.createUserId;
    }

    public String getCreateUsername() {
        return this.createUsername;
    }

    /**
     * 关联 notification 表 ID（SITE_MSG 渠道专用，消除双系统割裂）
     */
    public Long getNotificationId() {
        return this.notificationId;
    }

    public void setRecordId(final Long recordId) {
        this.recordId = recordId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    /**
     * 收件人地址/ID（邮箱/用户ID/手机号）
     */
    public void setRecipient(final String recipient) {
        this.recipient = recipient;
    }

    /**
     * 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
     */
    public void setRecipientType(final Integer recipientType) {
        this.recipientType = recipientType;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    /**
     * 发送状态: 0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL
     */
    public void setSendStatus(final Integer sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void setMaxRetry(final Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void setSendTime(final LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public void setFinishTime(final LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
     */
    public void setTriggerType(final Integer triggerType) {
        this.triggerType = triggerType;
    }

    public void setBizType(final String bizType) {
        this.bizType = bizType;
    }

    public void setBizId(final String bizId) {
        this.bizId = bizId;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setCreateUserId(final Long createUserId) {
        this.createUserId = createUserId;
    }

    public void setCreateUsername(final String createUsername) {
        this.createUsername = createUsername;
    }

    /**
     * 关联 notification 表 ID（SITE_MSG 渠道专用，消除双系统割裂）
     */
    public void setNotificationId(final Long notificationId) {
        this.notificationId = notificationId;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MsgSendRecord)) return false;
        final MsgSendRecord other = (MsgSendRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$recordId = this.getRecordId();
        final java.lang.Object other$recordId = other.getRecordId();
        if (this$recordId == null ? other$recordId != null : !this$recordId.equals(other$recordId)) return false;
        final java.lang.Object this$templateId = this.getTemplateId();
        final java.lang.Object other$templateId = other.getTemplateId();
        if (this$templateId == null ? other$templateId != null : !this$templateId.equals(other$templateId)) return false;
        final java.lang.Object this$recipientType = this.getRecipientType();
        final java.lang.Object other$recipientType = other.getRecipientType();
        if (this$recipientType == null ? other$recipientType != null : !this$recipientType.equals(other$recipientType)) return false;
        final java.lang.Object this$sendStatus = this.getSendStatus();
        final java.lang.Object other$sendStatus = other.getSendStatus();
        if (this$sendStatus == null ? other$sendStatus != null : !this$sendStatus.equals(other$sendStatus)) return false;
        final java.lang.Object this$retryCount = this.getRetryCount();
        final java.lang.Object other$retryCount = other.getRetryCount();
        if (this$retryCount == null ? other$retryCount != null : !this$retryCount.equals(other$retryCount)) return false;
        final java.lang.Object this$maxRetry = this.getMaxRetry();
        final java.lang.Object other$maxRetry = other.getMaxRetry();
        if (this$maxRetry == null ? other$maxRetry != null : !this$maxRetry.equals(other$maxRetry)) return false;
        final java.lang.Object this$triggerType = this.getTriggerType();
        final java.lang.Object other$triggerType = other.getTriggerType();
        if (this$triggerType == null ? other$triggerType != null : !this$triggerType.equals(other$triggerType)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$createUserId = this.getCreateUserId();
        final java.lang.Object other$createUserId = other.getCreateUserId();
        if (this$createUserId == null ? other$createUserId != null : !this$createUserId.equals(other$createUserId)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) return false;
        final java.lang.Object this$recipient = this.getRecipient();
        final java.lang.Object other$recipient = other.getRecipient();
        if (this$recipient == null ? other$recipient != null : !this$recipient.equals(other$recipient)) return false;
        final java.lang.Object this$subject = this.getSubject();
        final java.lang.Object other$subject = other.getSubject();
        if (this$subject == null ? other$subject != null : !this$subject.equals(other$subject)) return false;
        final java.lang.Object this$content = this.getContent();
        final java.lang.Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        final java.lang.Object this$channel = this.getChannel();
        final java.lang.Object other$channel = other.getChannel();
        if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$sendTime = this.getSendTime();
        final java.lang.Object other$sendTime = other.getSendTime();
        if (this$sendTime == null ? other$sendTime != null : !this$sendTime.equals(other$sendTime)) return false;
        final java.lang.Object this$finishTime = this.getFinishTime();
        final java.lang.Object other$finishTime = other.getFinishTime();
        if (this$finishTime == null ? other$finishTime != null : !this$finishTime.equals(other$finishTime)) return false;
        final java.lang.Object this$bizType = this.getBizType();
        final java.lang.Object other$bizType = other.getBizType();
        if (this$bizType == null ? other$bizType != null : !this$bizType.equals(other$bizType)) return false;
        final java.lang.Object this$bizId = this.getBizId();
        final java.lang.Object other$bizId = other.getBizId();
        if (this$bizId == null ? other$bizId != null : !this$bizId.equals(other$bizId)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$createUsername = this.getCreateUsername();
        final java.lang.Object other$createUsername = other.getCreateUsername();
        if (this$createUsername == null ? other$createUsername != null : !this$createUsername.equals(other$createUsername)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MsgSendRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $recordId = this.getRecordId();
        result = result * PRIME + ($recordId == null ? 43 : $recordId.hashCode());
        final java.lang.Object $templateId = this.getTemplateId();
        result = result * PRIME + ($templateId == null ? 43 : $templateId.hashCode());
        final java.lang.Object $recipientType = this.getRecipientType();
        result = result * PRIME + ($recipientType == null ? 43 : $recipientType.hashCode());
        final java.lang.Object $sendStatus = this.getSendStatus();
        result = result * PRIME + ($sendStatus == null ? 43 : $sendStatus.hashCode());
        final java.lang.Object $retryCount = this.getRetryCount();
        result = result * PRIME + ($retryCount == null ? 43 : $retryCount.hashCode());
        final java.lang.Object $maxRetry = this.getMaxRetry();
        result = result * PRIME + ($maxRetry == null ? 43 : $maxRetry.hashCode());
        final java.lang.Object $triggerType = this.getTriggerType();
        result = result * PRIME + ($triggerType == null ? 43 : $triggerType.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $createUserId = this.getCreateUserId();
        result = result * PRIME + ($createUserId == null ? 43 : $createUserId.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $recipient = this.getRecipient();
        result = result * PRIME + ($recipient == null ? 43 : $recipient.hashCode());
        final java.lang.Object $subject = this.getSubject();
        result = result * PRIME + ($subject == null ? 43 : $subject.hashCode());
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $channel = this.getChannel();
        result = result * PRIME + ($channel == null ? 43 : $channel.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $sendTime = this.getSendTime();
        result = result * PRIME + ($sendTime == null ? 43 : $sendTime.hashCode());
        final java.lang.Object $finishTime = this.getFinishTime();
        result = result * PRIME + ($finishTime == null ? 43 : $finishTime.hashCode());
        final java.lang.Object $bizType = this.getBizType();
        result = result * PRIME + ($bizType == null ? 43 : $bizType.hashCode());
        final java.lang.Object $bizId = this.getBizId();
        result = result * PRIME + ($bizId == null ? 43 : $bizId.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $createUsername = this.getCreateUsername();
        result = result * PRIME + ($createUsername == null ? 43 : $createUsername.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MsgSendRecord(recordId=" + this.getRecordId() + ", templateId=" + this.getTemplateId() + ", templateCode=" + this.getTemplateCode() + ", templateName=" + this.getTemplateName() + ", recipient=" + this.getRecipient() + ", recipientType=" + this.getRecipientType() + ", subject=" + this.getSubject() + ", content=" + this.getContent() + ", channel=" + this.getChannel() + ", sendStatus=" + this.getSendStatus() + ", errorMessage=" + this.getErrorMessage() + ", retryCount=" + this.getRetryCount() + ", maxRetry=" + this.getMaxRetry() + ", sendTime=" + this.getSendTime() + ", finishTime=" + this.getFinishTime() + ", triggerType=" + this.getTriggerType() + ", bizType=" + this.getBizType() + ", bizId=" + this.getBizId() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createTime=" + this.getCreateTime() + ", createUserId=" + this.getCreateUserId() + ", createUsername=" + this.getCreateUsername() + ")";
    }
}
