package com.foodtraceability.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * bug-④ 证据单测：jsonb 规范化会让「写入串」与「读回串」逐字节不等，
 * 但规范化摘要必须相等（否则同一笔数据会产生假的篡改告警）。
 *
 * 数据取自实测：PostgreSQL SELECT '{...}'::jsonb::text 的结果。
 */
class JsonCanonicalHashUtilTest {

    @Test
    @DisplayName("简单数组：w='[{\"a\":1}]' 与 r='[{\"a\": 1}]' 逐字节不等，但规范化摘要相等")
    void simpleArrayCanonicalHashEqual() {
        String w = "[{\"a\":1}]";          // 写入时的原始串
        String r = "[{\"a\": 1}]";         // jsonb::text 读回（实测 length 9 → 10）
        assertNotEquals(w, r, "原始串与读回串本就不同（jsonb 规范化）");
        assertEquals(JsonCanonicalHashUtil.sha256HexCanonical(w),
                JsonCanonicalHashUtil.sha256HexCanonical(r),
                "规范化后摘要必须相等");
    }

    @Test
    @DisplayName("含空格/键乱序/嵌套：w 39 字节，r 35 字节，规范化摘要仍相等")
    void nestedObjectCanonicalHashEqual() {
        String w = "{ \"b\" : [1, 2, {\"c\" : \"x y\"}],  \"a\":1 }";
        String r = "{\"a\": 1, \"b\": [1, 2, {\"c\": \"x y\"}]}";
        assertNotEquals(w, r);
        assertEquals(JsonCanonicalHashUtil.sha256HexCanonical(w),
                JsonCanonicalHashUtil.sha256HexCanonical(r));
        assertEquals("{\"a\":1,\"b\":[1,2,{\"c\":\"x y\"}]}", JsonCanonicalHashUtil.canonicalize(w),
                "规范化形式：键字典序 + 无空格");
    }

    @Test
    @DisplayName("键顺序不同但语义相同的两个串，规范化后完全相同")
    void keyOrderIndependent() {
        assertEquals(JsonCanonicalHashUtil.canonicalize("{\"a\":1,\"b\":2}"),
                JsonCanonicalHashUtil.canonicalize("{\"b\":2,\"a\":1}"));
    }
}
