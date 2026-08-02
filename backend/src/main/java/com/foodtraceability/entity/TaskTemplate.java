package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 任务模板实体类
 *
 * 对应数据库表 task_templates
 * 预定义的任务模板，用于快速创建标准化任务
 */
@TableName("task_templates")
public class TaskTemplate {

    /** 模板ID */
    @TableId(type = IdType.AUTO)
    private Long templateId;

    /** 适用类别：daily/training/business_trip/inventory/assessment */
    private String category;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** 默认标题模板 */
    private String defaultTitle;

    /** 默认描述模板 */
    private String defaultDescription;

    /** 默认工作流阶段配置（JSON） */
    private String stagesConfig;

    /** 是否系统内置模板 */
    private Boolean isSystem;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除 / 1=已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDefaultTitle() { return defaultTitle; }
    public void setDefaultTitle(String defaultTitle) { this.defaultTitle = defaultTitle; }

    public String getDefaultDescription() { return defaultDescription; }
    public void setDefaultDescription(String defaultDescription) { this.defaultDescription = defaultDescription; }

    public String getStagesConfig() { return stagesConfig; }
    public void setStagesConfig(String stagesConfig) { this.stagesConfig = stagesConfig; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
