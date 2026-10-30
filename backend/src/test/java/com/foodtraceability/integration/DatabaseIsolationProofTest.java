package com.foodtraceability.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 数据库隔离证明（Lead 2026-10-30，Step 2）。
 *
 * <p>唯一职责：**证明 IT 连的是测试库**，与「建单用例成不成功」完全解耦。</p>
 *
 * <p>为什么需要它：两库 orders 都是 0 时，无法区分「隔离生效」与「建单未执行」——
 * 这就是「失败与成功看起来一样」。本用例直接读 {@code current_database()}，
 * 把「连到哪个库」变成一条断言，而不是用副作用反推。</p>
 */
@SpringBootTest
class DatabaseIsolationProofTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("[DB-PROOF] IT 必须连 food_traceability_test，不得连共享库 food_traceability")
    void connectsToTestDatabase() throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_database(), current_user")) {
            rs.next();
            String db = rs.getString(1);
            String user = rs.getString(2);
            System.out.println("[DB-PROOF] current_database=" + db + " user=" + user);
            assertEquals("food_traceability_test", db,
                    "IT 连错库了：连到 " + db + " —— 会污染共享业务库（Lead 2026-10-30 P1）");
        }
    }
}
