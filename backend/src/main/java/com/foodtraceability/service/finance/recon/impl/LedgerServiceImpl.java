package com.foodtraceability.service.finance.recon.impl;

import com.foodtraceability.dto.finance.recon.LedgerDailyVO;
import com.foodtraceability.dto.finance.recon.LedgerSummaryVO;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.service.finance.recon.LedgerService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 日记账与月结（S1/S2）。**纯只读**：不写任何表。
 *
 * <p>口径（design §4.1 / §4.2）：期初 = 该业务日之前最后一笔**非豁免** balance_after；
 * 期末 = 期初 + 当日净额，且必须等于当日最后一笔非豁免 balance_after（V1）。</p>
 *
 * <p><b>豁免口径（A7，唯一）</b>：`flow_no LIKE 'FFH%' OR created_by LIKE 'system/backfill-%'`。
 * 补账行 balance_after 按设计写 0，不参与比对，否则会把整条账面链判成断裂。</p>
 *
 * <p>查不到数据一律抛业务异常，**不返回 0 默认值**。</p>
 */
@Service
public class LedgerServiceImpl implements LedgerService {

    /** 非豁免条件（A7 口径，唯一来源） */
    private static final String NON_EXEMPT =
            " AND COALESCE(ff.flow_no, '') NOT LIKE 'FFH%' AND COALESCE(ff.created_by, '') NOT LIKE 'system/backfill-%' ";

    private final JdbcTemplate jdbc;

    public LedgerServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerDailyVO daily(Long accountId, LocalDate from, LocalDate to) {
        if (accountId == null) {
            throw err("accountId 必填");
        }
        if (from == null || to == null) {
            throw err("from / to 必填（yyyy-MM-dd）");
        }
        if (to.isBefore(from)) {
            throw err("to 不能早于 from");
        }
        requireAccount(accountId);

        LedgerDailyVO vo = new LedgerDailyVO();
        vo.accountId = accountId;
        vo.from = from;
        vo.to = to;
        vo.rows = new ArrayList<>();
        boolean allV1 = true;

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LedgerDailyVO.Row row = new LedgerDailyVO.Row();
            row.businessDate = d;
            row.openingBalance = openingBalance(accountId, d);
            if (row.openingBalance == null) {
                throw err("账户 " + accountId + " 没有任何非豁免流水，无法确定期初余额（不默认 0）");
            }
            Map<String, Object> agg = one(
                    "SELECT COALESCE(SUM(CASE WHEN ff.flow_direction = 1 THEN ff.amount ELSE 0 END), 0) AS income, "
                            + "       COALESCE(SUM(CASE WHEN ff.flow_direction = 2 THEN ff.amount ELSE 0 END), 0) AS expense, "
                            + "       COUNT(*) FILTER (WHERE ff.status = 2) AS cnt2, "
                            + "       COUNT(*) FILTER (WHERE ff.status = 1) AS cnt1, "
                            + "       COUNT(*) FILTER (WHERE ff.status = 3) AS cnt3 "
                            + "  FROM fund_flows ff "
                            + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.business_date = ? "
                            + NON_EXEMPT_NO_ALIAS_PLACEHOLDER,
                    accountId, d);
            row.incomeAmount = num(agg.get("income"));
            row.expenseAmount = num(agg.get("expense"));
            row.netAmount = row.incomeAmount - row.expenseAmount;
            row.closingBalance = row.openingBalance + row.netAmount;
            row.flowCount = num(agg.get("cnt2")).intValue();
            row.pendingCount = num(agg.get("cnt1")).intValue();
            row.cancelledCount = num(agg.get("cnt3")).intValue();

            Long last = lastBalanceOnOrBefore(accountId, d);
            row.lastBalanceAfter = last;
            row.v1Diff = last == null ? null : row.closingBalance - last;
            row.v1Matched = row.v1Diff != null && row.v1Diff == 0L;
            if (!Boolean.TRUE.equals(row.v1Matched)) {
                allV1 = false;
            }
            vo.rows.add(row);
        }
        vo.allV1Matched = allV1;
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerSummaryVO summary(Long accountId, String month) {
        if (accountId == null) {
            throw err("accountId 必填");
        }
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw err("month 格式必须是 yyyy-MM");
        }
        requireAccount(accountId);
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        LedgerSummaryVO vo = new LedgerSummaryVO();
        vo.accountId = accountId;
        vo.month = month;
        vo.monthStart = start;
        vo.monthEnd = end;
        vo.openingBalance = openingBalance(accountId, start);
        if (vo.openingBalance == null) {
            throw err("账户 " + accountId + " 没有任何非豁免流水，无法确定月初余额（不默认 0）");
        }
        Map<String, Object> agg = one(
                "SELECT COALESCE(SUM(CASE WHEN ff.flow_direction = 1 THEN ff.amount ELSE 0 END), 0) AS income, "
                        + "       COALESCE(SUM(CASE WHEN ff.flow_direction = 2 THEN ff.amount ELSE 0 END), 0) AS expense, "
                        + "       COUNT(*) FILTER (WHERE ff.status = 2) AS cnt2 "
                        + "  FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 "
                        + "   AND ff.business_date BETWEEN ? AND ? " + NON_EXEMPT_NO_ALIAS_PLACEHOLDER,
                accountId, start, end);
        vo.incomeAmount = num(agg.get("income"));
        vo.expenseAmount = num(agg.get("expense"));
        vo.netAmount = vo.incomeAmount - vo.expenseAmount;
        vo.flowCount = num(agg.get("cnt2")).intValue();

