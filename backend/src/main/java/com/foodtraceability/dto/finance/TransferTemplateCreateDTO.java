package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 结转模板创建DTO
 */
@Schema(description = "结转模板创建请求")
public class TransferTemplateCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板名称", required = true)
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100位")
    private String templateName;

    @Schema(description = "模板类型：1-损益结转 2-定期计提 3-其他", required = true)
    @NotNull(message = "模板类型不能为空")
    private Integer templateType;

    @Schema(description = "源科目ID", required = true)
    @NotNull(message = "源科目ID不能为空")
    private Long sourceSubjectId;

    @Schema(description = "目标科目ID", required = true)
    @NotNull(message = "目标科目ID不能为空")
    private Long targetSubjectId;

    @Schema(description = "金额表达式")
    @Size(max = 500, message = "金额表达式长度不能超过500位")
    private String amountExpression;

    @Schema(description = "摘要模板")
    @Size(max = 200, message = "摘要模板长度不能超过200位")
    private String summaryTemplate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
