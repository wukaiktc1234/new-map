package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.finance.AtomicBalanceUpdater;
import com.foodtraceability.common.finance.FinancePeriodGuard;
import com.foodtraceability.common.sequence.SequenceAllocator;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 退款资金流水 recorder（P4-5 R5：**三入口统一的唯一写点**）。
 *
 * <p><b>为什么只有这里写</b>：退款流水只能有一处产生，否则同一退款单可能被多个入口各写一笔；
 * 唯一性由 uk_fund_flows_refund_record（唯一索引，仅 flow_category=2）兜底，
 * 应用层先按 refund_record_id 幂等查重。</p>
 *
 * <p><b>时点选择</b>：所有入口都必须在「退款资金动作成立」的那一刻调用本组件 ——
 * 审批流在 approveRefund（同意且置 refund_status=已退款）时写；POS 即时退款在退款成功后写。
 * applyRefund（仅提交申请、可能被拒）不写，避免「申请即出账、拒绝后无法回滚」。</p>
 *
 * <p><b>字段口径</b>：flow_direction=2（出账）、flow_category=2（退款专用，不复用 SALE）、
 * refund_record_id=退款单号、order_id=业务订单、status=2、
 * balance_after=原子更新后的账户余额（禁止自己算）。</p>
 */
@Component
public class PosRefundFlowRecorder {

    private static final Logger log = LoggerFactory.getLogger(PosRefundFlowRecorder.class);

    private final FundFlowMapper fundFlowMapper;
    private final SequenceAllocator sequenceAllocator;
    private final AtomicBalanceUpdater atomicBalanceUpdater;
    private final FinancePeriodGuard financePeriodGuard;
    private final PosCashAccountResolver posCashAccountResolver;

    public PosRefundFlowRecorder(FundFlowMapper fundFlowMapper,
                                 SequenceAllocator sequenceAllocator,
                                 AtomicBalanceUpdater atomicBalanceUpdater,
                                 FinancePeriodGuard financePeriodGuard,
                                 PosCashAccountResolver posCashAccountResolver) {
        this.fundFlowMapper = fundFlowMapper;
        this.sequenceAllocator = sequenceAllocator;
        this.atomicBalanceUpdater = atomicBalanceUpdater;
        this.financePeriodGuard = financePeriodGuard;
        this.posCashAccountResolver = posCashAccountResolver;
    }

    /** 写流水结果（duplicate=true 表示该退款单已入账，本次未重复写） */
    public record Outcome(String flowNo, Long accountId, long amountFen, Long balanceAfter, boolean duplicate) {
    }

    /**
     * 记录一笔退款资金流水（幂等：按 refund_record_id）。
     *
     * @param orderId        业务订单ID（可为 null，仅历史遗留单）
     * @param refundRecordId 退款单号（order_refund_records.refund_id，**幂等键**）
     * @param amountFen      本次退款金额（分，必须 > 0）
     * @param accountIdHint  指定账户（null → 按 storeId + paymentMethod 解析）
     * @param storeId        订单门店（解析账户用）
     * @param paymentMethod  原支付方式（1 现金 / 2 微信 / 3 支付宝 / 5 余额）
     * @param operatorId     操作人ID
     * @param operatorName   操作人名（写 created_by）
     * @param businessDate   业务日期（退款发生日；null → 今天）
     * @param scene          场景标签（POS_REFUND / ORDER_REFUND_APPROVE …，仅日志）
     * @return 写入或命中的流水
     */
    public Outcome record(String orderId, Long refundRecordId, long amountFen, Long accountIdHint,
                          Long storeId, Integer paymentMethod, Long operatorId, String operatorName,
                          LocalDate businessDate, String scene) {
        if (refundRecordId == null) {
            throw new BusinessException(500, "退款入账失败：缺少退款单号（refund_record_id 为幂等键）");
        }
        if (amountFen <= 0) {
            throw new BusinessException(500, "退款入账失败：退款金额必须为正数，收到 " + amountFen + " 分");
        }
        LocalDate date = businessDate != null ? businessDate : LocalDate.now();
        // TD-39：期间封账 → 409，调用方整体回滚
        financePeriodGuard.assertOpen(date);

        FundFlow existing = findExisting(refundRecordId);
        if (existing != null) {
            log.warn("[METRIC] pos_refund_flow result=duplicate scene={} refundRecordId={} flowNo={} amountFen={}",
                    scene, refundRecordId, existing.getFlowNo(), existing.getAmount());
            return new Outcome(existing.getFlowNo(), existing.getAccountId(), existing.getAmount(),
                    existing.getBalanceAfter(), true);
        }

        Long accountId = accountIdHint;
        if (accountId == null) {
            PosCashAccountResolver.Resolved pick =
                    posCashAccountResolver.resolve(storeId, paymentMethod, orderId, scene);
            if (pick == null) {
                throw new BusinessException(500, "退款入账失败：门店 " + storeId
                        + " 没有匹配支付方式的资金账户，请先补配账户");
            }
            accountId = pick.accountId();
        }

        String ymd = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer seq = sequenceAllocator.next("FLOW" + ymd, "FF" + ymd, "fund_flows", "flow_no");
        if (seq == null || seq <= 0) {
            throw new BusinessException(500, "退款流水号段分配失败：FLOW" + ymd);
        }
        String flowNo = String.format("FF%s%04d", ymd, seq);
        AtomicBalanceUpdater.BalanceSnapshot snapshot =
                atomicBalanceUpdater.addBalanceGuarded(accountId, -amountFen, "POS退款", flowNo);

        FundFlow flow = new FundFlow();
        flow.setFlowNo(flowNo);
        flow.setAccountId(accountId);
        flow.setFlowDirection(2);
        flow.setFlowCategory(PosSaleFinanceRecorder.FLOW_CATEGORY_REFUND);
        flow.setAmount(amountFen);
        flow.setBalanceAfter(snapshot.getBalanceAfter());
        flow.setBusinessDate(date);
        flow.setStatus(2);
        flow.setOrderId(orderId);
        flow.setRefundRecordId(refundRecordId);
        flow.setRemark("POS退款：退款单#" + refundRecordId
                + (orderId == null ? "" : " 订单=" + orderId) + " [" + scene + "]");
        flow.setCreateUserId(operatorId);
        flow.setCreatedBy(operatorName);
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        flow.setDeleted(0);
        fundFlowMapper.insert(flow);
        log.info("[METRIC] pos_refund_flow result=success scene={} orderId={} refundRecordId={} flowNo={} accountId={} amountFen={} balanceAfter={}",
                scene, orderId, refundRecordId, flowNo, accountId, amountFen, snapshot.getBalanceAfter());
        return new Outcome(flowNo, accountId, amountFen, snapshot.getBalanceAfter(), false);
    }

    /** 幂等查重：同一退款单只允许一笔退款流水（唯一索引兜底） */
    public FundFlow findExisting(Long refundRecordId) {
        if (refundRecordId == null) {
            return null;
        }
        return fundFlowMapper.selectOne(new LambdaQueryWrapper<FundFlow>()
                .eq(FundFlow::getRefundRecordId, refundRecordId)
                .eq(FundFlow::getFlowCategory, PosSaleFinanceRecorder.FLOW_CATEGORY_REFUND)
                .eq(FundFlow::getFlowDirection, 2)
                .eq(FundFlow::getStatus, 2)
                .eq(FundFlow::getDeleted, 0)
                .last("LIMIT 1"));
    }
}
