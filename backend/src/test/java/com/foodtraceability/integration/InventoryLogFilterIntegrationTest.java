package com.foodtraceability.integration;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLog;
import com.foodtraceability.mapper.InventoryLogMapper;
import com.foodtraceability.service.InventoryMovementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-INVENTORY-LOG-FILTER-001：InventoryLogMapper.selectInventoryLogPage 动态 WHERE 集成测试。
 * <p>
 * M3-M4 S9a-2 重锚（fixture → 统一流水 inventory_movement）：
 * <ul>
 *   <li>旧 inventory_log 软删除 fixture 行（deleted=1）废弃：movement 为追加型账本，无 deleted 列；
 *       基线场景替换为"追加型账本 baseline"（insert 后即可读 + 服务层无 update/delete API，宪法 §III.7）；</li>
 *   <li>全局场景（场景 2）因真实数据共存改包含断言（total ≥ 4 + anyMatch 4 条 fixture 行）；
 *       scoped 场景（场景 1/3）保持精确值（fixture ID 空间 88xxxx 与真实数据无碰撞；
 *       场景 3 前提：活体账本当前无 fixture 之外的 OUT 行）。</li>
 * </ul>
 * <p>
 * 覆盖验收：
 * <ol>
 *   <li>productId + warehouseId 过滤 → 只返回该物料该仓库的记录；</li>
 *   <li>全参数为 null（WarehouseOverview 全局场景）→ 返回全部记录（包含断言：真实数据共存）；</li>
 *   <li>仅 operationType（InventoryConsumptionController "out" 场景）→ 只返回该类型记录。</li>
 * </ol>
 * 另验证基线：追加型账本 —— insert 后即可读；InventoryMovementService 无 update/delete API。
 * <p>
 * 环境要求：本地 PostgreSQL 可达，库 food_traceability schema 已就绪
 * （与 {@link MaterialDeductionAuditIntegrationTest} 同模式：直连 dev 库、禁用 Flyway）。
 * fixture 仅清理 source_type='IT_FIXTURE' AND source_ref LIKE 'IT-%' 的行，不触碰真实数据。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
@DisplayName("P1-INVENTORY-LOG-FILTER-001 selectInventoryLogPage 动态 WHERE（S9a-2 重锚 movement）")
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
        // 物料 A / 仓库 1 / OUT —— 场景 1 唯一应返回的行
        insertMovement(PRODUCT_A, WAREHOUSE_1, "OUT", -1, "IT-A-W1-OUT");
        // 物料 A / 仓库 2 / IN —— 同物料不同仓库
        insertMovement(PRODUCT_A, WAREHOUSE_2, "IN", 1, "IT-A-W2-IN");
        // 物料 B / 仓库 1 / IN —— 不同物料
        insertMovement(PRODUCT_B, WAREHOUSE_1, "IN", 1, "IT-B-W1-IN");
        // 物料 B / 仓库 2 / OUT —— 不同物料且为 OUT 类型
        insertMovement(PRODUCT_B, WAREHOUSE_2, "OUT", -1, "IT-B-W2-OUT");
        // （旧 deleted=1 fixture 行已移除：movement 无 deleted 列，追加型账本无软删除语义）
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void insertMovement(Long materialId, Long locationId, String movementType, int changeQty, String sourceRef) {
        jdbcTemplate.update(
                "INSERT INTO inventory_movement (location_id, material_id, change_qty, balance_after, "
                        + "movement_type, source_type, source_ref, operator_id, remark, create_time) "
                        + "VALUES (?, ?, ?, 0, ?, 'IT_FIXTURE', ?, 1, 'S9a-2 fixture', NOW())",
                locationId, materialId, changeQty, movementType, sourceRef);
    }

    private void cleanFixture() {
        jdbcTemplate.update(
                "DELETE FROM inventory_movement WHERE source_type = 'IT_FIXTURE' AND source_ref LIKE 'IT-%'");
    }

    @Test
    @DisplayName("场景 1：productId + warehouseId 过滤 → 只返回该物料该仓库记录")
    void scenario1_productAndWarehouseFilter_returnsOnlyMatchingRow() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), PRODUCT_A, WAREHOUSE_1, null, null, null, null);

        assertEquals(1, page.getTotal(), "物料 A + 仓库 1 应恰好 1 条（fixture ID 空间无碰撞）");
        List<InventoryLog> rows = page.getRecords();
        assertEquals(1, rows.size());
        assertEquals("IT-A-W1-OUT", rows.get(0).getRemark());
        assertEquals(PRODUCT_A, rows.get(0).getProductId());
        assertEquals(WAREHOUSE_1, rows.get(0).getWarehouseId());
    }

    @Test
    @DisplayName("场景 2：全参数 null（全局）→ 真实数据共存（包含断言：total ≥ 4 + 4 条 fixture 行均在结果）")
    void scenario2_noParams_global_inclusion() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 20), null, null, null, null, null, null);

        assertTrue(page.getTotal() >= 4, "全局 total 应 ≥ 4 条 fixture 行（实际=" + page.getTotal() + "）");
        List<InventoryLog> rows = page.getRecords();
        // fixture 行 create_time=NOW() 为最新，ORDER BY create_time DESC 下位于首页
        assertTrue(rows.stream().anyMatch(r -> "IT-A-W1-OUT".equals(r.getRemark())), "IT-A-W1-OUT 应在全局结果");
        assertTrue(rows.stream().anyMatch(r -> "IT-A-W2-IN".equals(r.getRemark())), "IT-A-W2-IN 应在全局结果");
        assertTrue(rows.stream().anyMatch(r -> "IT-B-W1-IN".equals(r.getRemark())), "IT-B-W1-IN 应在全局结果");
        assertTrue(rows.stream().anyMatch(r -> "IT-B-W2-OUT".equals(r.getRemark())), "IT-B-W2-OUT 应在全局结果");
    }

    @Test
    @DisplayName("场景 3：仅 operationType=out → 只返回 OUT 类型记录")
    void scenario3_operationTypeOnly_returnsOnlyThatType() {
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), null, null, "out", null, null, null);

        assertEquals(2, page.getTotal(), "out 类型应恰好 2 条（fixture 2 条 OUT；活体当前无其他 OUT 行）");
        page.getRecords().forEach(r -> assertEquals("out", r.getOperationType()));
    }

    @Test
    @DisplayName("基线（S9a-2 重锚）：追加型账本 —— insert 后即可读；服务层无 update/delete API（§III.7）")
    void baseline_appendOnlyLedger_insertReadable_noUpdateDeleteApi() {
        // insert → 立即可读：fixture IT-A-W1-OUT 由 JdbcTemplate insert 后，定向分页查询即返回
        IPage<InventoryLog> page = inventoryLogMapper.selectInventoryLogPage(
                new Page<>(1, 10), PRODUCT_A, WAREHOUSE_1, "out", null, null, null);
        assertTrue(page.getRecords().stream().anyMatch(r -> "IT-A-W1-OUT".equals(r.getRemark())),
                "追加型账本 insert 后应立即可读");

        // 服务层契约：InventoryMovementService 仅提供 recordMovement，无 update/delete API（宪法 §III.7）
        assertTrue(Arrays.stream(InventoryMovementService.class.getMethods())
                        .noneMatch(m -> m.getName().toLowerCase().startsWith("update")
                                || m.getName().toLowerCase().startsWith("delete")),
                "InventoryMovementService 不得提供 update/delete API");
    }
}
