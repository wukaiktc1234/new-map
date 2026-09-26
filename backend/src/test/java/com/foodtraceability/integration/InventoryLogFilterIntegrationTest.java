package com.foodtraceability.integration;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLog;
import com.foodtraceability.mapper.InventoryLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * P1-INVENTORY-LOG-FILTER-001：InventoryLogMapper.selectInventoryLogPage 动态 WHERE 集成测试。
 * <p>
 * 修复前：XML 无 WHERE，mapper 接口的 6 个 @Param 全部被静默丢弃，
 * 库存详情弹窗传 productId + warehouseId 仍返回全局库存日志。
 * <p>
 * 覆盖验收三场景：
 * <ol>
 *   <li>productId + warehouseId 过滤 → 只返回该物料该仓库的记录；</li>
 *   <li>全参数为 null（WarehouseOverview 全局场景）→ 返回全部未删除记录（分页）；</li>
 *   <li>仅 operationType（InventoryConsumptionController "out" 场景）→ 只返回该类型记录。</li>
 * </ol>
 * 另验证基线：deleted=1 逻辑删除行不进入任何查询结果。
 * <p>
 * 环境要求：本地 PostgreSQL 可达，库 food_traceability schema 已就绪
 * （与 {@link MaterialDeductionAuditIntegrationTest} 同模式：直连 dev 库、禁用 Flyway）。
 * fixture 仅清理 remark LIKE 'IT-%' 的行，不触碰真实数据。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
@DisplayName("P1-INVENTORY-LOG-FILTER-001 selectInventoryLogPage 动态 WHERE")
class InventoryLogFilterIntegrationTest {

    private static final Long PRODUCT_A = 880001L;
    private static final Long PRODUCT_B = 880002L;
    private static final Long WAREHOUSE_1 = 881001L;
    private static final Long WAREHOUSE_2 = 881002L;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixture() {
        cleanFixture();
        // 物料 A / 仓库 1 / out —— 场景 1 唯一应返回的行
        insertLog(PRODUCT_A, WAREHOUSE_1, "out", "IT-A-W1-OUT");
        // 物料 A / 仓库 2 / in —— 同物料不同仓库
        insertLog(PRODUCT_A, WAREHOUSE_2, "in", "IT-A-W2-IN");
        // 物料 B / 仓库 1 / in —— 不同物料
        insertLog(PRODUCT_B, WAREHOUSE_1, "in", "IT-B-W1-IN");
        // 物料 B / 仓库 2 / out —— 不同物料且为 out 类型
        insertLog(PRODUCT_B, WAREHOUSE_2, "out", "IT-B-W2-OUT");
        // 逻辑删除行 —— 任何查询都不应返回
        insertLog(PRODUCT_A, WAREHOUSE_1, "out", "IT-A-W1-DELETED", 1);
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void insertLog(Long productId, Long warehouseId, String operationType, String remark) {
        insertLog(productId, warehouseId, operationType, remark, 0);
    }

    private void insertLog(Long productId, Long warehouseId, String operationType, String remark, int deleted) {
        jdbcTemplate.update(
                "INSERT INTO inventory_log (product_id, warehouse_id, operation_type, "
                        + "before_stock, after_stock, change_amount, operator_id, remark, "
                        + "create_time, update_time, deleted) "
                        + "VALUES (?, ?, ?, 0, 0, 1, 1, ?, NOW(), NOW(), ?)",
                productId, warehouseId, operationType, remark, deleted);
    }

    private void cleanFixture() {
        jdbcTemplate.update("DELETE FROM inventory_log WHERE remark LIKE 'IT-%'");
    }

    @Test
    @DisplayName("场景 1：productId + warehouseId 过滤 → 只返回该物料该仓库记录")
    void scenario1_productAndWarehouseFilter_returnsOnlyMatchingRow() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), PRODUCT_A, WAREHOUSE_1, null, null, null, null);

        assertEquals(1, page.getTotal(), "物料 A + 仓库 1 应恰好 1 条");
        List<InventoryLog> rows = page.getRecords();
        assertEquals(1, rows.size());
        assertEquals("IT-A-W1-OUT", rows.get(0).getRemark());
        assertEquals(PRODUCT_A, rows.get(0).getProductId());
        assertEquals(WAREHOUSE_1, rows.get(0).getWarehouseId());
    }

    @Test
    @DisplayName("场景 2：全参数 null（全局）→ 返回全部未删除记录（分页）")
    void scenario2_noParams_returnsAllNonDeletedGlobal() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), null, null, null, null, null, null);

        assertEquals(4, page.getTotal(), "4 条未删除记录应全部返回（全局语义维持）");
        assertEquals(4, page.getRecords().size());
    }

    @Test
    @DisplayName("场景 3：仅 operationType=out → 只返回 out 类型记录")
    void scenario3_operationTypeOnly_returnsOnlyThatType() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), null, null, "out", null, null, null);

        assertEquals(2, page.getTotal(), "out 类型应恰好 2 条");
        page.getRecords().forEach(r -> assertEquals("out", r.getOperationType()));
    }

    @Test
    @DisplayName("基线：deleted=1 逻辑删除行不进入任何查询结果")
    void baseline_softDeletedRow_excluded() {
        IPage<InventoryLog> global = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), null, null, null, null, null, null);
        global.getRecords().forEach(r ->
                assertNotEquals("IT-A-W1-DELETED", r.getRemark(), "逻辑删除行不应出现"));

        // 删除行与 IT-A-W1-OUT 同物料同仓库同类型：若 WHERE 无 deleted=0，场景 1 会返回 2 条
        IPage<InventoryLog> filtered = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), PRODUCT_A, WAREHOUSE_1, "out", null, null, null);
        assertEquals(1, filtered.getTotal());
        assertEquals("IT-A-W1-OUT", filtered.getRecords().get(0).getRemark());
    }
}
