package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 标签模板实体
 */
@TableName(value = "label_template", autoResultMap = true)
public class LabelTemplate {
    @TableId
    private String id;
    private String name;
    private String templateName;
    private String templateCode;
    private String templateType;
    private String description;
    /**
     * 分类（label_template 表无 category 列，此字段不映射数据库）
     */
    @TableField(exist = false)
    private String category;
    private Integer labelWidth;
    private Integer labelHeight;
    private Integer dpi;
    private Integer gapSize;
    private Integer printSpeed;
    private Integer printDensity;
    private Integer direction;
    private Integer sortOrder;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> elements;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> layoutConfig;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> size;
    private String backgroundColor;
    private Boolean enabled;
    @JsonProperty("isDefault")
    private Boolean isDefault;
    @JsonProperty("isActive")
    @TableField(exist = false)
    private Boolean isActive;
    @Version
    private Integer version;
    @JsonProperty("createdAt")
    @JsonAlias({"createTime", "created_at", "createTimeAt"})
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    @JsonProperty("updatedAt")
    @JsonAlias({"updateTime", "updated_at", "updateTimeAt"})
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;
    @JsonProperty("createdBy")
    @JsonAlias({"createBy", "created_by"})
    private String createdBy;
    @JsonProperty("updatedBy")
    @JsonAlias({"updateBy", "updated_by"})
    private String updatedBy;


