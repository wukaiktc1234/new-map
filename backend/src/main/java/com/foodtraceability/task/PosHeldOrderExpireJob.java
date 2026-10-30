package com.foodtraceability.task;

import com.foodtraceability.service.PosHeldOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 挂单 TTL 清理（Owner 2026-10-30 P0）。
 *
 * <p>每小时把 expire_at &lt; now() 的 HOLD 置 EXPIRED。幂等：可重复执行，行数只减不增。</p>
 */
@Component
public class PosHeldOrderExpireJob {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(PosHeldOrderExpireJob.class);

    private final PosHeldOrderService service;

    public PosHeldOrderExpireJob(PosHeldOrderService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 5 * * * ?")
    public void expire() {
        try {
            int n = service.expireOverdue();
            if (n > 0) {
                log.info("[METRIC] hold_expire_job rows={}", n);
            }
        } catch (Exception e) {
            log.warn("[METRIC] hold_expire_job_failed reason={}", e.getMessage());
        }
    }
}
