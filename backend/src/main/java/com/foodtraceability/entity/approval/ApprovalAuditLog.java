package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录实体类
 * 对应数据库表 approval_audit_logs
 * 记录每次审批操作（提交/通过/驳回/撤回）的详细日志
 */
@TableName("approval_audit_logs")
@Schema(description = "审批记录实体")
public class ApprovalAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批记录ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    @Schema(description = "审批记录ID")
    private String logId;

    /** 审批流程ID */
    @TableField("workflow_id")
    @Schema(description = "审批流程ID")
    private String workflowId;

    /** 业务ID（如采购单号、合同号等） */
    @TableField("business_id")
    @Schema(description = "业务ID")
    private String businessId;

    /** 业务类型 */
    @TableField("business_type")
    @Schema(description = "业务类型")
    private String businessType;

    /** 节点ID */
    @TableField("node_id")
    @Schema(description = "审批节点ID")
    private String nodeId;

    /** 节点名称 */
    @TableField("node_name")
    @Schema(description = "审批节点名称")
    private String nodeName;

    /** 审批人类型: role/superior/form_field/specific_user */
    @TableField("approver_type")
    @Schema(description = "审批人类型")
    private String approverType;

    /** 审批人姓名 */
    @TableField("approver_name")
    @Schema(description = "审批人姓名")
    private String approverName;

    /** 操作类型: submit/approve/reject/withdraw */
    @TableField("action")
    @Schema(description = "操作类型")
    private String action;

    /** 审批意见 */
    @TableField("opinion")
    @Schema(description = "审批意见")
    private String opinion;

    /** 操作时间 */
    @TableField("operate_time")
    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记: 0-未删除 1-已删除")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== 操作类型常量 ====================

    /** 提交审批 */
    public static final String ACTION_SUBMIT = "submit";
    /** 审批通过 */
    public static final String ACTION_APPROVE = "approve";
    /** 审批驳回 */
    public static final String ACTION_REJECT = "reject";
    /** 撤回审批 */
    public static final String ACTION_WITHDRAW = "withdraw";

    // ==================== 审批人类型常量 ====================

    /** 角色 */
    public static final String APPROVER_TYPE_ROLE = "role";
    /** 上级 */
    public static final String APPROVER_TYPE_SUPERIOR = "superior";
    /** 表单字段 */
    public static final String APPROVER_TYPE_FORM_FIELD = "form_field";
    /** 指定用户 */
    public static final String APPROVER_TYPE_SPECIFIC_USER = "specific_user";

    // ==================== Getter & Setter ====================

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
