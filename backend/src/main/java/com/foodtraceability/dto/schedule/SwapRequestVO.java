package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 换班请求视图对象
 * 包含嵌套的发起人和目标人班次信息
 */
@Schema(description = "换班请求详情")
public class SwapRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求ID */
    @Schema(description = "换班请求ID")
    private String requestId;

    /** 所属计划ID */
    @Schema(description = "排班方案ID")
    private String planId;

    /** 计划名称 */
    @Schema(description = "排班方案名称")
    private String planName;

    /** 请求状态 */
    @Schema(description = "请求状态: pending/approved/rejected/cancelled")
    private String status;

    /** 发起人ID */
    @Schema(description = "发起人ID")
    private String initiatorId;

    /** 发起人姓名 */
    @Schema(description = "发起人姓名")
    private String initiatorName;

    /** 发起人的原班次 */
    @Schema(description = "发起人换出的班次信息")
    private SwapEntryInfoVO initiatorEntry;

    /** 目标员工ID */
    @Schema(description = "目标员工ID")
    private String targetEmployeeId;

    /** 目标员工姓名 */
    @Schema(description = "目标员工姓名")
    private String targetEmployeeName;

    /** 目标员工的班次 */
    @Schema(description = "目标员工换出的班次信息")
    private SwapEntryInfoVO targetEntry;

    /** 换班原因 */
    @Schema(description = "换班原因")
    private String reason;

    /** 申请时间 */
    @Schema(description = "申请时间")
    private String createTime;

    /** 审批人ID */
    @Schema(description = "审批人ID")
    private String approverId;

    /** 审批人姓名 */
    @Schema(description = "审批人姓名")
    private String approverName;

    /** 审批时间 */
    @Schema(description = "审批时间")
    private String approveTime;

    /** 审批意见 */
    @Schema(description = "审批意见（通过时记录）")
    private String approveComment;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInitiatorId() { return initiatorId; }
    public void setInitiatorId(String initiatorId) { this.initiatorId = initiatorId; }

    public String getInitiatorName() { return initiatorName; }
    public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }

    public SwapEntryInfoVO getInitiatorEntry() { return initiatorEntry; }
    public void setInitiatorEntry(SwapEntryInfoVO initiatorEntry) { this.initiatorEntry = initiatorEntry; }

    public String getTargetEmployeeId() { return targetEmployeeId; }
    public void setTargetEmployeeId(String targetEmployeeId) { this.targetEmployeeId = targetEmployeeId; }

    public String getTargetEmployeeName() { return targetEmployeeName; }
    public void setTargetEmployeeName(String targetEmployeeName) { this.targetEmployeeName = targetEmployeeName; }

    public SwapEntryInfoVO getTargetEntry() { return targetEntry; }
    public void setTargetEntry(SwapEntryInfoVO targetEntry) { this.targetEntry = targetEntry; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApproveTime() { return approveTime; }
    public void setApproveTime(String approveTime) { this.approveTime = approveTime; }

    public String getApproveComment() { return approveComment; }
    public void setApproveComment(String approveComment) { this.approveComment = approveComment; }
}
