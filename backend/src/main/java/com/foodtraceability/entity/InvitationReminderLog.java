package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 邀请码提醒记录实体类
 * 对应数据库表：invitation_reminder_log
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@TableName("invitation_reminder_log")
@Schema(description = "邀请码提醒记录实体")
public class InvitationReminderLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "提醒记录ID")
    private Long id;
    @Schema(description = "邀请码ID")
    private Long invitationCodeId;
    @Schema(description = "档案ID")
    private Long archiveId;
    @Schema(description = "提醒类型：EXPIRING_24H-24小时内过期, EXPIRED-已过期, NOT_REGISTERED-未注册提醒")
    private String reminderType;
    @Schema(description = "接收人ID")
    private Long recipientId;
    @Schema(description = "接收人姓名")
    private String recipientName;
    @Schema(description = "接收人类型：MANAGER-部门主管, HR-HR专员, CANDIDATE-候选人")
    private String recipientType;
    @Schema(description = "发送方式：EMAIL, SMS, SYSTEM-系统通知")
    private String sendMethod;
    @Schema(description = "发送状态：PENDING-待发送, SENT-已发送, FAILED-发送失败")
    private String sendStatus;
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
    @Schema(description = "阅读状态：UNREAD-未读, READ-已读")
    private String readStatus;
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
    @Schema(description = "发送失败原因")
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    // 提醒类型常量
    public static final String TYPE_EXPIRING_24H = "EXPIRING_24H";
    public static final String TYPE_EXPIRED = "EXPIRED";
    public static final String TYPE_NOT_REGISTERED = "NOT_REGISTERED";
    // 接收人类型常量
    public static final String RECIPIENT_MANAGER = "MANAGER";
    public static final String RECIPIENT_HR = "HR";
    public static final String RECIPIENT_CANDIDATE = "CANDIDATE";
    // 发送方式常量
    public static final String METHOD_EMAIL = "EMAIL";
    public static final String METHOD_SMS = "SMS";
    public static final String METHOD_SYSTEM = "SYSTEM";
    // 发送状态常量
    public static final String SEND_STATUS_PENDING = "PENDING";
    public static final String SEND_STATUS_SENT = "SENT";
    public static final String SEND_STATUS_FAILED = "FAILED";
    // 阅读状态常量
    public static final String READ_STATUS_UNREAD = "UNREAD";
    public static final String READ_STATUS_READ = "READ";

    public InvitationReminderLog() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getInvitationCodeId() {
        return this.invitationCodeId;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public String getReminderType() {
        return this.reminderType;
    }

    public Long getRecipientId() {
        return this.recipientId;
    }

    public String getRecipientName() {
        return this.recipientName;
    }

    public String getRecipientType() {
        return this.recipientType;
    }

    public String getSendMethod() {
        return this.sendMethod;
    }

    public String getSendStatus() {
        return this.sendStatus;
    }

    public LocalDateTime getSendTime() {
        return this.sendTime;
    }

    public String getReadStatus() {
        return this.readStatus;
    }

    public LocalDateTime getReadTime() {
        return this.readTime;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setInvitationCodeId(final Long invitationCodeId) {
        this.invitationCodeId = invitationCodeId;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setReminderType(final String reminderType) {
        this.reminderType = reminderType;
    }

    public void setRecipientId(final Long recipientId) {
        this.recipientId = recipientId;
    }

    public void setRecipientName(final String recipientName) {
        this.recipientName = recipientName;
    }

    public void setRecipientType(final String recipientType) {
        this.recipientType = recipientType;
    }

    public void setSendMethod(final String sendMethod) {
        this.sendMethod = sendMethod;
    }

    public void setSendStatus(final String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setSendTime(final LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public void setReadStatus(final String readStatus) {
        this.readStatus = readStatus;
    }

    public void setReadTime(final LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvitationReminderLog)) return false;
        final InvitationReminderLog other = (InvitationReminderLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$invitationCodeId = this.getInvitationCodeId();
        final java.lang.Object other$invitationCodeId = other.getInvitationCodeId();
        if (this$invitationCodeId == null ? other$invitationCodeId != null : !this$invitationCodeId.equals(other$invitationCodeId)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$recipientId = this.getRecipientId();
        final java.lang.Object other$recipientId = other.getRecipientId();
        if (this$recipientId == null ? other$recipientId != null : !this$recipientId.equals(other$recipientId)) return false;
        final java.lang.Object this$reminderType = this.getReminderType();
        final java.lang.Object other$reminderType = other.getReminderType();
        if (this$reminderType == null ? other$reminderType != null : !this$reminderType.equals(other$reminderType)) return false;
        final java.lang.Object this$recipientName = this.getRecipientName();
        final java.lang.Object other$recipientName = other.getRecipientName();
        if (this$recipientName == null ? other$recipientName != null : !this$recipientName.equals(other$recipientName)) return false;
        final java.lang.Object this$recipientType = this.getRecipientType();
        final java.lang.Object other$recipientType = other.getRecipientType();
        if (this$recipientType == null ? other$recipientType != null : !this$recipientType.equals(other$recipientType)) return false;
        final java.lang.Object this$sendMethod = this.getSendMethod();
        final java.lang.Object other$sendMethod = other.getSendMethod();
        if (this$sendMethod == null ? other$sendMethod != null : !this$sendMethod.equals(other$sendMethod)) return false;
        final java.lang.Object this$sendStatus = this.getSendStatus();
        final java.lang.Object other$sendStatus = other.getSendStatus();
        if (this$sendStatus == null ? other$sendStatus != null : !this$sendStatus.equals(other$sendStatus)) return false;
        final java.lang.Object this$sendTime = this.getSendTime();
        final java.lang.Object other$sendTime = other.getSendTime();
        if (this$sendTime == null ? other$sendTime != null : !this$sendTime.equals(other$sendTime)) return false;
        final java.lang.Object this$readStatus = this.getReadStatus();
        final java.lang.Object other$readStatus = other.getReadStatus();
        if (this$readStatus == null ? other$readStatus != null : !this$readStatus.equals(other$readStatus)) return false;
        final java.lang.Object this$readTime = this.getReadTime();
        final java.lang.Object other$readTime = other.getReadTime();
        if (this$readTime == null ? other$readTime != null : !this$readTime.equals(other$readTime)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvitationReminderLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $invitationCodeId = this.getInvitationCodeId();
        result = result * PRIME + ($invitationCodeId == null ? 43 : $invitationCodeId.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $recipientId = this.getRecipientId();
        result = result * PRIME + ($recipientId == null ? 43 : $recipientId.hashCode());
        final java.lang.Object $reminderType = this.getReminderType();
        result = result * PRIME + ($reminderType == null ? 43 : $reminderType.hashCode());
        final java.lang.Object $recipientName = this.getRecipientName();
        result = result * PRIME + ($recipientName == null ? 43 : $recipientName.hashCode());
        final java.lang.Object $recipientType = this.getRecipientType();
        result = result * PRIME + ($recipientType == null ? 43 : $recipientType.hashCode());
        final java.lang.Object $sendMethod = this.getSendMethod();
        result = result * PRIME + ($sendMethod == null ? 43 : $sendMethod.hashCode());
        final java.lang.Object $sendStatus = this.getSendStatus();
        result = result * PRIME + ($sendStatus == null ? 43 : $sendStatus.hashCode());
        final java.lang.Object $sendTime = this.getSendTime();
        result = result * PRIME + ($sendTime == null ? 43 : $sendTime.hashCode());
        final java.lang.Object $readStatus = this.getReadStatus();
        result = result * PRIME + ($readStatus == null ? 43 : $readStatus.hashCode());
        final java.lang.Object $readTime = this.getReadTime();
        result = result * PRIME + ($readTime == null ? 43 : $readTime.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvitationReminderLog(id=" + this.getId() + ", invitationCodeId=" + this.getInvitationCodeId() + ", archiveId=" + this.getArchiveId() + ", reminderType=" + this.getReminderType() + ", recipientId=" + this.getRecipientId() + ", recipientName=" + this.getRecipientName() + ", recipientType=" + this.getRecipientType() + ", sendMethod=" + this.getSendMethod() + ", sendStatus=" + this.getSendStatus() + ", sendTime=" + this.getSendTime() + ", readStatus=" + this.getReadStatus() + ", readTime=" + this.getReadTime() + ", errorMessage=" + this.getErrorMessage() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
