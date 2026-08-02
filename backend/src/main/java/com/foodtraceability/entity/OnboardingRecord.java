package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("onboarding_records")
@Schema(description = "入职记录实体")
public class OnboardingRecord {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "入职编号")
    private String onboardingCode;

    @Schema(description = "简历ID")
    private String resumeId;

    @Schema(description = "招聘需求ID")
    private String requirementId;

    @Schema(description = "员工ID")
    private String employeeId;

    @Schema(description = "员工编码")
    private String employeeCode;

    @Schema(description = "候选人姓名")
    private String candidateName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "门店ID")
    private String storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "职位ID")
    private String positionId;

    @Schema(description = "职位名称")
    private String positionName;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "试用期开始日期")
    private LocalDate probationStart;

    @Schema(description = "试用期结束日期")
    private LocalDate probationEnd;

    @Schema(description = "试用期月数")
    private Integer probationMonths;

    @Schema(description = "薪资")
    private BigDecimal salary;

    @Schema(description = "注册码")
    private String registrationCode;

    @Schema(description = "注册码过期时间")
    private LocalDateTime codeExpiryTime;

    @Schema(description = "注册码状态（UNUSED：未使用，USED：已使用，EXPIRED：已过期）")
    private String registrationCodeStatus;

    @Schema(description = "面试ID")
    private String interviewId;

    @Schema(description = "状态（pending：待入职，onboarding：入职中，completed：已完成，cancelled：已取消）")
    private String status;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "创建人ID")
    private String createdBy;

    @Schema(description = "创建人姓名")
    private String createdByName;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // Getter and Setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOnboardingCode() {
        return onboardingCode;
    }

    public void setOnboardingCode(String onboardingCode) {
        this.onboardingCode = onboardingCode;
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public LocalDate getProbationStart() {
        return probationStart;
    }

    public void setProbationStart(LocalDate probationStart) {
        this.probationStart = probationStart;
    }

    public LocalDate getProbationEnd() {
        return probationEnd;
    }

    public void setProbationEnd(LocalDate probationEnd) {
        this.probationEnd = probationEnd;
    }

    public Integer getProbationMonths() {
        return probationMonths;
    }

    public void setProbationMonths(Integer probationMonths) {
        this.probationMonths = probationMonths;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }

    public LocalDateTime getCodeExpiryTime() {
        return codeExpiryTime;
    }

    public void setCodeExpiryTime(LocalDateTime codeExpiryTime) {
        this.codeExpiryTime = codeExpiryTime;
    }

    public String getRegistrationCodeStatus() {
        return registrationCodeStatus;
    }

    public void setRegistrationCodeStatus(String registrationCodeStatus) {
        this.registrationCodeStatus = registrationCodeStatus;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
