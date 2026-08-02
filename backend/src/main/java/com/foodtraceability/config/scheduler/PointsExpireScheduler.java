package com.foodtraceability.config.scheduler;

import com.foodtraceability.service.PointsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 积分过期清理定时任务
 * 每天凌晨2点执行，清理已过期的积分
 */
@Component
public class PointsExpireScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PointsExpireScheduler.class);

    private final PointsService pointsService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param pointsService 积分服务
     */
    public PointsExpireScheduler(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    /**
     * 每天凌晨2:00执行过期积分清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expirePoints() {
        logger.info("========== 开始执行积分过期清理任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            int count = pointsService.expirePoints();
            long costTime = System.currentTimeMillis() - startTime;
            logger.info("积分过期清理完成: 处理{}条记录, 耗时{}ms", count, costTime);
        } catch (Exception e) {
            logger.error("积分过期清理任务执行失败", e);
        }

        logger.info("========== 积分过期清理任务结束 ==========");
    }
}
