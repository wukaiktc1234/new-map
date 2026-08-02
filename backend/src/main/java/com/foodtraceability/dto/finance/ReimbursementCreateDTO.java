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
 * 发票报销申请创建DTO
 *
 * <p>Sprint 3.1 P0 F-001：从 dto/ 根目录迁移至 dto/finance/，金额字段
 * BigDecimal → Long（分），补齐 @Valid 校验注解，对齐 InvoiceReimbursement 实体字段。</p>
 *
 * <p>字段含义参考 {@link com.foodtraceability.entity.finance.InvoiceReimbursement}。</p>
 */
public class ReimbursementCreateDTO {

    /**
     * 申请人ID（由 Controller 从 SecurityContext 获取并注入，前端无需传）
     *
     * <p>Sprint 3.1 P0 修复：移除 @NotNull，由 InvoiceReimbursementController.create
     * 调用 SecurityUtils.getCurrentUserId() 自动填充。前端传入的值会被覆盖。</p>
     */
    private Long applicantId;

    /**
     * 申请人姓名（冗余字段，便于查询展示）
     *
     * <p>由 Controller 从 SecurityContext 获取并注入，前端无需传。</p>
     */
    @Size(max = 50, message = "申请人姓名长度不能超过50个字符")
    private String applicantName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称（冗余字段） */
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String departmentName;

    /** 报销类型（差旅费/招待费/办公费/交通费/通讯费/其他，必填） */
    @NotBlank(message = "报销类型不能为空")
    @Size(max = 20, message = "报销类型长度不能超过20个字符")
    private String reimbursementType;

    /** 报销总金额（单位：分，必填，必须大于0） */
    @NotNull(message = "报销金额不能为空")
    @Min(value = 1, message = "报销金额必须大于0")
    private Long totalAmount;

    /** 申请日期（必填） */
    @NotNull(message = "申请日期不能为空")
    private LocalDate applyDate;

    /** 报销明细列表（必填，至少1条） */
    @NotEmpty(message = "报销明细不能为空")
    @Valid
    private List<ReimbursementItemDTO> items;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public ReimbursementCreateDTO() {
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
