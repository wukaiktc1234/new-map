package com.foodtraceability.integration;

import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.service.MaterialConsumptionAuditService;
import com.foodtraceability.service.OrderNewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * P0-1：审计自身失败时的残余风险验证（真实 Spring + PostgreSQL）。
 * <p>
 * 场景：主扣料失败 → 调用 recordDeductionFailure → 审计写入自身抛异常。
 * 期望：原始 MaterialDeductionFailureException 仍传播；主事务 rollback；
 * material_consumed=0；库存恢复；不伪装成功。
 * <p>
 * 不用 Mockito 冒充 REQUIRES_NEW（REQUIRES_NEW 由 MaterialDeductionAuditIntegrationTest 覆盖）。
 * <p>
 * M3-M4 S9a-2 重锚：store_inventory fixture → 统一账 inventory
 * （门店 9902 → locations STORE 型行 + location_id_map 桥，规则 4）；
 * stock 读写经 inventory.quantity。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
class MaterialDeductionAuditSelfFailureIntegrationTest {

    private static final String STORE_ID = "9902";
    private static final String LOCATION_CODE = "IT-LOC-9902";
    private static final Long MATERIAL_OK = 990011L;
    private static final Long MATERIAL_SHORT = 990012L;
    private static final Long FOOD_ID = 990011L;
    private static final String ORDER_ID = "IT-ORD-AF";
    private static final String ORDER_NUMBER = "IT-ON-AF";
    private static final String ITEM_ID = "IT-ITEM-AF";
    private static final String KITCHEN_ORDER_ID = "IT-KO-AF";
    private static final String TRAY_CODE = "IT-TRAY-AF";
    private static final String SOURCE_REF = "集成测试-审计自身失败";

    /** fixture 位置 ID（门店 9902 经 location_id_map 解析到的 locations 行） */
    private Long locationId;

    @Autowired
    private OrderNewService orderNewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 模拟审计写入自身失败（不替换主扣料路径的真实 DB 事务） */
    @MockBean
    private MaterialConsumptionAuditService materialConsumptionAuditService;

