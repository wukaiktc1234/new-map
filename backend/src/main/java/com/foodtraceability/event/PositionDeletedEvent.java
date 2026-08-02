package com.foodtraceability.event;

public class PositionDeletedEvent extends HREvent {

    private final Long positionId;
    private final String positionName;
    private final Long departmentId;

    public PositionDeletedEvent(Object source, Long positionId, String positionName,
                                 Long departmentId, String operatorId) {
        super(source, "POSITION_DELETED", operatorId);
        this.positionId = positionId;
        this.positionName = positionName;
        this.departmentId = departmentId;
    }

    // Getters
    public Long getPositionId() {
        return positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }
}
