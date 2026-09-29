package com.foodtraceability.integration;

import com.foodtraceability.mapper.InventoryLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-INVENTORY-CONSUMPTION-STATS-001：InventoryLogMapper.selectConsumptionStats 列名 + 动态 WHERE 集成测试。
 * <p>
 * M3-M4 S9a-2 重锚（fixture → 统一流水 inventory_movement）：
 * <ul>
 *   <li>旧 inventory_log fixture 行（含 deleted=1 逻辑删除行）全部废弃：movement 为追加型账本，
 *       无 deleted 列，软删除场景不成立（宪法 §III.7）；</li>
 *   <li>change_qty 符号约定按 -001 §1.4：正=入、负=出（迁移 4b"legacy 已带正入负出符号，原值直拷"），
 *       故 scoped 精确断言沿用旧期望的绝对值、按符号约定取负（30→-30、20→-20）；</li>
 *   <li>全局场景（验收 1/3）因真实数据共存改包含断言（anyMatch + total ≥ fixture 贡献下界），
 *       精确值由新增验收 6"真实数据共存 delta"承载；scoped 场景（验收 2/4/5）保持精确值
 *       （前提：活体账本当前无 fixture 之外的 OUT 行，15 行全 IN 正数量；若出现真实 OUT 行先核实现状再改 delta）。</li>
 * </ul>
 * <p>
 * 覆盖验收（mapper 级直接注入 InventoryLogMapper 测三参数组合；
 * Service 层 getConsumptionStats 硬编码 operationType="out"，故 Service/端点级只测 "out" 语义）：
 * <ol>
 *   <li>不传参 → 全局统计（包含断言：fixture 行进入范围 + total ≥ fixture 贡献下界）；</li>
 *   <li>仅 operationType="out" → 只统计 OUT 行（精确值）；</li>
 *   <li>仅 startTime/endTime → 只统计区间内行（包含断言）；</li>
 *   <li>组合参数（out + 区间）→ 交集（精确值）；</li>
 *   <li>端点 GET /v1/inventory/consumptions/stats 不再 500（code=0），Service 硬编码 "out" 语义保持；</li>
 *   <li>（S9a-2 新增）真实数据共存 → 全局 total = 非 fixture 基线 + fixture 贡献（delta 精确）。</li>
 * </ol>
 * <p>
 * 环境要求：本地 PostgreSQL 可达，库 food_traceability schema 已就绪
 * （与 {@link InventoryLogFilterIntegrationTest} 同模式：直连 dev 库、禁用 Flyway）。
 * fixture 仅清理 source_type='IT_FIXTURE' AND source_ref LIKE 'IT-CS-%' 的行，不触碰真实数据；
 * 基线查询为 SELECT-only（活体验证不写，Owner 规则）。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
@AutoConfigureMockMvc
@DisplayName("P1-INVENTORY-CONSUMPTION-STATS-001 selectConsumptionStats 列名 + 动态 WHERE（S9a-2 重锚 movement）")
class InventoryLogConsumptionStatsIntegrationTest {

    private static final Long MATERIAL = 990001L;
    private static final Long LOCATION = 991001L;

