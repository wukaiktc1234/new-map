package com.foodtraceability.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 排班模板视图对象
 * 包含完整模板信息及使用统计
 */
@Schema(description = "排班模板VO")
public class ScheduleTemplateVO {

    @Schema(description = "主键ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "时段需求矩阵(JSON)")
    private String demandMatrix;

    @Schema(description = "启用的规则ID列表(JSON数组)")
    private String enabledRuleIds;

    @Schema(description = "是否默认模板(每个门店仅一个)")
    private Boolean isDefault;

    @Schema(description = "状态(active:启用, inactive:停用)")
    private String status;

    @Schema(description = "被使用次数")
    private Integer useCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter ====================

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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
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
}