        vo.closingBalance = lastBalanceOnOrBefore(accountId, end);
        if (vo.closingBalance == null) {
            throw err("账户 " + accountId + " 在 " + end + " 及之前没有任何非豁免流水，无法确定月末余额（不默认 0）");
        }
        // V2：月末余额 == 次月 1 日期初
        LocalDate nextMonthFirst = end.plusDays(1);
        Long nextOpening = lastBalanceBefore(accountId, nextMonthFirst);
        vo.v2Matched = nextOpening != null && nextOpening.equals(vo.closingBalance);
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 兼容：NON_EXEMPT 常量里用别名 ff，占位在末尾拼接 */
    private static final String NON_EXEMPT_NO_ALIAS_PLACEHOLDER = NON_EXEMPT;

    /**
     * 业务异常助手：必须用 **common** 包的错误码构造器（code=400），
     * GlobalExceptionHandler 才会把 message 原样返回给前端；否则被当成 500「系统繁忙」把文案吞掉。
     */
    private static BusinessException err(String msg) {
        return new BusinessException(400, msg);
    }

    private void requireAccount(Long accountId) {
        Integer cnt = jdbc.queryForObject(
                "SELECT count(*) FROM bank_accounts WHERE account_id = ? AND deleted = 0", Integer.class, accountId);
        if (cnt == null || cnt == 0) {
            throw err("账户不存在或已删除: " + accountId);
        }
    }

    /**
     * 期初余额（分）：
     * ① 该业务日之前最后一笔非豁免 balance_after；
     * ② 若该账户在此日之前没有流水（账户首日），用「最早一笔非豁免流水的 balance_after 反推」：
     *    opening = 首笔.balance_after − (方向=收入 ? +amount : −amount)，避免默认 0 造成假账；
     * ③ 账户压根没有非豁免流水 → 返回 null（调用方抛业务异常）。
     */
    private Long openingBalance(Long accountId, LocalDate date) {
        Long before = lastBalanceBefore(accountId, date);
        if (before != null) {
            return before;
        }
        List<Map<String, Object>> first = jdbc.queryForList(
                "SELECT ff.balance_after, ff.amount, ff.flow_direction FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 "
                        + NON_EXEMPT
                        + " ORDER BY ff.business_date, ff.flow_id LIMIT 1",
                accountId);
        if (first.isEmpty()) {
            return null;
        }
        Map<String, Object> r = first.get(0);
        long bal = ((Number) r.get("balance_after")).longValue();
        long amt = ((Number) r.get("amount")).longValue();
        long dir = ((Number) r.get("flow_direction")).longValue();
        return dir == 1 ? bal - amt : bal + amt;
    }

    /** 该业务日之前最后一笔非豁免 balance_after（严格小于 date） */
    private Long lastBalanceBefore(Long accountId, LocalDate date) {
        List<Long> list = jdbc.queryForList(
                "SELECT ff.balance_after FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 AND ff.business_date < ? "
                        + NON_EXEMPT
                        + " ORDER BY ff.business_date DESC, ff.flow_id DESC LIMIT 1",
                Long.class, accountId, date);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 该业务日（含）之前最后一笔非豁免 balance_after */
    private Long lastBalanceOnOrBefore(Long accountId, LocalDate date) {
        List<Long> list = jdbc.queryForList(
                "SELECT ff.balance_after FROM fund_flows ff "
                        + " WHERE ff.account_id = ? AND ff.deleted = 0 AND ff.status = 2 AND ff.business_date <= ? "
                        + NON_EXEMPT
                        + " ORDER BY ff.business_date DESC, ff.flow_id DESC LIMIT 1",
                Long.class, accountId, date);
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) {
            throw err("查询无结果（不应发生）");
        }
        return rows.get(0);
    }

    private static Long num(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(o));
    }
}
