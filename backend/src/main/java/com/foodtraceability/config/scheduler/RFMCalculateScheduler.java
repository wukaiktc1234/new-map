package com.foodtraceability.config.scheduler;

import com.foodtraceability.service.MarketingAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RFM模型计算定时任务
 * 每月1号凌晨3点执行，为所有会员重新计算RFM得分和客户分层
 */
@Component
public class RFMCalculateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RFMCalculateScheduler.class);

    private final MarketingAnalysisService analysisService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param analysisService 营销分析服务
     */
    public RFMCalculateScheduler(MarketingAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 每月1号凌晨3:00执行RFM模型计算
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void calculateRFM() {
        logger.info("========== 开始执行RFM模型月度计算任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> result = analysisService.calculateRFM();

            // 记录计算日志（可扩展：写入rfm_calculation_log表）
            int totalMembers = result.get("totalMembers") != null ?
                    ((Number) result.get("totalMembers")).intValue() : 0;
            int processedCount = result.get("processedCount") != null ?
                    ((Number) result.get("processedCount")).intValue() : 0;
            long costTime = System.currentTimeMillis() - startTime;

            @SuppressWarnings("unchecked")
            Map<String, Integer> distribution =
                    (Map<String, Integer>) result.get("distribution");

            logger.info("RFM计算完成: 总会员={}, 处理={}, 耗时={}ms", totalMembers, processedCount, costTime);
            if (distribution != null) {
                for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                    logger.info("  - {}: {}人", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            logger.error("RFM计算任务执行失败", e);
        }

        logger.info("========== RFM模型计算任务结束 ==========");
    }
}
