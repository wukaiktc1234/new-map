package com.foodtraceability.dto.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 发票报销更新DTO
 *
 * <p>Sprint 3.1 P0 F-001：用于更新报销单，仅允许修改待审批（draft/0）状态的报销单，
 * 金额字段为 Long（分），含 @Valid 校验注解。</p>
 */
public class InvoiceReimbursementUpdateDTO {

    /** 报销单ID（必填，由路径参数传入） */
    @NotNull(message = "报销单ID不能为空")
    private Long reimbursementId;

    /** 申请人ID（可选，更新时若传入则覆盖） */
    private Long applicantId;

    /** 申请人姓名 */
    @Size(max = 50, message = "申请人姓名长度不能超过50个字符")
    private String applicantName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String departmentName;

    /** 报销类型 */
    @NotBlank(message = "报销类型不能为空")
    @Size(max = 20, message = "报销类型长度不能超过20个字符")
    private String reimbursementType;

    /** 报销总金额（单位：分，必填，必须大于0） */
    @NotNull(message = "报销金额不能为空")
    @Min(value = 1, message = "报销金额必须大于0")
    private Long totalAmount;

    /** 申请日期 */
    @NotNull(message = "申请日期不能为空")
    private LocalDate applyDate;

    /** 报销明细列表（必填，至少1条） */
    @NotEmpty(message = "报销明细不能为空")
    @Valid
    private List<ReimbursementItemDTO> items;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public InvoiceReimbursementUpdateDTO() {
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(Long reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getReimbursementType() {
        return reimbursementType;
    }

    public void setReimbursementType(String reimbursementType) {
        this.reimbursementType = reimbursementType;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDate applyDate) {
        this.applyDate = applyDate;
    }

    public List<ReimbursementItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReimbursementItemDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
