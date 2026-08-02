package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新任务模板请求DTO
 */
public class TaskTemplateUpdateDTO {

    /** 适用类别 */
    @NotBlank(message = "模板类别不能为空")
    private String category;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "名称不超过100个字符")
    private String name;

    /** 模板描述 */
    private String description;

    /** 默认标题模板 */
    private String defaultTitle;

    /** 默认描述模板 */
    private String defaultDescription;

    /** 工作流阶段配置（JSON字符串） */
    private String stagesConfig;

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
}
