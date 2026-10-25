package com.foodtraceability.service.finance.impl;

import com.foodtraceability.entity.finance.FundFlowNoBurnLedger;
import com.foodtraceability.mapper.finance.FundFlowNoBurnLedgerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 烧号账写入器（BL-5）：记录「号段已消耗、fund_flows 未落库」的流水号。
 *
 * <p><b>为什么单独成 Bean（关键）</b>：调用方 {@code FundFlowServiceImpl#create} 正处于
 * 业务事务中，且马上要因为插入/记账失败而整体回滚。若把烧号 INSERT 写在同一个事务里，
 * 它会跟着业务一起被回滚 —— 账就没了，A25 也就永远对不上。因此这里必须
 * {@code propagation = REQUIRES_NEW}，并且**必须经由注入的 Bean 调用**（走 Spring 代理）；
 * 同类 {@code this.recordBurn(...)} 自调用不会开新事务，等于白写。</p>
 *
 * <p>不开 {@code readOnly}，不吞异常：写失败由调用方 {@code addSuppressed} + ERROR 日志暴露，
 * 绝不静默。</p>
 */
@Service
public class FlowNoBurnLedgerRecorder {

    private static final Logger log = LoggerFactory.getLogger(FlowNoBurnLedgerRecorder.class);

    /** reason 列宽 200，超长截断，避免写账本身因长度失败 */
    private static final int REASON_MAX_LEN = 200;

    private final FundFlowNoBurnLedgerMapper fundFlowNoBurnLedgerMapper;

    public FlowNoBurnLedgerRecorder(FundFlowNoBurnLedgerMapper fundFlowNoBurnLedgerMapper) {
        this.fundFlowNoBurnLedgerMapper = fundFlowNoBurnLedgerMapper;
    }

    /**
     * 写一条烧号账（独立事务，不受调用方回滚影响）。
     *
     * @param flowNo   已消耗但未落库的流水号
     * @param seqKey   号段键，如 FLOW20261025
     * @param seqValue 被消耗的序号（号段自增返回值）
     * @param reason   失败原因（duplicate-key / 乐观锁耗尽 / 业务校验失败 …）
     * @param createdBy 写账人（可为 null，落库为 null 而不伪造）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordBurn(String flowNo, String seqKey, Long seqValue, String reason, String createdBy) {
        FundFlowNoBurnLedger ledger = new FundFlowNoBurnLedger();
        ledger.setFlowNo(flowNo);
        ledger.setSeqKey(seqKey);
        ledger.setSeqValue(seqValue == null ? 0L : seqValue);
        ledger.setReason(truncate(reason));
        ledger.setCreatedBy(createdBy);
        ledger.setCreateTime(LocalDateTime.now());
        ledger.setArchivedFlag(Boolean.FALSE);
        fundFlowNoBurnLedgerMapper.insert(ledger);
        log.warn("[A25] 烧号记账：flowNo={} seqKey={} seqValue={} reason={}",
                flowNo, seqKey, seqValue, truncate(reason));
    }

    private String truncate(String reason) {
        String r = (reason == null || reason.isBlank()) ? "unknown" : reason;
        return r.length() <= REASON_MAX_LEN ? r : r.substring(0, REASON_MAX_LEN);
    }
}
