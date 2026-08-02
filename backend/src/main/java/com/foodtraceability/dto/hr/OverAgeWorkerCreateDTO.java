package com.foodtraceability.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 超龄劳动者创建DTO
 */
public class OverAgeWorkerCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工ID
     */
    @NotBlank(message = "员工ID不能为空")
    private String employeeId;

    /**
     * 员工姓名
     */
    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    /**
     * 退休日期
     */
    @NotBlank(message = "退休日期不能为空")
    private String retirementDate;

    /**
     * 返聘日期
     */
    @NotBlank(message = "返聘日期不能为空")
    private String reemploymentDate;

    /**
     * 返聘协议编号
     */
    @NotBlank(message = "返聘协议编号不能为空")
    private String agreementNo;

    /**
     * 协议开始日期
     */
    @NotBlank(message = "协议开始日期不能为空")
    private String agreementStartDate;

    /**
     * 协议结束日期
     */
    @NotBlank(message = "协议结束日期不能为空")
    private String agreementEndDate;

    /**
     * 工伤保险编号
     */
    private String workInjuryInsuranceNo;

    /**
     * 工伤保险到期日
     */
    private String workInjuryInsuranceExpiry;

    /**
     * 健康体检到期日
     */
    @NotNull(message = "健康体检到期日不能为空")
    private String healthCheckExpiry;

    /**
     * 健康体检结果
     */
    @NotBlank(message = "健康体检结果不能为空")
    private String healthCheckResult;

    /**
     * 限制岗位（JSON数组字符串）
     */
    private String restrictedPositions;

    /**
     * 备注
     */
    private String remark;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getRetirementDate() {
        return retirementDate;
    }

    public void setRetirementDate(String retirementDate) {
        this.retirementDate = retirementDate;
    }

    public String getReemploymentDate() {
        return reemploymentDate;
    }

    public void setReemploymentDate(String reemploymentDate) {
        this.reemploymentDate = reemploymentDate;
    }

    public String getAgreementNo() {
        return agreementNo;
    }

    public void setAgreementNo(String agreementNo) {
        this.agreementNo = agreementNo;
    }

    public String getAgreementStartDate() {
        return agreementStartDate;
    }

    public void setAgreementStartDate(String agreementStartDate) {
        this.agreementStartDate = agreementStartDate;
    }

    public String getAgreementEndDate() {
        return agreementEndDate;
    }

    public void setAgreementEndDate(String agreementEndDate) {
        this.agreementEndDate = agreementEndDate;
    }

    public String getWorkInjuryInsuranceNo() {
        return workInjuryInsuranceNo;
    }

    public void setWorkInjuryInsuranceNo(String workInjuryInsuranceNo) {
        this.workInjuryInsuranceNo = workInjuryInsuranceNo;
    }

    public String getWorkInjuryInsuranceExpiry() {
        return workInjuryInsuranceExpiry;
    }

    public void setWorkInjuryInsuranceExpiry(String workInjuryInsuranceExpiry) {
        this.workInjuryInsuranceExpiry = workInjuryInsuranceExpiry;
    }

    public String getHealthCheckExpiry() {
        return healthCheckExpiry;
    }

    public void setHealthCheckExpiry(String healthCheckExpiry) {
        this.healthCheckExpiry = healthCheckExpiry;
    }

    public String getHealthCheckResult() {
        return healthCheckResult;
    }

    public void setHealthCheckResult(String healthCheckResult) {
        this.healthCheckResult = healthCheckResult;
    }

    public String getRestrictedPositions() {
        return restrictedPositions;
    }

    public void setRestrictedPositions(String restrictedPositions) {
        this.restrictedPositions = restrictedPositions;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