    @BeforeEach
    void setUpFixture() {
        doThrow(new IllegalStateException("模拟审计写入自身失败"))
                .when(materialConsumptionAuditService).recordDeductionFailure(any());

        cleanFixture();
        insertStoreLocation();
        insertInventory(MATERIAL_OK, "审计失败原料A", new BigDecimal("100"));
        insertInventory(MATERIAL_SHORT, "审计失败原料B", new BigDecimal("0"));
        insertFood();
        jdbcTemplate.update(
                "INSERT INTO dish_recipes (food_id, material_id, material_name, required_quantity, unit, loss_rate, version, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0)",
                FOOD_ID, MATERIAL_OK, "审计失败原料A", new BigDecimal("1"), "个");
        jdbcTemplate.update(
                "INSERT INTO dish_recipes (food_id, material_id, material_name, required_quantity, unit, loss_rate, version, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0)",
                FOOD_ID, MATERIAL_SHORT, "审计失败原料B", new BigDecimal("1"), "个");
        jdbcTemplate.update(
                "INSERT INTO orders (order_id, order_number, order_code, order_type, order_source, order_status, payment_status, total_amount, final_amount, store_id, deleted, create_time, update_time) "
                        + "VALUES (?, ?, ?, 1, 1, 1, 0, 1000, 1000, ?, 0, NOW(), NOW())",
                ORDER_ID, ORDER_NUMBER, "IT-OC-AF", Long.valueOf(STORE_ID));
        jdbcTemplate.update(
                "INSERT INTO order_items (item_id, order_id, product_type, food_id, product_name, unit_price, quantity, amount, kitchen_status, deleted, create_time, update_time) "
                        + "VALUES (?, ?, 1, ?, ?, 1000, 1, 1000, 0, 0, NOW(), NOW())",
                ITEM_ID, ORDER_ID, String.valueOf(FOOD_ID), "审计失败菜品");
        jdbcTemplate.update(
                "INSERT INTO kitchen_order (kitchen_order_id, order_id, order_number, order_type, status, priority, total_dishes, store_id, store_name, material_consumed, material_locked, deleted, create_time, update_time) "
                        + "VALUES (?, ?, ?, 0, 'ready', 0, 1, ?, '集成测试门店AF', 0, 0, 0, NOW(), NOW())",
                KITCHEN_ORDER_ID, ORDER_ID, ORDER_NUMBER, Long.valueOf(STORE_ID));
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void cleanFixture() {
        jdbcTemplate.update("DELETE FROM material_consumption WHERE kitchen_order_id = ?", KITCHEN_ORDER_ID);
        jdbcTemplate.update("DELETE FROM kitchen_order WHERE kitchen_order_id = ?", KITCHEN_ORDER_ID);
        jdbcTemplate.update("DELETE FROM order_items WHERE item_id = ?", ITEM_ID);
        jdbcTemplate.update("DELETE FROM orders WHERE order_id = ?", ORDER_ID);
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

    /**
     * S9a-2：门店 → 统一位置（stores_new 行 + locations STORE 型行 + location_id_map 桥，规则 4）。
     */
    private void insertStoreLocation() {
        jdbcTemplate.update(
                "INSERT INTO stores_new (store_id, store_code, store_name, status, create_time, update_time, deleted) "
                        + "OVERRIDING SYSTEM VALUE "
                        + "VALUES (?, 'IT-STORE-AF', '集成测试门店AF', 1, NOW(), NOW(), 0)",
                Long.valueOf(STORE_ID));
        locationId = jdbcTemplate.queryForObject(
                "INSERT INTO locations (location_code, location_name, location_type, status) "
                        + "VALUES (?, ?, 'STORE', 1) RETURNING location_id",
                Long.class, LOCATION_CODE, "集成测试门店AF");
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

    private void insertFood() {
        jdbcTemplate.update(
                "INSERT INTO foods (food_id, food_code, food_name, category_id, specification, unit, sale_price, cost_price, status, version, deleted, create_time, update_time) "
                        + "OVERRIDING SYSTEM VALUE "
                        + "VALUES (?, 'ITFD-AF', '审计失败菜品', NULL, '份', '份', 1000, 500, 1, 0, 0, NOW(), NOW())",
                FOOD_ID);
    }

    private int materialConsumed(String kitchenOrderId) {
        Integer v = jdbcTemplate.queryForObject(
                "SELECT material_consumed FROM kitchen_order WHERE kitchen_order_id = ?",
                Integer.class, kitchenOrderId);
        return v == null ? -1 : v;
    }

    /** S9a-2：统一账库存读取（store_inventory.current_stock → inventory.quantity） */
    private BigDecimal stock(Long materialId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE location_id = ? AND material_id = ?",
                BigDecimal.class, locationId, materialId);
    }

    @Test
    @DisplayName("审计写入自身失败：原 STOCK_INSUFFICIENT 仍传播；主事务回滚 material_consumed=0；库存恢复；不伪装成功")
    void auditSelfFailure_originalExceptionPropagates_mainTxStillRollsBack() {
        BigDecimal okBefore = stock(MATERIAL_OK);

        MaterialDeductionFailureException ex = assertThrows(
                MaterialDeductionFailureException.class,
                () -> orderNewService.deductMaterialsForServe(KITCHEN_ORDER_ID, SOURCE_REF, TRAY_CODE));

        assertEquals(MaterialDeductionFailureException.STOCK_INSUFFICIENT, ex.getFailureType());
        assertEquals(1, ex.getSuppressed().length, "审计异常应作为 suppressed，不得替换主异常");
        assertEquals("模拟审计写入自身失败", ex.getSuppressed()[0].getMessage());

        assertEquals(0, materialConsumed(KITCHEN_ORDER_ID), "审计失败时主事务仍须回滚 material_consumed=0");
        assertEquals(0, stock(MATERIAL_SHORT).compareTo(BigDecimal.ZERO));
        assertEquals(0, okBefore.compareTo(stock(MATERIAL_OK)), "已扣库存须随主事务回滚恢复");

        Long failedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM material_consumption WHERE kitchen_order_id = ? AND status = 'FAILED'",
                Long.class, KITCHEN_ORDER_ID);
        assertEquals(0L, failedRows, "审计写入失败时不可能留下 FAILED 行（预期：无结构化持久化兜底）");
    }
}
