package com.foodtraceability.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 发票报销审批通过事件（F-011 联动）
 *
 * <p>Sprint 3.1 P0：报销单审批通过、状态更新为"已通过"后发布此事件，
 * 监听器 {@code InvoiceReimbursementApprovedEventListener} 消费此事件：
 * 1. 调用 {@code AutoVoucherService.generateReimbursementVoucher()} 生成财务凭证；
 * 2. 调用 {@code CostRecordService.recordReimbursementCost()} 触发成本归集。</p>
 *
 * <p>事件发布采用 try-catch 隔离，发布失败不影响主事务（报销审批）。
 * 监听器使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 确保
 * 主事务提交后才消费。</p>
 *
 * <p>本类由 T-020 提前创建（T-020 依赖此事件类存在以声明 AutoVoucherService 方法签名），
 * 等价于完成 T-040 的事件类定义部分。事件发布逻辑在阶段5 T-042 完成。</p>
 */
public class InvoiceReimbursementApprovedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /** 报销单ID */
    private final Long reimbursementId;

    /** 报销单号 */
    private final String reimbursementNo;

    /** 申请人ID */
    private final Long applicantId;

    /** 部门ID */
    private final Long departmentId;

    /** 部门名称 */
    private final String departmentName;

    /** 报销类型（差旅费/招待费/办公费/交通费/通讯费/其他） */
    private final String reimbursementType;

    /** 审批通过金额（单位：分） */
    private final Long approvedAmount;

    /** 审批人ID */
    private final Long approverId;

    /** 审批时间 */
    private final LocalDateTime approveTime;

    public InvoiceReimbursementApprovedEvent(Object source,
                                             Long reimbursementId,
                                             String reimbursementNo,
                                             Long applicantId,
                                             Long departmentId,
                                             String departmentName,
                                             String reimbursementType,
                                             Long approvedAmount,
                                             Long approverId,
                                             LocalDateTime approveTime) {
        super(source);
        this.reimbursementId = reimbursementId;
        this.reimbursementNo = reimbursementNo;
        this.applicantId = applicantId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.reimbursementType = reimbursementType;
        this.approvedAmount = approvedAmount;
        this.approverId = approverId;
        this.approveTime = approveTime;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public String getReimbursementNo() {
        return reimbursementNo;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getReimbursementType() {
        return reimbursementType;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public Long getApproverId() {
        return approverId;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    @Override
    public String toString() {
        return "InvoiceReimbursementApprovedEvent{" +
                "reimbursementId=" + reimbursementId +
                ", reimbursementNo='" + reimbursementNo + '\'' +
                ", applicantId=" + applicantId +
                ", departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", reimbursementType='" + reimbursementType + '\'' +
                ", approvedAmount=" + approvedAmount +
                ", approverId=" + approverId +
                ", approveTime=" + approveTime +
                '}';
    }
}
