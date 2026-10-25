package com.foodtraceability.service.finance.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ConflictException;
import com.foodtraceability.dto.finance.period.FinancePeriodVO;
import com.foodtraceability.service.finance.FinancePeriodService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 期末结账 / 反结账实现（P4-4）。
 *
 * <p><b>结账语义</b>：期间封账后只读；补录必须先反结账（设计 §4 / TD-39）。
 * RK-8：结账前必须没有未处置差异（recon_differences.status='OPEN'）与未提交调节表（DRAFT）。</p>
 *
 * <p><b>V1 校验（TD-39）</b>：逐账户 {@code closing_fen = opening + income - expense} 必须等于
 * 「期间内最后一笔 fund_flows.balance_after」（按 business_date, flow_id 倒序）。
 * 不等说明期间内存在直写 SQL / 漏记流水，**必须硬拦**（400，列出 account_id/计算值/快照值/差额）。</p>
 */
@Service
public class FinancePeriodServiceImpl implements FinancePeriodService {

    private static final Logger log = LoggerFactory.getLogger(FinancePeriodServiceImpl.class);

    private final JdbcTemplate jdbc;

    public FinancePeriodServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================================================================
    // list / summary
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public List<FinancePeriodVO> list() {
        List<FinancePeriodVO> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(BASE_SELECT + " ORDER BY start_date DESC")) {
            FinancePeriodVO vo = toVO(r);
            fillAccounts(vo, r);
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancePeriodVO summary(String periodCode) {
        Map<String, Object> p = requirePeriod(periodCode);
        FinancePeriodVO vo = toVO(p);
        fillAccounts(vo, p);
        return vo;
    }

    // ==================================================================
    // close
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancePeriodVO close(String periodCode, String remark) {
        Map<String, Object> p = requirePeriod(periodCode);
        if (Boolean.TRUE.equals(p.get("is_closed"))) {
            throw new ConflictException("期间 " + periodCode + " 已结账，不能重复结账");
        }
        LocalDate start = toDate(p.get("start_date"));
        LocalDate end = toDate(p.get("end_date"));

        // ---- RK-8 门禁 ①：期间内有 OPEN 差异 ----
        Integer openDiffs = jdbc.queryForObject(
                "SELECT count(*) FROM recon_differences d "
                        + "  JOIN recon_runs r ON r.run_id = d.run_id "
                        + " WHERE d.status = 'OPEN' AND r.business_date BETWEEN ? AND ?",
                Integer.class, start, end);
        if (openDiffs != null && openDiffs > 0) {
            throw new ConflictException("RK-8 门禁失败：期间 " + periodCode + " 内仍有 " + openDiffs
                    + " 条未处置的对账差异（status=OPEN），请先全部关闭后再结账");
        }
        // ---- RK-8 门禁 ②：期间内有 DRAFT 调节表 ----
        Integer drafts = jdbc.queryForObject(
                "SELECT count(*) FROM reconciliation_statements "
                        + " WHERE status = 'DRAFT' AND deleted = 0 AND period_start >= ? AND period_end <= ?",
                Integer.class, start, end);
        if (drafts != null && drafts > 0) {
            throw new ConflictException("RK-8 门禁失败：期间 " + periodCode + " 内仍有 " + drafts
                    + " 张未提交的调节表（status=DRAFT），请先提交或作废后再结账");
        }

        // ---- 逐账户快照 + V1 校验 ----
        Set<Long> accountIds = new LinkedHashSet<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT DISTINCT account_id FROM fund_flows "
                        + " WHERE deleted = 0 AND status = 2 AND business_date BETWEEN ? AND ?", start, end)) {
            accountIds.add(((Number) r.get("account_id")).longValue());
        }
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT account_id FROM bank_accounts WHERE deleted = 0 AND account_type = 2")) {
            accountIds.add(((Number) r.get("account_id")).longValue());
        }

        List<FinancePeriodVO.Row> rows = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        long openingTotal = 0, incomeTotal = 0, expenseTotal = 0, closingTotal = 0;
        for (Long accId : accountIds) {
            Map<String, Object> agg = jdbc.queryForMap(
                    "SELECT COALESCE(SUM(amount) FILTER (WHERE flow_direction = 1), 0) AS income_fen,"
                            + "       COALESCE(SUM(amount) FILTER (WHERE flow_direction = 2), 0) AS expense_fen,"
                            + "       count(*) AS txn_count "
                            + "  FROM fund_flows WHERE deleted = 0 AND status = 2 AND account_id = ?"
                            + "   AND business_date BETWEEN ? AND ?", accId, start, end);
            long income = ((Number) agg.get("income_fen")).longValue();
            long expense = ((Number) agg.get("expense_fen")).longValue();
            long txn = ((Number) agg.get("txn_count")).longValue();

            long opening = nz(lastBalanceBefore(accId, start));
            Long lastInPeriod = lastBalanceInPeriod(accId, start, end);
            long computedClosing = opening + income - expense;

            FinancePeriodVO.Row row = new FinancePeriodVO.Row();
            row.accountId = accId;
            row.openingFen = opening;
            row.incomeFen = income;
            row.expenseFen = expense;
            row.closingFen = computedClosing;
            row.txnCount = txn;
            row.lastBalanceAfter = lastInPeriod;
            rows.add(row);

            // V1：期间内无流水时，用「期初」代表期末（不能拿 null 触发误报）
            long snapshotClosing = lastInPeriod == null ? opening : lastInPeriod;
            if (snapshotClosing != computedClosing) {
                failures.add("account_id=" + accId + " 计算值=" + computedClosing + " 分，快照值="
                        + snapshotClosing + " 分，差额=" + (computedClosing - snapshotClosing) + " 分");
            }
            openingTotal += opening;
            incomeTotal += income;
            expenseTotal += expense;
            closingTotal += computedClosing;
        }
        if (!failures.isEmpty()) {
            throw new BusinessException(400, "V1 试算校验失败（期间 " + periodCode + "，共 " + failures.size()
                    + " 个账户）：" + String.join("；", failures)
                    + "。请先补记/修正流水或直写 SQL 造成的差额，再重新结账");
        }

        // ---- 写快照（幂等：ON CONFLICT DO UPDATE）----
        Long userId = currentUserId();
        for (FinancePeriodVO.Row row : rows) {
            jdbc.update("INSERT INTO finance_period_account_balance "
                            + "(period_code, account_id, opening_fen, income_fen, expense_fen, closing_fen,"
                            + " txn_count, last_balance_after, create_time, create_user_id) "
                            + "VALUES (?,?,?,?,?,?,?,?,NOW(),?) "
                            + "ON CONFLICT (period_code, account_id) DO UPDATE SET "
                            + "  opening_fen = EXCLUDED.opening_fen, income_fen = EXCLUDED.income_fen,"
                            + "  expense_fen = EXCLUDED.expense_fen, closing_fen = EXCLUDED.closing_fen,"
                            + "  txn_count = EXCLUDED.txn_count, last_balance_after = EXCLUDED.last_balance_after,"
                            + "  create_time = NOW(), create_user_id = EXCLUDED.create_user_id",
                    periodCode, row.accountId, row.openingFen, row.incomeFen, row.expenseFen, row.closingFen,
                    row.txnCount, row.lastBalanceAfter, userId);
        }

        // ---- 期间置已结账 ----
        jdbc.update("UPDATE accounting_periods SET is_closed = true, status = 1, close_time = NOW(),"
                        + " close_user_id = ?, trial_balance_passed = true, remark = ?, update_time = NOW(),"
                        + " version = version + 1 WHERE period_code = ? AND deleted = 0",
                userId, remark, periodCode);

        // ---- 审计 ----
        Map<String, Object> after = requirePeriod(periodCode);
        long periodId = ((Number) after.get("period_id")).longValue();
        String userName = currentUserName();
        jdbc.update("INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module,"
                        + " target_id, target_type, operation_desc, operation_data, create_time, created_by, version) "
                        + "VALUES (?,?,'FINANCE_PERIOD_CLOSED','finance',?,'ACCOUNTING_PERIOD',?,?,NOW(),?,0)",
                userId, userName, periodId,
                "期间结账：" + periodCode + " 账户数=" + rows.size()
                        + " 总期初=" + openingTotal + " 总收入=" + incomeTotal
                        + " 总支出=" + expenseTotal + " 总期末=" + closingTotal + " 操作人=" + userName,
                "{\"periodCode\":\"" + periodCode + "\",\"accountCount\":" + rows.size()
                        + ",\"openingTotalFen\":" + openingTotal + ",\"incomeTotalFen\":" + incomeTotal
                        + ",\"expenseTotalFen\":" + expenseTotal + ",\"closingTotalFen\":" + closingTotal + "}",
                userName);
        log.info("[PERIOD_CLOSE] period={} accounts={} opening={} income={} expense={} closing={}",
                periodCode, rows.size(), openingTotal, incomeTotal, expenseTotal, closingTotal);

        FinancePeriodVO vo = toVO(requirePeriod(periodCode));
        fillAccounts(vo, requirePeriod(periodCode));
        return vo;
    }

    // ==================================================================
    // reopen
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinancePeriodVO reopen(String periodCode, String reason) {
        if (reason == null || reason.trim().length() < 10) {
            throw new BusinessException(400, "反结账原因至少 10 个字");
        }
        Map<String, Object> p = requirePeriod(periodCode);
        if (!Boolean.TRUE.equals(p.get("is_closed"))) {
            throw new ConflictException("期间 " + periodCode + " 未结账，无需反结账");
        }
        // 只允许「当前最后一个已结账期间」
        List<Map<String, Object>> later = jdbc.queryForList(
                "SELECT period_code FROM accounting_periods WHERE deleted = 0 AND is_closed = true"
                        + " AND start_date > ? ORDER BY start_date DESC LIMIT 1", toDate(p.get("start_date")));
        if (!later.isEmpty()) {
            throw new ConflictException("只允许对最后一个已结账期间反结账：期间 " + periodCode
                    + " 之后还有已结账期间 " + later.get(0).get("period_code") + "，请先反结账更晚的期间");
        }

        Long userId = currentUserId();
        String userName = currentUserName();
        jdbc.update("UPDATE accounting_periods SET is_closed = false, status = 0, reopen_time = NOW(),"
                        + " reopen_user_id = ?, reopen_reason = ?, update_time = NOW(), version = version + 1 "
                        + " WHERE period_code = ? AND deleted = 0", userId, reason, periodCode);

        long periodId = ((Number) p.get("period_id")).longValue();
        jdbc.update("INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module,"
                        + " target_id, target_type, operation_desc, operation_data, create_time, created_by, version) "
                        + "VALUES (?,?,'FINANCE_PERIOD_REOPENED','finance',?,'ACCOUNTING_PERIOD',?,?,NOW(),?,0)",
                userId, userName, periodId,
                "期间反结账：" + periodCode + " 原因=" + reason.trim() + " 操作人=" + userName,
                "{\"periodCode\":\"" + periodCode + "\",\"reason\":\""
                        + reason.trim().replace("\"", "'") + "\"}",
                userName);
        log.info("[PERIOD_REOPEN] period={} reason={} operator={}", periodCode, reason.trim(), userName);
        // 快照行保留（历史留档）：不删 finance_period_account_balance
        Map<String, Object> after2 = requirePeriod(periodCode);
        FinancePeriodVO vo = toVO(after2);
        fillAccounts(vo, after2);
        return vo;
    }

    // ==================================================================
    // helpers
    // ==================================================================
    /**
     * 填充 accounts[] 与合计（P4-4 收口，问题 2 修复）。
     *
     * <p>数据源二选一：</p>
     * <ul>
     *   <li><b>已结账期间</b> → 读 {@code finance_period_account_balance} 快照（历史留档，不因后续改动漂移），
     *       {@code dirty} = {@code close_time} 之后仍有该期间 business_date 的活流水（与 A28 同判据）；</li>
     *   <li><b>未结账期间</b> → 按当前 {@code fund_flows} **现算**（与 close() 的 V1 同一套口径：
     *       opening = 期间首日前最后一笔 balance_after，closing = opening + income − expense），
     *       逐账户 {@code trialBalancePassed = (closing == 期间内末笔 balance_after)}，{@code dirty=false}。</li>
     * </ul>
     * <p>{@code openingTotalFen / closingTotalFen} 一律由 accounts[] 求和得出，保证「合计 = 明细之和」。</p>
     */
    private void fillAccounts(FinancePeriodVO vo, Map<String, Object> periodRow) {
        LocalDate start = toDate(periodRow.get("start_date"));
        LocalDate end = toDate(periodRow.get("end_date"));
        boolean closed = Boolean.TRUE.equals(periodRow.get("is_closed"));
        List<FinancePeriodVO.Row> rows;
        boolean dirty = false;
        if (closed) {
            rows = loadSnapshotRows(vo.periodCode);
            LocalDateTime closeTime = toDateTime(periodRow.get("close_time"));
            // 与 A28 同判据：结账之后又有该期间的活流水 → dirty
            Integer late = jdbc.queryForObject(
                    "SELECT count(*) FROM fund_flows WHERE deleted = 0 AND status = 2"
                            + " AND business_date BETWEEN ? AND ? AND create_time > ?",
                    Integer.class, start, end, closeTime == null ? LocalDateTime.now().plusYears(100) : closeTime);
            dirty = late != null && late > 0;
            for (FinancePeriodVO.Row row : rows) {
                row.accountName = accountName(row.accountId);
                row.matched = !dirty;
                row.trialBalancePassed = Boolean.TRUE.equals(periodRow.get("trial_balance_passed"));
                row.dirty = dirty;
            }
        } else {
            rows = computeAccountRows(start, end);
            for (FinancePeriodVO.Row row : rows) {
                row.accountName = accountName(row.accountId);
                long snapshotClosing = row.lastBalanceAfter == null ? row.openingFen : row.lastBalanceAfter;
                boolean ok = snapshotClosing == row.closingFen;
                row.trialBalancePassed = ok;
                row.matched = ok;
                row.dirty = false;
            }
        }
        vo.accounts = rows;
        vo.accountCount = (long) rows.size();
        long openingTotal = 0L, incomeTotal = 0L, expenseTotal = 0L, closingTotal = 0L;
        for (FinancePeriodVO.Row row : rows) {
            openingTotal += nz(row.openingFen);
            incomeTotal += nz(row.incomeFen);
            expenseTotal += nz(row.expenseFen);
            closingTotal += nz(row.closingFen);
        }
        vo.openingTotalFen = openingTotal;
        vo.incomeTotalFen = incomeTotal;
        vo.expenseTotalFen = expenseTotal;
        vo.closingTotalFen = closingTotal;
        vo.dirty = dirty;
    }

    /** 按当前流水现算逐账户行（与 close() 的 V1 口径完全一致） */
    private List<FinancePeriodVO.Row> computeAccountRows(LocalDate start, LocalDate end) {
        Set<Long> accountIds = new LinkedHashSet<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT DISTINCT account_id FROM fund_flows "
                        + " WHERE deleted = 0 AND status = 2 AND business_date BETWEEN ? AND ?", start, end)) {
            accountIds.add(((Number) r.get("account_id")).longValue());
        }
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT account_id FROM bank_accounts WHERE deleted = 0 AND account_type = 2")) {
            accountIds.add(((Number) r.get("account_id")).longValue());
        }
        List<FinancePeriodVO.Row> rows = new ArrayList<>();
        for (Long accId : accountIds) {
            Map<String, Object> agg = jdbc.queryForMap(
                    "SELECT COALESCE(SUM(amount) FILTER (WHERE flow_direction = 1), 0) AS income_fen,"
                            + "       COALESCE(SUM(amount) FILTER (WHERE flow_direction = 2), 0) AS expense_fen,"
                            + "       count(*) AS txn_count "
                            + "  FROM fund_flows WHERE deleted = 0 AND status = 2 AND account_id = ?"
                            + "   AND business_date BETWEEN ? AND ?", accId, start, end);
            long income = ((Number) agg.get("income_fen")).longValue();
            long expense = ((Number) agg.get("expense_fen")).longValue();
            long txn = ((Number) agg.get("txn_count")).longValue();
            long opening = nz(lastBalanceBefore(accId, start));
            FinancePeriodVO.Row row = new FinancePeriodVO.Row();
            row.accountId = accId;
            row.openingFen = opening;
            row.incomeFen = income;
            row.expenseFen = expense;
            row.closingFen = opening + income - expense;
            row.txnCount = txn;
            row.lastBalanceAfter = lastBalanceInPeriod(accId, start, end);
            rows.add(row);
        }
        return rows;
    }

    /** 账户名（用于 accounts[].accountName） */
    private String accountName(Long accountId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT account_name FROM bank_accounts WHERE account_id = ?", accountId);
        return rows.isEmpty() ? null : (String) rows.get(0).get("account_name");
    }

    private static final String BASE_SELECT =
            "SELECT p.*, (SELECT count(*) FROM finance_period_account_balance b WHERE b.period_code = p.period_code) AS account_count,"
                    + " (SELECT COALESCE(SUM(b.opening_fen),0) FROM finance_period_account_balance b WHERE b.period_code = p.period_code) AS opening_total,"
                    + " (SELECT COALESCE(SUM(b.income_fen),0) FROM finance_period_account_balance b WHERE b.period_code = p.period_code) AS income_total,"
                    + " (SELECT COALESCE(SUM(b.expense_fen),0) FROM finance_period_account_balance b WHERE b.period_code = p.period_code) AS expense_total,"
                    + " (SELECT COALESCE(SUM(b.closing_fen),0) FROM finance_period_account_balance b WHERE b.period_code = p.period_code) AS closing_total,"
                    + " (SELECT string_agg(u.username, ',') FROM users u WHERE u.user_id::text = p.close_user_id::text) AS close_user_name"
                    + " FROM accounting_periods p WHERE p.deleted = 0";

    private Map<String, Object> requirePeriod(String periodCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(BASE_SELECT + " AND p.period_code = ?", periodCode);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "会计期间不存在：" + periodCode);
        }
        return rows.get(0);
    }

    private FinancePeriodVO toVO(Map<String, Object> r) {
        FinancePeriodVO vo = new FinancePeriodVO();
        vo.periodCode = (String) r.get("period_code");
        vo.periodName = (String) r.get("period_name");
        vo.startDate = toDate(r.get("start_date"));
        vo.endDate = toDate(r.get("end_date"));
        vo.isClosed = r.get("is_closed") != null && Boolean.TRUE.equals(r.get("is_closed"));
        vo.statusCode = r.get("status") == null ? null : ((Number) r.get("status")).intValue();
        vo.closeTime = toDateTime(r.get("close_time"));
        // 语义化状态：CLOSED / REOPENED（反结账过）/ OPEN
        boolean reopened = r.get("reopen_time") != null;
        vo.reopened = reopened;
        vo.status = Boolean.TRUE.equals(vo.isClosed) ? "CLOSED" : (reopened ? "REOPENED" : "OPEN");
        vo.closeUserId = r.get("close_user_id") == null ? null : ((Number) r.get("close_user_id")).longValue();
        vo.closeUser = (String) r.get("close_user_name");
        vo.trialBalancePassed = r.get("trial_balance_passed") != null && Boolean.TRUE.equals(r.get("trial_balance_passed"));
        vo.reopenTime = toDateTime(r.get("reopen_time"));
        vo.reopenUserId = r.get("reopen_user_id") == null ? null : ((Number) r.get("reopen_user_id")).longValue();
        vo.reopenReason = (String) r.get("reopen_reason");
        vo.remark = (String) r.get("remark");
        vo.accountCount = r.get("account_count") == null ? 0L : ((Number) r.get("account_count")).longValue();
        vo.openingTotalFen = r.get("opening_total") == null ? 0L : ((Number) r.get("opening_total")).longValue();
        vo.incomeTotalFen = r.get("income_total") == null ? 0L : ((Number) r.get("income_total")).longValue();
        vo.expenseTotalFen = r.get("expense_total") == null ? 0L : ((Number) r.get("expense_total")).longValue();
        vo.closingTotalFen = r.get("closing_total") == null ? 0L : ((Number) r.get("closing_total")).longValue();
        return vo;
    }

    private List<FinancePeriodVO.Row> loadSnapshotRows(String periodCode) {
        List<FinancePeriodVO.Row> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT account_id, opening_fen, income_fen, expense_fen, closing_fen, txn_count, last_balance_after "
                        + "  FROM finance_period_account_balance WHERE period_code = ? ORDER BY account_id", periodCode)) {
            FinancePeriodVO.Row row = new FinancePeriodVO.Row();
            row.accountId = ((Number) r.get("account_id")).longValue();
            row.openingFen = ((Number) r.get("opening_fen")).longValue();
            row.incomeFen = ((Number) r.get("income_fen")).longValue();
            row.expenseFen = ((Number) r.get("expense_fen")).longValue();
            row.closingFen = ((Number) r.get("closing_fen")).longValue();
            row.txnCount = ((Number) r.get("txn_count")).longValue();
            row.lastBalanceAfter = r.get("last_balance_after") == null ? null : ((Number) r.get("last_balance_after")).longValue();
            out.add(row);
        }
        return out;
    }

    /** 期间首日前最后一笔 balance_after（无则 null） */
    private Long lastBalanceBefore(Long accountId, LocalDate start) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT balance_after FROM fund_flows WHERE deleted = 0 AND status = 2 AND account_id = ?"
                        + "  AND business_date < ? ORDER BY business_date DESC, flow_id DESC LIMIT 1", accountId, start);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("balance_after")).longValue();
    }

    /** 期间内最后一笔 balance_after（无则 null） */
    private Long lastBalanceInPeriod(Long accountId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT balance_after FROM fund_flows WHERE deleted = 0 AND status = 2 AND account_id = ?"
                        + "  AND business_date BETWEEN ? AND ? ORDER BY business_date DESC, flow_id DESC LIMIT 1",
                accountId, start, end);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("balance_after")).longValue();
    }

    private Map<String, Object> currentTotals(Long accountId, LocalDate start, LocalDate end) {
        Map<String, Object> r = jdbc.queryForMap(
                "SELECT COALESCE(SUM(amount) FILTER (WHERE flow_direction = 1), 0) AS income,"
                        + "       COALESCE(SUM(amount) FILTER (WHERE flow_direction = 2), 0) AS expense "
                        + "  FROM fund_flows WHERE deleted = 0 AND status = 2 AND account_id = ?"
                        + "   AND business_date BETWEEN ? AND ?", accountId, start, end);
        long income = ((Number) r.get("income")).longValue();
        long expense = ((Number) r.get("expense")).longValue();
        long opening = nz(lastBalanceBefore(accountId, start));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("income", income);
        out.put("expense", expense);
        out.put("closing", opening + income - expense);
        return out;
    }

    private static boolean eq(Long a, Long b) {
        return a != null && a.equals(b);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static LocalDate toDate(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }
        return LocalDate.parse(String.valueOf(v));
    }

    private static LocalDateTime toDateTime(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime) {
            return (LocalDateTime) v;
        }
        if (v instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) v).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(v).replace(' ', 'T'));
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String currentUserName() {
        try {
            String n = SecurityUtils.getCurrentUsername();
            return n == null || n.isBlank() ? "system" : n;
        } catch (Exception e) {
            return "system";
        }
    }
}
