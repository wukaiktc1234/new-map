package com.foodtraceability.config.job;

import com.foodtraceability.service.PurchaseArrivalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 到货单超期自动关闭定时任务
 * 每日凌晨1点扫描并关闭超期待收货的到货单
 */
@Component
public class ArrivalAutoCloseJob {

    private static final Logger log = LoggerFactory.getLogger(ArrivalAutoCloseJob.class);

    private final PurchaseArrivalService purchaseArrivalService;

    public ArrivalAutoCloseJob(PurchaseArrivalService purchaseArrivalService) {
        this.purchaseArrivalService = purchaseArrivalService;
    }

    /**
     * 每日凌晨1点执行超期到货单自动关闭
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void autoCloseOverdueArrivals() {
        log.info("开始执行到货单超期自动关闭任务");
        int count = purchaseArrivalService.autoCloseOverdueArrivals();
        log.info("到货单超期自动关闭任务完成，共关闭 {} 条", count);
    }
}
