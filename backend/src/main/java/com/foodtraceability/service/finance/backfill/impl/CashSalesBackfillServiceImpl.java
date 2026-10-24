package com.foodtraceability.service.finance.backfill.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.finance.recon.CashSalesBackfillRequest;
import com.foodtraceability.dto.finance.recon.CashSalesBackfillVO;
import com.foodtraceability.service.finance.backfill.CashSalesBackfillService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 现金销售 SALE 流水补记（P2-B）。
 *
 * <p><b>口径与 consistency_check A1 完全一致</b>：orders.payment_status=2（已支付）且存在现金支付记录
 * （order_payment_records.payment_method=1）但缺少 SALE 流水（fund_flows.order_id 命中 + flow_direction=1
 * + flow_category=1 + deleted=0）。补记金额只算**现金部分**（SUM(payment_amount) WHERE payment_method=1）。</p>
 *
 * <p><b>幂等</b>：remark 内含 [posShift=&lt;shift_id&gt;][cashBackfill]，同一 shift + 同一 order 已存在补记行 → 跳过，
 * 重复调用新增 0 条。</p>
 *
 * <p><b>余额</b>：只走 {@link ReconCashSupport#applyDelta} 的乐观锁 UPDATE；补记行 <b>不是</b> A7 豁免行
 * （created_by='system-backfill-cashsales'，不匹配 'system/backfill-%'），因此它必须带真实 balance_after，
 * 否则 A7（账户余额 vs 最新非豁免 balance_after）会被判红。</p>
 */
@Service
public class CashSalesBackfillServiceImpl implements CashSalesBackfillService {

    private final JdbcTemplate jdbc;
    private final ReconCashSupport support;

    public CashSalesBackfillServiceImpl(JdbcTemplate jdbc, ReconCashSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashSalesBackfillVO backfill(CashSalesBackfillRequest req) {
        if (req == null) {
            throw err("入参不能为空");
        }
        boolean dryRun = req.getDryRun() == null || Boolean.TRUE.equals(req.getDryRun());
        LocalDateTime fromTs;
        LocalDateTime toTs;
        Long shiftId = req.getShiftId();

        if (shiftId != null) {
            List<Map<String, Object>> shifts = jdbc.queryForList(
                    "SELECT shift_id, start_time, end_time FROM pos_shifts WHERE shift_id = ? AND deleted = 0", shiftId);
            if (shifts.isEmpty()) {
                throw err("班次不存在: " + shiftId);
            }
            Object st = shifts.get(0).get("start_time");
            Object et = shifts.get(0).get("end_time");
            if (st == null || et == null) {
                throw err("班次 " + shiftId + " 缺少开始/结束时间，无法界定补记窗口");
            }
            fromTs = ((java.sql.Timestamp) st).toLocalDateTime();
            toTs = ((java.sql.Timestamp) et).toLocalDateTime();
        } else {
            if (req.getFrom() == null || req.getTo() == null) {
                throw err("shiftId 与 from/to 必须二选一（都不给无法界定补记范围）");
            }
            if (req.getTo().isBefore(req.getFrom())) {
                throw err("to 不能早于 from");
            }
            fromTs = req.getFrom().atStartOfDay();
            toTs = req.getTo().atTime(23, 59, 59);
        }

        // 口径 = A1：已支付 + 有现金支付 + 缺 SALE 流水
        String sql = "SELECT o.order_id, o.order_number, o.order_code, o.store_id, o.payment_time, "
                + "       SUM(opr.payment_amount) AS cash_amount "
                + "  FROM orders o "
                + "  JOIN order_payment_records opr ON opr.order_id = o.order_id AND opr.deleted = 0 "
                + "       AND opr.payment_method = 1 "
                + " WHERE o.deleted = 0 AND o.payment_status = 2 "
                + "   AND COALESCE(o.payment_time, o.create_time) BETWEEN ? AND ? "
                + "   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.order_id = o.order_id::text "
                + "                    AND f.flow_direction = 1 AND f.flow_category = 1 AND f.deleted = 0) "
                + " GROUP BY o.order_id, o.order_number, o.order_code, o.store_id, o.payment_time "
                + " ORDER BY o.order_id";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, fromTs, toTs);

        CashSalesBackfillVO vo = new CashSalesBackfillVO();
        vo.dryRun = dryRun;
        vo.shiftId = shiftId;
        vo.from = req.getFrom() != null ? req.getFrom() : fromTs.toLocalDate();
        vo.to = req.getTo() != null ? req.getTo() : toTs.toLocalDate();
        vo.candidates = new ArrayList<>();
        vo.candidateCount = rows.size();
        vo.writtenCount = 0;
        vo.skippedCount = 0;
        long total = 0;

        Long userId = currentUserId();
        String userName = currentUserName();
        Map<Long, Long> accountCache = new HashMap<>();
        String token = "[posShift=" + (shiftId == null ? "range" : shiftId) + "][cashBackfill]";

        for (Map<String, Object> r : rows) {
            String orderId = String.valueOf(r.get("order_id"));
            Long storeId = r.get("store_id") == null ? null : ((Number) r.get("store_id")).longValue();
            long cash = ((Number) r.get("cash_amount")).longValue();
            LocalDate bizDate = r.get("payment_time") == null
                    ? LocalDate.now()
                    : ((java.sql.Timestamp) r.get("payment_time")).toLocalDateTime().toLocalDate();

            CashSalesBackfillVO.Candidate c = new CashSalesBackfillVO.Candidate();
            c.orderId = orderId;
            c.orderNumber = r.get("order_number") == null ? null : String.valueOf(r.get("order_number"));
            c.storeId = storeId;
            c.cashAmountFen = cash;
            c.businessDate = bizDate;

            // 幂等：同 shift + 同 order 已有补记行 → 跳过
            Integer dup = jdbc.queryForObject(
                    "SELECT count(*) FROM fund_flows WHERE deleted = 0 AND order_id = ? AND remark LIKE ?",
                    Integer.class, String.valueOf(orderId), "%" + token + "%");
            if (dup != null && dup > 0) {
                c.status = "SKIPPED_IDEMPOTENT";
                vo.skippedCount++;
                vo.candidates.add(c);
                continue;
            }

            // 账户解析：该门店现金户（account_type=2）
            Long accountId = accountCache.computeIfAbsent(storeId, sid -> {
                List<Long> ids = jdbc.queryForList(
                        "SELECT account_id FROM bank_accounts WHERE deleted = 0 AND account_type = 2 "
                                + " AND store_id = ? ORDER BY account_id LIMIT 1", Long.class, sid);
                if (ids.isEmpty()) {
                    throw err("门店 " + sid + " 没有现金账户（account_type=2），无法补记 SALE 流水");
                }
                return ids.get(0);
            });
            c.accountId = accountId;
            total += cash;

            if (dryRun) {
                c.status = "DRY_RUN";
                vo.candidates.add(c);
                continue;
            }

            // 真写：余额原子更新 → 插入 SALE 流水
            ReconCashSupport.BalanceChange change = support.applyDelta(accountId, cash);
            String flowNo = support.nextFlowNo(bizDate);
            String remark = token + " POS 销售补记：" + (c.orderNumber == null ? orderId : c.orderNumber)
                    + "（现金 " + cash + " 分）" + (req.getReason() == null ? "" : " 原因：" + req.getReason());
            LocalDateTime now = LocalDateTime.now();
            jdbc.update("INSERT INTO fund_flows "
                            + "(flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date,"
                            + " status, remark, order_id, create_user_id, created_by, create_time, update_time,"
                            + " deleted, version) "
                            + "VALUES (?,?,1,1,?,?,?,2,?,?,?,?,?,?,0,0)",
                    flowNo, accountId, cash, change.balanceAfter(), bizDate, remark, orderId,
                    userId, "system-backfill-cashsales", now, now);
            c.flowNo = flowNo;
            c.status = "WRITTEN";
            vo.writtenCount++;
            vo.candidates.add(c);
        }
        vo.totalAmountFen = total;
        return vo;
    }

    private static BusinessException err(String msg) {
        return new BusinessException(400, msg);
    }

    private Long currentUserId() {
        try {
            Long id = SecurityUtils.getCurrentUserId();
            return id == null ? 0L : id;
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
