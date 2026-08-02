package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 消息模板实体类
 * 用于存储邮件、站内消息、短信等通知模板定义
 * 支持${var}语法的变量替换模板渲染
 */
@TableName("msg_template")
@Schema(description = "消息模板实体")
public class MsgTemplate {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long templateId;
    @TableField("template_code")
    @Schema(description = "模板编码(唯一标识)")
    private String templateCode;
    @TableField("template_name")
    @Schema(description = "模板名称")
    private String templateName;
    @TableField("subject_pattern")
    @Schema(description = "主题模板(支持${var}语法)")
    private String subjectPattern;
    @TableField("content_pattern")
    @Schema(description = "内容模板(支持${var}语法)")
    private String contentPattern;
    /**
     * 模板类型: 1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK
     */
    @TableField("template_type")
    @Schema(description = "模板类型:1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK")
    private Integer templateType;
    @TableField("channel")
    @Schema(description = "发送渠道")
    private String channel;
    /**
     * 状态: 0=DISABLED 1=ENABLED 2=DRAFT
     */
    @TableField("status")
    @Schema(description = "状态:0=DISABLED 1=ENABLED 2=DRAFT")
    private Integer status;
    /**
     * 模板变量定义(JSONB格式)
     */
    @TableField(value = "variables", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "变量定义(JSONB格式)")
    private String variables;
    /**
     * 示例数据(JSONB格式)
     */
    @TableField(value = "example_data", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "示例数据(JSONB格式)")
    private String exampleData;
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
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private Long createUserId;
    @TableField("create_username")
    @Schema(description = "创建用户名")
    private String createUsername;
    @TableField("update_user_id")
    @Schema(description = "更新用户ID")
    private Long updateUserId;
    @TableField("update_username")
    @Schema(description = "更新用户名")
    private String updateUsername;

    public MsgTemplate() {
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

    public String getSubjectPattern() {
        return this.subjectPattern;
    }

    public String getContentPattern() {
        return this.contentPattern;
    }

    /**
     * 模板类型: 1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK
     */
    public Integer getTemplateType() {
        return this.templateType;
    }

    public String getChannel() {
        return this.channel;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED 2=DRAFT
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 模板变量定义(JSONB格式)
     */
    public String getVariables() {
        return this.variables;
    }

    /**
     * 示例数据(JSONB格式)
     */
    public String getExampleData() {
        return this.exampleData;
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

    public Long getCreateUserId() {
        return this.createUserId;
    }

    public String getCreateUsername() {
        return this.createUsername;
    }

    public Long getUpdateUserId() {
        return this.updateUserId;
    }

    public String getUpdateUsername() {
        return this.updateUsername;
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

    public void setSubjectPattern(final String subjectPattern) {
        this.subjectPattern = subjectPattern;
    }

    public void setContentPattern(final String contentPattern) {
        this.contentPattern = contentPattern;
    }

    /**
     * 模板类型: 1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK
     */
    public void setTemplateType(final Integer templateType) {
        this.templateType = templateType;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED 2=DRAFT
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 模板变量定义(JSONB格式)
     */
    public void setVariables(final String variables) {
        this.variables = variables;
    }

    /**
     * 示例数据(JSONB格式)
     */
    public void setExampleData(final String exampleData) {
        this.exampleData = exampleData;
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

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateUserId(final Long createUserId) {
        this.createUserId = createUserId;
    }

    public void setCreateUsername(final String createUsername) {
        this.createUsername = createUsername;
    }

    public void setUpdateUserId(final Long updateUserId) {
        this.updateUserId = updateUserId;
    }

    public void setUpdateUsername(final String updateUsername) {
        this.updateUsername = updateUsername;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MsgTemplate)) return false;
        final MsgTemplate other = (MsgTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$templateId = this.getTemplateId();
        final java.lang.Object other$templateId = other.getTemplateId();
        if (this$templateId == null ? other$templateId != null : !this$templateId.equals(other$templateId)) return false;
        final java.lang.Object this$templateType = this.getTemplateType();
        final java.lang.Object other$templateType = other.getTemplateType();
        if (this$templateType == null ? other$templateType != null : !this$templateType.equals(other$templateType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$createUserId = this.getCreateUserId();
        final java.lang.Object other$createUserId = other.getCreateUserId();
        if (this$createUserId == null ? other$createUserId != null : !this$createUserId.equals(other$createUserId)) return false;
        final java.lang.Object this$updateUserId = this.getUpdateUserId();
        final java.lang.Object other$updateUserId = other.getUpdateUserId();
        if (this$updateUserId == null ? other$updateUserId != null : !this$updateUserId.equals(other$updateUserId)) return false;
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
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createUsername = this.getCreateUsername();
        final java.lang.Object other$createUsername = other.getCreateUsername();
        if (this$createUsername == null ? other$createUsername != null : !this$createUsername.equals(other$createUsername)) return false;
        final java.lang.Object this$updateUsername = this.getUpdateUsername();
        final java.lang.Object other$updateUsername = other.getUpdateUsername();
        if (this$updateUsername == null ? other$updateUsername != null : !this$updateUsername.equals(other$updateUsername)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MsgTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $templateId = this.getTemplateId();
        result = result * PRIME + ($templateId == null ? 43 : $templateId.hashCode());
        final java.lang.Object $templateType = this.getTemplateType();
        result = result * PRIME + ($templateType == null ? 43 : $templateType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $createUserId = this.getCreateUserId();
        result = result * PRIME + ($createUserId == null ? 43 : $createUserId.hashCode());
        final java.lang.Object $updateUserId = this.getUpdateUserId();
        result = result * PRIME + ($updateUserId == null ? 43 : $updateUserId.hashCode());
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
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createUsername = this.getCreateUsername();
        result = result * PRIME + ($createUsername == null ? 43 : $createUsername.hashCode());
        final java.lang.Object $updateUsername = this.getUpdateUsername();
        result = result * PRIME + ($updateUsername == null ? 43 : $updateUsername.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MsgTemplate(templateId=" + this.getTemplateId() + ", templateCode=" + this.getTemplateCode() + ", templateName=" + this.getTemplateName() + ", subjectPattern=" + this.getSubjectPattern() + ", contentPattern=" + this.getContentPattern() + ", templateType=" + this.getTemplateType() + ", channel=" + this.getChannel() + ", status=" + this.getStatus() + ", variables=" + this.getVariables() + ", exampleData=" + this.getExampleData() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createUserId=" + this.getCreateUserId() + ", createUsername=" + this.getCreateUsername() + ", updateUserId=" + this.getUpdateUserId() + ", updateUsername=" + this.getUpdateUsername() + ")";
    }
}
