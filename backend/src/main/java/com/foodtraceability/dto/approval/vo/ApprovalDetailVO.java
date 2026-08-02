package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批详情VO
 * 用于审批详情页面展示的完整信息
 * 包含基础信息、完整表单数据、风险预警、审批流程和操作日志
 */
@Schema(description = "审批详情VO")
public class ApprovalDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息（继承自EmployeeApprovalVO的字段） ====================

    /** 审批ID */
    @Schema(description = "审批ID", example = "a1b2c3d4e5f6...")
    private String id;

    /** 审批编号 */
    @Schema(description = "审批编号", example = "AP20260301001")
    private String approvalNo;

    /** 审批类型 */
    @Schema(description = "审批类型", example = "leave")
    private String type;

    /** 标题 */
    @Schema(description = "标题", example = "年假申请-张三")
    private String title;

    /** 审批状态 */
    @Schema(description = "审批状态", example = "pending")
    private String status;

    /** 优先级 */
    @Schema(description = "优先级", example = "normal")
    private String priority;

    /** 申请人ID */
    @Schema(description = "申请人ID", example = "emp001")
    private String applicantId;

    /** 申请人姓名 */
    @Schema(description = "申请人姓名", example = "张三")
    private String applicantName;

    /** 申请人头像URL */
    @Schema(description = "申请人头像URL", example = "/api/files/avatar/emp001.jpg")
    private String applicantAvatar;

    /** 部门名称 */
    @Schema(description = "部门名称", example = "前厅部")
    private String departmentName;

    /** 职位名称 */
    @Schema(description = "职位名称", example = "服务员")
    private String positionName;

    /** 提交时间 */
    @Schema(description = "提交时间", example = "2026-06-03T10:30:00")
    private LocalDateTime submitTime;

    /** 审批完成时间 */
    @Schema(description = "审批完成时间", example = "2026-06-03T14:20:00")
    private LocalDateTime approveTime;

    /** 当前审批人姓名 */
    @Schema(description = "当前审批人姓名", example = "王经理")
    private String currentApproverName;

    /** 风险等级 */
    @Schema(description = "风险等级", example = "info")
    private String riskLevel;

    // ==================== 详情扩展字段 ====================

    /**
     * 完整表单数据（Map结构）
     * 包含该审批类型的所有详细字段
     * 前端根据type字段决定如何渲染
     */
    @Schema(description = "完整表单数据")
    private Map<String, Object> formFields;

    /**
     * 类型化的上下文数据
     * 可以为具体的LeaveContext/OvertimeContext等对象
     * 也可以为Map<String,Object>通用格式
     */
    @Schema(description = "类型化上下文数据")
    private Object contextData;

    /**
     * 风险预警信息
     * 当riskLevel不为null时有值
     */
    @Schema(description = "风险预警信息")
    private RiskWarningVO riskWarning;

    /**
     * 审批流程节点列表
     * 记录完整的审批流转过程，用于展示审批时间线
     */
    @Schema(description = "审批流程节点列表")
    private List<ApprovalFlowNodeVO> approvalFlow;

    /**
     * 操作日志列表
     * 记录所有操作历史（提交/审批/驳回/撤回/催办等）
     */
    @Schema(description = "操作日志列表")
    private List<ApprovalOperationLogVO> operationLog;

    // ==================== Getter & Setter 方法 ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantAvatar() { return applicantAvatar; }
    public void setApplicantAvatar(String applicantAvatar) { this.applicantAvatar = applicantAvatar; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public String getCurrentApproverName() { return currentApproverName; }
    public void setCurrentApproverName(String currentApproverName) { this.currentApproverName = currentApproverName; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Map<String, Object> getFormFields() { return formFields; }
    public void setFormFields(Map<String, Object> formFields) { this.formFields = formFields; }

    public Object getContextData() { return contextData; }
    public void setContextData(Object contextData) { this.contextData = contextData; }

    public RiskWarningVO getRiskWarning() { return riskWarning; }
    public void setRiskWarning(RiskWarningVO riskWarning) { this.riskWarning = riskWarning; }

    public List<ApprovalFlowNodeVO> getApprovalFlow() { return approvalFlow; }
    public void setApprovalFlow(List<ApprovalFlowNodeVO> approvalFlow) { this.approvalFlow = approvalFlow; }

    public List<ApprovalOperationLogVO> getOperationLog() { return operationLog; }
    public void setOperationLog(List<ApprovalOperationLogVO> operationLog) { this.operationLog = operationLog; }
}
