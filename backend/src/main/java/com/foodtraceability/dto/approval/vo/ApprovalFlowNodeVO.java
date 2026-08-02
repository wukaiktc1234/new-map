package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批流程节点VO
 * 用于描述审批流程中的每一个节点信息
 * 展示在审批详情的时间线组件中
 */
@Schema(description = "审批流程节点VO")
public class ApprovalFlowNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 节点名称（如"直属领导审批"、"部门经理审批"） */
    @Schema(description = "节点名称", example = "直属领导审批")
    private String nodeName;

    /** 操作人ID（兼容历史调用方） */
    @Schema(description = "操作人ID", example = "user001")
    private String operatorId;

    /** 操作人姓名 */
    @Schema(description = "操作人姓名", example = "王经理")
    private String operatorName;

    /** 操作人头像URL */
    @Schema(description = "操作人头像URL", example = "/api/files/avatar/mgr001.jpg")
    private String operatorAvatar;

    /**
     * 操作动作
     * submit=提交, approve=通过, reject=驳回, withdraw=撤回,
     * urge=催办, transfer=转审
     */
    @Schema(description = "操作动作", example = "approve",
            allowableValues = {"submit", "approve", "reject", "withdraw", "urge", "transfer"})
    private String action;

    /** 操作意见/备注 */
    @Schema(description = "操作意见/备注", example = "同意，已确认工作交接安排妥当")
    private String comment;

    /** 操作时间 */
    @Schema(description = "操作时间", example = "2026-06-03T11:30:00")
    private LocalDateTime operateTime;

    /**
     * 节点状态
     * pending=待处理, completed=已完成, skipped=已跳过
     */
    @Schema(description = "节点状态", example = "completed",
            allowableValues = {"pending", "completed", "skipped"})
    private String status;

    /** 操作动作中文名称（别名，与 action 配合使用，兼容历史调用方） */
    @Schema(description = "操作动作中文名称", example = "通过审批")
    private String actionName;

    /** 操作时间别名（兼容历史调用方，与 operateTime 等价） */
    @Schema(description = "操作时间别名")
    private LocalDateTime actionTime;

    // ==================== Getter & Setter 方法 ====================

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorAvatar() {
        return operatorAvatar;
    }

    public void setOperatorAvatar(String operatorAvatar) {
        this.operatorAvatar = operatorAvatar;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }
}
