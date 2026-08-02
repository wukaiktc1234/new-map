package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 薪资记录VO
 * 用于前端展示的完整薪资信息
 */
@Schema(description = "薪资记录视图对象")
public class SalaryRecordVO {

    @Schema(description = "薪资记录ID", example = "1")
    private Long id;

    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "部门名称", example = "后厨部")
    private String departmentName;

    @Schema(description = "职位名称", example = "厨师")
    private String positionName;

    @Schema(description = "薪资月份", example = "2026-04")
    private String salaryMonth;

    /** 基本工资（元） */
    @Schema(description = "基本工资（元）", example = "8000.00")
    private String baseSalaryDisplay;

    /** 岗位工资（元） */
    @Schema(description = "岗位工资（元）", example = "2000.00")
    private String positionSalaryDisplay;

    /** 绩效奖金（元） */
    @Schema(description = "绩效奖金（元）", example = "1000.00")
    private String performanceBonusDisplay;

    /** 加班费（元） */
    @Schema(description = "加班费（元）", example = "500.00")
    private String overtimePayDisplay;

    /** 应发合计（元） */
    @Schema(description = "应发合计（元）", example = "11500.00")
    private String totalEarningsDisplay;

    /** 扣款（元） */
    @Schema(description = "扣款（元）", example = "0.00")
    private String deductionDisplay;

    /** 社保个人部分（元） */
    @Schema(description = "社保个人部分（元）", example = "800.00")
    private String socialInsuranceDisplay;

    /** 公积金个人部分（元） */
    @Schema(description = "公积金个人部分（元）", example = "400.00")
    private String housingFundDisplay;

    /** 个人所得税（元） */
    @Schema(description = "个人所得税（元）", example = "100.00")
    private String taxDisplay;

    /** 扣款合计（元） */
    @Schema(description = "扣款合计（元）", example = "1300.00")
    private String totalDeductionDisplay;

    /** 实发工资（元） */
    @Schema(description = "实发工资（元）", example = "10200.00")
    private String actualSalaryDisplay;

    @Schema(description = "发放日期", example = "2026-05-10")
    private String payDate;

    /** 发放状态: 0未发 1已发 */
    @Schema(description = "发放状态", example = "1")
    private Integer payStatus;

    @Schema(description = "发放状态名称", example = "已发")
    private String payStatusName;

    // ==================== Getter & Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public String getSalaryMonth() { return salaryMonth; }
    public void setSalaryMonth(String salaryMonth) { this.salaryMonth = salaryMonth; }
    public String getBaseSalaryDisplay() { return baseSalaryDisplay; }
    public void setBaseSalaryDisplay(String baseSalaryDisplay) { this.baseSalaryDisplay = baseSalaryDisplay; }
    public String getPositionSalaryDisplay() { return positionSalaryDisplay; }
    public void setPositionSalaryDisplay(String positionSalaryDisplay) { this.positionSalaryDisplay = positionSalaryDisplay; }
    public String getPerformanceBonusDisplay() { return performanceBonusDisplay; }
    public void setPerformanceBonusDisplay(String performanceBonusDisplay) { this.performanceBonusDisplay = performanceBonusDisplay; }
    public String getOvertimePayDisplay() { return overtimePayDisplay; }
    public void setOvertimePayDisplay(String overtimePayDisplay) { this.overtimePayDisplay = overtimePayDisplay; }
    public String getTotalEarningsDisplay() { return totalEarningsDisplay; }
    public void setTotalEarningsDisplay(String totalEarningsDisplay) { this.totalEarningsDisplay = totalEarningsDisplay; }
    public String getDeductionDisplay() { return deductionDisplay; }
    public void setDeductionDisplay(String deductionDisplay) { this.deductionDisplay = deductionDisplay; }
    public String getSocialInsuranceDisplay() { return socialInsuranceDisplay; }
    public void setSocialInsuranceDisplay(String socialInsuranceDisplay) { this.socialInsuranceDisplay = socialInsuranceDisplay; }
    public String getHousingFundDisplay() { return housingFundDisplay; }
    public void setHousingFundDisplay(String housingFundDisplay) { this.housingFundDisplay = housingFundDisplay; }
    public String getTaxDisplay() { return taxDisplay; }
    public void setTaxDisplay(String taxDisplay) { this.taxDisplay = taxDisplay; }
    public String getTotalDeductionDisplay() { return totalDeductionDisplay; }
    public void setTotalDeductionDisplay(String totalDeductionDisplay) { this.totalDeductionDisplay = totalDeductionDisplay; }
    public String getActualSalaryDisplay() { return actualSalaryDisplay; }
    public void setActualSalaryDisplay(String actualSalaryDisplay) { this.actualSalaryDisplay = actualSalaryDisplay; }
    public String getPayDate() { return payDate; }
    public void setPayDate(String payDate) { this.payDate = payDate; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public String getPayStatusName() { return payStatusName; }
    public void setPayStatusName(String payStatusName) { this.payStatusName = payStatusName; }
}
