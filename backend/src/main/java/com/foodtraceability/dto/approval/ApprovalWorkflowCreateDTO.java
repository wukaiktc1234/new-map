package com.foodtraceability.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 审批流程创建/更新 DTO
 * 前端传入的 nodes / conditions 为结构化对象数组，由 Service 层序列化为 JSON 字符串存储
 */
public class ApprovalWorkflowCreateDTO {

    /** 流程名称 */
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称长度不能超过100")
    private String workflowName;

    /** 业务类型 */
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 50, message = "业务类型长度不能超过50")
    private String businessType;

    /** 权限模板编码 */
    @NotBlank(message = "权限模板编码不能为空")
    @Size(max = 50, message = "权限模板编码长度不能超过50")
    private String templateCode;

    /** 是否启用 */
    private Boolean enabled;

    /** 审批节点列表（结构化对象数组） */
    private List<Map<String, Object>> nodes;

    /** 条件分支列表（结构化对象数组） */
    private List<Map<String, Object>> conditions;

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
}