    /**
     * ISO 8601 日期格式反序列化器
     */
    public static class IsoLocalDateTimeDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
            String dateStr = p.getText();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }
            try {
                // 尝试解析 ISO 8601 格式 (带 Z 后缀)
                if (dateStr.endsWith("Z")) {
                    Instant instant = Instant.parse(dateStr);
                    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                }
                // 尝试直接解析 LocalDateTime 格式
                return LocalDateTime.parse(dateStr);
            } catch (Exception e) {
                // 如果都失败，返回当前时间
                return LocalDateTime.now();
            }
        }
    }

    public LabelTemplate() {
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
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

    public String getDescription() {
        return this.description;
    }

    public String getCategory() {
        return this.category;
    }

    public Integer getLabelWidth() {
        return this.labelWidth;
    }

    public Integer getLabelHeight() {
        return this.labelHeight;
    }

    public Integer getDpi() {
        return this.dpi;
    }

    public Integer getGapSize() {
        return this.gapSize;
    }

    public Integer getPrintSpeed() {
        return this.printSpeed;
    }

    public Integer getPrintDensity() {
        return this.printDensity;
    }

    public Integer getDirection() {
        return this.direction;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public List<Map<String, Object>> getElements() {
        return this.elements;
    }

    public Map<String, Object> getLayoutConfig() {
        return this.layoutConfig;
    }

    public Map<String, Object> getSize() {
        return this.size;
    }

    public String getBackgroundColor() {
        return this.backgroundColor;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public Boolean getIsDefault() {
        return this.isDefault;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public Integer getVersion() {
        return this.version;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
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

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public void setLabelWidth(final Integer labelWidth) {
        this.labelWidth = labelWidth;
    }

    public void setLabelHeight(final Integer labelHeight) {
        this.labelHeight = labelHeight;
    }

    public void setDpi(final Integer dpi) {
        this.dpi = dpi;
    }

    public void setGapSize(final Integer gapSize) {
        this.gapSize = gapSize;
    }

    public void setPrintSpeed(final Integer printSpeed) {
        this.printSpeed = printSpeed;
    }

    public void setPrintDensity(final Integer printDensity) {
        this.printDensity = printDensity;
    }

    public void setDirection(final Integer direction) {
        this.direction = direction;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setElements(final List<Map<String, Object>> elements) {
        this.elements = elements;
    }

    public void setLayoutConfig(final Map<String, Object> layoutConfig) {
        this.layoutConfig = layoutConfig;
    }

    public void setSize(final Map<String, Object> size) {
        this.size = size;
    }

    public void setBackgroundColor(final String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    @JsonProperty("isDefault")
    public void setIsDefault(final Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @JsonProperty("isActive")
    public void setIsActive(final Boolean isActive) {
        this.isActive = isActive;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    @JsonProperty("createdAt")
    @JsonAlias({"createTime", "created_at", "createTimeAt"})
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("updatedAt")
    @JsonAlias({"updateTime", "updated_at", "updateTimeAt"})
    @JsonDeserialize(using = IsoLocalDateTimeDeserializer.class)
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonProperty("createdBy")
    @JsonAlias({"createBy", "created_by"})
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("updatedBy")
    @JsonAlias({"updateBy", "updated_by"})
    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LabelTemplate)) return false;
        final LabelTemplate other = (LabelTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$labelWidth = this.getLabelWidth();
        final java.lang.Object other$labelWidth = other.getLabelWidth();
        if (this$labelWidth == null ? other$labelWidth != null : !this$labelWidth.equals(other$labelWidth)) return false;
        final java.lang.Object this$labelHeight = this.getLabelHeight();
        final java.lang.Object other$labelHeight = other.getLabelHeight();
        if (this$labelHeight == null ? other$labelHeight != null : !this$labelHeight.equals(other$labelHeight)) return false;
        final java.lang.Object this$dpi = this.getDpi();
        final java.lang.Object other$dpi = other.getDpi();
        if (this$dpi == null ? other$dpi != null : !this$dpi.equals(other$dpi)) return false;
        final java.lang.Object this$gapSize = this.getGapSize();
        final java.lang.Object other$gapSize = other.getGapSize();
        if (this$gapSize == null ? other$gapSize != null : !this$gapSize.equals(other$gapSize)) return false;
        final java.lang.Object this$printSpeed = this.getPrintSpeed();
        final java.lang.Object other$printSpeed = other.getPrintSpeed();
        if (this$printSpeed == null ? other$printSpeed != null : !this$printSpeed.equals(other$printSpeed)) return false;
        final java.lang.Object this$printDensity = this.getPrintDensity();
        final java.lang.Object other$printDensity = other.getPrintDensity();
        if (this$printDensity == null ? other$printDensity != null : !this$printDensity.equals(other$printDensity)) return false;
        final java.lang.Object this$direction = this.getDirection();
        final java.lang.Object other$direction = other.getDirection();
        if (this$direction == null ? other$direction != null : !this$direction.equals(other$direction)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$enabled = this.getEnabled();
        final java.lang.Object other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !this$enabled.equals(other$enabled)) return false;
        final java.lang.Object this$isDefault = this.getIsDefault();
        final java.lang.Object other$isDefault = other.getIsDefault();
        if (this$isDefault == null ? other$isDefault != null : !this$isDefault.equals(other$isDefault)) return false;
        final java.lang.Object this$isActive = this.getIsActive();
        final java.lang.Object other$isActive = other.getIsActive();
        if (this$isActive == null ? other$isActive != null : !this$isActive.equals(other$isActive)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode)) return false;
        final java.lang.Object this$templateType = this.getTemplateType();
        final java.lang.Object other$templateType = other.getTemplateType();
        if (this$templateType == null ? other$templateType != null : !this$templateType.equals(other$templateType)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$category = this.getCategory();
        final java.lang.Object other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) return false;
        final java.lang.Object this$elements = this.getElements();
        final java.lang.Object other$elements = other.getElements();
        if (this$elements == null ? other$elements != null : !this$elements.equals(other$elements)) return false;
        final java.lang.Object this$layoutConfig = this.getLayoutConfig();
        final java.lang.Object other$layoutConfig = other.getLayoutConfig();
        if (this$layoutConfig == null ? other$layoutConfig != null : !this$layoutConfig.equals(other$layoutConfig)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$backgroundColor = this.getBackgroundColor();
        final java.lang.Object other$backgroundColor = other.getBackgroundColor();
        if (this$backgroundColor == null ? other$backgroundColor != null : !this$backgroundColor.equals(other$backgroundColor)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LabelTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $labelWidth = this.getLabelWidth();
        result = result * PRIME + ($labelWidth == null ? 43 : $labelWidth.hashCode());
        final java.lang.Object $labelHeight = this.getLabelHeight();
        result = result * PRIME + ($labelHeight == null ? 43 : $labelHeight.hashCode());
        final java.lang.Object $dpi = this.getDpi();
        result = result * PRIME + ($dpi == null ? 43 : $dpi.hashCode());
        final java.lang.Object $gapSize = this.getGapSize();
        result = result * PRIME + ($gapSize == null ? 43 : $gapSize.hashCode());
        final java.lang.Object $printSpeed = this.getPrintSpeed();
        result = result * PRIME + ($printSpeed == null ? 43 : $printSpeed.hashCode());
        final java.lang.Object $printDensity = this.getPrintDensity();
        result = result * PRIME + ($printDensity == null ? 43 : $printDensity.hashCode());
        final java.lang.Object $direction = this.getDirection();
        result = result * PRIME + ($direction == null ? 43 : $direction.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $enabled = this.getEnabled();
        result = result * PRIME + ($enabled == null ? 43 : $enabled.hashCode());
        final java.lang.Object $isDefault = this.getIsDefault();
        result = result * PRIME + ($isDefault == null ? 43 : $isDefault.hashCode());
        final java.lang.Object $isActive = this.getIsActive();
        result = result * PRIME + ($isActive == null ? 43 : $isActive.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $templateType = this.getTemplateType();
        result = result * PRIME + ($templateType == null ? 43 : $templateType.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $category = this.getCategory();
        result = result * PRIME + ($category == null ? 43 : $category.hashCode());
        final java.lang.Object $elements = this.getElements();
        result = result * PRIME + ($elements == null ? 43 : $elements.hashCode());
        final java.lang.Object $layoutConfig = this.getLayoutConfig();
        result = result * PRIME + ($layoutConfig == null ? 43 : $layoutConfig.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $backgroundColor = this.getBackgroundColor();
        result = result * PRIME + ($backgroundColor == null ? 43 : $backgroundColor.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LabelTemplate(id=" + this.getId() + ", name=" + this.getName() + ", templateName=" + this.getTemplateName() + ", templateCode=" + this.getTemplateCode() + ", templateType=" + this.getTemplateType() + ", description=" + this.getDescription() + ", category=" + this.getCategory() + ", labelWidth=" + this.getLabelWidth() + ", labelHeight=" + this.getLabelHeight() + ", dpi=" + this.getDpi() + ", gapSize=" + this.getGapSize() + ", printSpeed=" + this.getPrintSpeed() + ", printDensity=" + this.getPrintDensity() + ", direction=" + this.getDirection() + ", sortOrder=" + this.getSortOrder() + ", elements=" + this.getElements() + ", layoutConfig=" + this.getLayoutConfig() + ", size=" + this.getSize() + ", backgroundColor=" + this.getBackgroundColor() + ", enabled=" + this.getEnabled() + ", isDefault=" + this.getIsDefault() + ", isActive=" + this.getIsActive() + ", version=" + this.getVersion() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
