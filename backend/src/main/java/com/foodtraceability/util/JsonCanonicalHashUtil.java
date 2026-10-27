package com.foodtraceability.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * jsonb 字段的**规范化摘要**（bug-④）。
 *
 * <p><b>为什么需要</b>：PostgreSQL {@code jsonb} 会规范化存储（去多余空格、键重排、去重键），
 * 因此「写入时的原始字符串」与「读回的字符串」**逐字节不相等**（实测
 * {@code '[{"a":1}]'} 读回是 {@code '[{"a": 1}]'}，长度 9 → 10）。
 * 若直接对原始字符串取 SHA-256，同一笔数据「写时算一次、读回算一次」会得到**不同**哈希 → 假的篡改告警。</p>
 *
 * <p><b>⚠ 使用方纪律（务必遵守）</b>：调用方必须在**写入时**与**读回后**各算一次并比对，
 * <b>直接对原始输入串算哈希会因 jsonb 规范化产生假篡改告警</b>。本次实测（PostgreSQL 18）：
 * <pre>
 *   写入 w = [{"a":1}]              → 读回 r = [{"a": 1}]            （9 → 10 字节，逐字节不等）
 *   写入 w = { "b" : [...],  "a":1 } → 读回 r = {"a": 1, "b": [...]}  （39 → 35 字节，键被重排）
 *   但 sha256HexCanonical(w) == sha256HexCanonical(r)               （两种用例均相等，见 JsonCanonicalHashUtilTest）
 * </pre>
 * 当前本项目**尚无任何调用点**（chain_data 只做原样回显）；接入完整性校验时，两处都必须调用本工具，
 * 且只对 {@link #canonicalize(String)} 的结果取摘要。</p>
 *
 * <p><b>规范化口径</b>：解析成 Jackson 树 → 对象键按字典序重排（递归）→ 无空格序列化（{@code writeValueAsString}），
 * 再对该文本取 SHA-256（小写 hex）。该文本与「jsonb::text 的等价类」一一对应，故
 * {@code sha256Hex(w) == sha256Hex(r)}，只要 w 与 r 表示同一 JSON 值。</p>
 */
public final class JsonCanonicalHashUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonCanonicalHashUtil() {
    }

    /** 把任意 JSON 文本规范化为「键有序、无空格」的紧凑形式；非 JSON 输入抛 IllegalArgumentException */
    public static String canonicalize(String json) {
        if (json == null) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return MAPPER.writeValueAsString(sortKeys(node));
        } catch (Exception e) {
            throw new IllegalArgumentException("不是合法 JSON，无法规范化：" + json, e);
        }
    }

    /** 规范化后的 SHA-256（小写 hex）。null → null（不摘要，避免与空串混淆） */
    public static String sha256HexCanonical(String json) {
        String canonical = canonicalize(json);
        if (canonical == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static JsonNode sortKeys(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
                names.add(it.next());
            }
            names.sort(String::compareTo);
            ObjectNode sorted = MAPPER.createObjectNode();
            for (String name : names) {
                sorted.set(name, sortKeys(node.get(name)));
            }
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = MAPPER.createArrayNode();
            for (JsonNode child : node) {
                array.add(sortKeys(child));
            }
            return array;
        }
        return node;
    }
}
