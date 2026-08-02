package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 打印模板实体类
 * 对应数据库表 print_templates
 */
@TableName("print_templates")
@Schema(description = "打印模板实体")
public class PrintTemplate {

    /** 模板ID */
    @TableId(value = "template_id", type = IdType.AUTO)
    @Schema(description = "模板ID")
    private Long templateId;

    /** 模板名称 */
    @TableField("template_name")
    @Schema(description = "模板名称")
    private String templateName;

    /** 模板类型：1小票模板 2标签模板 3报表模板 */
    @TableField("template_type")
    @Schema(description = "模板类型")
    private Integer templateType;

    /** 模板内容（支持变量占位符如${orderNo},${productName}） */
    @TableField("template_content")
    @Schema(description = "模板内容")
    private String templateContent;

    /** 纸宽（mm） */
    @TableField("page_width")
    @Schema(description = "纸宽(mm)")
    private Integer pageWidth;

    /** 纸高（mm） */
    @TableField("page_height")
    @Schema(description = "纸高(mm)")
    private Integer pageHeight;

    /** 是否默认模板 */
    @TableField("is_default")
    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    /** 是否启用 */
    @TableField("is_enabled")
    @Schema(description = "是否启用")
    private Boolean isEnabled;

    /** 所属门店ID（null表示全局模板） */
    @TableField("store_id")
    @Schema(description = "所属门店ID")
    private Long storeId;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 启用状态（兼容性字段，映射到isDefault） */
    @Schema(description = "启用状态", hidden = true)
    private transient Boolean enabled;

    // ==================== Getter & Setter 方法 ====================

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Integer getTemplateType() {
        return templateType;
    }

    public void setTemplateType(Integer templateType) {
        this.templateType = templateType;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public Integer getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(Integer pageWidth) {
        this.pageWidth = pageWidth;
    }

    public Integer getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(Integer pageHeight) {
        this.pageHeight = pageHeight;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    // ==================== enabled/isEnabled 兼容性方法 ====================

    /**
     * 获取启用状态，优先返回enabled字段，否则映射自isDefault
     */
    public Boolean getIsEnabled() {
        return enabled != null ? enabled : isDefault;
    }

    /**
     * 设置启用状态（Boolean类型）
     */
    public void setIsEnabled(Boolean isEnabled) {
        this.enabled = isEnabled;
        this.isDefault = isEnabled;
    }

    /**
     * 设置启用状态（Integer类型兼容）
     */
    public void setIsEnabled(int isEnabled) {
        this.enabled = isEnabled == 1;
        this.isDefault = isEnabled == 1;
    }

    public Boolean getEnabled() {
        return enabled != null ? enabled : isDefault;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        this.isDefault = enabled;
    }
}
