package com.foodtraceability.config.scheduler;

import com.foodtraceability.service.MarketingAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 流失预警定时任务
 * 每周一上午10点执行，识别流失风险会员并触发预警处理
 */
@Component
public class ChurnWarningScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ChurnWarningScheduler.class);

    private final MarketingAnalysisService analysisService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param analysisService 营销分析服务
     */
    public ChurnWarningScheduler(MarketingAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 每周一上午10:00执行流失预警
     * cron: 秒 分 时 日 月 周 (周一=1)
     */
    @Scheduled(cron = "0 0 10 ? * 1")
    public void processChurnWarning() {
        logger.info("========== 开始执行流失预警任务（周一）==========");
        long startTime = System.currentTimeMillis();

        try {
            // 默认30天未消费视为流失风险
            int processed = analysisService.processChurnWarning(30);
            long costTime = System.currentTimeMillis() - startTime;

            logger.info("流失预警处理完成: 处理{}个风险会员, 耗时{}ms", processed, costTime);

            if (processed > 0) {
                logger.info("建议关注流失风险会员，考虑发送召回优惠券或短信提醒");
            }
        } catch (Exception e) {
            logger.error("流失预警任务执行失败", e);
        }

        logger.info("========== 流失预警任务结束 ==========");
    }

    /**
     * 每日轻量检查：仅统计不处理（用于监控）
     * 每天凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void churnRiskDailyCheck() {
        try {
            var atRiskMembers = analysisService.getChurnRiskMembers(30, 10);
            int riskCount = atRiskMembers.size();
            if (riskCount > 50) {
                logger.warn("流失预警: 当前有{}个会员超过30天未消费，建议关注", riskCount);
            } else if (riskCount > 10) {
                logger.info("流失预警提示: {}个会员超过30天未消费", riskCount);
            } else {
                logger.debug("流失风险会员数量正常: {}人", riskCount);
            }
        } catch (Exception e) {
            logger.error("每日流失检查失败", e);
        }
    }
}
