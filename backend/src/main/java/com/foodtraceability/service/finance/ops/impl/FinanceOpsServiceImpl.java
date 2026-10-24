package com.foodtraceability.service.finance.ops.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.finance.AtomicBalanceUpdater;
import com.foodtraceability.dto.finance.recon.FixtureReverseRequest;
import com.foodtraceability.dto.finance.recon.FixtureReverseVO;
import com.foodtraceability.service.finance.backfill.impl.ReconCashSupport;
import com.foodtraceability.service.finance.ops.FinanceOpsService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 夹具余额抹平（运维端点，**业务上不存在此场景**）。
 *
 * <p>背景：P2 自测时补记过 2 条夹具 SALE 流水（FF202610250010/011，各 100 分，已 status=3 作废），
 * 但当时已经把余额加上去了 —— 于是「账面少了 200 分、账户余额多了 200 分」两边同时错，
 * V3（balance vs 最新 balance_after）反而假 PASS。本动作补一条反向流水把这 200 分抹平，
 * 让两边都回到真值。</p>
 *
 * <p><b>红线</b>：①只接受 status=3 且 remark 含 [testFixture 的流水，普通业务流水一律 400；
 * ②余额只走 {@link AtomicBalanceUpdater#addBalance}（乐观锁），禁止裸 SET；
 * ③反向流水只增不删；④整批一个事务，任一失败整体回滚；⑤写 finance_audit_logs 留痕。</p>
 */
@Service
public class FinanceOpsServiceImpl implements FinanceOpsService {

    private static final Logger log = LoggerFactory.getLogger(FinanceOpsServiceImpl.class);

    private static final String FIXTURE_MARK = "[testFixture";

    private final JdbcTemplate jdbc;
    private final AtomicBalanceUpdater balanceUpdater;
    private final ReconCashSupport cashSupport;

    public FinanceOpsServiceImpl(JdbcTemplate jdbc,
                                 AtomicBalanceUpdater balanceUpdater,
                                 ReconCashSupport cashSupport) {
        this.jdbc = jdbc;
        this.balanceUpdater = balanceUpdater;
        this.cashSupport = cashSupport;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixtureReverseVO reverseFixtures(FixtureReverseRequest req) {
        if (req == null || req.getFlowNos() == null || req.getFlowNos().isEmpty()) {
            throw err("flowNos 不能为空");
        }
        // 审计要求：原因必填，且不得静默兜底成默认文案（否则审计里看不出「谁为什么抹平」）
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw err("抹平原因不能为空");
        }
        // 去重且保序，避免同一批里重复抹平
        List<String> flowNos = new ArrayList<>(new LinkedHashSet<>(req.getFlowNos()));

        FixtureReverseVO vo = new FixtureReverseVO();
        Long accountId = null;
        for (String flowNo : flowNos) {
            if (flowNo == null || flowNo.isBlank()) {
                throw err("flowNo 不能为空");
            }
            // ---- 锁原流水 ----
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT flow_id, flow_no, account_id, flow_direction, flow_category, amount, status, remark "
                            + "  FROM fund_flows WHERE flow_no = ? AND deleted = 0 FOR UPDATE", flowNo);
            if (rows.isEmpty()) {
                throw err("流水不存在或已删除：" + flowNo);
            }
            Map<String, Object> f = rows.get(0);
            int status = ((Number) f.get("status")).intValue();
            String remark = f.get("remark") == null ? "" : String.valueOf(f.get("remark"));
            Long accId = f.get("account_id") == null ? null : ((Number) f.get("account_id")).longValue();
            long amount = ((Number) f.get("amount")).longValue();
            int direction = ((Number) f.get("flow_direction")).intValue();
            int category = ((Number) f.get("flow_category")).intValue();

            if (status != 3) {
                throw err("只允许抹平已作废（status=3）的夹具流水，流水 " + flowNo + " 当前 status=" + status);
            }
            if (!remark.contains(FIXTURE_MARK)) {
                throw err("流水 " + flowNo + " 不是测试夹具（remark 缺少 " + FIXTURE_MARK
                        + " 标记），普通业务流水禁止抹平");
            }
            if (accId == null) {
                throw err("流水 " + flowNo + " 没有账户，无法抹平余额");
            }
            if (accountId == null) {
                accountId = accId;
            } else if (!accountId.equals(accId)) {
                throw err("一次只能抹平同一账户的流水（" + accountId + " vs " + accId + "）");
            }
            Integer dup = jdbc.queryForObject(
                    "SELECT count(*) FROM fund_flows WHERE deleted = 0 AND remark LIKE ?",
                    Integer.class, "%[testFixtureReverse=" + flowNo + "]%");
            if (dup != null && dup > 0) {
                throw err("流水 " + flowNo + " 已抹平过（存在 [testFixtureReverse=" + flowNo + "]），拒绝重复执行");
            }

            // ---- 反向流水：方向取反、金额相同 ----
            int reverseDirection = direction == 1 ? 2 : 1;
            long delta = reverseDirection == 1 ? amount : -amount;
            // BL-4：方向为 IN 的原流水，其反向是**扣款**（delta<0）——必须走带下限校验的共用方法，
            // 余额不足时抛 ConflictException(409) 并整体回滚（不允许把账户余额打成负数）。
            AtomicBalanceUpdater.BalanceSnapshot snap = balanceUpdater.addBalanceGuarded(accId, delta, "抹平", flowNo);
            // 批内只有第一条记录「抹平前」快照，最后一条记录「抹平后」快照，
            // 否则会变成「最后一条的前值」，看报告的人会误以为总额只动了 100 分。
            if (vo.balanceBefore == null) {
                vo.balanceBefore = snap.getBalanceBefore();
                vo.versionBefore = snap.getVersionBefore();
            }
            vo.balanceAfter = snap.getBalanceAfter();
            vo.versionAfter = snap.getVersionAfter();

            LocalDate bizDate = LocalDate.now();
            String reverseFlowNo = cashSupport.nextFlowNo(bizDate);
            String newRemark = "[testFixtureReverse=" + flowNo + "] 夹具余额抹平（原流水 " + flowNo
                    + "，方向 " + direction + "→" + reverseDirection + "，金额 " + amount + " 分）原因：" + req.getReason();
            LocalDateTime now = LocalDateTime.now();
            Long userId = currentUserId();
            String userName = currentUserName();
            jdbc.update("INSERT INTO fund_flows "
                            + "(flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date,"
                            + " status, remark, create_user_id, created_by, create_time, update_time, deleted, version) "
                            + "VALUES (?,?,?,?,?,?,?,2,?,?,?,?,?,0,0)",
                    reverseFlowNo, accId, reverseDirection, category, amount, snap.getBalanceAfter(), bizDate,
                    newRemark, userId, userName, now, now);

            FixtureReverseVO.Item item = new FixtureReverseVO.Item();
            item.originalFlowNo = flowNo;
            item.reverseFlowNo = reverseFlowNo;
            item.amountFen = amount;
            item.reverseDirection = reverseDirection;
            item.balanceAfterFen = snap.getBalanceAfter();
            vo.items.add(item);
            log.info("[FIXTURE_REVERSE] 原流水={} 反向流水={} 账户={} delta={} 余额 {}→{}",
                    flowNo, reverseFlowNo, accId, delta, snap.getBalanceBefore(), snap.getBalanceAfter());
        }

        vo.accountId = accountId;

        // ---- 审计留痕：finance_audit_logs ----
        StringBuilder desc = new StringBuilder("夹具流水抹平：原流水=");
        for (int i = 0; i < vo.items.size(); i++) {
            if (i > 0) {
                desc.append(",");
            }
            desc.append(vo.items.get(i).originalFlowNo);
        }
        desc.append(" 反向流水=");
        for (int i = 0; i < vo.items.size(); i++) {
            if (i > 0) {
                desc.append(",");
            }
            desc.append(vo.items.get(i).reverseFlowNo);
        }
        desc.append(" 金额合计=").append(vo.items.stream().mapToLong(it -> it.amountFen).sum()).append("分");
        desc.append(" 账户=").append(accountId);
        desc.append(" 余额 ").append(vo.balanceBefore).append("→").append(vo.balanceAfter);
        desc.append(" 原因=").append(req.getReason());

        // 结构化审计数据（含 reason / 全部原流水号 / 全部反向流水号 / 金额合计 / 时间）
        StringBuilder originalsJson = new StringBuilder("[");
        StringBuilder reversesJson = new StringBuilder("[");
        for (int i = 0; i < vo.items.size(); i++) {
            if (i > 0) {
                originalsJson.append(",");
                reversesJson.append(",");
            }
            originalsJson.append("\"").append(vo.items.get(i).originalFlowNo).append("\"");
            reversesJson.append("\"").append(vo.items.get(i).reverseFlowNo).append("\"");
        }
        originalsJson.append("]");
        reversesJson.append("]");
        long sumFen = vo.items.stream().mapToLong(it -> it.amountFen).sum();
        String auditDataJson = "{\"reason\":\"" + req.getReason().replace("\"", "'") + "\","
                + "\"originalFlowNos\":" + originalsJson + ","
                + "\"reverseFlowNos\":" + reversesJson + ","
                + "\"amountTotalFen\":" + sumFen + ","
                + "\"accountId\":" + accountId + ","
                + "\"balanceBefore\":" + vo.balanceBefore + ","
                + "\"balanceAfter\":" + vo.balanceAfter + ","
                + "\"occurredAt\":\"" + LocalDateTime.now() + "\"}";

        KeyHolder kh = new GeneratedKeyHolder();
        final String descText = desc.toString();
        final Long operatorId = currentUserId();
        final String operatorName = currentUserName();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module,"
                            + " target_id, target_type, operation_desc, operation_data, create_time, created_by, version) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,0)", new String[]{"log_id"});
            ps.setLong(1, operatorId);
            ps.setString(2, operatorName);
            ps.setString(3, "FIXTURE_FLOW_REVERSED");
            ps.setString(4, "finance");
            ps.setObject(5, null);
            ps.setString(6, "FUND_FLOW");
            ps.setString(7, descText);
            // operation_data：结构化审计（机器可读，便于巡检/追查；desc 供人看）
            ps.setString(8, auditDataJson);
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(10, operatorName);
            return ps;
        }, kh);
        vo.auditLogId = kh.getKey() == null ? null : kh.getKey().longValue();
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
