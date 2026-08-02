package com.foodtraceability.event;

import com.foodtraceability.entity.HealthCertificate;
import org.springframework.context.ApplicationEvent;

/**
 * 健康证审核通过事件
 * 用于通知财务系统记录报销费用
 */
public class HealthCertificateApprovedEvent extends ApplicationEvent {
    private final HealthCertificate healthCertificate;

    public HealthCertificateApprovedEvent(Object source, HealthCertificate healthCertificate) {
        super(source);
        this.healthCertificate = healthCertificate;
    }

    public HealthCertificate getHealthCertificate() {
        return healthCertificate;
    }
}
