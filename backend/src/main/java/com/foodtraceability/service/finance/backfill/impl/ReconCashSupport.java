package com.foodtraceability.service.finance.backfill.impl;

import com.foodtraceability.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * P2 记账公共件：**账户余额乐观锁** + **流水号生成**（全项目唯一实现，禁止第二处手写）。
 *
 * <p>红线：余额只能走 {@code UPDATE bank_accounts SET balance = balance + delta, version = version + 1
 * WHERE account_id = ? AND version = ? AND deleted = 0}，影响行数必须 = 1；
 * <b>禁止</b>调用 BankAccountServiceImpl.updateBalance（裸 SET，会丢更新）。</p>
 */
@Component
public class ReconCashSupport {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbc;

    public ReconCashSupport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 余额原子调整结果 */
    public record BalanceChange(Long accountId, Long balanceBefore, Long balanceAfter, Integer appliedVersion) {
    }

    /**
     * 原子调整余额（带 version 前置条件，冲突即重试；3 次仍失败抛业务异常由事务回滚）。
     *
     * @param delta 分，正=入账 负=出账
     */
    public BalanceChange applyDelta(Long accountId, long delta) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT COALESCE(balance, 0) AS balance, COALESCE(version, 0) AS version "
                            + "  FROM bank_accounts WHERE account_id = ? AND deleted = 0", accountId);
            if (rows.isEmpty()) {
                throw err("账户不存在或已删除: " + accountId);
            }
            long before = ((Number) rows.get(0).get("balance")).longValue();
            long version = ((Number) rows.get(0).get("version")).longValue();
            int updated = jdbc.update(
                    "UPDATE bank_accounts SET balance = COALESCE(balance, 0) + ?, "
                            + "       version = COALESCE(version, 0) + 1, update_time = NOW() "
                            + " WHERE account_id = ? AND version = ? AND deleted = 0",
                    delta, accountId, version);
            if (updated == 1) {
                Long after = jdbc.queryForObject(
                        "SELECT balance FROM bank_accounts WHERE account_id = ?", Long.class, accountId);
                return new BalanceChange(accountId, before, after, (int) version + 1);
            }
            // 影响行数 ≠ 1：被别人抢先改了版本 → 重读重试
        }
        throw err("账户 " + accountId + " 余额乐观锁更新失败（连续 3 次版本冲突），已回滚，请重试");
    }

    /**
     * 生成资金流水号：FF + yyyyMMdd + 4 位当日序号。
     *
     * <p><b>必须与 FundFlowServiceImpl#generateFlowNo 共用同一个号段表</b>（order_sequence，键 FLOWyyyyMMdd）：
     * 曾经这里用「查当天 max + 1」，而 POS 入账走号段表 → 两边各发各的号，P2 冲正发出的
     * FF202610250008 与 POS 号段发出的 008 撞唯一键 fund_flows_flow_no_key，
     * 导致 POS 支付成功却没写进 SALE 流水（A1 报 4 条缺账）。现改为：
     * ① 先把号段抬到不低于当天已有最大序号（修复历史直写 SQL 造成的空档）；
     * ② 再从号段原子取下一个号（ON CONFLICT DO UPDATE ... RETURNING，并发安全）。</p>
     */
    public String nextFlowNo(LocalDate businessDate) {
        String day = businessDate.format(DAY);
        String prefix = "FF" + day;
        String sequenceKey = "FLOW" + day;
        Integer maxExisting = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS INTEGER)), 0) "
                        + "  FROM fund_flows WHERE flow_no LIKE ?", Integer.class, prefix + "%");
        int floor = maxExisting == null ? 0 : maxExisting;
        // ① 号段对齐到当天已有最大值（GREATEST 保证只升不降，不会把并发方的号段压低）
        jdbc.update("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (?, ?, NOW()) "
                        + "ON CONFLICT (sequence_key) DO UPDATE "
                        + "SET sequence_value = GREATEST(order_sequence.sequence_value, EXCLUDED.sequence_value)",
                sequenceKey, floor);
        // ② 原子取下一个号
        Integer seq = jdbc.queryForObject(
                "INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (?, 1, NOW()) "
                        + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = order_sequence.sequence_value + 1 "
                        + "RETURNING sequence_value", Integer.class, sequenceKey);
        if (seq == null || seq <= 0) {
            throw err("资金流水号段分配失败（order_sequence 返回空），已回滚");
        }
        return prefix + String.format("%04d", seq);
    }

    public static BusinessException err(String msg) {
        return new BusinessException(400, msg);
    }
}
