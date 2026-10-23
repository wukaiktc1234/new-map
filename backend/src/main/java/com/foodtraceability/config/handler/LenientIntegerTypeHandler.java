package com.foodtraceability.config.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 宽容型 Integer 类型处理器（用于 bank_accounts.account_type 等历史脏数据列）
 * <p>
 * 背景：bank_accounts.account_type 历史上是 character varying，混杂 'bank' / 'cash' / '1' 等值，
 * 而实体字段是 Integer。PostgreSQL JDBC 直接 getInt() 会抛
 * "不良的类型值 int : bank"，异常向上冒泡成
 * DataIntegrityViolationException，导致整个列表接口 500（traceId=680bd8650713）。
 * </p>
 * <p>
 * 该处理器在读取侧做兜底：数字原样解析，常见历史字符串按统一口径映射，
 * 任何无法识别的值降级为 5（其他）并打 WARN，不再让整页查询失败。
 * 列类型已于 2026-10-23 归一为 integer（见 scripts/db-fix/2026-10-23-bank-account-type-normalize.sql），
 * 本处理器只作为残留下游/回滚场景的防御层，不改变正常路径行为。
 * </p>
 */
@MappedTypes(Integer.class)
@MappedJdbcTypes({JdbcType.INTEGER, JdbcType.VARCHAR, JdbcType.CHAR, JdbcType.NUMERIC, JdbcType.OTHER})
public class LenientIntegerTypeHandler extends BaseTypeHandler<Integer> {

    private static final Logger log = LoggerFactory.getLogger(LenientIntegerTypeHandler.class);

    /** 无法识别时的兜底值：5-其他 */
    private static final int FALLBACK = 5;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter);
    }

    @Override
    public Integer getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName), columnName);
    }

    @Override
    public Integer getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex), "column#" + columnIndex);
    }

    @Override
    public Integer getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex), "column#" + columnIndex);
    }

    /** 读取侧宽容解析 */
    private Integer parse(String raw, String locate) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            // 落到历史字符串口径映射
        }
        Integer mapped = mapLegacy(value);
        if (mapped != null) {
            log.warn("account_type 读取到历史非数字值 '{}'（{}），已按口径映射为 {}", value, locate, mapped);
            return mapped;
        }
        log.warn("account_type 读取到无法识别的值 '{}'（{}），降级为 {}（其他），不再中断查询", value, locate, FALLBACK);
        return FALLBACK;
    }

    /** 历史字符串 → 数字口径（与前端 BankAccountTypeMap / 实体注释一致） */
    private Integer mapLegacy(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "bank":
            case "basic":
            case "基本户":
                return 1;
            case "cash":
            case "general":
            case "一般户":
                return 2;
            case "payroll":
            case "工资户":
                return 3;
            case "reserve":
            case "备用金":
                return 4;
            case "other":
            case "其他":
                return 5;
            default:
                return null;
        }
    }
}
