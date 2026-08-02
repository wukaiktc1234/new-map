package com.foodtraceability.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 通知定时任务服务
 * 处理合同到期提醒、健康证到期提醒、库存预警、失败重试、过期清理等定时任务
 */
@Service
public class NotificationScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduleService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SiteNotificationService siteNotificationService;

    public NotificationScheduleService(JdbcTemplate jdbcTemplate,
                                        SiteNotificationService siteNotificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.siteNotificationService = siteNotificationService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkContractExpiry() {
        logger.info("开始检查合同到期提醒...");
        try {
            checkAndNotifyExpiry(
                    "employee_labor_contract",
                    "contract_id",
                    "employee_id",
                    "end_date",
                    "contract_no",
                    "CONTRACT_EXPIRY",
                    "contract-expiry",
                    new int[]{30, 15, 7, 1},
                    "劳动合同"
            );
        } catch (Exception e) {
            logger.error("合同到期提醒任务异常", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkHealthCertExpiry() {
        logger.info("开始检查健康证到期提醒...");
        try {
            checkAndNotifyExpiry(
                    "health_certificate",
                    "health_cert_id",
                    "employee_id",
                    "expiry_date",
                    "cert_no",
                    "HEALTH_CERT_EXPIRY",
                    "health-cert-expiry",
                    new int[]{60, 30, 15, 7},
                    "健康证"
            );
        } catch (Exception e) {
            logger.error("健康证到期提醒任务异常", e);
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void checkInventoryWarning() {
        logger.info("开始检查库存预警...");
        try {
            // 使用 inventory 表实际字段：safety_stock 为安全库存阈值（而非 warning_threshold）
            // 物料名称优先使用 material_name（新字段），回退到 product_name（旧字段）
            String sql = "SELECT i.inventory_id, COALESCE(i.material_name, i.product_name) AS product_name, " +
                    "i.current_stock, i.unit, w.warehouse_name, i.safety_stock " +
                    "FROM inventory i " +
                    "LEFT JOIN warehouses w ON i.warehouse_id = w.warehouse_id " +
                    "WHERE i.deleted = 0 AND i.current_stock <= i.safety_stock " +
                    "AND i.safety_stock > 0 " +
                    "AND NOT EXISTS (" +
                    "  SELECT 1 FROM notification n " +
                    "  WHERE n.deleted = 0 AND n.type = 'INVENTORY_WARNING' " +
                    "  AND n.business_type = 'INVENTORY' " +
                    "  AND n.business_id = i.inventory_id " +
                    "  AND n.create_time > CURRENT_TIMESTAMP - INTERVAL '24 hours'" +
                    ")";

            List<Map<String, Object>> warnings = jdbcTemplate.queryForList(sql);

            String managerSql = "SELECT user_id FROM users WHERE deleted = 0 AND status = 1 LIMIT 1";
            List<Map<String, Object>> managers = jdbcTemplate.queryForList(managerSql);

            for (Map<String, Object> warning : warnings) {
                String productName = String.valueOf(warning.get("product_name"));
                String currentStock = String.valueOf(warning.get("current_stock"));
                String unit = String.valueOf(warning.getOrDefault("unit", ""));
                String threshold = String.valueOf(warning.get("safety_stock"));
                String warehouseName = String.valueOf(warning.getOrDefault("warehouse_name", "默认仓库"));
                Long inventoryId = ((Number) warning.get("inventory_id")).longValue();

                Map<String, Object> variables = Map.of(
                        "materialName", productName,
                        "currentStock", currentStock,
                        "unit", unit,
                        "threshold", threshold,
                        "warehouseName", warehouseName
                );

                for (Map<String, Object> manager : managers) {
                    Long userId = ((Number) manager.get("user_id")).longValue();
                    siteNotificationService.sendBusinessNotification(
                            "inventory-warning", userId, variables,
                            "INVENTORY", String.valueOf(inventoryId), "HIGH"
                    );
                }
            }

            logger.info("库存预警检查完成: 发现{}条预警", warnings.size());
        } catch (Exception e) {
            logger.error("库存预警检查异常", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedMessages() {
        logger.debug("开始重试失败消息...");
        try {
            String sql = "SELECT record_id FROM msg_send_record " +
                    "WHERE deleted = 0 AND send_status = 3 " +
                    "AND retry_count < max_retry " +
                    "AND finish_time < CURRENT_TIMESTAMP - INTERVAL '5 minutes' " +
                    "LIMIT 50";
            List<Map<String, Object>> failedRecords = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> record : failedRecords) {
                Long recordId = ((Number) record.get("record_id")).longValue();
                try {
                    String updateSql = "UPDATE msg_send_record SET send_status = 0, " +
                            "retry_count = retry_count + 1, send_time = CURRENT_TIMESTAMP " +
                            "WHERE record_id = ? AND version = (SELECT version FROM msg_send_record WHERE record_id = ?)";
                    jdbcTemplate.update(updateSql, recordId, recordId);
                } catch (Exception e) {
                    logger.warn("重试消息失败: recordId={}", recordId, e);
                }
            }

            if (!failedRecords.isEmpty()) {
                logger.info("重试失败消息: 处理{}条", failedRecords.size());
            }
        } catch (Exception e) {
            logger.error("重试失败消息异常", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        logger.info("开始清理过期通知...");
        try {
            String sql = "UPDATE notification SET deleted = 1 " +
                    "WHERE deleted = 0 AND is_read = 1 " +
                    "AND read_time < CURRENT_TIMESTAMP - INTERVAL '90 days'";
            int rows = jdbcTemplate.update(sql);
            logger.info("清理过期通知: 删除{}条已读超过90天的通知", rows);
        } catch (Exception e) {
            logger.error("清理过期通知异常", e);
        }
    }

    private void checkAndNotifyExpiry(String tableName, String idColumn, String userIdColumn,
                                       String dateColumn, String codeColumn,
                                       String notificationType, String templateCode,
                                       int[] daysBeforeExpiry, String bizName) {
        for (int days : daysBeforeExpiry) {
            LocalDate targetDate = LocalDate.now().plusDays(days);

            String sql = String.format(
                    "SELECT e.%s, e.%s, e.%s, emp.user_id " +
                    "FROM %s e " +
                    "INNER JOIN employees emp ON e.employee_id = emp.employee_id " +
                    "WHERE e.deleted = 0 AND emp.deleted = 0 " +
                    "AND e.%s = ? " +
                    "AND NOT EXISTS (" +
                    "  SELECT 1 FROM notification n " +
                    "  WHERE n.deleted = 0 AND n.type = ? " +
                    "  AND n.business_type = ? " +
                    "  AND n.business_id = e.%s " +
                    "  AND n.create_time > CURRENT_TIMESTAMP - INTERVAL '24 hours'" +
                    ")",
                    idColumn, codeColumn, dateColumn, tableName, dateColumn, idColumn
            );

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, targetDate, notificationType, notificationType);

            for (Map<String, Object> record : records) {
                Long recordId = ((Number) record.get(idColumn)).longValue();
                String code = String.valueOf(record.getOrDefault(codeColumn, ""));
                String expiryDate = String.valueOf(record.get(dateColumn));
                Long userId = ((Number) record.get("user_id")).longValue();

                String employeeName = getEmployeeName(userId);

                Map<String, Object> variables = Map.of(
                        "employeeName", employeeName,
                        "expiryDate", expiryDate,
                        "contractNo", code,
                        "certNo", code
                );

                String priority = days <= 7 ? "HIGH" : "NORMAL";
                siteNotificationService.sendBusinessNotification(
                        templateCode, userId, variables,
                        notificationType, String.valueOf(recordId), priority
                );
            }

            if (!records.isEmpty()) {
                logger.info("{}到期提醒: {}天后到期{}条", bizName, days, records.size());
            }
        }
    }

    private String getEmployeeName(Long userId) {
        try {
            String sql = "SELECT e.employee_name FROM employees e " +
                    "INNER JOIN users u ON e.user_id = u.user_id " +
                    "WHERE u.user_id = ? AND e.deleted = 0 LIMIT 1";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            if (!results.isEmpty()) {
                return String.valueOf(results.get(0).get("employee_name"));
            }
        } catch (Exception e) {
            logger.warn("获取员工姓名失败: userId={}", userId);
        }
        return "用户";
    }
}
