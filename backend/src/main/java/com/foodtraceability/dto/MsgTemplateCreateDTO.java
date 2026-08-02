package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * 消息模板创建DTO
 * 用于创建新的消息模板，包含完整的校验规则
 */
@Schema(description = "消息模板创建请求")
public class MsgTemplateCreateDTO {
    @NotBlank(message = "模板编码不能为空")
    @Size(min = 2, max = 100, message = "模板编码长度必须在2-100个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "模板编码只能包含字母、数字、下划线和横线")
    @Schema(description = "模板编码(唯一标识)", example = "welcome-email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateCode;
    @NotBlank(message = "模板名称不能为空")
    @Size(min = 2, max = 200, message = "模板名称长度必须在2-200个字符之间")
    @Schema(description = "模板名称", example = "用户注册欢迎邮件", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateName;
    @NotBlank(message = "主题模板不能为空")
    @Size(max = 500, message = "主题模板长度不能超过500个字符")
    @Schema(description = "主题模板(支持${var}语法)", example = "欢迎加入食品溯源系统 - ${userName}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subjectPattern;
    @NotBlank(message = "内容模板不能为空")
    @Size(max = 10000, message = "内容模板长度不能超过10000个字符")
    @Schema(description = "内容模板(支持${var}语法)", example = "<h3>尊敬的${userName}：</h3>...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentPattern;
    @NotNull(message = "模板类型不能为空")
    @Min(value = 1, message = "模板类型值不合法")
    @Max(value = 4, message = "模板类型值不合法")
    @Schema(description = "模板类型:1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer templateType;
    @Size(max = 50, message = "发送渠道长度不能超过50个字符")
    @Schema(description = "发送渠道", example = "EMAIL")
    private String channel;
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 2, message = "状态值不合法")
    @Schema(description = "状态:0=DISABLED 1=ENABLED 2=DRAFT", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
    @Schema(description = "变量定义(JSON格式)", example = "[\"userName\", \"userEmail\"]")
    private String variables;
    @Schema(description = "示例数据(JSON格式)")
    private String exampleData;


    public static class MsgTemplateCreateDTOBuilder {
        private String templateCode;
        private String templateName;
        private String subjectPattern;
        private String contentPattern;
        private Integer templateType;
        private String channel;
        private Integer status;
        private String variables;
        private String exampleData;

        MsgTemplateCreateDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder templateCode(final String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder templateName(final String templateName) {
            this.templateName = templateName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder subjectPattern(final String subjectPattern) {
            this.subjectPattern = subjectPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder contentPattern(final String contentPattern) {
            this.contentPattern = contentPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder templateType(final Integer templateType) {
            this.templateType = templateType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder channel(final String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder variables(final String variables) {
            this.variables = variables;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder exampleData(final String exampleData) {
            this.exampleData = exampleData;
            return this;
        }

        public MsgTemplateCreateDTO build() {
            return new MsgTemplateCreateDTO(this.templateCode, this.templateName, this.subjectPattern, this.contentPattern, this.templateType, this.channel, this.status, this.variables, this.exampleData);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder(templateCode=" + this.templateCode + ", templateName=" + this.templateName + ", subjectPattern=" + this.subjectPattern + ", contentPattern=" + this.contentPattern + ", templateType=" + this.templateType + ", channel=" + this.channel + ", status=" + this.status + ", variables=" + this.variables + ", exampleData=" + this.exampleData + ")";
        }
    }

    public static MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder builder() {
        return new MsgTemplateCreateDTO.MsgTemplateCreateDTOBuilder();
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public String getSubjectPattern() {
        return this.subjectPattern;
    }

    public String getContentPattern() {
        return this.contentPattern;
    }

    public Integer getTemplateType() {
        return this.templateType;
    }

    public String getChannel() {
        return this.channel;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getVariables() {
        return this.variables;
    }

    public String getExampleData() {
        return this.exampleData;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    public void setSubjectPattern(final String subjectPattern) {
        this.subjectPattern = subjectPattern;
    }

    public void setContentPattern(final String contentPattern) {
        this.contentPattern = contentPattern;
    }

    public void setTemplateType(final Integer templateType) {
        this.templateType = templateType;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setVariables(final String variables) {
        this.variables = variables;
    }

    public void setExampleData(final String exampleData) {
        this.exampleData = exampleData;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MsgTemplateCreateDTO)) return false;
        final MsgTemplateCreateDTO other = (MsgTemplateCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$templateType = this.getTemplateType();
        final java.lang.Object other$templateType = other.getTemplateType();
        if (this$templateType == null ? other$templateType != null : !this$templateType.equals(other$templateType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) return false;
        final java.lang.Object this$subjectPattern = this.getSubjectPattern();
        final java.lang.Object other$subjectPattern = other.getSubjectPattern();
        if (this$subjectPattern == null ? other$subjectPattern != null : !this$subjectPattern.equals(other$subjectPattern)) return false;
        final java.lang.Object this$contentPattern = this.getContentPattern();
        final java.lang.Object other$contentPattern = other.getContentPattern();
        if (this$contentPattern == null ? other$contentPattern != null : !this$contentPattern.equals(other$contentPattern)) return false;
        final java.lang.Object this$channel = this.getChannel();
        final java.lang.Object other$channel = other.getChannel();
        if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel)) return false;
        final java.lang.Object this$variables = this.getVariables();
        final java.lang.Object other$variables = other.getVariables();
        if (this$variables == null ? other$variables != null : !this$variables.equals(other$variables)) return false;
        final java.lang.Object this$exampleData = this.getExampleData();
        final java.lang.Object other$exampleData = other.getExampleData();
        if (this$exampleData == null ? other$exampleData != null : !this$exampleData.equals(other$exampleData)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MsgTemplateCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $templateType = this.getTemplateType();
        result = result * PRIME + ($templateType == null ? 43 : $templateType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $subjectPattern = this.getSubjectPattern();
        result = result * PRIME + ($subjectPattern == null ? 43 : $subjectPattern.hashCode());
        final java.lang.Object $contentPattern = this.getContentPattern();
        result = result * PRIME + ($contentPattern == null ? 43 : $contentPattern.hashCode());
        final java.lang.Object $channel = this.getChannel();
        result = result * PRIME + ($channel == null ? 43 : $channel.hashCode());
        final java.lang.Object $variables = this.getVariables();
        result = result * PRIME + ($variables == null ? 43 : $variables.hashCode());
        final java.lang.Object $exampleData = this.getExampleData();
        result = result * PRIME + ($exampleData == null ? 43 : $exampleData.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MsgTemplateCreateDTO(templateCode=" + this.getTemplateCode() + ", templateName=" + this.getTemplateName() + ", subjectPattern=" + this.getSubjectPattern() + ", contentPattern=" + this.getContentPattern() + ", templateType=" + this.getTemplateType() + ", channel=" + this.getChannel() + ", status=" + this.getStatus() + ", variables=" + this.getVariables() + ", exampleData=" + this.getExampleData() + ")";
    }

    public MsgTemplateCreateDTO() {
    }

    public MsgTemplateCreateDTO(final String templateCode, final String templateName, final String subjectPattern, final String contentPattern, final Integer templateType, final String channel, final Integer status, final String variables, final String exampleData) {
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.subjectPattern = subjectPattern;
        this.contentPattern = contentPattern;
        this.templateType = templateType;
        this.channel = channel;
        this.status = status;
        this.variables = variables;
        this.exampleData = exampleData;
    }
}
