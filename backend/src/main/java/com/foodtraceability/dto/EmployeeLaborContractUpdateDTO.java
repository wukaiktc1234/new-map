package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工劳动合同更新DTO
 *
 * 用于更新劳动合同信息（不含状态变更，状态变更走签署/终止/延长等专用接口）。
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-07-03
 */
@Schema(description = "员工劳动合同更新DTO")
public class EmployeeLaborContractUpdateDTO {

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeCode;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "员工ID（签署后填充，可选）", example = "1234567890")
    private String employeeId;

    @Pattern(regexp = "^(fixed-term|open-ended|project)$",
            message = "合同类型必须为fixed-term、open-ended或project")
    @Schema(description = "合同类型: fixed-term固定期限, open-ended无固定期限, project项目制",
            example = "fixed-term")
    private String contractType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "合同开始日期", example = "2026-01-01")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "合同结束日期(无固定期限可为空)", example = "2029-01-01")
    private LocalDate endDate;

    @Schema(description = "试用期月数", example = "3")
    private Integer probationMonths;

    @Schema(description = "合同约定薪资", example = "10000.00")
    private BigDecimal salary;

    @Schema(description = "工作地点", example = "北京市朝阳区")
    private String workLocation;

    @Schema(description = "合同约定岗位", example = "厨师长")
    private String position;

    @Schema(description = "签署方式: electronic电子签, paper纸质签", example = "electronic")
    private String signMethod;

    @Schema(description = "关联入职档案ID（可选）", example = "1")
    private Long archiveId;

    @Schema(description = "合同模板ID（可选）", example = "1")
    private Long templateId;

    @Schema(description = "备注", example = "更新合同信息")
    private String remark;

    public EmployeeLaborContractUpdateDTO() {
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }

    public Long getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(Long archiveId) {
        this.archiveId = archiveId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
