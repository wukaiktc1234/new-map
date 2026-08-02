package com.foodtraceability.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class KitchenOrderMigrationConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KitchenOrderMigrationConfig.class);
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try {
            log.info("=== 开始执行 kitchen_order 表重构迁移 ===");
            ensureIdColumn();
            ensureBaseColumns();
            if (isMigrationExecuted()) {
                log.info("迁移已执行，跳过");
                return;
            }
            dropRedundantColumns();
            addIndex();
            createView();
            syncExistingOrders();
            markMigrationExecuted();
            log.info("=== kitchen_order 表重构迁移完成 ===");
        } catch (Exception e) {
            log.error("迁移失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 确保 kitchen_order 表包含自增主键 id 列（与 KitchenOrder 实体对应）
     */
    private void ensureIdColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'kitchen_order' AND COLUMN_NAME = 'id'",
                    Integer.class);
            if (count == null || count == 0) {
                log.info("kitchen_order 表缺少 id 列，开始添加");
                jdbcTemplate.execute("ALTER TABLE kitchen_order ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY");
                log.info("kitchen_order 表 id 列添加成功");
            } else {
                log.info("kitchen_order 表 id 列已存在");
            }
        } catch (Exception e) {
            log.warn("确保 kitchen_order id 列时出错: {}", e.getMessage());
        }
    }

    /**
     * 确保 kitchen_order 表包含 MyBatis-Plus 自动填充所需的基础字段
     */
    private void ensureBaseColumns() {
        log.info("检查 kitchen_order 表基础字段...");
        checkAndAddColumn("create_time", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        checkAndAddColumn("update_time", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        checkAndAddColumn("create_by", "VARCHAR(50)");
        checkAndAddColumn("update_by", "VARCHAR(50)");
        checkAndAddColumn("deleted", "INTEGER NOT NULL DEFAULT 0");
    }

    private void checkAndAddColumn(String columnName, String columnDefinition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'kitchen_order' AND COLUMN_NAME = '" + columnName + "'",
                    Integer.class);
            if (count == null || count == 0) {
                log.info("kitchen_order 表缺少 {} 列，开始添加", columnName);
                jdbcTemplate.execute("ALTER TABLE kitchen_order ADD COLUMN " + columnName + " " + columnDefinition);
                log.info("kitchen_order 表 {} 列添加成功", columnName);
            } else {
                log.info("kitchen_order 表 {} 列已存在", columnName);
            }
        } catch (Exception e) {
            log.warn("确保 kitchen_order {} 列时出错: {}", columnName, e.getMessage());
        }
    }

    private boolean isMigrationExecuted() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS " + "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = \'kitchen_order\' AND COLUMN_NAME = \'total_amount\'", Integer.class);
            return count != null && count == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void dropRedundantColumns() {
        log.info("删除冗余字段 total_amount, payment_method...");
        try {
            jdbcTemplate.execute("ALTER TABLE kitchen_order DROP COLUMN total_amount");
            log.info("删除 total_amount 成功");
        } catch (Exception e) {
            log.warn("删除 total_amount 时出现警告: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE kitchen_order DROP COLUMN payment_method");
            log.info("删除 payment_method 成功");
        } catch (Exception e) {
            log.warn("删除 payment_method 时出现警告: {}", e.getMessage());
        }
    }

    private void addIndex() {
        log.info("添加 order_id 索引...");
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getIndexInfo(null, null, "kitchen_order", false, false);
            boolean hasIndex = false;
            while (rs.next()) {
                if ("order_id".equals(rs.getString("COLUMN_NAME"))) {
                    hasIndex = true;
                    break;
                }
            }
            rs.close();
            if (!hasIndex) {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_order_id ON kitchen_order(order_id)");
                log.info("索引添加成功");
            } else {
                log.info("索引已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("添加索引时出现警告: {}", e.getMessage());
        }
    }

    private void createView() {
        log.info("创建 v_kitchen_order_full 视图...");
        try {
            jdbcTemplate.execute("DROP VIEW IF EXISTS v_kitchen_order_full");
            String viewSql = """
                CREATE VIEW v_kitchen_order_full AS
                SELECT
                  ko.id,
                  ko.kitchen_order_id,
                  ko.order_id,
                  ko.order_number,
                  ko.order_type,
                  ko.table_number,
                  ko.dish_items,
                  ko.total_dishes,
                  o.ORDER_AMOUNT AS total_amount,
                  o.ACTUAL_AMOUNT AS actual_amount,
                  CASE o.PAYMENT_METHOD
                    WHEN 0 THEN \'微信支付\'
                    WHEN 1 THEN \'支付宝\'
                    WHEN 2 THEN \'现金\'
                    WHEN 3 THEN \'银行卡\'
                    ELSE \'未知\'
                  END AS payment_method,
                  ko.priority,
                  ko.status,
                  ko.receive_time,
                  ko.make_start_time,
                  ko.make_complete_time,
                  ko.serve_time,
                  ko.cancel_time,
                  ko.cancel_reason,
                  ko.chef_id,
                  ko.chef_name,
                  ko.store_id,
                  ko.store_name,
                  ko.material_consumed,
                  ko.material_consume_time,
                  ko.material_locked,
                  ko.material_lock_time,
                  ko.food_trace_codes,
                  ko.remark,
                  ko.create_time,
                  ko.update_time,
                  ko.create_by,
                  ko.update_by,
                  ko.deleted,
                  o.ORDER_STATUS AS order_status,
                  o.PAYMENT_TIME AS payment_time,
                  o.REMARKS AS order_remarks,
                  o.CONTACT_NAME AS contact_name,
                  o.CONTACT_PHONE AS contact_phone
                FROM kitchen_order ko
                LEFT JOIN orders o ON ko.order_id = o.ORDER_ID
                WHERE ko.deleted = 0
                """;
            jdbcTemplate.execute(viewSql);
            log.info("视图创建成功");
        } catch (Exception e) {
            log.warn("创建视图时出现警告: {}", e.getMessage());
        }
    }

    private void syncExistingOrders() {
        log.info("同步现有 orders 数据到 kitchen_order...");
        try {
            String insertSql = """
                INSERT INTO kitchen_order (
                  kitchen_order_id,
                  order_id,
                  order_number,
                  order_type,
                  table_number,
                  dish_items,
                  total_dishes,
                  status,
                  priority,
                  create_time,
                  deleted
                )
                SELECT
                  CONCAT(\'KO\', o.ORDER_ID) AS kitchen_order_id,
                  o.ORDER_ID AS order_id,
                  o.ORDER_NUMBER AS order_number,
                  o.ORDER_TYPE AS order_type,
                  NULL AS table_number,
                  \'[]\' AS dish_items,
                  (SELECT COUNT(*) FROM order_items oi WHERE oi.ORDER_ID = o.ORDER_ID) AS total_dishes,
                  CASE o.ORDER_STATUS
                    WHEN 0 THEN \'pending\'
                    WHEN 1 THEN \'completed\'
                    WHEN 2 THEN \'pending\'
                    WHEN 3 THEN \'making\'
                    WHEN 4 THEN \'served\'
                    WHEN 5 THEN \'cancelled\'
                    WHEN 6 THEN \'cancelled\'
                    WHEN 7 THEN \'cancelled\'
                    ELSE \'pending\'
                  END AS status,
                  0 AS priority,
                  o.CREATE_TIME AS create_time,
                  0 AS deleted
                FROM orders o
                WHERE o.DELETED = 0
                AND NOT EXISTS (
                  SELECT 1 FROM kitchen_order ko WHERE ko.order_id = o.ORDER_ID
                )
                """;
            int rows = jdbcTemplate.update(insertSql);
            log.info("同步完成，新增 {} 条记录", rows);
        } catch (Exception e) {
            log.warn("同步数据时出现警告: {}", e.getMessage());
        }
    }

    private void markMigrationExecuted() {
        log.info("迁移标记完成");
    }

    public KitchenOrderMigrationConfig(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
