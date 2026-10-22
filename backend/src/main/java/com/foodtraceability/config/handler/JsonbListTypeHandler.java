package com.foodtraceability.config.handler;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL JSONB 类型处理器（用于 List<String> 字段）
 * <p>
 * 与 JacksonTypeHandler 不同，本处理器使用 ps.setObject() + Types.OTHER
 * 正确设置 JSONB 类型，避免 JacksonTypeHandler 底层调用 ps.setString()
 * 导致的类型不匹配错误。
 * </p>
 */
public class JsonbListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final Logger log = LoggerFactory.getLogger(JsonbListTypeHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = MAPPER.writeValueAsString(parameter);
            // 使用 Types.OTHER 告知驱动使用目标列类型（jsonb），避免 ps.setString() 的 varchar 绑定
            ps.setObject(i, json, Types.OTHER);
        } catch (Exception e) {
            throw new SQLException("序列化 List<String> 为 JSONB 失败", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 假成功修复：原来静默返回空列表，DB 中损坏的 JSONB 会被下游当成「本来就没有数据」
            // （如追溯码列表凭空清空），且无任何痕迹可查。
            log.error("JSONB 反序列化失败，数据可能损坏，已降级为空列表: json={}, error={}",
                    json.length() > 200 ? json.substring(0, 200) + "...(truncated)" : json,
                    e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}