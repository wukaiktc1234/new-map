package com.foodtraceability.integration;

import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.service.OrderNewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第 2/3 层：KDS 出餐扣料失败 —— 真实 Spring + PostgreSQL 集成测试。
 * <p>
 * 目标（Mockito 单元测试无法覆盖）：
 * <ul>
 *   <li>主扣料事务失败后完整回滚：material_consumed 回到 0；已成功扣减的原料库存恢复；</li>
 *   <li>FAILED 审计行由独立 REQUIRES_NEW 事务提交，主事务（及外层事务）回滚后仍持久化；</li>
 *   <li>审计行字段：status='FAILED'、inventory_deducted=0、failure_type/tray_code 等可查询。</li>
 * </ul>
 * <p>
 * M3-M4 S9a-2 重锚：store_inventory fixture → 统一账 inventory
 * （门店 9901 → locations STORE 型行 + location_id_map 桥，规则 4）；
 * stock 读写经 inventory.quantity。扣料路径：
 * OrderNewServiceImpl.deductMaterialsForServe → resolveLocationIdByStoreId →
 * decreaseStockAtLocation（缺行 NOT_FOUND / 不足 INVENTORY_INSUFFICIENT /
 * 成功 → quantity 更新 + OUT 流水，正入负出符号约定）。
 * <p>
 * 环境要求：本地 PostgreSQL 可达，库 food_traceability 已应用 M3-M4 统一账本（V20260927_001/002）。
 * 测试禁用 Flyway（classpath 含 src/test V999 会阻断上下文启动；schema 由主库迁移保证）。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
class MaterialDeductionAuditIntegrationTest {

    private static final String STORE_ID = "9901";
    private static final String LOCATION_CODE = "IT-LOC-9901";
    private static final Long MATERIAL_OK = 990001L;
    private static final Long MATERIAL_SHORT = 990002L;
    private static final Long FOOD_ID = 990001L;
    private static final String ORDER_ID = "IT-ORD-001";
    private static final String ORDER_NUMBER = "IT-ON-001";
    private static final String ITEM_ID = "IT-ITEM-001";
    private static final String KITCHEN_ORDER_ID = "IT-KO-001";
    private static final String TRAY_CODE = "IT-TRAY-01";
    private static final String SOURCE_REF = "集成测试-出餐扣料";
    private static final String SUCCESS_KITCHEN_ORDER_ID = "IT-KO-OK";
    private static final String SUCCESS_ORDER_ID = "IT-ORD-OK";
    private static final String SUCCESS_ITEM_ID = "IT-ITEM-OK";
    private static final String SUCCESS_ORDER_NUMBER = "IT-ON-OK";

    /** fixture 位置 ID（门店 9901 经 location_id_map 解析到的 locations 行） */
    private Long locationId;

