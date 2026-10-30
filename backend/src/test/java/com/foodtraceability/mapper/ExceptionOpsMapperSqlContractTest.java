package com.foodtraceability.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0-C 单测：{@code casMarkPaid} 的 SQL 必须含 {@code order_status = 0} 条件。
 *
 * <p>为什么要断言注解文本：这条 SQL 是 MyBatis 注解内联的，条件漏写不会编译失败、
 * 也不会有任何运行期告警 —— 只有对「已取消/已退款」的单真跑一次才会暴露（2026-10-30 就是这么漏的）。
 * 把契约钉在单测里，改名/改条件时立刻红。</p>
 */
class ExceptionOpsMapperSqlContractTest {

    private String sqlOf(String methodName) throws Exception {
        for (Method m : ExceptionOpsMapper.class.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            Update up = m.getAnnotation(Update.class);
            if (up != null) {
                return String.join(" ", up.value());
            }
        }
        fail("找不到 @Update 方法：" + methodName);
        return null;
    }

    @Test
    @DisplayName("casMarkPaid 必须限定 order_status = 0（拒绝已取消/已退款单补收款）")
    void casMarkPaidRequiresPendingOrderStatus() throws Exception {
        String sql = sqlOf("casMarkPaid").replaceAll("\\s+", " ");

        assertNotNull(sql);
        assertTrue(sql.contains("order_status"), "casMarkPaid 必须出现 order_status 条件；实际 SQL：" + sql);
        assertTrue(sql.replace(" ", "").contains("order_status,0)=0"),
                "casMarkPaid 的 order_status 条件必须是 = 0（待确认）；实际 SQL：" + sql);
        assertTrue(sql.contains("payment_status"), "casMarkPaid 必须保留 payment_status 条件（双条件）");
        assertTrue(sql.contains("deleted = 0"), "casMarkPaid 必须保留 deleted = 0");
    }

    @Test
    @DisplayName("casCancel 必须限定 order_status = 0（RT-3：拒绝二次作废已终态的单）")
    void casCancelRequiresPendingOrderStatus() throws Exception {
        String sql = sqlOf("casCancel").replaceAll("\\s+", " ");
        assertTrue(sql.contains("payment_status"), "casCancel 必须保留 payment_status 条件");
        assertTrue(sql.contains("deleted = 0"));
        assertTrue(sql.replace(" ", "").contains("order_status,0)=0"),
                "casCancel 的 order_status 条件必须是 = 0；实际 SQL：" + sql);
    }
}