    /** 测试区间：2026-09-05 ~ 2026-09-20（IT-CS-OUT-0901 在区间外） */
    private static final String START = "2026-09-05 00:00:00";
    private static final String END = "2026-09-20 23:59:59";

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpFixture() {
        cleanFixture();
        // OUT / -10 / 09-01 —— 区间外
        insertMovement("OUT", new BigDecimal("-10"), "IT-CS-OUT-0901", "2026-09-01 10:00:00");
        // OUT / -20 / 09-15 —— 区间内
        insertMovement("OUT", new BigDecimal("-20"), "IT-CS-OUT-0915", "2026-09-15 10:00:00");
        // IN / +5 / 09-10 —— 区间内，不同操作类型
        insertMovement("IN", new BigDecimal("5"), "IT-CS-IN-0910", "2026-09-10 10:00:00");
        // （旧 deleted=1 fixture 行已移除：movement 无 deleted 列，追加型账本无软删除语义）
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void insertMovement(String movementType, BigDecimal changeQty, String sourceRef, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO inventory_movement (location_id, material_id, change_qty, balance_after, "
                        + "movement_type, source_type, source_ref, operator_id, remark, create_time) "
                        + "VALUES (?, ?, ?, 0, ?, 'IT_FIXTURE', ?, 1, 'S9a-2 fixture', ?::timestamp)",
                LOCATION, MATERIAL, changeQty, movementType, sourceRef, createTime);
    }

    private void cleanFixture() {
        jdbcTemplate.update(
                "DELETE FROM inventory_movement WHERE source_type = 'IT_FIXTURE' AND source_ref LIKE 'IT-CS-%'");
    }

    private static void assertTotalExactly(Map<String, Object> stats, String expected, String message) {
        assertNotNull(stats, "stats 结果不应为 null");
        Object total = stats.get("total");
        assertNotNull(total, "total 键应存在（COALESCE 无匹配行时为 0）");
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) total),
                message + "（实际=" + total + "）");
    }

    private static void assertTotalAtLeast(Map<String, Object> stats, String lowerBound, String message) {
        assertNotNull(stats, "stats 结果不应为 null");
        Object total = stats.get("total");
        assertNotNull(total, "total 键应存在（COALESCE 无匹配行时为 0）");
        assertTrue(new BigDecimal(lowerBound).compareTo((BigDecimal) total) <= 0,
                message + "（实际=" + total + "）");
    }

    /** fixture 行在全局范围内的 source_ref 集合（SELECT-only） */
    private List<String> fixtureRefsInScope() {
        return jdbcTemplate.queryForList(
                "SELECT source_ref FROM inventory_movement "
                        + "WHERE source_type = 'IT_FIXTURE' AND source_ref LIKE 'IT-CS-%'",
                String.class);
    }

    /** fixture 行在 [START, END] 窗口内的 source_ref 集合（SELECT-only） */
    private List<String> fixtureRefsInWindow() {
        return jdbcTemplate.queryForList(
                "SELECT source_ref FROM inventory_movement "
                        + "WHERE source_type = 'IT_FIXTURE' AND source_ref LIKE 'IT-CS-%' "
                        + "AND create_time >= ?::timestamp AND create_time <= ?::timestamp",
                String.class, START, END);
    }

    /** 非 fixture（真实）行的基线合计（SELECT-only，不写活体） */
    private BigDecimal baselineExcludingFixture() {
        BigDecimal s = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(change_qty), 0) FROM inventory_movement WHERE source_ref NOT LIKE 'IT-CS-%'",
                BigDecimal.class);
        return s == null ? BigDecimal.ZERO : s;
    }

    @Test
    @DisplayName("验收 1：不传参 → 全局统计（包含断言：fixture 行进入范围 + total ≥ fixture 贡献下界）")
    void noParams_globalStats_inclusionAndBound() {
        // 3 条 fixture 行均进入无参全局范围（anyMatch）
        List<String> refs = fixtureRefsInScope();
        assertTrue(refs.stream().anyMatch("IT-CS-OUT-0901"::equals), "fixture OUT-0901 应在全局范围");
        assertTrue(refs.stream().anyMatch("IT-CS-OUT-0915"::equals), "fixture OUT-0915 应在全局范围");
        assertTrue(refs.stream().anyMatch("IT-CS-IN-0910"::equals), "fixture IN-0910 应在全局范围");

        // 包含断言下界：活体真实账本全 IN 型正数量（15 行，基线 ≥ 0），
        // fixture 贡献 = -10 + -20 + 5 = -25 → 全局 total ≥ -25
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(null, null, null);
        assertTotalAtLeast(stats, "-25", "全局 total 应 ≥ fixture 贡献下界 -25（基线 ≥ 0）");
    }

    @Test
    @DisplayName("验收 2：仅 operationType=out → 只统计 OUT 行（精确值 -30）")
    void operationTypeOnly_outTypeOnly() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(null, null, "out");
        // OUT 负号约定：-10 + -20 = -30；IN 行（+5）不计入
        // （活体账本当前无 fixture 之外的 OUT 行；若出现先核实现状再改 delta 断言）
        assertTotalExactly(stats, "-30", "out 类型应恰好 -30（fixture -10 + -20，无其他 OUT 行）");
    }

    @Test
    @DisplayName("验收 3：仅 startTime/endTime → 只统计区间内行（包含断言）")
    void timeRangeOnly_withinWindow() {
        // 区间内 2 条 fixture 行均进入窗口范围（anyMatch）
        List<String> refs = fixtureRefsInWindow();
        assertTrue(refs.stream().anyMatch("IT-CS-OUT-0915"::equals), "fixture OUT-0915（09-15）应在窗口范围");
        assertTrue(refs.stream().anyMatch("IT-CS-IN-0910"::equals), "fixture IN-0910（09-10）应在窗口范围");

        // 包含断言下界：活体窗口内基线为 1 行 IN 正数量（≥ 0），
        // 窗口内 fixture 贡献 = -20 + 5 = -15 → 窗口 total ≥ -15
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(START, END, null);
        assertTotalAtLeast(stats, "-15", "窗口 total 应 ≥ 窗口 fixture 贡献下界 -15（窗口基线 ≥ 0）");
    }

    @Test
    @DisplayName("验收 4：组合参数（out + 区间）→ 交集（精确值 -20）")
    void combined_operationTypeAndWindow_intersection() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(START, END, "out");
        // 仅 IT-CS-OUT-0915（-20）同时满足 out 且在区间内（活体当前无其他 OUT 行）
        assertTotalExactly(stats, "-20", "out ∩ 区间内应恰好 -20（仅 IT-CS-OUT-0915）");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"inventory:query"})
    @DisplayName("验收 5：端点 GET /v1/inventory/consumptions/stats 不再 500（code=0），Service 硬编码 out 语义保持")
    void endpoint_stats_no500_andOutSemantics() throws Exception {
        mockMvc.perform(get("/v1/inventory/consumptions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(-30));
        // Service 层硬编码 operationType="out"（InventoryLogServiceImpl）：
        // 端点不带参数也只统计 OUT 行 = fixture -30（活体当前无其他 OUT 行）
    }

    @Test
    @DisplayName("验收 6（S9a-2 新增）：真实数据共存 → 全局 total = 非 fixture 基线 + fixture 贡献（delta 精确）")
    void noParams_realDataCoexistence_deltaExact() {
        BigDecimal baseline = baselineExcludingFixture();
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(null, null, null);
        BigDecimal expected = baseline.add(new BigDecimal("-25"));
        assertTotalExactly(stats, expected.toPlainString(),
                "全局 total 应 = 非 fixture 基线（" + baseline + "）+ fixture 贡献（-25）");
    }
}