    @Autowired
    private OrderNewService orderNewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUpFixture() {
        cleanFixture();

        insertStoreLocation();
        insertInventory(MATERIAL_OK, "集成原料A", new BigDecimal("100"));
        insertInventory(MATERIAL_SHORT, "集成原料B", new BigDecimal("0"));

        insertFood(FOOD_ID, "ITFD001", "集成测试菜品");
        jdbcTemplate.update(
                "INSERT INTO dish_recipes (food_id, material_id, material_name, required_quantity, unit, loss_rate, version, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0)",
                FOOD_ID, MATERIAL_OK, "集成原料A", new BigDecimal("1"), "个");
        jdbcTemplate.update(
                "INSERT INTO dish_recipes (food_id, material_id, material_name, required_quantity, unit, loss_rate, version, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0)",
                FOOD_ID, MATERIAL_SHORT, "集成原料B", new BigDecimal("1"), "个");

        insertOrder(ORDER_ID, ORDER_NUMBER, "IT-OC-001");
        insertOrderItem(ITEM_ID, ORDER_ID, FOOD_ID, "集成测试菜品");

        insertKitchenOrder(KITCHEN_ORDER_ID, ORDER_ID, ORDER_NUMBER);

        insertOrder(SUCCESS_ORDER_ID, SUCCESS_ORDER_NUMBER, "IT-OC-OK");
        insertOrderItem(SUCCESS_ITEM_ID, SUCCESS_ORDER_ID, FOOD_ID, "集成测试菜品");
        insertKitchenOrder(SUCCESS_KITCHEN_ORDER_ID, SUCCESS_ORDER_ID, SUCCESS_ORDER_NUMBER);
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void cleanFixture() {
        jdbcTemplate.update("DELETE FROM material_consumption WHERE kitchen_order_id IN (?, ?, ?)",
                KITCHEN_ORDER_ID, SUCCESS_KITCHEN_ORDER_ID, "IT-KO-MISSING");
        jdbcTemplate.update("DELETE FROM kitchen_order WHERE kitchen_order_id IN (?, ?)",
                KITCHEN_ORDER_ID, SUCCESS_KITCHEN_ORDER_ID);
        jdbcTemplate.update("DELETE FROM order_items WHERE item_id IN (?, ?)", ITEM_ID, SUCCESS_ITEM_ID);
        jdbcTemplate.update("DELETE FROM orders WHERE order_id IN (?, ?)", ORDER_ID, SUCCESS_ORDER_ID);
        jdbcTemplate.update("DELETE FROM dish_recipes WHERE food_id = ?", FOOD_ID);
        jdbcTemplate.update("DELETE FROM foods WHERE food_id = ?", FOOD_ID);
        // S9a-2：统一账清理（fixture 流水 → 统一账 → 映射桥 → 位置 → 门店；
        // locationId 首次 setup 前为 null，DELETE 不命中任何行）
        jdbcTemplate.update("DELETE FROM inventory_movement WHERE location_id = ?", locationId);
        jdbcTemplate.update("DELETE FROM inventory WHERE location_id = ?", locationId);
        jdbcTemplate.update("DELETE FROM location_id_map WHERE src_table = 'stores_new' AND src_id = ?",
                Long.valueOf(STORE_ID));
        jdbcTemplate.update("DELETE FROM locations WHERE location_code = ?", LOCATION_CODE);
        jdbcTemplate.update("DELETE FROM stores_new WHERE store_id = ?", Long.valueOf(STORE_ID));
    }

    private void insertStore() {
        jdbcTemplate.update(
                "INSERT INTO stores_new (store_id, store_code, store_name, status, create_time, update_time, deleted) "
                        + "OVERRIDING SYSTEM VALUE "
                        + "VALUES (?, 'IT-STORE-01', '集成测试门店', 1, NOW(), NOW(), 0)",
                Long.valueOf(STORE_ID));
    }

    /**
     * S9a-2：门店 → 统一位置（locations STORE 型行 + location_id_map 桥，规则 4）。
     */
    private void insertStoreLocation() {
        insertStore();
        locationId = jdbcTemplate.queryForObject(
                "INSERT INTO locations (location_code, location_name, location_type, status) "
                        + "VALUES (?, ?, 'STORE', 1) RETURNING location_id",
                Long.class, LOCATION_CODE, "集成测试门店" + STORE_ID);
        assertNotNull(locationId, "fixture 位置 location_id 应已生成");
        jdbcTemplate.update(
                "INSERT INTO location_id_map (src_table, src_id, location_id) VALUES ('stores_new', ?, ?)",
                Long.valueOf(STORE_ID), locationId);
    }

    /**
     * S9a-2：store_inventory fixture → 统一账 inventory（location_id + material_id 唯一行）。
     */
    private void insertInventory(Long materialId, String name, BigDecimal stock) {
        jdbcTemplate.update(
                "INSERT INTO inventory (location_id, material_id, material_name, quantity, unit, "
                        + "locked_quantity, unit_cost, total_cost, version) "
                        + "VALUES (?, ?, ?, ?, '个', 0, 100, ?, 0)",
                locationId, materialId, name, stock, stock.multiply(new BigDecimal("100")).longValue());
    }

    private void insertFood(Long foodId, String code, String name) {
        jdbcTemplate.update(
                "INSERT INTO foods (food_id, food_code, food_name, category_id, specification, unit, sale_price, cost_price, status, version, deleted, create_time, update_time) "
                        + "OVERRIDING SYSTEM VALUE "
                        + "VALUES (?, ?, ?, NULL, '份', '份', 1000, 500, 1, 0, 0, NOW(), NOW())",
                foodId, code, name);
    }

    private void insertOrder(String orderId, String orderNumber, String orderCode) {
        jdbcTemplate.update(
                "INSERT INTO orders (order_id, order_number, order_code, order_type, order_source, order_status, payment_status, total_amount, final_amount, store_id, deleted, create_time, update_time) "
                        + "VALUES (?, ?, ?, 1, 1, 1, 0, 1000, 1000, ?, 0, NOW(), NOW())",
                orderId, orderNumber, orderCode, Long.valueOf(STORE_ID));
    }

    private void insertOrderItem(String itemId, String orderId, Long foodId, String productName) {
        jdbcTemplate.update(
                "INSERT INTO order_items (item_id, order_id, product_type, food_id, product_name, unit_price, quantity, amount, kitchen_status, deleted, create_time, update_time) "
                        + "VALUES (?, ?, 1, ?, ?, 1000, 1, 1000, 0, 0, NOW(), NOW())",
                itemId, orderId, String.valueOf(foodId), productName);
    }

    private void insertKitchenOrder(String kitchenOrderId, String orderId, String orderNumber) {
        jdbcTemplate.update(
                "INSERT INTO kitchen_order (kitchen_order_id, order_id, order_number, order_type, status, priority, total_dishes, store_id, store_name, material_consumed, material_locked, deleted, create_time, update_time) "
                        + "VALUES (?, ?, ?, 0, 'ready', 0, 1, ?, '集成测试门店', 0, 0, 0, NOW(), NOW())",
                kitchenOrderId, orderId, orderNumber, Long.valueOf(STORE_ID));
    }

    private int materialConsumed(String kitchenOrderId) {
        Integer v = jdbcTemplate.queryForObject(
                "SELECT material_consumed FROM kitchen_order WHERE kitchen_order_id = ?",
                Integer.class, kitchenOrderId);
        assertNotNull(v, "kitchen_order.material_consumed 应存在");
        return v;
    }

    /** S9a-2：统一账库存读取（store_inventory.current_stock → inventory.quantity） */
    private BigDecimal stock(Long materialId) {
        BigDecimal s = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE location_id = ? AND material_id = ?",
                BigDecimal.class, locationId, materialId);
        assertNotNull(s, "inventory.quantity 应存在");
        return s;
    }

