package com.foodtraceability.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 写隔离证明（Lead 2026-10-30，Step 2）。
 *
 * <p><b>为什么不能用 ROLLBACK 版</b>：{@code BEGIN→INSERT→断言→ROLLBACK} 之后两库都是 0 行，
 * 与今天那个「两边都是 0，无法区分隔离是否生效」的坑完全同型 —— **弱证明**。</p>
 *
 * <p>因此本用例：**真提交** → 在 test 库能查到该 tag（1 行）→ 在【共享库】查同 tag = 0 → 再按 tag 删除。</p>
 */
@SpringBootTest
class DatabaseWriteIsolationProofTest {

    private static final String SHARED_URL =
            System.getenv().getOrDefault("SHARED_DB_URL", "jdbc:postgresql://localhost:5432/food_traceability");
    private static final String DB_USER = System.getenv().getOrDefault("PG_USERNAME", "postgres");
    private static final String DB_PASS = System.getenv().getOrDefault("PG_PASSWORD", "123456");

    @Autowired
    private JdbcTemplate jdbcTemplate;   // 指向 test 库（由 spring.profiles.active=test 决定）

    @Test
    @DisplayName("[DB-WRITE-PROOF] 提交到 test 库的行，共享库必须查不到")
    void writeLandsOnlyInTestDatabase() throws Exception {
        String tag = "[DB-PROOF:" + System.currentTimeMillis() + "]";
        String orderId = "DBPROOF-" + System.currentTimeMillis();
        try {
            // ① 真提交（自动提交，不包事务）
            // 只用【两库交集】的列：orders 在 test 库 30 列 / 共享库 54 列（缺 24 列，P0 部署阻断），
            // final_amount / remark 等新列在 test 库不存在 → 不能用
            jdbcTemplate.update(
                    "INSERT INTO orders (order_id, order_number, order_status, remarks, create_time) "
                            + "VALUES (?, ?, 3, ?, now())",
                    orderId, "DB-" + orderId, tag);

            // ② test 库必须能读到
            Integer inTest = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM orders WHERE remarks = ?", Integer.class, tag);
            System.out.println("[DB-WRITE-PROOF] test_lib_rows=" + inTest + " tag=" + tag);
            assertEquals(1, inTest, "真提交后 test 库应能读到该行");

            // ③ 共享库必须读不到（第二连接，显式指向共享库）
            int inShared = countInShared(tag);
            System.out.println("[DB-WRITE-PROOF] shared_lib_rows=" + inShared);
            assertEquals(0, inShared, "共享库读到了该行 —— 写隔离失效（会污染业务库）");
        } finally {
            // ④ 兜底清理（test 库内，按 tag 精确删；即使断言失败也不留残留）
            int deleted = jdbcTemplate.update("DELETE FROM orders WHERE remarks = ?", tag);
            Integer left = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM orders WHERE remarks = ?", Integer.class, tag);
            System.out.println("[DB-WRITE-PROOF] cleanup_deleted=" + deleted + " left=" + left);
            assertEquals(0, left, "清理后 test 库不得残留该 tag");
        }
    }

    private int countInShared(String tag) throws Exception {
        try (Connection c = DriverManager.getConnection(SHARED_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM orders WHERE remarks = ?")) {
            ps.setString(1, tag);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
