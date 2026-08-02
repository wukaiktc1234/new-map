package com.foodtraceability.event;

public class DepartmentDeletedEvent extends HREvent {

    private final Long departmentId;
    private final String departmentName;
    private final Long parentId;

    public DepartmentDeletedEvent(Object source, Long departmentId, String departmentName,
                                   Long parentId, String operatorId) {
        super(source, "DEPARTMENT_DELETED", operatorId);
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.parentId = parentId;
    }

    // Getters
    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Long getParentId() {
        return parentId;
    }
}
