package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 排班模板创建DTO
 */
@Schema(description = "排班模板创建DTO")
public class ScheduleTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "默认排班模板", required = true)
    private String templateName;

    @Schema(description = "模板描述", example = "适用于工作日的标准排班配置")
    private String description;

    @NotNull(message = "门店ID不能为空")
    @Schema(description = "门店ID", example = "1", required = true)
    private Long storeId;

    @Schema(description = "时段需求矩阵(JSON格式)",
            example = "{\"weekday\":{\"morning\":3,\"noon\":4,\"evening\":2},\"weekend\":{\"morning\":2,\"noon\":3,\"evening\":1}}")
    private String demandMatrix;

    @Schema(description = "启用的规则ID列表(JSON数组格式)", example = "[\"rule_001\",\"rule_002\"]")
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
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
