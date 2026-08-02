package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 发送邀请码DTO
 */
@Schema(description = "发送邀请码DTO")
public class SendInvitationCodeDTO {
    @Schema(description = "邀请码列表")
    private List<String> codes;
    @Schema(description = "发送方式：email-邮件, sms-短信", example = "email")
    private String sendType;
    @Schema(description = "收件人列表（邮箱或手机号）")
    private List<String> recipients;
    @Schema(description = "邮件标题")
    private String subject;
    @Schema(description = "自定义消息内容")
    private String customMessage;
    @Schema(description = "是否包含入职指引", example = "true")
    private Boolean includeGuide;

    public SendInvitationCodeDTO() {
    }

    public List<String> getCodes() {
        return this.codes;
    }

    public String getSendType() {
        return this.sendType;
    }

    public List<String> getRecipients() {
        return this.recipients;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getCustomMessage() {
        return this.customMessage;
    }

    public Boolean getIncludeGuide() {
        return this.includeGuide;
    }

    public void setCodes(final List<String> codes) {
        this.codes = codes;
    }

    public void setSendType(final String sendType) {
        this.sendType = sendType;
    }

    public void setRecipients(final List<String> recipients) {
        this.recipients = recipients;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public void setCustomMessage(final String customMessage) {
        this.customMessage = customMessage;
    }

    public void setIncludeGuide(final Boolean includeGuide) {
        this.includeGuide = includeGuide;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SendInvitationCodeDTO)) return false;
        final SendInvitationCodeDTO other = (SendInvitationCodeDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$includeGuide = this.getIncludeGuide();
        final java.lang.Object other$includeGuide = other.getIncludeGuide();
        if (this$includeGuide == null ? other$includeGuide != null : !this$includeGuide.equals(other$includeGuide)) return false;
        final java.lang.Object this$codes = this.getCodes();
        final java.lang.Object other$codes = other.getCodes();
        if (this$codes == null ? other$codes != null : !this$codes.equals(other$codes)) return false;
        final java.lang.Object this$sendType = this.getSendType();
        final java.lang.Object other$sendType = other.getSendType();
        if (this$sendType == null ? other$sendType != null : !this$sendType.equals(other$sendType)) return false;
        final java.lang.Object this$recipients = this.getRecipients();
        final java.lang.Object other$recipients = other.getRecipients();
        if (this$recipients == null ? other$recipients != null : !this$recipients.equals(other$recipients)) return false;
        final java.lang.Object this$subject = this.getSubject();
        final java.lang.Object other$subject = other.getSubject();
        if (this$subject == null ? other$subject != null : !this$subject.equals(other$subject)) return false;
        final java.lang.Object this$customMessage = this.getCustomMessage();
        final java.lang.Object other$customMessage = other.getCustomMessage();
        if (this$customMessage == null ? other$customMessage != null : !this$customMessage.equals(other$customMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SendInvitationCodeDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $includeGuide = this.getIncludeGuide();
        result = result * PRIME + ($includeGuide == null ? 43 : $includeGuide.hashCode());
        final java.lang.Object $codes = this.getCodes();
        result = result * PRIME + ($codes == null ? 43 : $codes.hashCode());
        final java.lang.Object $sendType = this.getSendType();
        result = result * PRIME + ($sendType == null ? 43 : $sendType.hashCode());
        final java.lang.Object $recipients = this.getRecipients();
        result = result * PRIME + ($recipients == null ? 43 : $recipients.hashCode());
        final java.lang.Object $subject = this.getSubject();
        result = result * PRIME + ($subject == null ? 43 : $subject.hashCode());
        final java.lang.Object $customMessage = this.getCustomMessage();
        result = result * PRIME + ($customMessage == null ? 43 : $customMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SendInvitationCodeDTO(codes=" + this.getCodes() + ", sendType=" + this.getSendType() + ", recipients=" + this.getRecipients() + ", subject=" + this.getSubject() + ", customMessage=" + this.getCustomMessage() + ", includeGuide=" + this.getIncludeGuide() + ")";
    }
}
