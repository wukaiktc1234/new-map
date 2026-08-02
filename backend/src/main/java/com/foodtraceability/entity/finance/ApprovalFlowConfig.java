package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;

/**
 * 审批流配置实体类
 * 用于管理财务单据的审批流配置，包括审批节点、单据类型等，支持多级审批流程
 */
@TableName("approval_flow_configs")
public class ApprovalFlowConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Long configId;

    /** 配置名称 */
    private String configName;

    /** 单据类型 */
    private String documentType;

    /** 审批节点（JSON） */
    private String approvalNodes;

    /** 是否启用 */
    private Boolean isEnabled;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getApprovalNodes() {
        return approvalNodes;
    }

    public void setApprovalNodes(String approvalNodes) {
        this.approvalNodes = approvalNodes;
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

    @Override
    public String toString() {
        return "ApprovalFlowConfig{" +
                "configId=" + configId +
                ", configName='" + configName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", isEnabled=" + isEnabled +
                '}';
    }
}
