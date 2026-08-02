package com.foodtraceability.dto.approval;

import java.time.LocalDateTime;

/**
 * 审批记录视图对象 VO
 */
public class ApprovalAuditLogVO {

    /** 审批记录ID */
    private String logId;

    /** 审批流程ID */
    private String workflowId;

    /** 业务ID */
    private String businessId;

    /** 业务类型 */
    private String businessType;

    /** 节点ID */
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 审批人类型 */
    private String approverType;

    /** 审批人姓名 */
    private String approverName;

    /** 操作类型: submit/approve/reject/withdraw */
    private String action;

    /** 审批意见 */
    private String opinion;

    /** 操作时间 */
    private LocalDateTime operateTime;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }
}
