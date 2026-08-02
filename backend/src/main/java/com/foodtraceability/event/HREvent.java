package com.foodtraceability.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

public abstract class HREvent extends ApplicationEvent {
    private final String eventType;
    private final String operatorId;
    private final LocalDateTime occurredAt;

    public HREvent(Object source, String eventType, String operatorId) {
        super(source);
        this.eventType = eventType;
        this.operatorId = operatorId;
        this.occurredAt = LocalDateTime.now();
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getOperatorId() {
        return this.operatorId;
    }

    public LocalDateTime getOccurredAt() {
        return this.occurredAt;
    }
}
