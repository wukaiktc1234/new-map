package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 薪资记录实体类
 * @author example
 * @since 2025-12-05
 */
@TableName("salary_record")
public class SalaryRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID
     */
    @TableField("employee_id")
    private Long employeeId;

    /**
     * 员工姓名
     */
    @TableField("employee_name")
    private String employeeName;

    /**
     * 工号
     */
    @TableField("employee_number")
    private String employeeNumber;

    /**
     * 部门
     */
    @TableField("department")
    private String department;

    /**
     * 职位
     */
    @TableField("position")
    private String position;

    /**
     * 基本工资
     */
    @TableField("basic_salary")
    private BigDecimal basicSalary;

    /**
     * 绩效奖金
     */
    @TableField("performance_bonus")
    private BigDecimal performanceBonus;

    /**
     * 加班费
     */
    @TableField("overtime_pay")
    private BigDecimal overtimePay;

    /**
     * 津贴
     */
    @TableField("allowance")
    private BigDecimal allowance;

    /**
     * 应发工资
     */
    @TableField("total_earnings")
    private BigDecimal totalEarnings;

    /**
     * 社保
     */
    @TableField("insurance")
    private BigDecimal insurance;

    /**
     * 个税
     */
    @TableField("tax")
    private BigDecimal tax;

    /**
     * 其他扣款
     */
    @TableField("other_deductions")
    private BigDecimal otherDeductions;

    /**
     * 实发工资
     */
    @TableField("final_salary")
    private BigDecimal finalSalary;

    /**
     * 薪资月份
     */
    @TableField("salary_month")
    private String salaryMonth;

    /**
     * 状态：待确认、已确认
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }

    public BigDecimal getPerformanceBonus() {
        return performanceBonus;
    }

    public void setPerformanceBonus(BigDecimal performanceBonus) {
        this.performanceBonus = performanceBonus;
    }

    public BigDecimal getOvertimePay() {
        return overtimePay;
    }

    public void setOvertimePay(BigDecimal overtimePay) {
        this.overtimePay = overtimePay;
    }

    public BigDecimal getAllowance() {
        return allowance;
    }

    public void setAllowance(BigDecimal allowance) {
        this.allowance = allowance;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public BigDecimal getInsurance() {
        return insurance;
    }

    public void setInsurance(BigDecimal insurance) {
        this.insurance = insurance;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public BigDecimal getFinalSalary() {
        return finalSalary;
    }

    public void setFinalSalary(BigDecimal finalSalary) {
        this.finalSalary = finalSalary;
    }

    public String getSalaryMonth() {
        return salaryMonth;
    }

    public void setSalaryMonth(String salaryMonth) {
        this.salaryMonth = salaryMonth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    @Override
    public String toString() {
        return "SalaryRecord{" +
            "id=" + id +
            ", employeeId=" + employeeId +
            ", employeeName='" + employeeName + '\'' +
            ", employeeNumber='" + employeeNumber + '\'' +
            ", department='" + department + '\'' +
            ", position='" + position + '\'' +
            ", basicSalary=" + basicSalary +
            ", performanceBonus=" + performanceBonus +
            ", overtimePay=" + overtimePay +
            ", allowance=" + allowance +
            ", totalEarnings=" + totalEarnings +
            ", insurance=" + insurance +
            ", tax=" + tax +
            ", otherDeductions=" + otherDeductions +
            ", finalSalary=" + finalSalary +
            ", salaryMonth='" + salaryMonth + '\'' +
            ", status='" + status + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", createBy='" + createBy + '\'' +
            ", updateBy='" + updateBy + '\'' +
            '}';
    }
}