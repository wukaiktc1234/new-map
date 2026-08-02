package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 合同模板实体类
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@TableName("contract_template")
@Schema(description = "合同模板实体")
public class ContractTemplate {
    @TableId(type = IdType.AUTO)
    @Schema(description = "模板ID")
    private Long id;
    @Schema(description = "模板名称")
    private String templateName;
    @Schema(description = "模板编码")
    private String templateCode;
    @Schema(description = "合同类型: fixed-term固定期限, open-ended无固定期限, project项目制")
    private String contractType;
    @Schema(description = "模板内容（HTML格式）")
    @TableField("template_content")
    private String templateContent;
    @Schema(description = "模板变量定义（JSON）")
    @TableField("template_variables")
    private String templateVariables;
    @Schema(description = "模板版本")
    private String version;
    @Schema(description = "状态: active-启用, inactive-停用")
    private String status;
    @Schema(description = "模板描述")
    private String description;
    @Schema(description = "创建人")
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    public static final String TYPE_FIXED_TERM = "fixed-term";
    public static final String TYPE_OPEN_ENDED = "open-ended";
    public static final String TYPE_PROJECT = "project";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    public ContractTemplate() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public String getTemplateCode() {
        return this.templateCode;
    }

    public String getContractType() {
        return this.contractType;
    }

    public String getTemplateContent() {
        return this.templateContent;
    }

    public String getTemplateVariables() {
        return this.templateVariables;
    }

    public String getVersion() {
        return this.version;
    }

    public String getStatus() {
        return this.status;
    }

    public String getDescription() {
        return this.description;
    }

    public Long getCreateBy() {
        return this.createBy;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public void setContractType(final String contractType) {
        this.contractType = contractType;
    }

    public void setTemplateContent(final String templateContent) {
        this.templateContent = templateContent;
    }

    public void setTemplateVariables(final String templateVariables) {
        this.templateVariables = templateVariables;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setCreateBy(final Long createBy) {
        this.createBy = createBy;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ContractTemplate)) return false;
        final ContractTemplate other = (ContractTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$contractType = this.getContractType();
        final java.lang.Object other$contractType = other.getContractType();
        if (this$contractType == null ? other$contractType != null : !this$contractType.equals(other$contractType)) return false;
        final java.lang.Object this$templateContent = this.getTemplateContent();
        final java.lang.Object other$templateContent = other.getTemplateContent();
        if (this$templateContent == null ? other$templateContent != null : !this$templateContent.equals(other$templateContent)) return false;
        final java.lang.Object this$templateVariables = this.getTemplateVariables();
        final java.lang.Object other$templateVariables = other.getTemplateVariables();
        if (this$templateVariables == null ? other$templateVariables != null : !this$templateVariables.equals(other$templateVariables)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ContractTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $contractType = this.getContractType();
        result = result * PRIME + ($contractType == null ? 43 : $contractType.hashCode());
        final java.lang.Object $templateContent = this.getTemplateContent();
        result = result * PRIME + ($templateContent == null ? 43 : $templateContent.hashCode());
        final java.lang.Object $templateVariables = this.getTemplateVariables();
        result = result * PRIME + ($templateVariables == null ? 43 : $templateVariables.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ContractTemplate(id=" + this.getId() + ", templateName=" + this.getTemplateName() + ", templateCode=" + this.getTemplateCode() + ", contractType=" + this.getContractType() + ", templateContent=" + this.getTemplateContent() + ", templateVariables=" + this.getTemplateVariables() + ", version=" + this.getVersion() + ", status=" + this.getStatus() + ", description=" + this.getDescription() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
