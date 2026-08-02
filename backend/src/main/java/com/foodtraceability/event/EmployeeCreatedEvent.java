package com.foodtraceability.event;

public class EmployeeCreatedEvent extends HREvent {

    private final String employeeId;
    private final String employeeName;
    private final String departmentId;
    private final String positionId;

    public EmployeeCreatedEvent(Object source, String employeeId, String employeeName, 
                                String departmentId, String positionId, String operatorId) {
        super(source, "EMPLOYEE_CREATED", operatorId);
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentId = departmentId;
        this.positionId = positionId;
    }

    // Getters
    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getPositionId() {
        return positionId;
    }
}
