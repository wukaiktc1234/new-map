package com.foodtraceability.dto;

import java.time.LocalDateTime;

/**
 * 任务模板视图对象
 */
public class TaskTemplateVO {

    /** 模板ID */
    private String templateId;

    /** 适用类别 */
    private String category;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** 默认标题模板 */
    private String defaultTitle;

    /** 默认描述模板 */
    private String defaultDescription;

    /** 工作流阶段配置 */
    private String stagesConfig;

    /** 是否系统内置 */
    private Boolean isSystem;

    /** 创建时间 */
    private LocalDateTime createTime;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

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
}
