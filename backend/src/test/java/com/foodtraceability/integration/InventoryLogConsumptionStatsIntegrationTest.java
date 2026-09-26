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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-INVENTORY-CONSUMPTION-STATS-001：InventoryLogMapper.selectConsumptionStats 列名 + 动态 WHERE 集成测试。
 * <p>
 * 修复前（两层缺陷）：
 * <ol>
 *   <li>SQL 列名写成 {@code SUM(change_quantity)}，而实体/实表列名是 {@code change_amount}
 *       → 任何调用抛 PG "column change_quantity does not exist" → 端点 500；</li>
 *   <li>mapper 接口 3 个 @Param（startTime/endTime/operationType）在 XML 全部未引用（无 WHERE）
 *       → 修列名后仍是全局统计。</li>
 * </ol>
 * <p>
 * 覆盖验收五场景（mapper 级直接注入 InventoryLogMapper 测三参数组合；
 * Service 层 getConsumptionStats 硬编码 operationType="out"，故 Service/端点级只测 "out" 语义）：
 * <ol>
 *   <li>不传参 → 全局统计 = 全部 deleted=0 行的 SUM(change_amount)（fixture 已知值精确断言）；</li>
 *   <li>仅 operationType="out" → 只统计该类型；</li>
 *   <li>仅 startTime/endTime → 只统计区间内；</li>
 *   <li>组合参数（operationType + 区间）→ 交集；</li>
 *   <li>端点 GET /v1/inventory/consumptions/stats 不再 500（code=0），且 Service 硬编码 "out" 语义保持。</li>
 * </ol>
 * <p>
 * 环境要求：本地 PostgreSQL 可达，库 food_traceability schema 已就绪
 * （与 {@link InventoryLogFilterIntegrationTest} 同模式：直连 dev 库、禁用 Flyway）。
 * fixture 仅清理 remark LIKE 'IT-CS-%' 的行，不触碰真实数据；
 * 全局断言沿用同模式前提：本地 inventory_log 无 fixture 之外的行（开卡时活体核对本表 0 行）。
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/food_traceability?currentSchema=public&useUnicode=true&characterEncoding=utf-8&sslmode=disable",
        "spring.datasource.username=postgres",
        "spring.datasource.password=123456"
})
@AutoConfigureMockMvc
@DisplayName("P1-INVENTORY-CONSUMPTION-STATS-001 selectConsumptionStats 列名 + 动态 WHERE")
class InventoryLogConsumptionStatsIntegrationTest {

    private static final Long PRODUCT = 990001L;
    private static final Long WAREHOUSE = 991001L;

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
        // out / 10 / 09-01 —— 区间外
        insertLog("out", 10, "IT-CS-OUT-0901", 0, "2026-09-01 10:00:00");
        // out / 20 / 09-15 —— 区间内
        insertLog("out", 20, "IT-CS-OUT-0915", 0, "2026-09-15 10:00:00");
        // in / 5 / 09-10 —— 区间内，不同操作类型
        insertLog("in", 5, "IT-CS-IN-0910", 0, "2026-09-10 10:00:00");
        // out / 100 / deleted=1 —— 逻辑删除行，任何查询都不应计入
        insertLog("out", 100, "IT-CS-OUT-DELETED", 1, "2026-09-15 11:00:00");
    }

    @AfterEach
    void tearDownFixture() {
        cleanFixture();
    }

    private void insertLog(String operationType, int changeAmount, String remark, int deleted, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO inventory_log (product_id, warehouse_id, operation_type, "
                        + "before_stock, after_stock, change_amount, operator_id, remark, "
                        + "create_time, update_time, deleted) "
                        + "VALUES (?, ?, ?, 0, 0, ?, 1, ?, ?::timestamp, NOW(), ?)",
                PRODUCT, WAREHOUSE, operationType, changeAmount, remark, createTime, deleted);
    }

    private void cleanFixture() {
        jdbcTemplate.update("DELETE FROM inventory_log WHERE remark LIKE 'IT-CS-%'");
    }

    private static void assertTotal(Map<String, Object> stats, String expected, String message) {
        assertNotNull(stats, "stats 结果不应为 null");
        Object total = stats.get("total");
        assertNotNull(total, "total 键应存在（SUM 无匹配行时为 NULL，本 fixture 均有匹配行）");
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) total), message);
    }

    @Test
    @DisplayName("验收 1：不传参 → 全局统计 = 全部 deleted=0 行 SUM(change_amount)（精确值）")
    void noParams_globalStats_exactSum() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(null, null, null);
        // 10 + 20 + 5 = 35；deleted=1 行（100）被 WHERE deleted=0 排除
        assertTotal(stats, "35", "全局统计应恰好 35（3 条未删除行之和，逻辑删除行排除）");
    }

    @Test
    @DisplayName("验收 2：仅 operationType=out → 只统计 out 类型")
    void operationTypeOnly_outTypeOnly() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(null, null, "out");
        // 10 + 20 = 30；in 行（5）与逻辑删除行（100）均不计入
        assertTotal(stats, "30", "out 类型应恰好 30（in 行与逻辑删除行排除）");
    }

    @Test
    @DisplayName("验收 3：仅 startTime/endTime → 只统计区间内行")
    void timeRangeOnly_withinWindow() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(START, END, null);
        // 20（09-15 out）+ 5（09-10 in）= 25；09-01 的 10 在区间外
        assertTotal(stats, "25", "区间内应恰好 25（区间外的 09-01 行排除）");
    }

    @Test
    @DisplayName("验收 4：组合参数（out + 区间）→ 交集")
    void combined_operationTypeAndWindow_intersection() {
        Map<String, Object> stats = inventoryLogMapper.selectConsumptionStats(START, END, "out");
        // 仅 IT-CS-OUT-0915（20）同时满足 out 且在区间内
        assertTotal(stats, "20", "out ∩ 区间内应恰好 20（仅 IT-CS-OUT-0915）");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"inventory:query"})
    @DisplayName("验收 5：端点 GET /v1/inventory/consumptions/stats 不再 500（code=0），Service 硬编码 out 语义保持")
    void endpoint_stats_no500_andOutSemantics() throws Exception {
        mockMvc.perform(get("/v1/inventory/consumptions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(30));
        // Service 层硬编码 operationType="out"（InventoryLogServiceImpl L62）：
        // 端点不带参数也只统计 out 行 = 30（含区间外 09-01 行，排除 in 行与逻辑删除行）
    }
}
