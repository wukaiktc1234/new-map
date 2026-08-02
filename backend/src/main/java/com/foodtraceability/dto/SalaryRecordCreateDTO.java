package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 薪资记录创建DTO
 */
@Schema(description = "薪资记录创建DTO")
public class SalaryRecordCreateDTO {

    @NotBlank(message = "员工ID不能为空")
    @Schema(description = "员工ID", example = "1234567890", required = true)
    private String employeeId;

    @NotBlank(message = "薪资月份不能为空")
    @Schema(description = "薪资月份（格式：2026-04）", example = "2026-04", required = true)
    private String salaryMonth;

    /** 基本工资（分） */
    @Schema(description = "基本工资（分）", example = "800000")
    private Long baseSalary;

    /** 岗位工资（分） */
    @Schema(description = "岗位工资（分）", example = "200000")
    private Long positionSalary;

    /** 绩效奖金（分） */
    @Schema(description = "绩效奖金（分）", example = "100000")
    private Long performanceBonus;

    /** 加班费（分） */
    @Schema(description = "加班费（分）", example = "50000")
    private Long overtimePay;

    /** 扣款（分） */
    @Schema(description = "扣款（分）", example = "0")
    private Long deduction;

    /** 社保个人部分（分） */
    @Schema(description = "社保个人部分（分）", example = "80000")
    private Long socialInsurance;

    /** 公积金个人部分（分） */
    @Schema(description = "公积金个人部分（分）", example = "40000")
    private Long housingFund;

    /** 个人所得税（分） */
    @Schema(description = "个人所得税（分）", example = "10000")
    private Long tax;

    /** 实发工资（分） - 系统自动计算 */
    @Schema(description = "实发工资（分，系统自动计算）", example = "1220000")
    private Long actualSalary;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "发放日期")
    private LocalDate payDate;

    /** 发放状态: 0未发 1已发 */
    @Schema(description = "发放状态（0未发 1已发）", example = "0")
    private Integer payStatus;

    // ==================== Getter & Setter ====================

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getSalaryMonth() { return salaryMonth; }
    public void setSalaryMonth(String salaryMonth) { this.salaryMonth = salaryMonth; }
    public Long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }
    public Long getPositionSalary() { return positionSalary; }
    public void setPositionSalary(Long positionSalary) { this.positionSalary = positionSalary; }
    public Long getPerformanceBonus() { return performanceBonus; }
    public void setPerformanceBonus(Long performanceBonus) { this.performanceBonus = performanceBonus; }
    public Long getOvertimePay() { return overtimePay; }
    public void setOvertimePay(Long overtimePay) { this.overtimePay = overtimePay; }
    public Long getDeduction() { return deduction; }
    public void setDeduction(Long deduction) { this.deduction = deduction; }
    public Long getSocialInsurance() { return socialInsurance; }
    public void setSocialInsurance(Long socialInsurance) { this.socialInsurance = socialInsurance; }
    public Long getHousingFund() { return housingFund; }
    public void setHousingFund(Long housingFund) { this.housingFund = housingFund; }
    public Long getTax() { return tax; }
    public void setTax(Long tax) { this.tax = tax; }
    public Long getActualSalary() { return actualSalary; }
    public void setActualSalary(Long actualSalary) { this.actualSalary = actualSalary; }
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
}
