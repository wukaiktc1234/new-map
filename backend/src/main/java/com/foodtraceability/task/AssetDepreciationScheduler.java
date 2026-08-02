package com.foodtraceability.task;

import com.foodtraceability.service.AssetMasterNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 资产折旧定时任务
 * 每月1号凌晨自动执行折旧计算
 */
@Component
public class AssetDepreciationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssetDepreciationScheduler.class);

    private final AssetMasterNewService assetMasterNewService;

    public AssetDepreciationScheduler(AssetMasterNewService assetMasterNewService) {
        this.assetMasterNewService = assetMasterNewService;
    }

    /**
     * 每月1日凌晨2点执行月度折旧计算
     * Cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void executeMonthlyDepreciation() {
        log.info("========== 开始执行月度折旧任务 ==========");
        
        try {
            // 获取上个月的期间（如2026-03）
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String period = lastMonth.toString(); // 格式: yyyy-MM
            
            log.info("执行折旧期间: {}", period);
            
            int count = assetMasterNewService.executeMonthlyDepreciation(period);
            
            log.info("========== 月度折旧任务完成: 处理{}个资产 ==========", count);
        } catch (Exception e) {
            log.error("月度折旧任务执行失败", e);
        }
    }
}
