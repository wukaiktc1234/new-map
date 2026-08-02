package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批操作日志VO
 * 用于记录审批过程中每一次操作的详细信息
 * 支持审计追踪和问题排查
 */
@Schema(description = "审批操作日志VO")
public class ApprovalOperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作人ID */
    @Schema(description = "操作人ID", example = "user001")
    private String operatorId;

    /** 操作人姓名 */
    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    /**
     * 操作动作
     * create=创建, submit=提交, approve=通过, reject=驳回,
     * withdraw=撤回, urge=催办, transfer=转审, cancel=取消
     */
    @Schema(description = "操作动作", example = "submit",
            allowableValues = {"create", "submit", "approve", "reject",
                              "withdraw", "urge", "transfer", "cancel"})
    private String action;

    /** 操作详情描述 */
    @Schema(description = "操作详情", example = "提交了年假申请，请假天数3天")
    private String detail;

    /** 操作时间 */
    @Schema(description = "操作时间", example = "2026-06-03T10:30:00")
    private LocalDateTime operateTime;

    /** 操作动作中文名称（别名，兼容历史调用方） */
    @Schema(description = "操作动作中文名称", example = "提交申请")
    private String actionName;

    /** 操作备注/审批意见（别名，兼容历史调用方） */
    @Schema(description = "操作备注/审批意见")
    private String comment;

    /** 操作时间别名（兼容历史调用方，与 operateTime 等价） */
    @Schema(description = "操作时间别名")
    private LocalDateTime actionTime;

    // ==================== Getter & Setter 方法 ====================

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }
}
