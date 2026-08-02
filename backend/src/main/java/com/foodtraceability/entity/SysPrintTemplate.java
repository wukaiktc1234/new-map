package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 打印模板实体类
 */
@TableName("sys_print_templates")
@Schema(description = "打印模板表")
public class SysPrintTemplate {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "模板ID")
    private Long id;
    @TableField("template_name")
    @Schema(description = "模板名称")
    private String templateName;
    @TableField("template_code")
    @Schema(description = "模板编码")
    private String templateCode;
    @TableField("template_type")
    @Schema(description = "模板类型：order/dish/receipt/refund/purchase/inventory/finance/hr")
    private String templateType;
    @TableField("default_device_id")
    @Schema(description = "默认打印设备ID")
    private Long defaultDeviceId;
    @TableField("template_content")
    @Schema(description = "模板内容")
    private String templateContent;
    @TableField("template_style")
    @Schema(description = "样式定义（CSS）")
    private String templateStyle;
    @TableField("paper_size")
    @Schema(description = "纸张宽度：58mm/80mm/A4")
    private String paperSize;
    @TableField("paper_orientation")
    @Schema(description = "方向：portrait-纵向/landscape-横向")
    private String paperOrientation;
    @TableField("store_id")
    @Schema(description = "所属门店ID")
    private Long storeId;
    @TableField("is_default")
    @Schema(description = "是否默认模板：0-否，1-是")
    private Integer isDefault;
    @TableField("is_enabled")
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer isEnabled;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public SysPrintTemplate() {
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

    public String getTemplateType() {
        return this.templateType;
    }

    public Long getDefaultDeviceId() {
        return this.defaultDeviceId;
    }

    public String getTemplateContent() {
        return this.templateContent;
    }

    public String getTemplateStyle() {
        return this.templateStyle;
    }

    public String getPaperSize() {
        return this.paperSize;
    }

    public String getPaperOrientation() {
        return this.paperOrientation;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getIsDefault() {
        return this.isDefault;
    }

    public Integer getIsEnabled() {
        return this.isEnabled;
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

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public void setTemplateType(final String templateType) {
        this.templateType = templateType;
    }

    public void setDefaultDeviceId(final Long defaultDeviceId) {
        this.defaultDeviceId = defaultDeviceId;
    }

    public void setTemplateContent(final String templateContent) {
        this.templateContent = templateContent;
    }

    public void setTemplateStyle(final String templateStyle) {
        this.templateStyle = templateStyle;
    }

    public void setPaperSize(final String paperSize) {
        this.paperSize = paperSize;
    }

    public void setPaperOrientation(final String paperOrientation) {
        this.paperOrientation = paperOrientation;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setIsDefault(final Integer isDefault) {
        this.isDefault = isDefault;
    }

    public void setIsEnabled(final Integer isEnabled) {
        this.isEnabled = isEnabled;
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
        if (!(o instanceof SysPrintTemplate)) return false;
        final SysPrintTemplate other = (SysPrintTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$defaultDeviceId = this.getDefaultDeviceId();
        final java.lang.Object other$defaultDeviceId = other.getDefaultDeviceId();
        if (this$defaultDeviceId == null ? other$defaultDeviceId != null : !this$defaultDeviceId.equals(other$defaultDeviceId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$isDefault = this.getIsDefault();
        final java.lang.Object other$isDefault = other.getIsDefault();
        if (this$isDefault == null ? other$isDefault != null : !this$isDefault.equals(other$isDefault)) return false;
        final java.lang.Object this$isEnabled = this.getIsEnabled();
        final java.lang.Object other$isEnabled = other.getIsEnabled();
        if (this$isEnabled == null ? other$isEnabled != null : !this$isEnabled.equals(other$isEnabled)) return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$templateType = this.getTemplateType();
        final java.lang.Object other$templateType = other.getTemplateType();
        if (this$templateType == null ? other$templateType != null : !this$templateType.equals(other$templateType)) return false;
        final java.lang.Object this$templateContent = this.getTemplateContent();
        final java.lang.Object other$templateContent = other.getTemplateContent();
        if (this$templateContent == null ? other$templateContent != null : !this$templateContent.equals(other$templateContent)) return false;
        final java.lang.Object this$templateStyle = this.getTemplateStyle();
        final java.lang.Object other$templateStyle = other.getTemplateStyle();
        if (this$templateStyle == null ? other$templateStyle != null : !this$templateStyle.equals(other$templateStyle)) return false;
        final java.lang.Object this$paperSize = this.getPaperSize();
        final java.lang.Object other$paperSize = other.getPaperSize();
        if (this$paperSize == null ? other$paperSize != null : !this$paperSize.equals(other$paperSize)) return false;
        final java.lang.Object this$paperOrientation = this.getPaperOrientation();
        final java.lang.Object other$paperOrientation = other.getPaperOrientation();
        if (this$paperOrientation == null ? other$paperOrientation != null : !this$paperOrientation.equals(other$paperOrientation)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysPrintTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $defaultDeviceId = this.getDefaultDeviceId();
        result = result * PRIME + ($defaultDeviceId == null ? 43 : $defaultDeviceId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $isDefault = this.getIsDefault();
        result = result * PRIME + ($isDefault == null ? 43 : $isDefault.hashCode());
        final java.lang.Object $isEnabled = this.getIsEnabled();
        result = result * PRIME + ($isEnabled == null ? 43 : $isEnabled.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $templateType = this.getTemplateType();
        result = result * PRIME + ($templateType == null ? 43 : $templateType.hashCode());
        final java.lang.Object $templateContent = this.getTemplateContent();
        result = result * PRIME + ($templateContent == null ? 43 : $templateContent.hashCode());
        final java.lang.Object $templateStyle = this.getTemplateStyle();
        result = result * PRIME + ($templateStyle == null ? 43 : $templateStyle.hashCode());
        final java.lang.Object $paperSize = this.getPaperSize();
        result = result * PRIME + ($paperSize == null ? 43 : $paperSize.hashCode());
        final java.lang.Object $paperOrientation = this.getPaperOrientation();
        result = result * PRIME + ($paperOrientation == null ? 43 : $paperOrientation.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysPrintTemplate(id=" + this.getId() + ", templateName=" + this.getTemplateName() + ", templateCode=" + this.getTemplateCode() + ", templateType=" + this.getTemplateType() + ", defaultDeviceId=" + this.getDefaultDeviceId() + ", templateContent=" + this.getTemplateContent() + ", templateStyle=" + this.getTemplateStyle() + ", paperSize=" + this.getPaperSize() + ", paperOrientation=" + this.getPaperOrientation() + ", storeId=" + this.getStoreId() + ", isDefault=" + this.getIsDefault() + ", isEnabled=" + this.getIsEnabled() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
