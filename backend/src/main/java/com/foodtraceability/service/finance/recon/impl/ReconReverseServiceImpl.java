package com.foodtraceability.service.finance.recon.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.finance.recon.ReverseExecutionVO;
import com.foodtraceability.service.finance.backfill.impl.ReconCashSupport;
import com.foodtraceability.service.finance.recon.ReconReverseService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 差异 REVERSE 实际执行（P2-B）。
 *
 * <p><b>事务边界</b>：整个 executeReverse 在**单个事务**内 —— 锁原流水 → 校验幂等 → 原子改余额
 * → 插入反向流水 → 原流水置 status=3 → 差异置 CLOSED。任一步抛错整体回滚，不会出现「余额改了但流水没写」。</p>
 *
 * <p><b>只增不删</b>：反向流水是**新行**，原行只把 status 由 2 改成 3，金额/日期/账户一律不动。</p>
 */
@Service
public class ReconReverseServiceImpl implements ReconReverseService {

    private final JdbcTemplate jdbc;
    private final ReconCashSupport support;

    public ReconReverseServiceImpl(JdbcTemplate jdbc, ReconCashSupport support) {
        this.jdbc = jdbc;
        this.support = support;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReverseExecutionVO executeReverse(Long differenceId) {
        if (differenceId == null) {
            throw err("differenceId 必填");
        }

        // ---- 1) 差异必须处于 PENDING_P2_REV + REVERSE ----
        List<Map<String, Object>> diffs = jdbc.queryForList(
                "SELECT difference_id, status, resolution, internal_ref, external_ref, amount_internal, difference_type "
                        + "  FROM recon_differences WHERE difference_id = ?", differenceId);
        if (diffs.isEmpty()) {
            throw err("差异不存在: " + differenceId);
        }
        Map<String, Object> diff = diffs.get(0);
        String status = String.valueOf(diff.get("status"));
        String resolution = diff.get("resolution") == null ? null : String.valueOf(diff.get("resolution"));
        String internalRef = diff.get("internal_ref") == null ? null : String.valueOf(diff.get("internal_ref"));

        if (!"PENDING_P2_REV".equals(status) || !"REVERSE".equals(resolution)) {
            throw err("差异 " + differenceId + " 当前状态为 " + status + "/" + resolution
                    + "，只有 PENDING_P2_REV + REVERSE 才能执行冲正");
        }
        if (internalRef == null || internalRef.isBlank()) {
            throw err("差异 " + differenceId + " 没有关联内部流水号（internal_ref），无法执行冲正"
                    + "（C/D 类单边账不通过本接口冲正）");
        }

        // ---- 2) 锁原流水（FOR UPDATE，防并发重复冲正）----
        List<Map<String, Object>> flows = jdbc.queryForList(
                "SELECT flow_id, flow_no, account_id, flow_direction, flow_category, amount, status, order_id, "
                        + "       counterparty_name, counterparty_account, business_date "
                        + "  FROM fund_flows WHERE flow_no = ? AND deleted = 0 FOR UPDATE", internalRef);
        if (flows.isEmpty()) {
            throw err("原流水不存在或已删除: " + internalRef);
        }
        Map<String, Object> flow = flows.get(0);
        Long flowId = ((Number) flow.get("flow_id")).longValue();
        int originalStatus = ((Number) flow.get("status")).intValue();
        if (originalStatus == 3) {
            throw err("原流水 " + internalRef + " 已是「已取消」状态（status=3），不能重复冲正");
        }
        Long accountId = flow.get("account_id") == null ? null : ((Number) flow.get("account_id")).longValue();
        if (accountId == null) {
            throw err("原流水 " + internalRef + " 没有账户（account_id 为空），无法做余额冲正");
        }
        int direction = ((Number) flow.get("flow_direction")).intValue();
        int category = ((Number) flow.get("flow_category")).intValue();
        long amount = ((Number) flow.get("amount")).longValue();

        // ---- 3) 幂等：已有 [reconReverseOf=<原flow_no>] 直接拒绝 ----
        Integer existing = jdbc.queryForObject(
                "SELECT count(*) FROM fund_flows WHERE deleted = 0 AND remark LIKE ?",
                Integer.class, "%[reconReverseOf=" + internalRef + "]%");
        if (existing != null && existing > 0) {
            throw err("流水 " + internalRef + " 已存在冲正流水（[reconReverseOf=" + internalRef
                    + "]），拒绝重复冲正（幂等保护，不生成第二条）");
        }

        // ---- 4) 余额原子更新（乐观锁），反方向 ----
        int reverseDirection = direction == 1 ? 2 : 1;
        long delta = reverseDirection == 1 ? amount : -amount;
        ReconCashSupport.BalanceChange change = support.applyDelta(accountId, delta);

        // ---- 5) 新增反向流水（金额相同、方向取反、分类同原流水）----
        LocalDate businessDate = LocalDate.now();
        String reverseFlowNo = support.nextFlowNo(businessDate);
        Long userId = currentUserId();
        String userName = currentUserName();
        String remark = "[reconReverseOf=" + internalRef + "] 冲正差异 #" + differenceId
                + "（原流水方向 " + direction + " → 反向 " + reverseDirection + "，金额 " + amount + " 分）"
                + " by " + userName;
        LocalDateTime now = LocalDateTime.now();

        jdbc.update("INSERT INTO fund_flows "
                        + "(flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date,"
                        + " status, remark, order_id, counterparty_name, counterparty_account,"
                        + " create_user_id, created_by, create_time, update_time, deleted, version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)",
                reverseFlowNo, accountId, reverseDirection, category, amount, change.balanceAfter(), businessDate,
                2, remark, flow.get("order_id"), flow.get("counterparty_name"), flow.get("counterparty_account"),
                userId, userName, now, now);

        // ---- 6) 原流水置「已取消」，不删除、不改金额 ----
        int cancelled = jdbc.update(
                "UPDATE fund_flows SET status = 3, update_time = ? WHERE flow_id = ? AND status IN (1, 2)",
                now, flowId);
        if (cancelled != 1) {
            throw err("原流水 " + internalRef + " 状态更新失败（影响行数 " + cancelled + "），已回滚");
        }

        // ---- 7) 差异置 CLOSED + 回写反向流水号 ----
        jdbc.update("UPDATE recon_differences SET status = 'CLOSED', resolution = 'REVERSE', reverse_flow_no = ?,"
                        + " resolution_by = ?, resolution_time = ?, update_time = ? WHERE difference_id = ?",
                reverseFlowNo, userId, now, now, differenceId);

        ReverseExecutionVO vo = new ReverseExecutionVO();
        vo.differenceId = differenceId;
        vo.originalFlowNo = internalRef;
        vo.originalStatus = 3;
        vo.reverseFlowNo = reverseFlowNo;
        vo.reverseDirection = reverseDirection;
        vo.reverseAmountFen = amount;
        vo.accountId = accountId;
        vo.balanceBeforeFen = change.balanceBefore();
        vo.balanceAfterFen = change.balanceAfter();
        vo.differenceStatus = "CLOSED";
        vo.resolution = "REVERSE";
        vo.idempotentHit = Boolean.FALSE;
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
