package com.foodtraceability.common.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL {@code jsonb} ↔ {@code String} 映射。
 *
 * <p>直接以 {@code String} 绑定时 PG 会报「column is of type jsonb but expression is of type
 * character varying」；本 handler 用 {@link Types#OTHER} 让驱动按未指定类型发送，由 PG 隐式转 jsonb；
 * 读取侧用 {@code getString} 直接拿 JSON 文本。</p>
 */
@MappedTypes(String.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
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
}
