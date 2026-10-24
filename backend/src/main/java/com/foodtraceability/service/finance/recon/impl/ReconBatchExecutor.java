package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.entity.finance.recon.BankTransaction;
import com.foodtraceability.entity.finance.recon.ReconDifference;
import com.foodtraceability.entity.finance.recon.ReconItem;
import com.foodtraceability.entity.finance.recon.ReconRun;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.mapper.finance.recon.BankTransactionMapper;
import com.foodtraceability.mapper.finance.recon.ReconDifferenceMapper;
import com.foodtraceability.mapper.finance.recon.ReconItemMapper;
import com.foodtraceability.mapper.finance.recon.ReconRunMapper;
import com.foodtraceability.service.finance.recon.ReconDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对账跑批执行器（R1 的核心事务体）。
 *
 * <p><b>事务边界</b>：本类的 {@link #execute(Long)} 是**唯一**一个跑批事务，items / differences /
 * run 汇总（FINALIZED）全部在其中完成；任何异常 → 整体回滚，调用方再用独立事务把 run 置 FAILED，
 * 因此不会出现「半套 items/differences」。</p>
 *
 * <p><b>红线（P1 范围锁）</b>：本类对 fund_flows / bank_accounts / bank_transactions / order_payment_records
 * <b>只做 SELECT</b>；写入目标只有 recon_runs / recon_items / recon_differences。
 * 不写 fund_flows、不改 bank_accounts.balance、不冲正、不生成缴款单/内部转账（P2）。</p>
 *
 * <p><b>匹配口径</b>（§2.2，自动只允许 1:1）：
 * L1 = 支付渠道流水号去前缀后相等（金额不等也算命中，但会生成 AMOUNT_DIFF）；
 * L2 = 金额相等（或 内部金额 = 外部金额 + fee 视为手续费平账）且时间差 ≤ 窗口（默认 ±10 分钟）；
 * 多候选 → 两侧置 MATCHED（待人工）+ AMBIGUOUS 差异；判不出方向的手续费场景 → AMBIGUOUS。</p>
 *
 * <p><b>fail-inject</b>：属性 app.finance.recon.fail-inject=true 时，在写完全部 items 后主动抛错，
 * 仅用于验收「跑批失败 → run=FAILED 且 items=0」的回滚证据；默认关闭。</p>
 */
@Service
public class ReconBatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReconBatchExecutor.class);

    private final ReconRunMapper reconRunMapper;
    private final ReconItemMapper reconItemMapper;
    private final ReconDifferenceMapper reconDifferenceMapper;
    private final BankAccountMapper bankAccountMapper;
    private final FundFlowMapper fundFlowMapper;
    private final BankTransactionMapper bankTransactionMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordMapper;

    /** 时间窗（分钟），默认 ±10 分钟（design §2.2） */
    private final long windowMinutes;

    /** 仅验收用失败注入，默认 false */
    private final boolean failInject;

    public ReconBatchExecutor(ReconRunMapper reconRunMapper,
                              ReconItemMapper reconItemMapper,
                              ReconDifferenceMapper reconDifferenceMapper,
                              BankAccountMapper bankAccountMapper,
                              FundFlowMapper fundFlowMapper,
                              BankTransactionMapper bankTransactionMapper,
                              OrderPaymentRecordNewMapper orderPaymentRecordMapper,
                              @Value("${app.finance.recon.time-window-minutes:10}") long windowMinutes,
                              @Value("${app.finance.recon.fail-inject:false}") boolean failInject) {
        this.reconRunMapper = reconRunMapper;
        this.reconItemMapper = reconItemMapper;
        this.reconDifferenceMapper = reconDifferenceMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.bankTransactionMapper = bankTransactionMapper;
        this.orderPaymentRecordMapper = orderPaymentRecordMapper;
        this.windowMinutes = windowMinutes;
        this.failInject = failInject;
    }

    /**
     * 执行一个 run（单事务）。失败时抛异常，由调用方把 run 置 FAILED。
     *
     * @param runId recon_runs.run_id（必须已存在且为 RUNNING）
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long runId) {
        ReconRun run = reconRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对账批次不存在：runId=" + runId);
        }
        LocalDate businessDate = run.getBusinessDate();
        if (businessDate == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "对账批次的 businessDate 为空：runId=" + runId);
        }
        BankAccount account = bankAccountMapper.selectById(run.getAccountId());
        if (account == null || Integer.valueOf(1).equals(account.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "银行账户不存在或已删除：accountId=" + run.getAccountId());
        }

        List<InternalSubject> internals = loadInternal(run, businessDate);
        List<ExternalSubject> externals = loadExternal(run.getAccountId(), businessDate);
        log.info("[METRIC] recon_batch_loaded runId={} accountId={} date={} internal={} external={}",
                runId, run.getAccountId(), businessDate, internals.size(), externals.size());

        List<DuplicatedExternal> duplicatedExternals = collapseDuplicateExternals(externals);
        List<ReconDifference> differences = new ArrayList<>();
        match(internals, externals, differences);
        collectOneSided(internals, externals, differences);
        collectDuplicateInternals(internals, differences);
        collectDuplicateExternals(duplicatedExternals, differences);

        persistItems(runId, internals, externals);
        if (failInject) {
            throw new IllegalStateException("fail-inject：跑批在写入 items 后主动失败（仅用于回滚验收）");
        }
        persistDifferences(runId, differences);
        finalizeRun(run, internals, externals, differences.size());
    }

    // ==================== 取数 ====================

    private List<InternalSubject> loadInternal(ReconRun run, LocalDate businessDate) {
        LocalDateTime snapshotAt = run.getStartedAt() == null ? LocalDateTime.now() : run.getStartedAt();
        List<FundFlow> flows = fundFlowMapper.selectList(new LambdaQueryWrapper<FundFlow>()
                .eq(FundFlow::getAccountId, run.getAccountId())
                .eq(FundFlow::getBusinessDate, businessDate)
                .eq(FundFlow::getFlowDirection, 1)
                .eq(FundFlow::getFlowCategory, 1)
                .eq(FundFlow::getStatus, 2)
                .eq(FundFlow::getDeleted, 0)
                .le(FundFlow::getCreateTime, snapshotAt)
                .orderByAsc(FundFlow::getFlowId));
        if (flows == null || flows.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> orderIds = new LinkedHashSet<>();
        for (FundFlow flow : flows) {
            if (flow.getOrderId() != null && !flow.getOrderId().isEmpty()) {
                orderIds.add(flow.getOrderId());
            }
        }
        Map<String, List<OrderPaymentRecordNew>> paymentsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderPaymentRecordNew> payments = orderPaymentRecordMapper.selectList(
                    new LambdaQueryWrapper<OrderPaymentRecordNew>()
                            .in(OrderPaymentRecordNew::getOrderId, orderIds)
                            .eq(OrderPaymentRecordNew::getDeleted, 0)
                            .orderByAsc(OrderPaymentRecordNew::getPaymentId));
            if (payments != null) {
                for (OrderPaymentRecordNew payment : payments) {
                    paymentsByOrder.computeIfAbsent(payment.getOrderId(), k -> new ArrayList<>()).add(payment);
                }
            }
        }

        List<InternalSubject> result = new ArrayList<>(flows.size());
        for (FundFlow flow : flows) {
            InternalSubject subject = new InternalSubject();
            subject.flowNo = flow.getFlowNo();
            subject.orderId = flow.getOrderId();
            subject.amount = flow.getAmount() == null ? 0L : flow.getAmount();
            subject.counterparty = flow.getCounterpartyName();
            LocalDateTime flowTime = flow.getCreateTime() != null
                    ? flow.getCreateTime()
                    : businessDate.atStartOfDay();
            List<OrderPaymentRecordNew> payments = flow.getOrderId() == null
                    ? null : paymentsByOrder.get(flow.getOrderId());
            if (payments != null && !payments.isEmpty()) {
                Set<String> methods = new LinkedHashSet<>();
                LocalDateTime earliestPayment = null;
                for (OrderPaymentRecordNew payment : payments) {
                    if (payment.getPaymentMethod() != null) {
                        methods.add(String.valueOf(payment.getPaymentMethod()));
                    }
                    String key = ReconDictionary.normalizeRef(payment.getTransactionNo());
                    if (key != null && !key.isEmpty()) {
                        subject.paymentKeys.add(key);
                    }
                    if (payment.getPaymentTime() != null
                            && (earliestPayment == null || payment.getPaymentTime().isBefore(earliestPayment))) {
                        earliestPayment = payment.getPaymentTime();
                    }
                }
                subject.paymentMethods = methods.isEmpty() ? null : String.join(",", methods);
                // L2 时间窗以「支付时间」为准；缺失时回落到台帐创建时间
                subject.txnTime = earliestPayment != null ? earliestPayment : flowTime;
            } else {
                subject.txnTime = flowTime;
            }
            result.add(subject);
        }
        return result;
    }

    private List<ExternalSubject> loadExternal(Long accountId, LocalDate businessDate) {
        LocalDate nextDay = businessDate.plusDays(1);
        List<BankTransaction> rows = bankTransactionMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getAccountId, accountId)
                .eq(BankTransaction::getDeleted, 0)
                .and(w -> w.eq(BankTransaction::getBusinessDate, businessDate)
                        .or(o -> o.isNull(BankTransaction::getBusinessDate)
                                .ge(BankTransaction::getTxnTime, businessDate.atStartOfDay())
                                .lt(BankTransaction::getTxnTime, nextDay.atStartOfDay())))
                .orderByAsc(BankTransaction::getTransactionId));
        List<ExternalSubject> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (BankTransaction row : rows) {
            ExternalSubject subject = new ExternalSubject();
            subject.transactionId = row.getTransactionId();
            subject.ref = row.getExternalTxnId() != null && !row.getExternalTxnId().isEmpty()
                    ? row.getExternalTxnId()
                    : String.valueOf(row.getTransactionId());
            subject.amount = row.getAmount() == null ? 0L : row.getAmount();
            subject.fee = row.getFee();
            subject.txnTime = row.getTxnTime() != null ? row.getTxnTime() : businessDate.atStartOfDay();
            subject.counterparty = row.getCounterpartyName();
            result.add(subject);
        }
        return result;
    }

    /** 同一 external_txn_id 重复导入：只保留一条 item，其余落 DUPLICATE 差异（recon_items 唯一键不允许重复主体） */
    private List<DuplicatedExternal> collapseDuplicateExternals(List<ExternalSubject> externals) {
        Map<String, ExternalSubject> firstByRef = new LinkedHashMap<>();
        List<DuplicatedExternal> duplicated = new ArrayList<>();
        List<ExternalSubject> keep = new ArrayList<>();
        for (ExternalSubject subject : externals) {
            String key = ReconDictionary.normalizeRef(subject.ref);
            if (key == null || key.isEmpty()) {
                keep.add(subject);
                continue;
            }
            ExternalSubject first = firstByRef.get(key);
            if (first == null) {
                firstByRef.put(key, subject);
                keep.add(subject);
            } else {
                duplicated.add(new DuplicatedExternal(subject, first));
            }
        }
        externals.clear();
        externals.addAll(keep);
        return duplicated;
    }

    // ==================== 匹配 ====================

    private void match(List<InternalSubject> internals, List<ExternalSubject> externals,
                       List<ReconDifference> differences) {
        // ---- L1：渠道流水号（去前缀）精确命中，1:1 自动 ----
        for (InternalSubject internal : internals) {
            if (internal.paymentKeys.isEmpty()) {
                continue;
            }
            List<ExternalSubject> candidates = new ArrayList<>();
            for (ExternalSubject external : externals) {
                if (!ReconDictionary.ITEM_UNMATCHED.equals(external.status)) {
                    continue;
                }
                String key = ReconDictionary.normalizeRef(external.ref);
                if (key != null && internal.paymentKeys.contains(key)) {
                    candidates.add(external);
                }
            }
            if (candidates.size() == 1) {
                ExternalSubject external = candidates.get(0);
                markAuto(internal, external, ReconDictionary.LEVEL_L1, "1.000");
                if (internal.amount != external.amount) {
                    differences.add(amountDiff(internal, external));
                }
            } else if (candidates.size() > 1) {
                markAmbiguous(internal, candidates, differences);
            }
        }

        // ---- L2：金额（含手续费平账）+ 时间窗，1:1 自动 ----
        for (InternalSubject internal : internals) {
            if (!ReconDictionary.ITEM_UNMATCHED.equals(internal.status)) {
                continue;
            }
            List<ExternalSubject> candidates = new ArrayList<>();
            for (ExternalSubject external : externals) {
                if (!ReconDictionary.ITEM_UNMATCHED.equals(external.status)) {
                    continue;
                }
                if (!timeWindowOk(internal.txnTime, external.txnTime)) {
                    continue;
                }
                if (internal.amount == external.amount || feeBalanced(internal.amount, external)) {
                    candidates.add(external);
                }
            }
            if (candidates.size() == 1) {
                markAuto(internal, candidates.get(0), ReconDictionary.LEVEL_L2, "0.900");
            } else if (candidates.size() > 1) {
                markAmbiguous(internal, candidates, differences);
            }
        }

        // ---- 金额/手续费有差：时间窗内唯一候选 → MATCHED + AMOUNT_DIFF / AMBIGUOUS ----
        for (InternalSubject internal : internals) {
            if (!ReconDictionary.ITEM_UNMATCHED.equals(internal.status)) {
                continue;
            }
            List<ExternalSubject> nearby = new ArrayList<>();
            for (ExternalSubject external : externals) {
                if (ReconDictionary.ITEM_UNMATCHED.equals(external.status)
                        && timeWindowOk(internal.txnTime, external.txnTime)) {
                    nearby.add(external);
                }
            }
            if (nearby.size() == 1) {
                ExternalSubject external = nearby.get(0);
                internal.status = ReconDictionary.ITEM_MATCHED;
                internal.matchedRef = external.ref;
                external.status = ReconDictionary.ITEM_MATCHED;
                external.matchedRef = internal.flowNo;
                boolean feeDirectionUnknown = external.fee != null && external.fee > 0;
                differences.add(feeDirectionUnknown
                        ? ambiguousDiff(internal, external)
                        : amountDiff(internal, external));
            } else if (nearby.size() > 1) {
                markAmbiguous(internal, nearby, differences);
            }
        }
    }

    private void markAuto(InternalSubject internal, ExternalSubject external, String level, String confidence) {
        internal.status = ReconDictionary.ITEM_AUTO_MATCHED;
        internal.matchLevel = level;
        internal.confidence = new BigDecimal(confidence);
        internal.matchedRef = external.ref;
        external.status = ReconDictionary.ITEM_AUTO_MATCHED;
        external.matchLevel = level;
        external.confidence = new BigDecimal(confidence);
        external.matchedRef = internal.flowNo;
    }

    /** 多候选：两侧都置 MATCHED（待人工），不进入勾稽率；记一条 AMBIGUOUS */
    private void markAmbiguous(InternalSubject internal, List<ExternalSubject> candidates,
                               List<ReconDifference> differences) {
        internal.status = ReconDictionary.ITEM_MATCHED;
        internal.matchedRef = candidates.get(0).ref;
        for (ExternalSubject candidate : candidates) {
            candidate.status = ReconDictionary.ITEM_MATCHED;
            candidate.matchedRef = internal.flowNo;
        }
        differences.add(ambiguousDiff(internal, candidates.get(0)));
    }

    private boolean timeWindowOk(LocalDateTime internalTime, LocalDateTime externalTime) {
        if (internalTime == null || externalTime == null) {
            return false;
        }
        long minutes = Math.abs(Duration.between(internalTime, externalTime).toMinutes());
        return minutes <= windowMinutes;
    }

    /** 手续费口径（§2.2 裁决 6）：内部金额 == 外部金额 + fee 视为平账 */
    private boolean feeBalanced(long internalAmount, ExternalSubject external) {
        return external.fee != null && external.fee > 0
                && internalAmount == external.amount + external.fee;
    }

    // ==================== 差异 ====================

    private ReconDifference amountDiff(InternalSubject internal, ExternalSubject external) {
        ReconDifference diff = baseDiff(ReconDictionary.DIFF_AMOUNT, internal, external);
        diff.setAmountInternal(internal.amount);
        diff.setAmountExternal(external.amount);
        diff.setDiffAmount(external.amount - internal.amount);
        return diff;
    }

    private ReconDifference ambiguousDiff(InternalSubject internal, ExternalSubject external) {
        ReconDifference diff = baseDiff(ReconDictionary.DIFF_AMBIGUOUS, internal, external);
        diff.setAmountInternal(internal.amount);
        diff.setAmountExternal(external.amount);
        diff.setDiffAmount(external.amount - internal.amount);
        return diff;
    }

    private ReconDifference baseDiff(String type, InternalSubject internal, ExternalSubject external) {
        ReconDifference diff = new ReconDifference();
        diff.setDifferenceType(type);
        diff.setInternalRef(internal == null ? null : internal.flowNo);
        diff.setExternalRef(external == null ? null : external.ref);
        diff.setStatus(ReconDictionary.DIFF_OPEN);
        LocalDateTime now = LocalDateTime.now();
        diff.setCreateTime(now);
        diff.setUpdateTime(now);
        return diff;
    }

    private void collectOneSided(List<InternalSubject> internals, List<ExternalSubject> externals,
                                 List<ReconDifference> differences) {
        for (InternalSubject internal : internals) {
            if (ReconDictionary.ITEM_UNMATCHED.equals(internal.status)) {
                ReconDifference diff = baseDiff(ReconDictionary.DIFF_ONE_SIDED_INTERNAL, internal, null);
                diff.setAmountInternal(internal.amount);
                diff.setDiffAmount(-internal.amount);
                differences.add(diff);
            }
        }
        for (ExternalSubject external : externals) {
            if (ReconDictionary.ITEM_UNMATCHED.equals(external.status)) {
                ReconDifference diff = baseDiff(ReconDictionary.DIFF_ONE_SIDED_EXTERNAL, null, external);
                diff.setAmountExternal(external.amount);
                diff.setDiffAmount(external.amount);
                differences.add(diff);
            }
        }
    }

    /** 同订单同金额多条 SALE 流水 → DUPLICATE（第 2 条起） */
    private void collectDuplicateInternals(List<InternalSubject> internals, List<ReconDifference> differences) {
        Map<String, List<InternalSubject>> grouped = new LinkedHashMap<>();
        for (InternalSubject internal : internals) {
            if (internal.orderId == null || internal.orderId.isEmpty()) {
                continue;
            }
            grouped.computeIfAbsent(internal.orderId + "|" + internal.amount, k -> new ArrayList<>()).add(internal);
        }
        for (List<InternalSubject> group : grouped.values()) {
            if (group.size() < 2) {
                continue;
            }
            for (int i = 1; i < group.size(); i++) {
                InternalSubject duplicated = group.get(i);
                ReconDifference diff = baseDiff(ReconDictionary.DIFF_DUPLICATE, duplicated, null);
                diff.setAmountInternal(duplicated.amount);
                diff.setDiffAmount(-duplicated.amount);
                differences.add(diff);
            }
        }
    }

    private void collectDuplicateExternals(List<DuplicatedExternal> duplicatedExternals,
                                           List<ReconDifference> differences) {
        for (DuplicatedExternal duplicated : duplicatedExternals) {
            ReconDifference diff = baseDiff(ReconDictionary.DIFF_DUPLICATE, null, duplicated.subject);
            diff.setAmountExternal(duplicated.subject.amount);
            diff.setDiffAmount(duplicated.subject.amount);
            differences.add(diff);
        }
    }

    // ==================== 落库 ====================

    private void persistItems(Long runId, List<InternalSubject> internals, List<ExternalSubject> externals) {
        LocalDateTime now = LocalDateTime.now();
        for (InternalSubject internal : internals) {
            ReconItem item = new ReconItem();
            item.setRunId(runId);
            item.setSide(ReconDictionary.SIDE_INTERNAL);
            item.setInternalRef(internal.flowNo);
            item.setPaymentMethods(internal.paymentMethods);
            item.setAmount(internal.amount);
            item.setTxnTime(internal.txnTime);
            item.setCounterpartyName(internal.counterparty);
            item.setSubjectStatus(internal.status);
            item.setMatchLevel(internal.matchLevel);
            item.setConfidence(internal.confidence);
            if (!ReconDictionary.ITEM_UNMATCHED.equals(internal.status)) {
                item.setMatchedSince(now);
            }
            item.setCreateTime(now);
            item.setUpdateTime(now);
            reconItemMapper.insert(item);
        }
        for (ExternalSubject external : externals) {
            ReconItem item = new ReconItem();
            item.setRunId(runId);
            item.setSide(ReconDictionary.SIDE_EXTERNAL);
            item.setExternalRef(external.ref);
            item.setAmount(external.amount);
            item.setTxnTime(external.txnTime);
            item.setCounterpartyName(external.counterparty);
            item.setSubjectStatus(external.status);
            item.setMatchLevel(external.matchLevel);
            item.setConfidence(external.confidence);
            if (!ReconDictionary.ITEM_UNMATCHED.equals(external.status)) {
                item.setMatchedSince(now);
            }
            item.setCreateTime(now);
            item.setUpdateTime(now);
            reconItemMapper.insert(item);
        }
    }

    private void persistDifferences(Long runId, List<ReconDifference> differences) {
        Set<String> dedupe = new HashSet<>();
        for (ReconDifference diff : differences) {
            String key = diff.getDifferenceType() + "|" + diff.getInternalRef() + "|" + diff.getExternalRef();
            if (!dedupe.add(key)) {
                continue;
            }
            diff.setRunId(runId);
            reconDifferenceMapper.insert(diff);
        }
    }

    private void finalizeRun(ReconRun run, List<InternalSubject> internals, List<ExternalSubject> externals,
                             int differenceCount) {
        long internalAmount = 0L;
        for (InternalSubject internal : internals) {
            internalAmount += internal.amount;
        }
        long externalAmount = 0L;
        for (ExternalSubject external : externals) {
            externalAmount += external.amount;
        }
        int matchedCount = 0;
        long matchedAmount = 0L;
        for (InternalSubject internal : internals) {
            if (ReconDictionary.ITEM_AUTO_MATCHED.equals(internal.status)
                    || ReconDictionary.ITEM_MANUAL_MATCHED.equals(internal.status)) {
                matchedCount++;
                matchedAmount += internal.amount;
            }
        }

        ReconRun patch = new ReconRun();
        patch.setRunId(run.getRunId());
        patch.setStatus(ReconDictionary.RUN_FINALIZED);
        patch.setInternalCount(internals.size());
        patch.setInternalAmount(internalAmount);
        patch.setExternalCount(externals.size());
        patch.setExternalAmount(externalAmount);
        patch.setMatchedCount(matchedCount);
        patch.setMatchedAmount(matchedAmount);
        patch.setDifferenceCount(differenceCount);
        patch.setFinishedAt(LocalDateTime.now());
        patch.setUpdateTime(LocalDateTime.now());
        reconRunMapper.updateById(patch);
        log.info("[METRIC] recon_run_finalized runId={} internal={}/{} external={}/{} matched={}/{} diffs={}",
                run.getRunId(), internals.size(), internalAmount, externals.size(), externalAmount,
                matchedCount, matchedAmount, differenceCount);
    }

    // ==================== 内部结构 ====================

    private static final class InternalSubject {
        private String flowNo;
        private String orderId;
        private long amount;
        private LocalDateTime txnTime;
        private String counterparty;
        private String paymentMethods;
        private final Set<String> paymentKeys = new HashSet<>();
        private String status = ReconDictionary.ITEM_UNMATCHED;
        private String matchLevel;
        private String matchedRef;
        private BigDecimal confidence;
    }

    private static final class ExternalSubject {
        private Long transactionId;
        private String ref;
        private long amount;
        private Long fee;
        private LocalDateTime txnTime;
        private String counterparty;
        private String status = ReconDictionary.ITEM_UNMATCHED;
        private String matchLevel;
        private String matchedRef;
        private BigDecimal confidence;
    }

    private static final class DuplicatedExternal {
        private final ExternalSubject subject;
        private final ExternalSubject first;

        private DuplicatedExternal(ExternalSubject subject, ExternalSubject first) {
            this.subject = subject;
            this.first = first;
        }
    }
}
