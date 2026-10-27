package com.foodtraceability.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
    @DisplayName("写入端与读取端必须同一规范形式：writeThenReadBackHashEqual（[{\"a\":1}] 写入 → [{\"a\": 1}] 读回 → 两哈希相等）")
    void writeThenReadBackHashEqual() throws Exception {
        // 写入端：业务代码拿到的是原始 JSON 文本
        String writeSide = "[{\"a\":1}]";
        // 读取端：TypeHandler 从 jsonb 读回的是 PG 规范化后的文本（实测多一个空格，9→10 字节）
        String readBackSide = "[{\"a\": 1}]";
        assertEquals(9, writeSide.length(), "写入串长度（实测）");
        assertEquals(10, readBackSide.length(), "读回串长度（实测 jsonb 加了一个空格）");
        assertNotEquals(writeSide, readBackSide, "两端逐字节不相等（jsonb 规范化）");
        // 关键：两端都必须走 sha256HexCanonical，哈希才一致；一端原始串一端规范串会得到不同哈希
        assertEquals(JsonCanonicalHashUtil.sha256HexCanonical(writeSide),
                JsonCanonicalHashUtil.sha256HexCanonical(readBackSide),
                "写入端与读取端哈希必须相等（防篡改校验的前提）");
        // 反证：若直接对**原始串**取 SHA-256（不做规范化），两端哈希会不同 → 假篡改告警
        assertNotEquals(plainSha256(writeSide), plainSha256(readBackSide),
                "直接对原始串取摘要 → 两端不一致（这正是必须规范化的原因）");
    }

    /** 不做规范化、直接对原始串取 SHA-256（用于反证「不能一端原始一端规范」） */
    private static String plainSha256(String raw) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("键顺序不同但语义相同的两个串，规范化后完全相同")
    void keyOrderIndependent() {
        assertEquals(JsonCanonicalHashUtil.canonicalize("{\"a\":1,\"b\":2}"),
                JsonCanonicalHashUtil.canonicalize("{\"b\":2,\"a\":1}"));
    }
}
