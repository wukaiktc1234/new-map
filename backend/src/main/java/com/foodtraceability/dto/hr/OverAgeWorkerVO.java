package com.foodtraceability.dto.hr;

import java.io.Serializable;

/**
 * 超龄劳动者视图对象
 * 用于前端展示超龄劳动者信息
 */
public class OverAgeWorkerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 员工ID
     */
    private String employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 退休日期
     */
    private String retirementDate;

    /**
     * 返聘日期
     */
    private String reemploymentDate;

    /**
     * 返聘协议编号
     */
    private String agreementNo;

    /**
     * 协议开始日期
     */
    private String agreementStartDate;

    /**
     * 协议结束日期
     */
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
    private String healthCheckExpiry;

    /**
     * 健康体检结果
     */
    private String healthCheckResult;

    /**
     * 限制岗位（JSON数组转String[]）
     */
    private String[] restrictedPositions;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String[] getRestrictedPositions() {
        return restrictedPositions;
    }

    public void setRestrictedPositions(String[] restrictedPositions) {
        this.restrictedPositions = restrictedPositions;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
