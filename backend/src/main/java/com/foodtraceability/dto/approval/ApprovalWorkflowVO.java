package com.foodtraceability.dto.approval;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批流程视图对象 VO
 * 返回给前端的视图数据，nodes / conditions 已反序列化为结构化对象数组
 */
public class ApprovalWorkflowVO {

    /** 流程ID */
    private String workflowId;

    /** 流程名称 */
    private String workflowName;

    /** 业务类型 */
    private String businessType;

    /** 权限模板编码 */
    private String templateCode;

    /** 是否启用 */
    private Boolean enabled;

    /** 审批节点列表（结构化对象数组） */
    private List<Map<String, Object>> nodes;

    /** 条件分支列表（结构化对象数组） */
    private List<Map<String, Object>> conditions;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public List<Map<String, Object>> getConditions() {
        return conditions;
    }

    public void setConditions(List<Map<String, Object>> conditions) {
        this.conditions = conditions;
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
