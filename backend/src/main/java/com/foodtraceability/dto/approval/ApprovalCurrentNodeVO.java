package com.foodtraceability.dto.approval;

/**
 * 当前审批节点信息 VO
 * 用于前端展示某个业务的当前审批进度
 */
public class ApprovalCurrentNodeVO {

    /** 节点ID */
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 审批人类型 */
    private String approverType;

    /** 审批人名称 */
    private String approverName;

    /** 审批人角色 */
    private String approverRole;

    /** 节点状态: pending/completed/waiting */
    private String status;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getApproverType() {
        return approverType;
    }

    public void setApproverType(String approverType) {
        this.approverType = approverType;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
