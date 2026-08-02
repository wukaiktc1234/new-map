package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * 消息发送DTO
 * 用于发送消息的请求参数，支持单发和批量发送
 */
@Schema(description = "消息发送请求")
public class MsgSendDTO {
    @NotBlank(message = "模板编码不能为空")
    @Size(min = 2, max = 100, message = "模板编码长度必须在2-100个字符之间")
    @Schema(description = "模板编码", example = "welcome-email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateCode;
    /**
     * 收件人地址（单发时使用）
     */
    @NotBlank(message = "收件人不能为空", groups = {SingleSend.class})
    @Email(message = "邮箱格式不正确", groups = {EmailSend.class})
    @Size(max = 500, message = "收件人长度不能超过500个字符")
    @Schema(description = "收件人地址(邮箱/用户ID/手机号)", example = "user@example.com")
    private String recipient;
    /**
     * 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
     */
    @Min(value = 1, message = "收件人类型不合法")
    @Max(value = 3, message = "收件人类型不合法")
    @Schema(description = "收件人类型:1=EMAIL 2=USER_ID 3=PHONE", example = "1")
    private Integer recipientType = 1;
    /**
     * 批量发送时的收件人列表
     */
    @Size(min = 1, max = 100, message = "批量发送收件人数量必须在1-100之间", groups = {BatchSend.class})
    @Schema(description = "批量发送收件人列表")
    private java.util.List<String> recipients;
    /**
     * 模板变量替换数据
     */
    @NotNull(message = "模板变量数据不能为空")
    @Schema(description = "模板变量替换数据", example = "{\"userName\":\"张三\",\"userEmail\":\"zhangsan@example.com\"}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> variables;
    /**
     * 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
     */
    @Min(value = 1, message = "触发类型不合法")
    @Max(value = 4, message = "触发类型不合法")
    @Schema(description = "触发类型:1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT", example = "1")
    private Integer triggerType = 1;
    @Size(max = 100, message = "业务类型长度不能超过100个字符")
    @Schema(description = "业务类型标识", example = "ORDER_STATUS_CHANGE")
    private String bizType;
    @Size(max = 100, message = "业务ID长度不能超过100个字符")
    @Schema(description = "业务关联ID", example = "ORD20260404001")
    private String bizId;


    /**
     * 单发校验组
     */
    public interface SingleSend {
    }


    /**
     * 邮件发送校验组
     */
    public interface EmailSend {
    }


    /**
     * 批量发送校验组
     */
    public interface BatchSend {
    }


    public static class MsgSendDTOBuilder {
        private String templateCode;
        private String recipient;
        private Integer recipientType;
        private java.util.List<String> recipients;
        private Map<String, Object> variables;
        private Integer triggerType;
        private String bizType;
        private String bizId;

        MsgSendDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder templateCode(final String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        /**
         * 收件人地址（单发时使用）
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder recipient(final String recipient) {
            this.recipient = recipient;
            return this;
        }

        /**
         * 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder recipientType(final Integer recipientType) {
            this.recipientType = recipientType;
            return this;
        }

        /**
         * 批量发送时的收件人列表
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder recipients(final java.util.List<String> recipients) {
            this.recipients = recipients;
            return this;
        }

        /**
         * 模板变量替换数据
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder variables(final Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder triggerType(final Integer triggerType) {
            this.triggerType = triggerType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder bizType(final String bizType) {
            this.bizType = bizType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgSendDTO.MsgSendDTOBuilder bizId(final String bizId) {
            this.bizId = bizId;
            return this;
        }

        public MsgSendDTO build() {
            return new MsgSendDTO(this.templateCode, this.recipient, this.recipientType, this.recipients, this.variables, this.triggerType, this.bizType, this.bizId);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "MsgSendDTO.MsgSendDTOBuilder(templateCode=" + this.templateCode + ", recipient=" + this.recipient + ", recipientType=" + this.recipientType + ", recipients=" + this.recipients + ", variables=" + this.variables + ", triggerType=" + this.triggerType + ", bizType=" + this.bizType + ", bizId=" + this.bizId + ")";
        }
    }

    public static MsgSendDTO.MsgSendDTOBuilder builder() {
        return new MsgSendDTO.MsgSendDTOBuilder();
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    /**
     * 收件人地址（单发时使用）
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

    /**
     * 批量发送时的收件人列表
     */
    public java.util.List<String> getRecipients() {
        return this.recipients;
    }

    /**
     * 模板变量替换数据
     */
    public Map<String, Object> getVariables() {
        return this.variables;
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

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    /**
     * 收件人地址（单发时使用）
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

    /**
     * 批量发送时的收件人列表
     */
    public void setRecipients(final java.util.List<String> recipients) {
        this.recipients = recipients;
    }

    /**
     * 模板变量替换数据
     */
    public void setVariables(final Map<String, Object> variables) {
        this.variables = variables;
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

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MsgSendDTO)) return false;
        final MsgSendDTO other = (MsgSendDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$recipientType = this.getRecipientType();
        final java.lang.Object other$recipientType = other.getRecipientType();
        if (this$recipientType == null ? other$recipientType != null : !this$recipientType.equals(other$recipientType)) return false;
        final java.lang.Object this$triggerType = this.getTriggerType();
        final java.lang.Object other$triggerType = other.getTriggerType();
        if (this$triggerType == null ? other$triggerType != null : !this$triggerType.equals(other$triggerType)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$recipient = this.getRecipient();
        final java.lang.Object other$recipient = other.getRecipient();
        if (this$recipient == null ? other$recipient != null : !this$recipient.equals(other$recipient)) return false;
        final java.lang.Object this$recipients = this.getRecipients();
        final java.lang.Object other$recipients = other.getRecipients();
        if (this$recipients == null ? other$recipients != null : !this$recipients.equals(other$recipients)) return false;
        final java.lang.Object this$variables = this.getVariables();
        final java.lang.Object other$variables = other.getVariables();
        if (this$variables == null ? other$variables != null : !this$variables.equals(other$variables)) return false;
        final java.lang.Object this$bizType = this.getBizType();
        final java.lang.Object other$bizType = other.getBizType();
        if (this$bizType == null ? other$bizType != null : !this$bizType.equals(other$bizType)) return false;
        final java.lang.Object this$bizId = this.getBizId();
        final java.lang.Object other$bizId = other.getBizId();
        if (this$bizId == null ? other$bizId != null : !this$bizId.equals(other$bizId)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MsgSendDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $recipientType = this.getRecipientType();
        result = result * PRIME + ($recipientType == null ? 43 : $recipientType.hashCode());
        final java.lang.Object $triggerType = this.getTriggerType();
        result = result * PRIME + ($triggerType == null ? 43 : $triggerType.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $recipient = this.getRecipient();
        result = result * PRIME + ($recipient == null ? 43 : $recipient.hashCode());
        final java.lang.Object $recipients = this.getRecipients();
        result = result * PRIME + ($recipients == null ? 43 : $recipients.hashCode());
        final java.lang.Object $variables = this.getVariables();
        result = result * PRIME + ($variables == null ? 43 : $variables.hashCode());
        final java.lang.Object $bizType = this.getBizType();
        result = result * PRIME + ($bizType == null ? 43 : $bizType.hashCode());
        final java.lang.Object $bizId = this.getBizId();
        result = result * PRIME + ($bizId == null ? 43 : $bizId.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MsgSendDTO(templateCode=" + this.getTemplateCode() + ", recipient=" + this.getRecipient() + ", recipientType=" + this.getRecipientType() + ", recipients=" + this.getRecipients() + ", variables=" + this.getVariables() + ", triggerType=" + this.getTriggerType() + ", bizType=" + this.getBizType() + ", bizId=" + this.getBizId() + ")";
    }

    public MsgSendDTO() {
    }

    /**
     * Creates a new {@code MsgSendDTO} instance.
     *
     * @param templateCode
     * @param recipient 收件人地址（单发时使用）
     * @param recipientType 收件人类型: 1=EMAIL 2=USER_ID 3=PHONE
     * @param recipients 批量发送时的收件人列表
     * @param variables 模板变量替换数据
     * @param triggerType 触发类型: 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
     * @param bizType
     * @param bizId
     */
    public MsgSendDTO(final String templateCode, final String recipient, final Integer recipientType, final java.util.List<String> recipients, final Map<String, Object> variables, final Integer triggerType, final String bizType, final String bizId) {
        this.templateCode = templateCode;
        this.recipient = recipient;
        this.recipientType = recipientType;
        this.recipients = recipients;
        this.variables = variables;
        this.triggerType = triggerType;
        this.bizType = bizType;
        this.bizId = bizId;
    }
}
