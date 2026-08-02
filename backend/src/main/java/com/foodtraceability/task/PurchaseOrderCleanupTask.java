package com.foodtraceability.task;

import com.foodtraceability.mapper.PurchaseOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderCleanupTask {
    
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderCleanupTask.class);
    

    public PurchaseOrderCleanupTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDeletedOrders() {
        log.info("开始清理超过30天的已删除采购订单...");
        try {
            String sql = "DELETE FROM purchase_orders WHERE deleted = 1 AND deleted_time < NOW() - INTERVAL '30 DAY'";
            int deleted = jdbcTemplate.update(sql);
            if (deleted > 0) {
                log.info("已清理 {} 条超过30天的已删除采购订单", deleted);
            } else {
                log.info("没有需要清理的已删除采购订单");
            }
        } catch (Exception e) {
            log.error("清理已删除采购订单失败: {}", e.getMessage());
        }
    }
}
