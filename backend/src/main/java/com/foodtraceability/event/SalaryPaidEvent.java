package com.foodtraceability.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资发放事件
 * 触发：财务支出记录、人工成本计算
 */
public class SalaryPaidEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private Long salaryRecordId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private Long storeId;
    private String storeName;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal deduction;
    private BigDecimal actualSalary;
    private String paymentMethod;
    private String paymentPeriod;
    private LocalDateTime paymentTime;
    private String operatorName;
    private LocalDateTime eventTime;

    public SalaryPaidEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() {
        return this.eventId;
    }

    public Long getSalaryRecordId() {
        return this.salaryRecordId;
    }

    public Long getEmployeeId() {
        return this.employeeId;
    }

    public String getEmployeeName() {
        return this.employeeName;
    }

    public String getEmployeeCode() {
        return this.employeeCode;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public Long getPositionId() {
        return this.positionId;
    }

    public String getPositionName() {
        return this.positionName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public BigDecimal getBaseSalary() {
        return this.baseSalary;
    }

    public BigDecimal getBonus() {
        return this.bonus;
    }

    public BigDecimal getDeduction() {
        return this.deduction;
    }

    public BigDecimal getActualSalary() {
        return this.actualSalary;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getPaymentPeriod() {
        return this.paymentPeriod;
    }

    public LocalDateTime getPaymentTime() {
        return this.paymentTime;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getEventTime() {
        return this.eventTime;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public void setSalaryRecordId(final Long salaryRecordId) {
        this.salaryRecordId = salaryRecordId;
    }

    public void setEmployeeId(final Long employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeName(final String employeeName) {
        this.employeeName = employeeName;
    }

    public void setEmployeeCode(final String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setPositionId(final Long positionId) {
        this.positionId = positionId;
    }

    public void setPositionName(final String positionName) {
        this.positionName = positionName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setBaseSalary(final BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public void setBonus(final BigDecimal bonus) {
        this.bonus = bonus;
    }

    public void setDeduction(final BigDecimal deduction) {
        this.deduction = deduction;
    }

    public void setActualSalary(final BigDecimal actualSalary) {
        this.actualSalary = actualSalary;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentPeriod(final String paymentPeriod) {
        this.paymentPeriod = paymentPeriod;
    }

    public void setPaymentTime(final LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setEventTime(final LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SalaryPaidEvent)) return false;
        final SalaryPaidEvent other = (SalaryPaidEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$salaryRecordId = this.getSalaryRecordId();
        final java.lang.Object other$salaryRecordId = other.getSalaryRecordId();
        if (this$salaryRecordId == null ? other$salaryRecordId != null : !this$salaryRecordId.equals(other$salaryRecordId)) return false;
        final java.lang.Object this$employeeId = this.getEmployeeId();
        final java.lang.Object other$employeeId = other.getEmployeeId();
        if (this$employeeId == null ? other$employeeId != null : !this$employeeId.equals(other$employeeId)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$positionId = this.getPositionId();
        final java.lang.Object other$positionId = other.getPositionId();
        if (this$positionId == null ? other$positionId != null : !this$positionId.equals(other$positionId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$employeeName = this.getEmployeeName();
        final java.lang.Object other$employeeName = other.getEmployeeName();
        if (this$employeeName == null ? other$employeeName != null : !this$employeeName.equals(other$employeeName)) return false;
        final java.lang.Object this$employeeCode = this.getEmployeeCode();
        final java.lang.Object other$employeeCode = other.getEmployeeCode();
        if (this$employeeCode == null ? other$employeeCode != null : !this$employeeCode.equals(other$employeeCode)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$positionName = this.getPositionName();
        final java.lang.Object other$positionName = other.getPositionName();
        if (this$positionName == null ? other$positionName != null : !this$positionName.equals(other$positionName)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$baseSalary = this.getBaseSalary();
        final java.lang.Object other$baseSalary = other.getBaseSalary();
        if (this$baseSalary == null ? other$baseSalary != null : !this$baseSalary.equals(other$baseSalary)) return false;
        final java.lang.Object this$bonus = this.getBonus();
        final java.lang.Object other$bonus = other.getBonus();
        if (this$bonus == null ? other$bonus != null : !this$bonus.equals(other$bonus)) return false;
        final java.lang.Object this$deduction = this.getDeduction();
        final java.lang.Object other$deduction = other.getDeduction();
        if (this$deduction == null ? other$deduction != null : !this$deduction.equals(other$deduction)) return false;
        final java.lang.Object this$actualSalary = this.getActualSalary();
        final java.lang.Object other$actualSalary = other.getActualSalary();
        if (this$actualSalary == null ? other$actualSalary != null : !this$actualSalary.equals(other$actualSalary)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$paymentPeriod = this.getPaymentPeriod();
        final java.lang.Object other$paymentPeriod = other.getPaymentPeriod();
        if (this$paymentPeriod == null ? other$paymentPeriod != null : !this$paymentPeriod.equals(other$paymentPeriod)) return false;
        final java.lang.Object this$paymentTime = this.getPaymentTime();
        final java.lang.Object other$paymentTime = other.getPaymentTime();
        if (this$paymentTime == null ? other$paymentTime != null : !this$paymentTime.equals(other$paymentTime)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$eventTime = this.getEventTime();
        final java.lang.Object other$eventTime = other.getEventTime();
        if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SalaryPaidEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $salaryRecordId = this.getSalaryRecordId();
        result = result * PRIME + ($salaryRecordId == null ? 43 : $salaryRecordId.hashCode());
        final java.lang.Object $employeeId = this.getEmployeeId();
        result = result * PRIME + ($employeeId == null ? 43 : $employeeId.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $positionId = this.getPositionId();
        result = result * PRIME + ($positionId == null ? 43 : $positionId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $employeeName = this.getEmployeeName();
        result = result * PRIME + ($employeeName == null ? 43 : $employeeName.hashCode());
        final java.lang.Object $employeeCode = this.getEmployeeCode();
        result = result * PRIME + ($employeeCode == null ? 43 : $employeeCode.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $positionName = this.getPositionName();
        result = result * PRIME + ($positionName == null ? 43 : $positionName.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $baseSalary = this.getBaseSalary();
        result = result * PRIME + ($baseSalary == null ? 43 : $baseSalary.hashCode());
        final java.lang.Object $bonus = this.getBonus();
        result = result * PRIME + ($bonus == null ? 43 : $bonus.hashCode());
        final java.lang.Object $deduction = this.getDeduction();
        result = result * PRIME + ($deduction == null ? 43 : $deduction.hashCode());
        final java.lang.Object $actualSalary = this.getActualSalary();
        result = result * PRIME + ($actualSalary == null ? 43 : $actualSalary.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $paymentPeriod = this.getPaymentPeriod();
        result = result * PRIME + ($paymentPeriod == null ? 43 : $paymentPeriod.hashCode());
        final java.lang.Object $paymentTime = this.getPaymentTime();
        result = result * PRIME + ($paymentTime == null ? 43 : $paymentTime.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $eventTime = this.getEventTime();
        result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SalaryPaidEvent(eventId=" + this.getEventId() + ", salaryRecordId=" + this.getSalaryRecordId() + ", employeeId=" + this.getEmployeeId() + ", employeeName=" + this.getEmployeeName() + ", employeeCode=" + this.getEmployeeCode() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", positionId=" + this.getPositionId() + ", positionName=" + this.getPositionName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", baseSalary=" + this.getBaseSalary() + ", bonus=" + this.getBonus() + ", deduction=" + this.getDeduction() + ", actualSalary=" + this.getActualSalary() + ", paymentMethod=" + this.getPaymentMethod() + ", paymentPeriod=" + this.getPaymentPeriod() + ", paymentTime=" + this.getPaymentTime() + ", operatorName=" + this.getOperatorName() + ", eventTime=" + this.getEventTime() + ")";
    }
}
