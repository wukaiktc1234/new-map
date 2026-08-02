package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;

/**
 * 摘要模板实体类
 * 用于管理凭证录入时的常用摘要模板，提升录入效率并保持摘要规范统一
 */
@TableName("summary_templates")
public class SummaryTemplate extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    @TableId(type = IdType.AUTO)
    private Long templateId;

    /** 摘要内容 */
    private String summaryContent;

    /** 分类 */
    private String category;

    /** 使用次数 */
    private Integer usageCount;

    /**
     * 状态
     * 1-启用 0-停用
     */
    private Integer status;

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

    public String getSummaryContent() {
        return summaryContent;
    }

    public void setSummaryContent(String summaryContent) {
        this.summaryContent = summaryContent;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    @Override
    public String toString() {
        return "SummaryTemplate{" +
                "templateId=" + templateId +
                ", summaryContent='" + summaryContent + '\'' +
                ", category='" + category + '\'' +
                ", usageCount=" + usageCount +
                ", status=" + status +
                '}';
    }
}
