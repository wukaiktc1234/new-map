package com.foodtraceability.event;

public class EmployeeTransferredEvent extends HREvent {

    private final String employeeId;
    private final String employeeName;
    private final String oldDepartmentId;
    private final String newDepartmentId;
    private final String oldPositionId;
    private final String newPositionId;

    public EmployeeTransferredEvent(Object source, String employeeId, String employeeName,
                                     String oldDepartmentId, String newDepartmentId,
                                     String oldPositionId, String newPositionId, String operatorId) {
        super(source, "EMPLOYEE_TRANSFERRED", operatorId);
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.oldDepartmentId = oldDepartmentId;
        this.newDepartmentId = newDepartmentId;
        this.oldPositionId = oldPositionId;
        this.newPositionId = newPositionId;
    }

    // Getters
    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getOldDepartmentId() {
        return oldDepartmentId;
    }

    public String getNewDepartmentId() {
        return newDepartmentId;
    }

    public String getOldPositionId() {
        return oldPositionId;
    }

    public String getNewPositionId() {
        return newPositionId;
    }
}
