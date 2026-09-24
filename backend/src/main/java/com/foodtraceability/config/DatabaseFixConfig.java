package com.foodtraceability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Configuration
public class DatabaseFixConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseFixConfig.class);

    @Bean
    public CommandLineRunner fixDatabaseSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("===== 检查并修复数据库表结构 =====");

            try {
                // 检查并添加缺失的字段
                checkAndAddColumn(jdbcTemplate, "food", "device_id", "VARCHAR(50) DEFAULT NULL");
                checkAndAddColumn(jdbcTemplate, "food", "sensor_data", "TEXT DEFAULT NULL");
                checkAndAddColumn(jdbcTemplate, "food", "rfid_tag", "VARCHAR(100) DEFAULT NULL");
                checkAndAddColumn(jdbcTemplate, "food", "gps_location", "VARCHAR(100) DEFAULT NULL");
                checkAndAddColumn(jdbcTemplate, "food", "collect_time", "TIMESTAMP DEFAULT NULL");

                // 修复departments表
                checkAndAddColumn(jdbcTemplate, "departments", "employee_count", "INT DEFAULT 0");
                checkAndAddColumn(jdbcTemplate, "departments", "base_salary", "DECIMAL(12,2) DEFAULT NULL");

                // 修复employees表
                checkAndAddColumn(jdbcTemplate, "employees", "base_salary", "DECIMAL(12,2) DEFAULT 0.00");
                
                // 修复kitchen_order表
                checkAndAddColumn(jdbcTemplate, "kitchen_order", "total_amount", "DECIMAL(12,2) DEFAULT 0.00");
                checkAndAddColumn(jdbcTemplate, "kitchen_order", "payment_method", "VARCHAR(50) DEFAULT NULL");

                // 修复purchase_stockin表
                checkAndAddColumn(jdbcTemplate, "purchase_stockin", "specification", "VARCHAR(100) DEFAULT NULL");
                
                // 迁移历史数据：将product_code中的规格值迁移到specification字段
                migratePurchaseStockinSpecification(jdbcTemplate);

                // 初始化全局配置表
                initializeGlobalConfigTable(jdbcTemplate);

                // 已废弃：position_code_rules 表仅 legacy frontend 使用，活跃前端均不调用
                // 但保留 purchase_orders 表的字段修复逻辑（reject_reason/deleted_by/deleted_time）
                fixPurchaseOrderColumns(jdbcTemplate);

                // 初始化基础数据
                initializeBaseData(jdbcTemplate);

                // 初始化采购入库表
                // C14已删除: initializePurchaseStockinTable (no-op, 由PurchaseDatabaseInitializer负责)

                // C14已删除: initializeSalesOrderTable (no-op, 由对应Initializer负责)
                // C14已删除: initializeFinanceVoucherTable (no-op, 由对应Initializer负责)
                // C14已删除: initializeComboInventoryTable (由InventoryDatabaseInitializer负责)
                // C14已删除: initializeDishInventoryTable (由InventoryDatabaseInitializer负责)

                // 初始化菜品表
                initializeFoodTable(jdbcTemplate);

                // 初始化套餐表
                initializeDishComboTable(jdbcTemplate);

                // 同步产品中心新表数据到 legacy 表，保证 POS 订单创建逻辑可用
                syncFoodsToLegacyFood(jdbcTemplate);
                syncDishCombosToLegacyDishCombo(jdbcTemplate);
                // P1-COMBO-ORDER-001: 过渡期同步 combo_ingredients → combo_ingredient
                syncComboIngredientsToLegacy(jdbcTemplate);

                // 已废弃：stores 表仅 legacy frontend 使用，活跃前端均不调用
                // initializeStoresTable(jdbcTemplate);

                // 初始化原料追溯码表
                initializeMaterialTraceCodeTable(jdbcTemplate);

                // C14已删除: initializeSuppliersTable (由PurchaseDatabaseInitializer负责)

                // 已废弃：material_template 表仅 legacy frontend 使用，活跃前端均不调用
                // initializeMaterialTemplateTable(jdbcTemplate);

                // 初始化产品定价历史记录表（审计表，用于记录所有价格变动）
                initializeProductPricingHistoryTable(jdbcTemplate);

                // 初始化订单管理相关表（orders/order_items/order_payment_records/order_refund_records）
                // schema.sql 中的 ORDERS 表为旧版结构，需重建为 V6.0.0 新版结构以匹配 OrderNew 实体
                initializeOrdersTables(jdbcTemplate);

                // 修复订单删除状态
                fixPurchaseOrderDeletedStatus(jdbcTemplate);

                log.info("===== 数据库表结构修复完成 =====");
            } catch (Exception e) {
                log.error("修复数据库表结构时出错: {}", e.getMessage(), e);
            }
        };
    }

    /**
     * 修复订单删除状态
     * 修复 deleted_by 有值但 deleted=0 的数据
     */
    private void fixPurchaseOrderDeletedStatus(JdbcTemplate jdbcTemplate) {
        try {
            String fixSql = "UPDATE purchase_orders SET deleted = 1 WHERE deleted_by IS NOT NULL AND deleted = 0";
            int fixed = jdbcTemplate.update(fixSql);
            if (fixed > 0) {
                log.info("修复了 {} 条订单删除状态异常的数据", fixed);
            }
        } catch (Exception e) {
            log.warn("修复订单删除状态时出错: {}", e.getMessage());
        }

        try {
            String fixNullSql = "UPDATE purchase_orders SET deleted_by = 'system', deleted_time = NOW() WHERE deleted = 1 AND deleted_by IS NULL";
            int fixedNull = jdbcTemplate.update(fixNullSql);
            if (fixedNull > 0) {
                log.info("修复了 {} 条订单删除人和删除时间为空的记录", fixedNull);
            }
        } catch (Exception e) {
            log.warn("修复订单删除人时间时出错: {}", e.getMessage());
        }
    }

    /**
     * 修复 purchase_orders 表缺失的字段
     * 从 initializePositionCodeRulesTable 方法中提取，仅保留 purchase_orders 相关字段修复
     */
    private void fixPurchaseOrderColumns(JdbcTemplate jdbcTemplate) {
        checkAndAddColumn(jdbcTemplate, "purchase_orders", "reject_reason", "TEXT DEFAULT NULL");
        checkAndAddColumn(jdbcTemplate, "purchase_orders", "deleted_by", "VARCHAR(50) DEFAULT NULL");
        checkAndAddColumn(jdbcTemplate, "purchase_orders", "deleted_time", "TIMESTAMP DEFAULT NULL");
    }

    /**
     * 初始化全局配置表的默认数据（必要种子数据）
     *
     * <p>表结构由 BaseDatabaseInitializer.createGlobalConfigTable() 负责（@Order(1)，先于本类执行）。</p>
     */
    private void initializeGlobalConfigTable(JdbcTemplate jdbcTemplate) {
        try {
            // 初始化默认的职位编码格式模板配置
            String checkConfigSql = "SELECT COUNT(*) FROM global_config WHERE config_key = 'position_code_format_template'";
            Integer count = jdbcTemplate.queryForObject(checkConfigSql, Integer.class);
            if (count == null || count == 0) {
                log.info("初始化默认的职位编码格式模板配置");
                String insertConfigSql = "INSERT INTO global_config (config_key, config_value, config_desc) VALUES ('position_code_format_template', '{DEPT}-{CODE}-{SEQ}', '职位编码格式模板')";
                jdbcTemplate.update(insertConfigSql);
                log.info("默认职位编码格式模板配置初始化成功");
            }
        } catch (Exception e) {
            log.warn("初始化全局配置表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查并添加缺失的列
     */
    private void checkAndAddColumn(@Nonnull JdbcTemplate jdbcTemplate, @Nonnull String tableName, @Nonnull String columnName, @Nonnull String columnDefinition) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try {
                // PostgreSQL information_schema 中表名/列名为小写
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName.toLowerCase(), columnName.toLowerCase());
                if (count == null || count == 0) {
                    String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                    jdbcTemplate.execute(alterSql);
                    log.info("添加列成功: {}.{}", tableName, columnName);
                } else {
                    log.debug("列已存在: {}.{}", tableName, columnName);
                }
            } catch (Exception e) {
                log.debug("检查列是否存在时出错（可能表不存在）: {}.{} - {}", tableName, columnName, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("添加列失败: {}.{} - {}", tableName, columnName, e.getMessage());
        }
    }

    /**
     * 初始化产品定价历史记录表
     * 对应迁移脚本 V20260629_001__create_product_pricing_history_table.sql
     */
    private void initializeProductPricingHistoryTable(@Nonnull JdbcTemplate jdbcTemplate) {
        try {
            // PostgreSQL information_schema 中表名为小写
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'product_pricing_history'";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
            if (!tables.isEmpty()) {
                log.debug("product_pricing_history 表已存在，跳过创建");
                return;
            }

            log.info("product_pricing_history 表不存在，开始创建");
            String createTableSql = "CREATE TABLE product_pricing_history (" +
                    "pricing_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "product_type      VARCHAR(20)  NOT NULL," +
                    "product_name      VARCHAR(200) NOT NULL," +
                    "product_id        BIGINT       NOT NULL," +
                    "old_sale_price    BIGINT," +
                    "new_sale_price    BIGINT       NOT NULL," +
                    "cost_price        BIGINT," +
                    "pricing_strategy  VARCHAR(30)  DEFAULT 'MANUAL'," +
                    "remark            TEXT," +
                    "operator_id       BIGINT," +
                    "operator_name     VARCHAR(100)," +
                    "effective_date    DATE," +
                    "create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP," +
                    "update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP," +
                    "deleted           SMALLINT     DEFAULT 0" +
                    ")";
            jdbcTemplate.execute(createTableSql);

            // 创建索引
            jdbcTemplate.execute("CREATE INDEX idx_pricing_history_product_id     ON product_pricing_history (product_id)");
            jdbcTemplate.execute("CREATE INDEX idx_pricing_history_product_type   ON product_pricing_history (product_type)");
            jdbcTemplate.execute("CREATE INDEX idx_pricing_history_effective_date ON product_pricing_history (effective_date)");
            jdbcTemplate.execute("CREATE INDEX idx_pricing_history_create_time    ON product_pricing_history (create_time)");
            jdbcTemplate.execute("CREATE INDEX idx_pricing_history_operator_id    ON product_pricing_history (operator_id)");

            log.info("product_pricing_history 表创建成功");
        } catch (Exception e) {
            log.warn("初始化 product_pricing_history 表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 初始化订单管理相关表
     * 创建与 Order / OrderItem 实体匹配的旧版结构表
     */
    private void initializeOrdersTables(@Nonnull JdbcTemplate jdbcTemplate) {
        try {
            // 检查 ORDERS 表是否与 Order 实体匹配（缺少 ORDER_NUMBER 字段则为不匹配的 V6.0.0 结构）
            boolean needRebuild = false;
            try {
                // PostgreSQL information_schema 中表名/列名为小写
                String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'order_number'";
                Integer count = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);
                if (count == null || count == 0) {
                    needRebuild = true;
                    log.info("ORDERS 表缺少 order_number 字段，需要重建为与 Order 实体匹配的结构");
                }
            } catch (Exception e) {
                log.debug("检查 ORDERS 表结构时出错: {}", e.getMessage());
                needRebuild = true;
            }

            if (needRebuild) {
                log.info("开始重建订单管理相关表（匹配 Order / OrderItem 实体结构）");
                // 删除旧版表（含外键依赖顺序：先子表后父表）
                jdbcTemplate.execute("DROP TABLE IF EXISTS order_items");
                jdbcTemplate.execute("DROP TABLE IF EXISTS orders");
                log.info("已删除旧版 ORDERS/ORDER_ITEMS 表");

                // 创建与 Order 实体匹配的 ORDERS 表
                jdbcTemplate.execute(
                        "CREATE TABLE orders (" +
                        "  order_id                  VARCHAR(32) PRIMARY KEY," +
                        "  user_id                   VARCHAR(50)," +
                        "  order_number              VARCHAR(32) NOT NULL UNIQUE," +
                        "  order_type                INTEGER DEFAULT 0," +
                        "  order_status              INTEGER DEFAULT 0," +
                        "  order_amount              DECIMAL(12,2) DEFAULT 0," +
                        "  discount_amount           DECIMAL(12,2) DEFAULT 0," +
                        "  actual_amount             DECIMAL(12,2) DEFAULT 0," +
                        "  payment_method            INTEGER DEFAULT 2," +
                        "  transaction_id            VARCHAR(100)," +
                        "  payment_time              TIMESTAMP," +
                        "  delivery_address          TEXT," +
                        "  contact_name              VARCHAR(50)," +
                        "  contact_phone             VARCHAR(20)," +
                        "  remarks                   TEXT," +
                        "  idempotency_key           VARCHAR(100)," +
                        "  order_source              INTEGER DEFAULT 4," +
                        "  merchant_id               VARCHAR(50)," +
                        "  deliveryman_id            VARCHAR(50)," +
                        "  estimated_delivery_time   TIMESTAMP," +
                        "  actual_delivery_time      TIMESTAMP," +
                        "  cancel_reason             TEXT," +
                        "  refund_reason             TEXT," +
                        "  refund_amount             DECIMAL(12,2) DEFAULT 0," +
                        "  refund_time               TIMESTAMP," +
                        "  create_time               TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "  update_time               TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "  create_by                 VARCHAR(50)," +
                        "  update_by                 VARCHAR(50)," +
                        "  deleted                   INTEGER DEFAULT 0" +
                        ")");
                jdbcTemplate.execute("CREATE UNIQUE INDEX uk_orders_order_number ON orders(order_number)");
                jdbcTemplate.execute("CREATE INDEX idx_orders_order_status ON orders(order_status)");
                jdbcTemplate.execute("CREATE INDEX idx_orders_order_type   ON orders(order_type)");
                jdbcTemplate.execute("CREATE INDEX idx_orders_create_time  ON orders(create_time)");
                log.info("orders 表（匹配 Order 实体）创建成功");
            }

            // 单独确保 order_items 表存在（与 OrderItem 实体匹配）
            // 历史数据场景下 orders 表已存在但 order_items 表可能缺失
            createTableIfNotExists(jdbcTemplate, "order_items",
                    "CREATE TABLE order_items (" +
                    "  order_item_id  VARCHAR(32) PRIMARY KEY," +
                    "  order_id       VARCHAR(32) NOT NULL," +
                    "  food_id        VARCHAR(32)," +
                    "  food_name      VARCHAR(100) NOT NULL," +
                    "  unit_price     DECIMAL(12,2) DEFAULT 0," +
                    "  quantity       INTEGER NOT NULL," +
                    "  subtotal_amount DECIMAL(12,2) DEFAULT 0," +
                    "  food_image_url VARCHAR(500)," +
                    "  specification  VARCHAR(200)," +
                    "  create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  deleted        INTEGER DEFAULT 0" +
                    ")",
                    new String[]{
                            "CREATE INDEX idx_order_items_order_id ON order_items(order_id)",
                            "CREATE INDEX idx_order_items_food_id  ON order_items(food_id)"
                    });

            // 为 order_items 添加外键约束（order_items.order_id → orders.order_id）
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order_id " +
                        "FOREIGN KEY (order_id) REFERENCES orders(order_id)");
                log.info("order_items 外键约束 fk_order_items_order_id 创建成功");
            } catch (Exception e) {
                // 外键约束已存在时忽略（幂等）
                log.debug("order_items 外键约束可能已存在: {}", e.getMessage());
            }

            // 创建支付记录表（如果不存在）
            createTableIfNotExists(jdbcTemplate, "order_payment_records",
                    "CREATE TABLE order_payment_records (" +
                    "  payment_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  order_id       BIGINT NOT NULL," +
                    "  payment_method INTEGER NOT NULL," +
                    "  payment_amount BIGINT NOT NULL," +
                    "  transaction_no VARCHAR(100)," +
                    "  payment_time   TIMESTAMP NOT NULL," +
                    "  operator_id    BIGINT," +
                    "  remark          TEXT," +
                    "  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  deleted         INTEGER DEFAULT 0" +
                    ")",
                    new String[]{
                            "CREATE INDEX idx_order_payment_records_order_id    ON order_payment_records(order_id)",
                            "CREATE INDEX idx_order_payment_records_payment_time ON order_payment_records(payment_time)"
                    });

            // 创建退款记录表（如果不存在）
            createTableIfNotExists(jdbcTemplate, "order_refund_records",
                    "CREATE TABLE order_refund_records (" +
                    "  refund_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  order_id         BIGINT NOT NULL," +
                    "  refund_type       INTEGER NOT NULL," +
                    "  refund_amount     BIGINT NOT NULL," +
                    "  refund_reason     VARCHAR(500)," +
                    "  refund_method     INTEGER DEFAULT 1," +
                    "  approve_user_id   BIGINT," +
                    "  refund_status     INTEGER DEFAULT 0," +
                    "  create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  complete_time     TIMESTAMP," +
                    "  update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  deleted           INTEGER DEFAULT 0" +
                    ")",
                    new String[]{
                            "CREATE INDEX idx_order_refund_records_order_id      ON order_refund_records(order_id)",
                            "CREATE INDEX idx_order_refund_records_refund_status ON order_refund_records(refund_status)"
                    });

            // DF-022/024 修复：为 order_refund_records 表补充支付渠道状态与退款明细字段
            // payment_channel_status：0未提交 1待渠道处理 2渠道处理成功 3渠道处理失败
            // payment_channel_submit_time：支付渠道提交时间
            // refund_item_ids：退款明细ID列表（CSV格式，部分退款时使用）
            checkAndAddColumn(jdbcTemplate, "order_refund_records", "payment_channel_status", "INTEGER DEFAULT 0");
            checkAndAddColumn(jdbcTemplate, "order_refund_records", "payment_channel_submit_time", "TIMESTAMP");
            checkAndAddColumn(jdbcTemplate, "order_refund_records", "refund_item_ids", "VARCHAR(2000)");

            // 创建桌台表（如果不存在，堂食订单需要）
            // 字段与 DiningTableNew 实体一致：store_id/seats_count/min_people/max_people
            createTableIfNotExists(jdbcTemplate, "dining_tables",
                    "CREATE TABLE dining_tables (" +
                    "  table_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  store_id        BIGINT DEFAULT 1," +
                    "  table_code      VARCHAR(32) NOT NULL," +
                    "  table_name      VARCHAR(50) NOT NULL," +
                    "  area_id         BIGINT," +
                    "  table_type      INTEGER DEFAULT 1," +
                    "  seats_count     INTEGER DEFAULT 4," +
                    "  min_people      INTEGER DEFAULT 1," +
                    "  max_people      INTEGER DEFAULT 10," +
                    "  status          INTEGER DEFAULT 1," +
                    "  current_order_id BIGINT," +
                    "  qr_code         VARCHAR(500)," +
                    "  sort_order      INTEGER DEFAULT 0," +
                    "  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  deleted         INTEGER DEFAULT 0" +
                    ")",
                    new String[]{
                            "CREATE INDEX idx_dining_tables_store_id  ON dining_tables(store_id)",
                            "CREATE INDEX idx_dining_tables_area_id   ON dining_tables(area_id)",
                            "CREATE INDEX idx_dining_tables_status     ON dining_tables(status)",
                            "CREATE UNIQUE INDEX uk_dining_tables_code ON dining_tables(table_code)"
                    });

            // 兼容性修复：旧版 dining_tables 表可能缺少 store_id/seats_count/min_people/max_people 字段
            // 通过 ALTER TABLE 添加缺失列，确保现有数据库也能正常工作
            checkAndAddColumn(jdbcTemplate, "dining_tables", "store_id", "BIGINT DEFAULT 1");
            checkAndAddColumn(jdbcTemplate, "dining_tables", "seats_count", "INTEGER DEFAULT 4");
            checkAndAddColumn(jdbcTemplate, "dining_tables", "min_people", "INTEGER DEFAULT 1");
            checkAndAddColumn(jdbcTemplate, "dining_tables", "max_people", "INTEGER DEFAULT 10");

            // 创建桌台预约表（如果不存在，预约管理需要）
            // 含操作人审计字段：confirm_operator_id / arrive_operator_id / cancel_operator_id
            createTableIfNotExists(jdbcTemplate, "table_reservations",
                    "CREATE TABLE table_reservations (" +
                    "  reservation_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  reservation_code    VARCHAR(32) NOT NULL," +
                    "  customer_name       VARCHAR(50) NOT NULL," +
                    "  customer_phone      VARCHAR(20) NOT NULL," +
                    "  table_id            BIGINT," +
                    "  reservation_date    DATE NOT NULL," +
                    "  reservation_time    TIME NOT NULL," +
                    "  people_count        INTEGER NOT NULL," +
                    "  deposit_amount      BIGINT DEFAULT 0," +
                    "  status              INTEGER DEFAULT 1," +
                    "  create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  confirm_time        TIMESTAMP," +
                    "  arrive_time         TIMESTAMP," +
                    "  cancel_time         TIMESTAMP," +
                    "  confirm_operator_id BIGINT," +
                    "  arrive_operator_id  BIGINT," +
                    "  cancel_operator_id  BIGINT," +
                    "  update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  deleted             INTEGER DEFAULT 0" +
                    ")",
                    new String[]{
                            "CREATE UNIQUE INDEX uk_table_reservations_code ON table_reservations(reservation_code)",
                            "CREATE INDEX idx_table_reservations_table_id ON table_reservations(table_id)"
                    });

            // 兼容性修复：旧版 table_reservations 表可能缺少操作人审计字段
            checkAndAddColumn(jdbcTemplate, "table_reservations", "confirm_operator_id", "BIGINT");
            checkAndAddColumn(jdbcTemplate, "table_reservations", "arrive_operator_id", "BIGINT");
            checkAndAddColumn(jdbcTemplate, "table_reservations", "cancel_operator_id", "BIGINT");

            log.info("订单管理相关表初始化完成");
        } catch (Exception e) {
            log.warn("初始化订单管理相关表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 如果表不存在则创建表并建立索引
     */
    private void createTableIfNotExists(@Nonnull JdbcTemplate jdbcTemplate, @Nonnull String tableName,
                                        @Nonnull String createTableSql, @Nonnull String[] indexSqls) {
        try {
            // PostgreSQL information_schema 中表名为小写
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = ?";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql, tableName.toLowerCase());
            if (!tables.isEmpty()) {
                log.debug("{} 表已存在，跳过创建", tableName);
                return;
            }

            log.info("{} 表不存在，开始创建", tableName);
            jdbcTemplate.execute(createTableSql);
            for (String indexSql : indexSqls) {
                jdbcTemplate.execute(indexSql);
            }
            log.info("{} 表创建成功", tableName);
        } catch (Exception e) {
            log.warn("创建 {} 表时出错: {}", tableName, e.getMessage());
        }
    }

    /**
     * 初始化基础数据（仅保留必要种子数据）
     */
    private void initializeBaseData(@Nonnull JdbcTemplate jdbcTemplate) {
        try {
            // 检查 departments 表是否存在，如果存在则初始化部门数据
            try {
                String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'departments'";
                List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
                if (tables.isEmpty()) {
                    log.info("departments 表不存在，跳过部门数据初始化");
                    return;
                }
            } catch (Exception e) {
                log.debug("检查表是否存在时出错: {}", e.getMessage());
                return;
            }

            // 检查是否需要初始化角色权限数据
            try {
                String checkRoleSql = "SELECT COUNT(*) FROM role_permissions";
                Integer roleCount = jdbcTemplate.queryForObject(checkRoleSql, Integer.class);
                if (roleCount != null && roleCount > 0) {
                    log.info("role_permissions表已有数据，跳过初始化");
                } else {
                    // 初始化角色权限数据
                    initializeRolePermissions(jdbcTemplate);
                }
            } catch (Exception e) {
                log.debug("检查角色权限数据时出错: {}", e.getMessage());
                initializeRolePermissions(jdbcTemplate);
            }
        } catch (Exception e) {
            log.warn("初始化基础数据失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化角色权限数据（必要种子数据）
     */
    private void initializeRolePermissions(@Nonnull JdbcTemplate jdbcTemplate) {
        try {
            // 检查 role_permissions 表是否存在
            try {
                String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'role_permissions'";
                List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
                if (tables.isEmpty()) {
                    log.info("role_permissions 表不存在，跳过角色权限数据初始化");
                    return;
                }
            } catch (Exception e) {
                log.debug("检查表是否存在时出错: {}", e.getMessage());
                return;
            }

            try {
                // 为admin角色（ROLE_ID=1）分配所有权限
                String[] adminPermissions = {
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 1, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 2, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 3, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 4, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 5, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 6, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 7, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 8, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 9, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (1, 10, 1)"
                };

                for (String sql : adminPermissions) {
                    try {
                        jdbcTemplate.update(sql);
                    } catch (Exception e) {
                        log.warn("执行SQL失败: {}", sql);
                    }
                }

                // 为user角色（ROLE_ID=2）分配基本权限
                String[] userPermissions = {
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (2, 6, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (2, 7, 1)",
                    "INSERT INTO role_permissions (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES (2, 8, 1)"
                };

                for (String sql : userPermissions) {
                    try {
                        jdbcTemplate.update(sql);
                    } catch (Exception e) {
                        log.warn("执行SQL失败: {}", sql);
                    }
                }

                log.info("角色权限数据初始化完成");
            } catch (Exception e) {
                log.warn("初始化角色权限数据失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("初始化基础数据失败: {}", e.getMessage());
        }
    }

    /**
     * 迁移采购入库表中的规格数据
     * 将product_code中的规格值迁移到specification字段
     */
    private void migratePurchaseStockinSpecification(@Nonnull JdbcTemplate jdbcTemplate) {
        try {
            // 检查是否有需要迁移的数据
            String checkSql = "SELECT COUNT(*) FROM purchase_stockin WHERE specification IS NULL AND product_code IS NOT NULL AND product_code != ''";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count != null && count > 0) {
                log.info("发现 {} 条需要迁移规格数据的采购入库记录", count);
                
                // 将product_code中的值迁移到specification字段，并生成新的产品编码
                String migrateSql = "UPDATE purchase_stockin SET specification = product_code, product_code = CONCAT('CG', TO_CHAR(stockin_date, 'YYYYMMDD'), LPAD(id::TEXT, 3, '0')) WHERE specification IS NULL AND product_code IS NOT NULL AND product_code != ''";
                int updated = jdbcTemplate.update(migrateSql);
                log.info("已迁移 {} 条采购入库记录的规格数据，并生成产品编码", updated);
            } else {
                log.info("没有需要迁移规格数据的采购入库记录");
            }
            
            // 检查是否有产品编码为空的记录
            String checkEmptySql = "SELECT COUNT(*) FROM purchase_stockin WHERE product_code IS NULL OR product_code = ''";
            Integer emptyCount = jdbcTemplate.queryForObject(checkEmptySql, Integer.class);
            
            if (emptyCount != null && emptyCount > 0) {
                log.info("发现 {} 条产品编码为空的采购入库记录", emptyCount);
                
                // 为空的产品编码生成新编码
                String fillSql = "UPDATE purchase_stockin SET product_code = CONCAT('CG', TO_CHAR(stockin_date, 'YYYYMMDD'), LPAD(id::TEXT, 3, '0')) WHERE product_code IS NULL OR product_code = ''";
                int filled = jdbcTemplate.update(fillSql);
                log.info("已为 {} 条采购入库记录生成产品编码", filled);
            }
            
            // 修复自有库存编码格式
            String fixInventoryCodeSql = "UPDATE purchase_stockin SET inventory_code = REPLACE(inventory_code, 'ICnull-', 'IC-NEW-') WHERE inventory_code LIKE 'ICnull-%'";
            int fixedInventory = jdbcTemplate.update(fixInventoryCodeSql);
            if (fixedInventory > 0) {
                log.info("已修复 {} 条自有库存编码格式", fixedInventory);
            }
        } catch (Exception e) {
            log.warn("迁移采购入库规格数据失败: {}", e.getMessage());
        }
    }

    // C14已删除以下4个方法（与对应 *DatabaseInitializer 重复或为 no-op）：
    //   - initializePurchaseStockinTable (no-op，仅检查不创建，由PurchaseDatabaseInitializer负责)
    //   - initializeFinanceVoucherTable  (no-op，仅检查不创建)
    //   - initializeComboInventoryTable  (与InventoryDatabaseInitializer重复)
    //   - initializeDishInventoryTable   (与InventoryDatabaseInitializer重复)

    private void initializeFoodTable(JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'food'";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
            if (tables.isEmpty()) {
                log.info("food 表不存在，创建表");
                String createTableSql = "CREATE TABLE IF NOT EXISTS food (" +
                        "food_code VARCHAR(50) PRIMARY KEY," +
                        "food_name VARCHAR(100) NOT NULL," +
                        "food_category VARCHAR(50)," +
                        "food_price DECIMAL(12,2)," +
                        "cost_price DECIMAL(12,2)," +
                        "food_desc VARCHAR(500)," +
                        "food_image VARCHAR(500)," +
                        "food_status VARCHAR(20) DEFAULT 'active'," +
                        "batch_number VARCHAR(100)," +
                        "trace_code VARCHAR(100)," +
                        "manufacturer VARCHAR(200)," +
                        "production_date TIMESTAMP," +
                        "expiration_date TIMESTAMP," +
                        "stock INT DEFAULT 999," +
                        "food_description VARCHAR(500)," +
                        "shelf_life_days INT," +
                        "production_address VARCHAR(500)," +
                        "nutrition_info VARCHAR(500)," +
                        "storage_conditions VARCHAR(200)," +
                        "price DECIMAL(12,2)," +
                        "weight DECIMAL(12,2)," +
                        "quality_grade VARCHAR(50)," +
                        "quality_report_no VARCHAR(100)," +
                        "quality_check_date TIMESTAMP," +
                        "certification_info VARCHAR(500)," +
                        "food_image_url VARCHAR(500)," +
                        "trace_code_image_url VARCHAR(500)," +
                        "trace_status SMALLINT DEFAULT 0," +
                        "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "create_by VARCHAR(50)," +
                        "update_by VARCHAR(50)," +
                        "remarks VARCHAR(500)," +
                        "device_id VARCHAR(50)," +
                        "sensor_data TEXT," +
                        "rfid_tag VARCHAR(100)," +
                        "gps_location VARCHAR(100)," +
                        "collect_time TIMESTAMP," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "deleted SMALLINT DEFAULT 0" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                log.info("food 表创建成功");
            } else {
                log.info("food 表已存在，检查并修复缺失列");
                checkAndAddColumn(jdbcTemplate, "food", "stock", "INT DEFAULT 999");
                checkAndAddColumn(jdbcTemplate, "food", "food_description", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "shelf_life_days", "INT");
                checkAndAddColumn(jdbcTemplate, "food", "production_address", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "nutrition_info", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "storage_conditions", "VARCHAR(200)");
                checkAndAddColumn(jdbcTemplate, "food", "price", "DECIMAL(12,2)");
                checkAndAddColumn(jdbcTemplate, "food", "weight", "DECIMAL(12,2)");
                checkAndAddColumn(jdbcTemplate, "food", "quality_grade", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "food", "quality_report_no", "VARCHAR(100)");
                checkAndAddColumn(jdbcTemplate, "food", "quality_check_date", "TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "food", "certification_info", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "food_image_url", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "trace_code_image_url", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "trace_status", "SMALLINT DEFAULT 0");
                checkAndAddColumn(jdbcTemplate, "food", "create_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "food", "update_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "food", "create_by", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "food", "update_by", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "food", "remarks", "VARCHAR(500)");
                checkAndAddColumn(jdbcTemplate, "food", "device_id", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "food", "sensor_data", "TEXT");
                checkAndAddColumn(jdbcTemplate, "food", "rfid_tag", "VARCHAR(100)");
                checkAndAddColumn(jdbcTemplate, "food", "gps_location", "VARCHAR(100)");
                checkAndAddColumn(jdbcTemplate, "food", "collect_time", "TIMESTAMP");
            }
        } catch (Exception e) {
            log.warn("初始化food表时出错: {}", e.getMessage());
        }
    }

    private void initializeDishComboTable(JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'dish_combo'";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
            if (tables.isEmpty()) {
                log.info("dish_combo 表不存在，创建表");
                String createTableSql = "CREATE TABLE IF NOT EXISTS dish_combo (" +
                        "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                        "combo_code VARCHAR(50) UNIQUE," +
                        "name VARCHAR(100) NOT NULL," +
                        "description VARCHAR(500)," +
                        "price DECIMAL(12,2) DEFAULT 0," +
                        "status VARCHAR(20) DEFAULT '1'," +
                        "image_url VARCHAR(500)," +
                        "priority INT DEFAULT 0," +
                        "people_count INT," +
                        "combo_type VARCHAR(50)," +
                        "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "deleted INT DEFAULT 0" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                log.info("dish_combo 表创建成功");
            } else {
                checkAndAddColumn(jdbcTemplate, "dish_combo", "combo_code", "VARCHAR(50) UNIQUE");
                checkAndAddColumn(jdbcTemplate, "dish_combo", "combo_type", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "dish_combo", "people_count", "INT");
                checkAndAddColumn(jdbcTemplate, "dish_combo", "create_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "dish_combo", "update_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
        } catch (Exception e) {
            log.warn("初始化dish_combo表时出错: {}", e.getMessage());
        }
    }

    // C14已删除: initializeSalesOrderTable (no-op，仅检查不创建)

    /**
     * 同步产品中心菜品表(foods)数据到 legacy 菜品表(food)
     * 保证 POS 订单创建时库存校验可用
     */
    private void syncFoodsToLegacyFood(JdbcTemplate jdbcTemplate) {
        try {
            // 先检查两张表是否都存在（PostgreSQL information_schema 中表名为小写）
            String checkFoodsSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'foods'";
            String checkFoodSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'food'";
            Integer foodsCount = jdbcTemplate.queryForObject(checkFoodsSql, Integer.class);
            Integer foodCount = jdbcTemplate.queryForObject(checkFoodSql, Integer.class);
            if ((foodsCount == null || foodsCount == 0) || (foodCount == null || foodCount == 0)) {
                log.info("foods 或 food 表不存在，跳过同步");
                return;
            }

            // foods 表中没有 create_by/update_by，使用默认值 'system'
            String upsertSql =
                "INSERT INTO food (" +
                "  food_code, food_name, food_category, food_price, cost_price, " +
                "  food_desc, food_description, food_image, food_image_url, food_status, " +
                "  stock, create_time, update_time, create_by, update_by, deleted" +
                ") " +
                "SELECT " +
                "  f.food_code, f.food_name, COALESCE(fc.category_name, '未分类'), " +
                "  f.sale_price / 100.00, f.cost_price / 100.00, " +
                "  f.description, f.description, f.image_url, f.image_url, " +
                "  CASE f.status WHEN 1 THEN 'active' WHEN 0 THEN 'inactive' WHEN 2 THEN 'sold_out' ELSE 'active' END, " +
                "  f.stock, f.create_time, f.update_time, 'system', 'system', f.deleted " +
                "FROM foods f " +
                "LEFT JOIN food_categories fc ON fc.category_id = f.category_id " +
                "WHERE f.deleted = 0 " +
                "ON CONFLICT (food_code) DO UPDATE SET " +
                "  food_name = EXCLUDED.food_name, " +
                "  food_category = EXCLUDED.food_category, " +
                "  food_price = EXCLUDED.food_price, " +
                "  cost_price = EXCLUDED.cost_price, " +
                "  food_desc = EXCLUDED.food_desc, " +
                "  food_description = EXCLUDED.food_description, " +
                "  food_image = EXCLUDED.food_image, " +
                "  food_image_url = EXCLUDED.food_image_url, " +
                "  food_status = EXCLUDED.food_status, " +
                "  stock = EXCLUDED.stock, " +
                "  update_time = EXCLUDED.update_time, " +
                "  update_by = EXCLUDED.update_by, " +
                "  deleted = EXCLUDED.deleted";
            int synced = jdbcTemplate.update(upsertSql);
            log.info("已同步 {} 条菜品数据从 foods 到 food 表", synced);
        } catch (Exception e) {
            log.warn("同步 foods 到 food 表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步产品中心套餐表(dish_combos)数据到 legacy 套餐表(dish_combo)
     */
    private void syncDishCombosToLegacyDishCombo(JdbcTemplate jdbcTemplate) {
        try {
            // PostgreSQL information_schema 中表名为小写
            String checkCombosSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'dish_combos'";
            String checkComboSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'dish_combo'";
            Integer combosCount = jdbcTemplate.queryForObject(checkCombosSql, Integer.class);
            Integer comboCount = jdbcTemplate.queryForObject(checkComboSql, Integer.class);
            if ((combosCount == null || combosCount == 0) || (comboCount == null || comboCount == 0)) {
                log.info("dish_combos 或 dish_combo 表不存在，跳过同步");
                return;
            }

            // 确保 dish_combo.combo_code 有唯一约束，否则 ON CONFLICT 会失败
            try {
                jdbcTemplate.execute("ALTER TABLE dish_combo ADD CONSTRAINT uk_dish_combo_combo_code UNIQUE (combo_code)");
                log.info("dish_combo 表添加 combo_code 唯一约束成功");
            } catch (Exception e) {
                log.debug("dish_combo 表 combo_code 唯一约束可能已存在: {}", e.getMessage());
            }

            // dish_combo 表字段为 combo_name/price/people_count，没有 name/priority
            // P1-COMBO-ORDER-001: Flyway V20260717 已将 created_at/updated_at 统一为 create_time/update_time
            String upsertSql =
                "INSERT INTO dish_combo (" +
                "  combo_code, combo_name, description, price, status, image_url, people_count, create_time, update_time, deleted" +
                ") " +
                "SELECT " +
                "  dc.combo_code, dc.combo_name, dc.description, dc.combo_price / 100.00, " +
                "  CASE dc.status WHEN 1 THEN 'active' WHEN 0 THEN 'inactive' ELSE 'active' END, " +
                "  dc.image_url, 2, dc.create_time, dc.update_time, dc.deleted " +
                "FROM dish_combos dc " +
                "WHERE dc.deleted = 0 " +
                "ON CONFLICT (combo_code) DO UPDATE SET " +
                "  combo_name = EXCLUDED.combo_name, " +
                "  description = EXCLUDED.description, " +
                "  price = EXCLUDED.price, " +
                "  status = EXCLUDED.status, " +
                "  image_url = EXCLUDED.image_url, " +
                "  people_count = EXCLUDED.people_count, " +
                "  update_time = EXCLUDED.update_time, " +
                "  deleted = EXCLUDED.deleted";
            int synced = jdbcTemplate.update(upsertSql);
            log.info("已同步 {} 条套餐数据从 dish_combos 到 dish_combo 表", synced);
        } catch (Exception e) {
            log.warn("同步 dish_combos 到 dish_combo 表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * P1-COMBO-ORDER-001: 同步产品中心套餐配料表(combo_ingredients)到 legacy 表(combo_ingredient)。
     * 过渡期兼容：POS 侧仍可能读 combo_ingredient（selectByComboId）。
     * combo_ingredient.food_id 为 VARCHAR，quantity 为 DECIMAL；新表 food_id BIGINT、quantity INT。
     */
    private void syncComboIngredientsToLegacy(JdbcTemplate jdbcTemplate) {
        try {
            String checkNewSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'combo_ingredients'";
            String checkLegacySql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'combo_ingredient'";
            Integer newCount = jdbcTemplate.queryForObject(checkNewSql, Integer.class);
            Integer legacyCount = jdbcTemplate.queryForObject(checkLegacySql, Integer.class);
            if ((newCount == null || newCount == 0) || (legacyCount == null || legacyCount == 0)) {
                log.info("combo_ingredients 或 combo_ingredient 表不存在，跳过同步");
                return;
            }

            // legacy combo_ingredient 主键自增、无 natural unique；按 (combo_id, food_id) 幂等补插
            // 时间列可能为 create_time/update_time 或历史 created_at/updated_at：按实际列名探测
            String colSql =
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'combo_ingredient'";
            List<String> cols = jdbcTemplate.queryForList(colSql, String.class);
            String createCol = cols != null && cols.contains("create_time") ? "create_time" : "created_at";
            String updateCol = cols != null && cols.contains("update_time") ? "update_time" : "updated_at";
            String upsertSql =
                "INSERT INTO combo_ingredient (combo_id, food_id, quantity, unit, " + createCol + ", " + updateCol + ", deleted) " +
                "SELECT ci.combo_id, CAST(ci.food_id AS VARCHAR(50)), CAST(ci.quantity AS DECIMAL), ci.unit, " +
                "       ci.create_time, ci.update_time, ci.deleted " +
                "FROM combo_ingredients ci " +
                "WHERE ci.deleted = 0 " +
                "  AND NOT EXISTS (" +
                "    SELECT 1 FROM combo_ingredient x " +
                "    WHERE x.combo_id = ci.combo_id AND x.food_id = CAST(ci.food_id AS VARCHAR(50))" +
                "  )";
            int synced = jdbcTemplate.update(upsertSql);
            log.info("已同步 {} 条套餐配料从 combo_ingredients 到 combo_ingredient 表", synced);
        } catch (Exception e) {
            log.warn("同步 combo_ingredients 到 combo_ingredient 表时出错: {}", e.getMessage(), e);
        }
    }

    private void initializeStoresTable(JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'stores'";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
            if (tables.isEmpty()) {
                log.info("stores 表不存在，创建表");
                String createTableSql = "CREATE TABLE IF NOT EXISTS stores (" +
                        "store_id VARCHAR(50) PRIMARY KEY," +
                        "store_name VARCHAR(100) NOT NULL," +
                        "store_code VARCHAR(50) UNIQUE," +
                        "address VARCHAR(200)," +
                        "phone VARCHAR(50)," +
                        "manager_id VARCHAR(50)," +
                        "manager_name VARCHAR(50)," +
                        "status VARCHAR(20) DEFAULT 'active'," +
                        "company_id VARCHAR(50)," +
                        "region VARCHAR(50)," +
                        "store_type VARCHAR(20) DEFAULT 'single'," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "created_by VARCHAR(50)," +
                        "updated_by VARCHAR(50)," +
                        "deleted SMALLINT DEFAULT 0," +
                        "version INT DEFAULT 1," +
                        "ext_data TEXT" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                log.info("stores 表创建成功");
                
                // 生产环境验收：不再通过迁移脚本插入模拟门店数据，
                // 真实门店由 RealWorldAcceptanceInitializer 通过服务层统一初始化。
                log.info("stores 表初始数据已跳过（使用真实门店初始化器）");
            } else {
                checkAndAddColumn(jdbcTemplate, "stores", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "stores", "updated_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                checkAndAddColumn(jdbcTemplate, "stores", "created_by", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "stores", "updated_by", "VARCHAR(50)");
                checkAndAddColumn(jdbcTemplate, "stores", "version", "INT DEFAULT 1");
                checkAndAddColumn(jdbcTemplate, "stores", "ext_data", "TEXT");
            }
        } catch (Exception e) {
            log.warn("初始化stores表时出错: {}", e.getMessage());
        }
    }

    // C14已删除: initializePositionCodeRulesTable (死代码，未被任何调用方引用)
    //   - position_code_rules 表仅 legacy frontend 使用，活跃前端均不调用
    //   - purchase_orders 字段修复逻辑已提取到 fixPurchaseOrderColumns() 方法

    /**
     * 初始化原料追溯码表的测试数据（已停用）
     *
     * <p>表结构和字段补齐由 BaseDatabaseInitializer.createMaterialTraceCodeTable() 和
     * TraceDatabaseInitializer.ensureMaterialTraceCodeColumns() 负责。</p>
     *
     * <p>原测试原料追溯码数据（面粉、猪肉、韭菜、鸡蛋、食用油）不再在启动时自动插入。</p>
     */
    private void initializeMaterialTraceCodeTable(JdbcTemplate jdbcTemplate) {
        // 已清理：不再自动插入/更新测试原料追溯码数据
    }

    // C14已删除: initializeSuppliersTable (与PurchaseDatabaseInitializer重复，由后者负责建表+初始数据)

    private void initializeMaterialTemplateTable(JdbcTemplate jdbcTemplate) {
        try {
            String checkTableSql = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA() AND table_name = 'material_template'";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkTableSql);
            if (tables.isEmpty()) {
                log.info("material_template 表不存在，创建表");
                String createTableSql = "CREATE TABLE IF NOT EXISTS material_template (" +
                        "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                        "template_code VARCHAR(50) UNIQUE," +
                        "material_name VARCHAR(100) NOT NULL," +
                        "category VARCHAR(50)," +
                        "default_shelf_life INT," +
                        "storage_condition VARCHAR(50)," +
                        "weight_unit VARCHAR(20)," +
                        "barcode VARCHAR(50)," +
                        "barcode_unique SMALLINT DEFAULT 0," +
                        "supplier_id VARCHAR(50)," +
                        "supplier_name VARCHAR(100)," +
                        "description VARCHAR(500)," +
                        "version INT DEFAULT 1," +
                        "parent_id BIGINT," +
                        "status VARCHAR(20) DEFAULT 'active'," +
                        "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "deleted SMALLINT DEFAULT 0" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                log.info("material_template 表创建成功");
                
                insertDefaultMaterialTemplates(jdbcTemplate);
            } else {
                checkAndAddColumn(jdbcTemplate, "material_template", "version", "INT DEFAULT 1");
                checkAndAddColumn(jdbcTemplate, "material_template", "parent_id", "BIGINT");
                checkAndAddColumn(jdbcTemplate, "material_template", "barcode_unique", "SMALLINT DEFAULT 0");
                
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM material_template WHERE deleted = 0", Integer.class);
                if (count == null || count == 0) {
                    insertDefaultMaterialTemplates(jdbcTemplate);
                }
            }
        } catch (Exception e) {
            log.warn("初始化material_template表时出错: {}", e.getMessage());
        }
    }
    
    private void insertDefaultMaterialTemplates(JdbcTemplate jdbcTemplate) {
        String insertDataSql = "INSERT INTO material_template (template_code, material_name, category, default_shelf_life, storage_condition, weight_unit, description, status, version) VALUES " +
                "('MT001', '猪肉馅', '肉类', 7, '冷藏', 'kg', '新鲜猪肉馅，需冷藏保存', 'active', 1)," +
                "('MT002', '牛肉', '肉类', 5, '冷藏', 'kg', '新鲜牛肉，需冷藏保存', 'active', 1)," +
                "('MT003', '鸡胸肉', '肉类', 3, '冷藏', 'kg', '新鲜鸡胸肉，需冷藏保存', 'active', 1)," +
                "('MT004', '大白菜', '蔬菜', 5, '常温', 'kg', '新鲜大白菜', 'active', 1)," +
                "('MT005', '西红柿', '蔬菜', 7, '常温', 'kg', '新鲜西红柿', 'active', 1)," +
                "('MT006', '土豆', '蔬菜', 30, '阴凉干燥', 'kg', '土豆，阴凉干燥处保存', 'active', 1)," +
                "('MT007', '鲜虾', '海鲜', 2, '冷藏', 'kg', '新鲜虾，需冷藏保存', 'active', 1)," +
                "('MT008', '三文鱼', '海鲜', 3, '冷藏', 'kg', '新鲜三文鱼，需冷藏保存', 'active', 1)," +
                "('MT009', '食用油', '调料', 365, '常温', 'L', '食用植物油', 'active', 1)," +
                "('MT010', '酱油', '调料', 365, '常温', 'L', '酿造酱油', 'active', 1)," +
                "('MT011', '面粉', '粮油', 180, '阴凉干燥', 'kg', '小麦面粉', 'active', 1)," +
                "('MT012', '大米', '粮油', 365, '阴凉干燥', 'kg', '优质大米', 'active', 1)";
        jdbcTemplate.execute(insertDataSql);
        log.info("material_template 表初始数据插入成功");
    }
}
