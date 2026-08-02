package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 结转模板VO
 */
@Schema(description = "结转模板信息")
public class TransferTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板类型：1-损益结转 2-定期计提 3-其他")
    private Integer templateType;

    @Schema(description = "源科目ID")
    private Long sourceSubjectId;

    @Schema(description = "目标科目ID")
    private Long targetSubjectId;

    @Schema(description = "金额表达式")
    private String amountExpression;

    @Schema(description = "摘要模板")
    private String summaryTemplate;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

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

    public Long getSourceSubjectId() {
        return sourceSubjectId;
    }

    public void setSourceSubjectId(Long sourceSubjectId) {
        this.sourceSubjectId = sourceSubjectId;
    }

    public Long getTargetSubjectId() {
        return targetSubjectId;
    }

    public void setTargetSubjectId(Long targetSubjectId) {
        this.targetSubjectId = targetSubjectId;
    }

    public String getAmountExpression() {
        return amountExpression;
    }

    public void setAmountExpression(String amountExpression) {
        this.amountExpression = amountExpression;
    }

    public String getSummaryTemplate() {
        return summaryTemplate;
    }

    public void setSummaryTemplate(String summaryTemplate) {
        this.summaryTemplate = summaryTemplate;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
