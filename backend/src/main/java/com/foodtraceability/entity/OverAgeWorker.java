package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 超龄劳动者实体类
 * 用于存储超龄劳动者（超过退休年龄仍继续聘用）的基本信息
 */
@TableName("over_age_worker")
public class OverAgeWorker implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 限制岗位（JSON数组字符串存储）
     */
    private String restrictedPositions;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
