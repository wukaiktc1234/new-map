package com.foodtraceability.service.finance.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.dto.finance.recon.FinanceOverviewDailyVO;
import com.foodtraceability.security.support.GlobalDataScope;
import com.foodtraceability.service.PermissionVerifyService;
import com.foodtraceability.service.finance.FinanceOverviewService;
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
import java.util.List;
import java.util.Map;

/**
 * 资金总览（日）只读聚合实现（P3-4）。
 *
 * <p><b>范围判定</b>：统一走 {@link GlobalDataScope}（all|company = 全公司视角），
 * 不认角色白名单、不认开关——{@code data_scope} 是唯一依据。
 * store/department（及 null/未知）→ 只能看本人 {@code location_id} 经
 * {@link LocationIdBridge#storeIdOf} 换算出的门店；换算不到 → 403（不返回空集）。</p>
 *
 * <p><b>abnormal 定义（javadoc 明示）</b>：当日 |净变动| &gt; 该店近 7 日 |净变动| 日均值 × 3。
 * 近 7 日窗口 = [date-7, date-1]（不含当日，避免自己拉高自己的基线）。
 * 无历史（均值 = 0）时退化为「当日 |净变动| ≥ 10000 分（100 元）」才判异常 —— 否则任何首日流水都会红。</p>
 *
 * <p><b>N+1 红线</b>：段1 一次查出「门店 × 现金户 × 当日净变动 × 近 7 日日均」；
 * 段2 一次查出当日批 + 一次查出未解释条数；段3 两次（状态分布 + 最老一条）。全部集合查询，无循环查库。</p>
 */
@Service
public class FinanceOverviewServiceImpl implements FinanceOverviewService {

    private static final Logger log = LoggerFactory.getLogger(FinanceOverviewServiceImpl.class);

    /** 无历史基线时的异常阈值：100 元（分） */
    private static final long ABNORMAL_FLOOR_FEN = 10_000L;

    private final JdbcTemplate jdbc;
    private final PermissionVerifyService permissionVerifyService;

    public FinanceOverviewServiceImpl(JdbcTemplate jdbc, PermissionVerifyService permissionVerifyService) {
        this.jdbc = jdbc;
        this.permissionVerifyService = permissionVerifyService;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceOverviewDailyVO daily(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw forbidden("未登录或身份上下文缺失，无权访问资金总览");
        }
        String dataScope = permissionVerifyService.getUserDataScope(String.valueOf(userId));
        boolean global = GlobalDataScope.isGlobal(dataScope);

        Long scopedStoreId = null;
        if (!global) {
            Long locationId = SecurityUtils.getCurrentUserLocationId();
            scopedStoreId = locationId == null ? null : LocationIdBridge.storeIdOf(locationId);
            if (scopedStoreId == null) {
                throw forbidden("当前用户未分配归属门店，无权访问资金总览（data_scope=" + dataScope + "）");
            }
        }

        FinanceOverviewDailyVO vo = new FinanceOverviewDailyVO();
        vo.date = day;
        vo.dataScope = dataScope;
        vo.storeId = scopedStoreId;
        vo.stores = loadStores(day, scopedStoreId);
        vo.channels = loadChannels(day, scopedStoreId);
        vo.statements = loadStatements(scopedStoreId);

        log.info("[FIN_OVERVIEW] date={} scope={} global={} storeId={} stores={} runs={} openStatements={}",
                day, dataScope, global, scopedStoreId, vo.stores.size(), vo.channels.runs.size(),
                vo.statements.openCount);
        return vo;
    }

