package com.foodtraceability.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL JSONB ↔ String 直通类型处理器（bug-④）。
 *
 * <p><b>为什么不用 JacksonTypeHandler</b>：{@code JacksonTypeHandler} 会把列内容**反序列化成字段声明的 Java 类型**；
 * 当字段类型是 {@code String} 而列里是数组/对象时，Jackson 报
 * {@code MismatchedInputException: Cannot deserialize value of type java.lang.String from Array value}
 * → {@code GET /v1/trace-codes/{code}} 直接 500（已实测复现）。</p>
 *
 * <p><b>本处理器语义</b>：写 = 原样把 JSON 文本按 {@code Types.OTHER} 绑定（由 PG 按目标列 jsonb 解析）；
 * 读 = 直接把 {@code jsonb::text} 取成 String，**不做任何反序列化**。
 * 因此「写入 String」与「写入 List/Map（调用方先序列化）」两种入参都能成功，
 * 且读回文本与库内 jsonb 的文本形式一致。</p>
 *
 * <p><b>⚠ 重要（float 哈希的前提）</b>：jsonb 会**规范化**（去多余空格、键重排、去重键），
 * 所以「写入的原始字符串 w」与「读回的字符串 r」**逐字节不相等**（实测
 * {@code '[{"a":1}]'} → {@code '[{"a": 1}]'}）。任何基于 chain_data 的摘要/比对
 * **必须按规范化 JSON 计算**（见 {@code JsonCanonicalHashUtil}），否则同一笔数据「写时算一次、读回算一次」会得到
 * 不同哈希，产生**假的篡改告警**。</p>
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(JsonbStringTypeHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 传入可能是「JSON 文本」也可能是调用方误传的 Java toString()（如 [{"a":1}] 的 list.toString()），
            // 两者在 PG 侧都会被 jsonb 解析/拒绝：先做一次语法校验，给出可读错误而不是数据库层 BadSqlGrammar
            MAPPER.readTree(parameter);
        } catch (Exception e) {
            throw new SQLException("chain_data 不是合法 JSON，拒绝写入（避免把脏字符串塞进 jsonb）：" + parameter, e);
        }
        // Types.OTHER → 驱动按目标列（jsonb）绑定，避免 ps.setString() 的 varchar 类型不匹配
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }

    /** 供调用方把 Map/List 等对象直接转成可写入的 JSON 文本（保持与读回同一套规范化口径交给 DB） */
    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("对象转 JSON 失败", e);
        }
    }
}
