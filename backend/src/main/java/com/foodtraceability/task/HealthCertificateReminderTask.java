package com.foodtraceability.task;

import com.foodtraceability.service.HealthCertificateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 健康证到期提醒定时任务
 * 已禁用所有定时任务
 */
@Component
public class HealthCertificateReminderTask {


    public HealthCertificateReminderTask(HealthCertificateService healthCertificateService) {
        this.healthCertificateService = healthCertificateService;
    }

    private final HealthCertificateService healthCertificateService;

    // 所有定时任务已禁用
}