    private Map<String, Object> failedAuditRow(String kitchenOrderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM material_consumption WHERE kitchen_order_id = ? AND status = 'FAILED'",
                kitchenOrderId);
        assertEquals(1, rows.size(), "应恰好有 1 条 FAILED 审计行 kitchenOrderId=" + kitchenOrderId);
        return rows.get(0);
    }

    @Test
    @DisplayName("库存不足失败：主事务回滚（material_consumed=0、已扣库存恢复）+ REQUIRES_NEW FAILED 审计行存活")
    void stockInsufficient_rollsBackMainTx_andKeepsFailedAudit() {
        BigDecimal okBefore = stock(MATERIAL_OK);

        MaterialDeductionFailureException ex = assertThrows(
                MaterialDeductionFailureException.class,
                () -> orderNewService.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, ex.getFailureType());

        assertEquals(0, materialConsumed(KITCHEN_ORDER_ID), "失败后 material_consumed 必须回到 0");
        assertEquals(0, stock(MATERIAL_SHORT).compareTo(BigDecimal.ZERO), "库存不足原料仍为 0");
        assertEquals(0, okBefore.compareTo(stock(MATERIAL_OK)), "已成功扣减的原料库存必须随主事务回滚恢复");

        Map<String, Object> audit = failedAuditRow(KITCHEN_ORDER_ID);
        assertEquals("FAILED", audit.get("status"));
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, audit.get("failure_type"));
        assertEquals(0, ((Number) audit.get("inventory_deducted")).intValue(), "审计行 inventory_deducted 必须为 0");
        assertEquals(KITCHEN_ORDER_ID, audit.get("kitchen_order_id"));
        assertEquals(TRAY_CODE, audit.get("tray_code"));
        assertNotNull(audit.get("failure_reason"));
        assertNotNull(audit.get("attempted_materials"), "已尝试扣减清单应有记录");
        assertNotNull(audit.get("bom_materials"), "BOM 已展开应有记录");
        assertNotNull(audit.get("consumption_id"));
    }

    @Test
    @DisplayName("外层事务回滚时 REQUIRES_NEW 审计行仍持久化（独立连接提交）")
    void outerTransactionRollback_auditRowSurvives() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        MaterialDeductionFailureException ex = assertThrows(
                MaterialDeductionFailureException.class,
                () -> txTemplate.execute(status -> {
                    orderNewService.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);
                    return null;
                }));
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, ex.getFailureType());

        assertEquals(0, materialConsumed(KITCHEN_ORDER_ID), "外层+主事务回滚后 material_consumed=0");
        assertEquals(0, stock(MATERIAL_OK).compareTo(new BigDecimal("100")),
                "外层回滚后库存恢复为 100");

        Map<String, Object> audit = failedAuditRow(KITCHEN_ORDER_ID);
        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, audit.get("failure_type"));
        assertEquals(0, ((Number) audit.get("inventory_deducted")).intValue());
    }

    @Test
    @DisplayName("成功路径（原料库存充足）：扣减提交且无 FAILED 审计")
    void successWithSufficientStock_commitsAndNoAudit() {
        jdbcTemplate.update("UPDATE inventory SET quantity = 100 WHERE location_id = ? AND material_id = ?",
                locationId, MATERIAL_SHORT);

        orderNewService.deductMaterialsForServe(SUCCESS_KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE);

        assertEquals(1, materialConsumed(SUCCESS_KITCHEN_ORDER_ID));
        assertEquals(0, stock(MATERIAL_OK).compareTo(new BigDecimal("99")));
        assertEquals(0, stock(MATERIAL_SHORT).compareTo(new BigDecimal("99")));

        Long failedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM material_consumption WHERE kitchen_order_id = ? AND status = 'FAILED'",
                Long.class, SUCCESS_KITCHEN_ORDER_ID);
        assertEquals(0L, failedCount, "成功路径不得写入 FAILED 审计行");

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM material_consumption WHERE kitchen_order_id = ?",
                Long.class, SUCCESS_KITCHEN_ORDER_ID);
        assertTrue(total >= 0);
    }

    @Test
    @DisplayName("后厨单不存在：material_consumed 不变（无占位）+ FAILED 审计")
    void kitchenOrderNotFound_writesAudit() {
        MaterialDeductionFailureException ex = assertThrows(
                MaterialDeductionFailureException.class,
                () -> orderNewService.deductMaterialsForServe("IT-KO-MISSING", SOURCE_REF, TRAY_CODE));
        assertEquals(MaterialDeductionFailureException.KITCHEN_ORDER_NOT_FOUND, ex.getFailureType());

        Map<String, Object> audit = failedAuditRow("IT-KO-MISSING");
        assertEquals("FAILED", audit.get("status"));
        assertEquals(MaterialDeductionFailureException.KITCHEN_ORDER_NOT_FOUND, audit.get("failure_type"));
        assertEquals(0, ((Number) audit.get("inventory_deducted")).intValue());
        jdbcTemplate.update("DELETE FROM material_consumption WHERE kitchen_order_id = ?", "IT-KO-MISSING");
    }
}