    /**
     * 段1：门店 × 现金户 × 当日净变动 × 近 7 日日均（**一条 SQL，无 N+1**）。
     *
     * <p>现金户口径：{@code bank_accounts.account_type = 2 AND deleted = 0}，按 {@code store_id} 归属；
     * {@code store_id IS NULL} 的总部现金户不进门店维度的列表（在 javadoc 说明，避免误以为漏数）。</p>
     */
    private List<FinanceOverviewDailyVO.StoreRow> loadStores(LocalDate day, Long scopedStoreId) {
        String sql = "SELECT s.store_id, s.store_name, a.account_id, a.balance, "
                + "       COALESCE(today.net_fen, 0) AS net_fen, "
                + "       COALESCE(hist.avg_abs_fen, 0) AS avg_abs_fen "
                + "  FROM stores_new s "
                + "  LEFT JOIN bank_accounts a "
                + "         ON a.store_id = s.store_id AND a.account_type = 2 AND a.deleted = 0 "
                + "  LEFT JOIN ( "
                + "        SELECT f.account_id, "
                + "               SUM(CASE WHEN f.flow_direction = 1 THEN f.amount ELSE -f.amount END) AS net_fen "
                + "          FROM fund_flows f "
                // ★ ①（Lead 2026-10-29）：测试资金冲销(category=8)不得计入今日净额
        + "         WHERE f.deleted = 0 AND f.status = 2 AND f.business_date = ? "
        + "           AND COALESCE(f.flow_category, 0) <> 8 "
                + "         GROUP BY f.account_id "
                + "  ) today ON today.account_id = a.account_id "
                + "  LEFT JOIN ( "
                + "        SELECT f.account_id, "
                + "               SUM(ABS(f.net_fen)) / 7.0 AS avg_abs_fen "
                + "          FROM ( "
                + "                SELECT f.account_id, f.business_date, "
                + "                       SUM(CASE WHEN f.flow_direction = 1 THEN f.amount ELSE -f.amount END) AS net_fen "
                + "                  FROM fund_flows f "
                + "                 WHERE f.deleted = 0 AND f.status = 2 "
                + "                   AND COALESCE(f.flow_category, 0) <> 8 "   // ★ ① 同上，排除测试冲销
                + "                   AND f.business_date >= ? AND f.business_date <= ? "
                + "                 GROUP BY f.account_id, f.business_date "
                + "               ) f "
                + "         GROUP BY f.account_id "
                + "  ) hist ON hist.account_id = a.account_id "
                + " WHERE s.deleted = 0 "
                + (scopedStoreId == null ? "" : "   AND s.store_id = ? ")
                + " ORDER BY s.store_id";
        LocalDate histFrom = day.minusDays(7);
        LocalDate histTo = day.minusDays(1);
        Object[] args = scopedStoreId == null
                ? new Object[]{day, histFrom, histTo}
                : new Object[]{day, histFrom, histTo, scopedStoreId};

        List<FinanceOverviewDailyVO.StoreRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(sql, args)) {
            FinanceOverviewDailyVO.StoreRow row = new FinanceOverviewDailyVO.StoreRow();
            row.storeId = asLong(r.get("store_id"));
            row.storeName = (String) r.get("store_name");
            row.cashAccountId = asLong(r.get("account_id"));
            row.balanceFen = row.cashAccountId == null ? 0L : nz(asLong(r.get("balance")));
            row.netChangeFen = nz(asLong(r.get("net_fen")));
            row.negative = row.balanceFen < 0;
            long avgAbs = Math.round(asDouble(r.get("avg_abs_fen")));
            long absNet = Math.abs(row.netChangeFen);
            row.abnormal = avgAbs > 0 ? absNet > avgAbs * 3 : absNet >= ABNORMAL_FLOOR_FEN;
            rows.add(row);
        }
        return rows;
    }

    /**
     * 段2：当日 FINALIZED 勾稽批聚合 + 未解释差异条数（两条 SQL，均集合查询）。
     *
     * <p>门店视角限制：批所属账户的 {@code bank_accounts.store_id} 必须等于本店
     * （{@code recon_runs} 无 store 列，按账户归属换算，与 AC-49 的账户口径一致）。</p>
     */
    private FinanceOverviewDailyVO.Channels loadChannels(LocalDate day, Long scopedStoreId) {
        String runSql = "SELECT r.run_id, r.account_id, r.internal_amount, r.external_amount, r.difference_count "
                + "  FROM recon_runs r "
                + "  LEFT JOIN bank_accounts a ON a.account_id = r.account_id "
                + " WHERE r.business_date = ? AND r.status = 'FINALIZED' "
                + (scopedStoreId == null ? "" : "   AND a.store_id = ? ")
                + " ORDER BY r.run_id";
        Object[] runArgs = scopedStoreId == null ? new Object[]{day} : new Object[]{day, scopedStoreId};

        FinanceOverviewDailyVO.Channels ch = new FinanceOverviewDailyVO.Channels();
        List<Long> runIds = new ArrayList<>();
        long internal = 0L;
        long external = 0L;
        long diff = 0L;
        for (Map<String, Object> r : jdbc.queryForList(runSql, runArgs)) {
            FinanceOverviewDailyVO.RunRow row = new FinanceOverviewDailyVO.RunRow();
            row.runId = asLong(r.get("run_id"));
            row.accountId = asLong(r.get("account_id"));
            row.internalAmount = nz(asLong(r.get("internal_amount")));
            row.externalAmount = nz(asLong(r.get("external_amount")));
            row.differenceCount = nz(asLong(r.get("difference_count")));
            ch.runs.add(row);
            runIds.add(row.runId);
            internal += row.internalAmount;
            external += row.externalAmount;
            diff += row.internalAmount - row.externalAmount;
        }
        ch.internalSaleFen = internal;
        ch.externalReceivedFen = external;
        ch.diffFen = diff;

        if (!runIds.isEmpty()) {
            String in = String.join(",", java.util.Collections.nCopies(runIds.size(), "?"));
            Integer unexplained = jdbc.queryForObject(
                    "SELECT count(*) FROM recon_differences "
                            + " WHERE run_id IN (" + in + ") AND status IN ('OPEN','ASSIGNED','RESOLVING')",
                    Integer.class, runIds.toArray());
            ch.unexplainedCount = unexplained == null ? 0L : unexplained.longValue();
        }
        return ch;
    }

    /**
     * 段3：调节表挂账（status NOT IN ('APPROVED')）：张数 / 状态分布 / 最长挂账 / 最老一条。
     *
     * <p>挂账天数：{@code submitted_at} 非空用它（已提交未审），DRAFT 用 {@code create_time}。
     * 门店视角按 {@code reconciliation_statements.store_id} 过滤。</p>
     */
    private FinanceOverviewDailyVO.Statements loadStatements(Long scopedStoreId) {
        FinanceOverviewDailyVO.Statements st = new FinanceOverviewDailyVO.Statements();
        String statusSql = "SELECT status, count(*) AS cnt FROM reconciliation_statements "
                + " WHERE status <> 'APPROVED' AND deleted = 0 "
                + (scopedStoreId == null ? "" : " AND store_id = ? ")
                + " GROUP BY status ORDER BY status";
        Object[] statusArgs = scopedStoreId == null ? new Object[]{} : new Object[]{scopedStoreId};
        long total = 0L;
        for (Map<String, Object> r : jdbc.queryForList(statusSql, statusArgs)) {
            FinanceOverviewDailyVO.StatusCount sc = new FinanceOverviewDailyVO.StatusCount();
            sc.status = (String) r.get("status");
            sc.count = asLong(r.get("cnt"));
            st.byStatus.add(sc);
            total += sc.count;
        }
        st.openCount = total;

        String oldestSql = "SELECT statement_id, status, submitted_by, submitted_at, "
                + "       COALESCE(submitted_at, create_time) AS anchor "
                + "  FROM reconciliation_statements "
                + " WHERE status <> 'APPROVED' AND deleted = 0 "
                + (scopedStoreId == null ? "" : " AND store_id = ? ")
                + " ORDER BY COALESCE(submitted_at, create_time) ASC, statement_id ASC LIMIT 1";
        List<Map<String, Object>> oldestRows = jdbc.queryForList(oldestSql, statusArgs);
        if (!oldestRows.isEmpty()) {
            Map<String, Object> r = oldestRows.get(0);
            FinanceOverviewDailyVO.Oldest o = new FinanceOverviewDailyVO.Oldest();
            o.statementId = asLong(r.get("statement_id"));
            o.status = (String) r.get("status");
            o.submittedBy = asLong(r.get("submitted_by"));
            o.submittedAt = asLocalDateTime(r.get("submitted_at"));
            LocalDateTime anchor = asLocalDateTime(r.get("anchor"));
            long days = anchor == null ? 0L : java.time.Duration.between(anchor, LocalDateTime.now()).toDays();
            o.daysPending = Math.max(days, 0L);
            st.oldest = o;
            st.oldestPendingDays = o.daysPending;
        }
        return st;
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static double asDouble(Object v) {
        if (v == null) {
            return 0d;
        }
        return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(String.valueOf(v));
    }

    private static LocalDateTime asLocalDateTime(Object v) {
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

    private static BusinessException forbidden(String msg) {
        return new BusinessException(ErrorCode.FORBIDDEN.getCode(), msg);
    }
}
