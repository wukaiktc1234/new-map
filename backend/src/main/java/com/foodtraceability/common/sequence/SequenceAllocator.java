package com.foodtraceability.common.sequence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 号段发号器 —— 全项目唯一的取号实现（P4-1 BL-2 收敛）。
 *
 * <p>语义（三处旧实现统一后，号段语义<strong>完全不变</strong>）：单条 SQL 完成
 * <b>floor 抬升 + 原子 +1 + RETURNING</b>：
 * <pre>
 * INSERT INTO order_sequence (sequence_key, sequence_value, create_time)
 * VALUES (?, (SELECT COALESCE(MAX(...), 0) FROM table WHERE ... ) + 1, NOW())
 * ON CONFLICT (sequence_key) DO UPDATE
 *   SET sequence_value = GREATEST(order_sequence.sequence_value, (SELECT ...)) + 1
 * RETURNING sequence_value
 * </pre>
 * 号源永远是号段表自身的行级锁自增（不是「查 MAX + 1」）；GREATEST 只做下限兜底，
 * 防「号段被调低 / 历史直写 SQL 越过号段」造成的落后。</p>
 *
 * <p><b>为什么用 JdbcTemplate 而不是 MyBatis @Select</b>：原实现靠
 * {@code @Options(flushCache = TRUE, useCache = false)} 绕开 MyBatis 一级缓存 —— 否则同一事务内
 * 第二次同参调用会被当缓存命中并返回同一个号（P2-A 缺陷 1：一次存行取两个流水号 → 撞唯一键）。
 * JdbcTemplate 每次都是真实 JDBC 执行，天然等价于 flushCache/useCache 的行为，且没有注解魔法。</p>
 *
 * <p><b>为什么必须 MANDATORY 事务</b>：号段行的 +1 与业务数据 insert 必须同生共死：
 * 调用方回滚时号段自增一起回滚，号可重用（不烧号）；只有「事务已提交但后续环节失败」才由烧号账补偿。
 * 若自开新事务，号段会先提交 → 号永久丢失。</p>
 *
 * <p><b>白名单</b>：表名/列名只允许 {@link SequenceTable} 枚举内的组合，禁止任意字符串拼接。</p>
 */
@Component
public class SequenceAllocator {

    /** 允许发号的表列白名单（新增号段必须在此登记）。 */
    public enum SequenceTable {
        /** 资金流水号：fund_flows.flow_no（FFyyyyMMdd + 4 位） */
        FUND_FLOW("fund_flows", "flow_no"),
        /** 缴款单号：deposit_slips.slip_no（DSyyyyMMdd + 4 位） */
        DEPOSIT_SLIP("deposit_slips", "slip_no");

        private final String table;
        private final String column;

        SequenceTable(String table, String column) {
            this.table = table;
            this.column = column;
        }

        public String getTable() {
            return table;
        }

        public String getColumn() {
            return column;
        }

        /** 白名单校验：表/列必须精确匹配某个枚举项，否则拒绝。 */
        public static SequenceTable of(String table, String column) {
            for (SequenceTable t : values()) {
                if (t.table.equals(table) && t.column.equals(column)) {
                    return t;
                }
            }
            throw new IllegalArgumentException(
                    "不在号段白名单内的表/列：" + table + "." + column
                            + "（允许：fund_flows.flow_no、deposit_slips.slip_no）");
        }
    }

    private final JdbcTemplate jdbc;

    public SequenceAllocator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 取下一个序号（floor + 原子 +1 + RETURNING，单条语句）。
     *
     * @param sequenceKey 号段键，如 FLOW20261025 / SLIP20261025
     * @param prefix      号码前缀，如 FF20261025；序号起始位置 = prefix.length() + 1（按 prefix 计算，不硬编码 11）
     * @param table       白名单表名（见 {@link SequenceTable}）
     * @param column      白名单列名
     * @return 分配到的序号（调用方自行 %04d 拼号）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer next(String sequenceKey, String prefix, String table, String column) {
        SequenceTable target = SequenceTable.of(table, column);
        if (sequenceKey == null || sequenceKey.isBlank()) {
            throw new IllegalArgumentException("sequenceKey 不能为空");
        }
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix 不能为空");
        }
        // 序号起始位置：prefix 长度 + 1（前缀长度变化也不会漂）
        int offset = prefix.length() + 1;
        String col = target.getColumn();
        String tbl = target.getTable();
        String sql = "INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES ("
                + "?, "
                + "(SELECT COALESCE(MAX(CAST(SUBSTRING(" + col + " FROM " + offset + ") AS INTEGER)), 0)"
                + "   FROM " + tbl
                + "  WHERE " + col + " LIKE CONCAT(?, '%')"
                + "    AND " + col + " ~ CONCAT('^', ?, '[0-9]+$')) + 1, "
                + "NOW()) "
                + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = GREATEST(order_sequence.sequence_value, "
                + "(SELECT COALESCE(MAX(CAST(SUBSTRING(" + col + " FROM " + offset + ") AS INTEGER)), 0)"
                + "   FROM " + tbl
                + "  WHERE " + col + " LIKE CONCAT(?, '%')"
                + "    AND " + col + " ~ CONCAT('^', ?, '[0-9]+$'))) + 1 "
                + "RETURNING sequence_value";
        return jdbc.queryForObject(sql, Integer.class, sequenceKey, prefix, prefix, prefix, prefix);
    }
}
