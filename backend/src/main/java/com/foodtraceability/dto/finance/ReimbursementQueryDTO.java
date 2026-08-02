package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Min;

/**
 * 报销查询DTO
 *
 * <p>Sprint 3.1 P0 F-001：从 dto/ 根目录迁移至 dto/finance/，
 * 无金额字段，仅迁移包路径并对齐 InvoiceReimbursement 字段命名。</p>
 */
public class ReimbursementQueryDTO {

    /** 报销单号（模糊查询） */
    private String reimbursementNo;

    /** 申请人姓名（模糊查询） */
    private String applicantName;

    /** 申请人ID */
    private Long applicantId;

    /** 部门ID */
    private Long departmentId;

    /** 单据状态（0-草稿 1-已审批 2-已付款 3-已取消 4-已拒绝） */
    private Integer status;

    /** 付款状态（0-未付款 1-已付款） */
    private Integer paymentStatus;

    /** 报销类型 */
    private String reimbursementType;

    /** 起始申请日期（yyyy-MM-dd） */
    private String startDate;

    /** 结束申请日期（yyyy-MM-dd） */
    private String endDate;

    /** 当前页码，默认1 */
    @Min(value = 1, message = "页码必须大于0")
    private Integer current = 1;

    /** 每页大小，默认10 */
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 10;

    public ReimbursementQueryDTO() {
    }

    public String getReimbursementNo() {
        return reimbursementNo;
    }

    public void setReimbursementNo(String reimbursementNo) {
        this.reimbursementNo = reimbursementNo;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getReimbursementType() {
        return reimbursementType;
    }

    public void setReimbursementType(String reimbursementType) {
        this.reimbursementType = reimbursementType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
