package com.foodtraceability.task;

import com.foodtraceability.mapper.TtlOrderCloseMapper;
import com.foodtraceability.service.impl.TtlOrderCloseTxBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TTL 超时关单作业（canonical orders）。
 *
 * <p>与既有 {@code OrderTimeoutTask} 的区别：后者操作遗留 {@code Order/OrderMapper}（orders 旧表），
 * 对 canonical orders 无效 —— 实测死单（order_status=0 且未支付）92 笔长期不清理。</p>
 *
 * <p>并发安全三件套：① 作业级 PG advisory lock（多实例只放一个扫）；② LIMIT 分批（默认 500）；
 * ③ 单笔 REQUIRES_NEW + 双 CAS（order_status=0 且 payment_status&lt;&gt;2），0 行即放弃不覆盖。</p>
 */
@Component
public class TtlOrderCloseJob {

    private static final Logger log = LoggerFactory.getLogger(TtlOrderCloseJob.class);

    /** advisory lock key（任意固定值，全库唯一即可） */
    private static final long LOCK_KEY = 20261029007L;

    /**
     * 超时阈值（分钟）：默认 15（Lead 2026-10-29 约束①）。
     * <p>application-pg.yml / application-prod.yml **不得覆盖**（约束②）；
     * 自测用启动参数覆盖：{@code -Dorder.pending.timeout.minutes=1}（约束②）。</p>
     */
    @org.springframework.beans.factory.annotation.Value("${order.pending.timeout.minutes:15}")
    private int timeoutMinutes = 15;

    /** 扫描间隔（毫秒），仅用于启动日志展示 */
    private static final long SCAN_DELAY_MS = 300000L;

    /** 单批上限 */
    private static final int BATCH_LIMIT = 500;

    /** 启动即打印生效值（约束③）：防止「以为在跑 15 分钟，实际是 1 分钟」 */
    @jakarta.annotation.PostConstruct
    public void logEffectiveConfig() {
        log.info("[TTL] timeout minutes={} scan delay={} batch={}", timeoutMinutes, SCAN_DELAY_MS, BATCH_LIMIT);
    }

    private final TtlOrderCloseMapper closeMapper;
    private final TtlOrderCloseTxBean txBean;

    public TtlOrderCloseJob(TtlOrderCloseMapper closeMapper, TtlOrderCloseTxBean txBean) {
        this.closeMapper = closeMapper;
        this.txBean = txBean;
    }

    @Scheduled(fixedDelay = 300000L, initialDelay = 60000L)
    public void sweep() {
        boolean locked = false;
        try {
            locked = closeMapper.tryAdvisoryLock(LOCK_KEY);
            if (!locked) {
                log.info("[METRIC] ttl_close_skipped reason=advisory_lock_held");
                return;
            }
            List<String> ids = closeMapper.findExpiredOrderIds(timeoutMinutes, BATCH_LIMIT);
            if (ids == null || ids.isEmpty()) {
                log.info("[METRIC] ttl_close_batch size=0");
                return;
            }
            int closed = 0;
            int skipped = 0;
            for (String id : ids) {
                try {
                    if (txBean.closeOne(id)) {
                        closed++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    skipped++;
                    log.error("[METRIC] ttl_close_failed orderId={} err={}", id, e.getMessage());
                }
            }
            log.info("[METRIC] ttl_close_batch size={} closed={} skipped={} limit={}",
                    ids.size(), closed, skipped, BATCH_LIMIT);
        } catch (Exception e) {
            log.error("[METRIC] ttl_close_error err={}", e.getMessage());
        } finally {
            if (locked) {
                try {
                    closeMapper.releaseAdvisoryLock(LOCK_KEY);
                } catch (Exception ignore) {
                    // 会话结束自动释放，这里失败不影响正确性
                }
            }
        }
    }
}
