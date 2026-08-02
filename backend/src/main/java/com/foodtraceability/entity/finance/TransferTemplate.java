package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;

/**
 * 结转模板实体类
 * 用于管理期末结转业务模板，包括损益结转、定期计提等自动化结转规则
 */
@TableName("transfer_templates")
public class TransferTemplate extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    @TableId(type = IdType.AUTO)
    private Long templateId;

    /** 模板名称 */
    private String templateName;

    /**
     * 模板类型
     * 1-损益结转 2-定期计提 3-其他
     */
    private Integer templateType;

    /** 源科目ID */
    private Long sourceSubjectId;

    /** 目标科目ID */
    private Long targetSubjectId;

    /** 金额表达式 */
    private String amountExpression;

    /** 摘要模板 */
    private String summaryTemplate;

    /** 是否启用 */
    private Boolean isEnabled;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    @Override
    public String toString() {
        return "TransferTemplate{" +
                "templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", templateType=" + templateType +
                ", sourceSubjectId=" + sourceSubjectId +
                ", targetSubjectId=" + targetSubjectId +
                ", isEnabled=" + isEnabled +
                '}';
    }
}
