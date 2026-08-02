package com.foodtraceability.config.scheduler;

import com.foodtraceability.service.CouponTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券过期处理定时任务
 * 每天凌晨2:30执行，将已过期的未使用优惠券标记为已过期
 */
@Component
public class CouponExpireScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CouponExpireScheduler.class);

    private final CouponTemplateService couponService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param couponService 优惠券服务
     */
    public CouponExpireScheduler(CouponTemplateService couponService) {
        this.couponService = couponService;
    }

    /**
     * 每天凌晨2:30执行优惠券过期处理
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void expireCoupons() {
        logger.info("========== 开始执行优惠券过期处理任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            int count = couponService.expireCoupons();
            long costTime = System.currentTimeMillis() - startTime;
            logger.info("优惠券过期处理完成: 标记{}张过期, 耗时{}ms", count, costTime);
        } catch (Exception e) {
            logger.error("优惠券过期处理任务执行失败", e);
        }

        logger.info("========== 优惠券过期处理任务结束 ==========");
    }
}
