package com.foodtraceability.event.listener;

import com.foodtraceability.dto.finance.FinanceRecordCreateDTO;
import com.foodtraceability.event.OrderRefundEvent;
import com.foodtraceability.service.finance.FinanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * 订单退款事件监听器
 * 处理退款审批通过后的财务联动：
 * <ol>
 *   <li>生成退款支出财务流水（finance_record，recordType=2 支出）</li>
 *   <li>（未来）生成红字凭证关联原销售凭证</li>
 * </ol>
 *
 * <p>DF-022 修复：approveRefund 中实现退款记录持久化 + 库存回补后，
 * 发布 {@link OrderRefundEvent}，本监听器在主事务提交后异步触发财务联动。</p>
 *
 * <p>ADR-004 事件隔离策略：使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * 确保主事务（退款审批）提交后才触发联动处理，避免主事务回滚后联动副作用残留。
 * 配合 {@code @Async} 在独立线程池执行，不阻塞主事务。</p>
 */
@Component
public class OrderRefundEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderRefundEventListener.class);

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param financeRecordService 财务记录服务，可选依赖（若未启用财务模块则不影响主流程）
     */
    public OrderRefundEventListener(@Nullable FinanceRecordService financeRecordService) {
        this.financeRecordService = financeRecordService;
    }

    private final FinanceRecordService financeRecordService;

    /**
     * 监听订单退款事件
     * 主事务（approveRefund）提交后异步触发：
     * 1. 记录退款支出财务流水
     * 2. （未来）生成红字凭证关联原销售凭证
     *
     * @param event 退款事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRefundEvent(OrderRefundEvent event) {
        log.info("处理订单退款事件: refundId={}, orderId={}, refundType={}, amount={}分",
                event.getRefundId(), event.getOrderId(), event.getRefundType(), event.getRefundAmount());

        try {
            recordRefundExpense(event);

            log.info("订单退款事件处理完成: refundId={}", event.getRefundId());
        } catch (Exception e) {
            // 异常隔离：仅记录日志，不重抛
            // 业务事件无持久化机制（DF-038），失败后只能人工补救
            log.error("处理订单退款事件失败: refundId={}, 错误={}",
                    event.getRefundId(), e.getMessage(), e);
        }
    }

    /**
     * 记录退款支出财务流水
     * <p>退款属于支出类型（recordType=2），退款类别为 205-其他支出（暂用，
     * 待财务科目体系完善后可细化退款子科目）。</p>
     *
     * <p>异常隔离：本方法内部 try-catch，仅 log.error 不重抛，
     * 不影响主流程及其他监听器执行。</p>
     *
     * @param event 退款事件
     */
    private void recordRefundExpense(OrderRefundEvent event) {
        log.info("记录退款支出财务流水: refundId={}, orderId={}, amount={}分",
                event.getRefundId(), event.getOrderId(), event.getRefundAmount());

        // 边界保护：refundId 或 refundAmount 为 null 时跳过
        if (event.getRefundId() == null) {
            log.warn("refundId 为空，跳过退款财务流水生成，orderId={}", event.getOrderId());
            return;
        }
        if (event.getRefundAmount() == null || event.getRefundAmount() <= 0L) {
            log.warn("refundAmount 为空或非正数，跳过退款财务流水生成，refundId={}", event.getRefundId());
            return;
        }

        if (financeRecordService == null) {
            log.info("FinanceRecordService 未启用，跳过退款财务流水生成，refundId={}", event.getRefundId());
            return;
        }

        try {
            FinanceRecordCreateDTO dto = new FinanceRecordCreateDTO();
            dto.setRecordType(2); // 2-支出（退款视为支出）
            dto.setRecordCategory(205); // 205-其他支出（退款）
            // 金额：事件中已是分（Long），直接使用
            dto.setAmount(event.getRefundAmount());
            // 业务发生日期：取退款完成时间对应的日期，若空则取当天
            LocalDate businessDate = event.getCompleteTime() != null
                    ? event.getCompleteTime().toLocalDate() : LocalDate.now();
            dto.setBusinessDate(businessDate);
            // 备注包含订单号 + 退款类型 + 退款原因，便于人工对账
            String refundTypeName = event.getRefundType() != null && event.getRefundType() == 1
                    ? "全额退款" : "部分退款";
            String remark = "订单退款 - " + refundTypeName;
            if (event.getOrderNumber() != null) {
                remark += "，订单编号：" + event.getOrderNumber();
            }
            if (event.getRefundReason() != null && !event.getRefundReason().isEmpty()) {
                remark += "，原因：" + event.getRefundReason();
            }
            dto.setRemark(remark);

            financeRecordService.create(dto);

            log.info("退款支出财务流水记录成功: refundId={}, orderId={}, amountFen={}",
                    event.getRefundId(), event.getOrderId(), event.getRefundAmount());
        } catch (Exception e) {
            // 异常隔离：仅记录日志，不重抛
            log.error("记录退款支出财务流水失败: refundId={}, orderId={}, 错误={}",
                    event.getRefundId(), event.getOrderId(), e.getMessage(), e);
        }
    }
}
