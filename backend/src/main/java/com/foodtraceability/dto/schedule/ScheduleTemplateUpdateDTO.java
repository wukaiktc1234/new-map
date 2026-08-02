package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 排班模板更新DTO
 * 所有字段可选，仅更新非null字段
 */
@Schema(description = "排班模板更新DTO")
public class ScheduleTemplateUpdateDTO {

    @Schema(description = "模板名称", example = "周末排班模板")
    private String templateName;

    @Schema(description = "模板描述", example = "适用于周末的排班配置")
    private String description;

    @Schema(description = "时段需求矩阵(JSON格式)",
            example = "{\"weekday\":{\"morning\":4,\"noon\":5,\"evening\":3}}")
    private String demandMatrix;

    @Schema(description = "启用的规则ID列表(JSON数组格式)", example = "[\"rule_003\"]")
    private String enabledRuleIds;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDemandMatrix() {
        return demandMatrix;
    }

    public void setDemandMatrix(String demandMatrix) {
        this.demandMatrix = demandMatrix;
    }

    public String getEnabledRuleIds() {
        return enabledRuleIds;
    }

    public void setEnabledRuleIds(String enabledRuleIds) {
        this.enabledRuleIds = enabledRuleIds;
    }
}
