package com.foodtraceability.event.listener;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.finance.FinanceRecordCreateDTO;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.service.finance.CostRecordService;
import com.foodtraceability.service.finance.FinanceRecordService;
import com.foodtraceability.service.finance.ReceivableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单完成事件监听器
 * 处理订单完成后的数据联动
 *
 * <p>DF-011 修复：将 @EventListener 改为 @TransactionalEventListener(AFTER_COMMIT)，
 * 确保主事务（订单完成、库存扣减、成本持久化）提交后才触发监听器，
 * 避免主事务回滚后异步线程仍执行产生脏数据。
 * 配合 @Transactional(REQUIRES_NEW) 使用独立事务，监听器异常不影响主事务。</p>
 *
 * <p>注意：订单成本（食材成本）已由 OrderNewServiceImpl.completeOrder 中的
 * persistOrderCost 方法同步持久化到 cost_record 表（DF-009 修复）。
 * 本监听器仅处理财务收入记录，以及当事件显式携带 materialCost/laborCost 时
 * 记录对应的成本明细（如后厨端 completeMake 流程）。</p>
 */
@Component
public class OrderCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCompletedEventListener.class);

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param costRecordService 成本记录服务
     * @param financeRecordService 财务记录服务，可选依赖
     */
    public OrderCompletedEventListener(CostRecordService costRecordService, @Nullable FinanceRecordService financeRecordService,
                                       OrderNewMapper orderNewMapper, @Nullable ReceivableService receivableService,
                                       com.foodtraceability.mapper.finance.FinanceRecordMapper financeRecordMapper) {
        this.costRecordService = costRecordService;
        this.financeRecordService = financeRecordService;
        this.orderNewMapper = orderNewMapper;
        this.receivableService = receivableService;
        this.financeRecordMapper = financeRecordMapper;
    }

    private final CostRecordService costRecordService;

    private final FinanceRecordService financeRecordService;

    private final OrderNewMapper orderNewMapper;

    private final ReceivableService receivableService;

    private final com.foodtraceability.mapper.finance.FinanceRecordMapper financeRecordMapper;

    /**
     * 监听订单完成事件（主事务提交后异步处理）
     * 1. 记录财务收入
     * 2. 记录成本（仅当事件显式携带 materialCost/laborCost 时）
     *
     * <p>使用 AFTER_COMMIT 阶段：确保订单状态变更、库存扣减、成本持久化等
     * 主事务操作已提交后才触发，避免主事务回滚后产生脏数据。
     * 使用 REQUIRES_NEW 传播：监听器在独立事务中执行，异常不影响主事务。</p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("处理订单完成事件: orderId={}, orderNumber={}", 
                event.getOrderId(), event.getOrderNumber());
        
        Exception firstFailure = null;

        try {
            recordFinanceIncome(event);
        } catch (Exception e) {
            log.error("订单完成事件联动-记录财务收入失败: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber(), e);
            firstFailure = e;
        }

        try {
            recordReceivableIfUnpaid(event);
        } catch (Exception e) {
            log.error("订单完成事件联动-生成应收失败: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber(), e);
            if (firstFailure == null) {
                firstFailure = e;
            }
        }

        // CODE-007：recordOrderCost 已删除。OrderCompletedEvent.materialCost/laborCost 从未被任何
        // 发布点赋值（恒为 null），该方法是死代码；成本已由订单完成主流程同步写入 cost_records，
        // 此处再记一次会重复入账。

        if (firstFailure != null) {
            // 假成功修复：原来两个步骤各自吞异常，外层仍打印「处理完成」。
            // 现在聚合失败后显式抛出，让「日志说完成、账面没记录」不再发生。
            log.error("订单完成事件联动不完整（财务收入/应收可能缺失，需人工补录）: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "订单完成联动失败（orderId=" + event.getOrderId() + "）：" + firstFailure.getMessage());
        }

        log.info("订单完成事件处理完成: orderId={}", event.getOrderId());
    }

    /**
     * F4：订单完成时若未付清（挂账/赊账），按未付金额生成应收账款。
     * 已付清订单不生成应收（现结业务），避免污染应收列表。
     */
    private void recordReceivableIfUnpaid(OrderCompletedEvent event) {
        if (receivableService == null) {
            return;
        }
        try {
            OrderNew order = orderNewMapper.selectById(event.getOrderId());
            if (order == null) {
                return;
            }
            // 已付清（paymentStatus=2）无需应收
            if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
                return;
            }
            long finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : 0L;
            long paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : 0L;
            long unpaid = finalAmount - paidAmount;
            if (unpaid <= 0) {
                return;
            }
            String orderNo = order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId());
            receivableService.createForOrderByNo(
                    orderNo,
                    order.getCustomerId(),
                    order.getCustomerName(),
                    unpaid,
                    java.sql.Date.valueOf(java.time.LocalDate.now()));
            log.info("订单未付清生成应收成功: orderId={}, orderNo={}, unpaid={}分",
                    event.getOrderId(), orderNo, unpaid);
        } catch (Exception e) {
            log.error("订单未付清生成应收失败: orderId={}, 错误={}", event.getOrderId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "生成应收账款失败（orderId=" + event.getOrderId() + "）：" + e.getMessage());
        }
    }
    
    /**
     * 记录财务收入
     */
    private void recordFinanceIncome(OrderCompletedEvent event) {
        log.info("记录财务收入: orderId={}, amount={}",
                event.getOrderId(), event.getActualAmount());

        try {
            if (financeRecordService != null) {
                // 2026-10-23 幂等：POS 支付成功时已写一条收入（record_category=101）。
                // 订单完成事件若再写一条，会造成同一订单营收重复入账（实测 02:50 那张单已出现两条）。
                // 以 order_code 为幂等键：已有收入流水则跳过，仅记日志。
                String orderCode = event.getOrderNumber();
                if (orderCode != null && financeRecordMapper != null) {
                    int existing = financeRecordMapper.countIncomeByOrderCode(orderCode);
                    if (existing > 0) {
                        log.info("财务收入已存在，幂等跳过（避免重复入账）: orderCode={}, existing={}", orderCode, existing);
                        return;
                    }
                }

                FinanceRecordCreateDTO dto = new FinanceRecordCreateDTO();
                dto.setRecordType(1); // 1-收入
                dto.setRecordCategory(101); // 101-销售收入
                // 金额：元（BigDecimal）转分（Long）
                java.math.BigDecimal amountYuan = event.getActualAmount() != null
                        ? event.getActualAmount() : java.math.BigDecimal.ZERO;
                if (amountYuan.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    log.warn("财务收入金额<=0，跳过写入（避免 0 元流水污染）: orderId={}, orderCode={}",
                            event.getOrderId(), orderCode);
                    return;
                }
                dto.setAmount(amountYuan.multiply(java.math.BigDecimal.valueOf(100L)).longValue());
                dto.setBusinessDate(java.time.LocalDate.now());
                // W1-EC-05: 设置 canonical order reference（orders.order_code）
                if (orderCode != null) {
                    dto.setOrderCode(orderCode);
                }
                dto.setRemark("订单收入 - " + orderCode);

                financeRecordService.create(dto);
            }

            log.info("财务收入记录成功: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("记录财务收入失败: orderId={}, orderCode={}, error={}",
                    event.getOrderId(), event.getOrderNumber(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "记录财务收入失败（orderId=" + event.getOrderId() + "）：" + e.getMessage());
        }
    }
    
}
