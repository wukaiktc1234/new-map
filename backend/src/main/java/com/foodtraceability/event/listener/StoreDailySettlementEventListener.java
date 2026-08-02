package com.foodtraceability.event.listener;

import com.foodtraceability.event.StoreDailySettlementCompletedEvent;
import com.foodtraceability.service.finance.AutoVoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 门店日结完成事件监听器（F-008 联动）
 *
 * <p>Sprint 3.1 P0 T-036：监听 {@link StoreDailySettlementCompletedEvent}，
 * 在主事务提交后（AFTER_COMMIT）异步调用
 * {@code AutoVoucherService.generateStoreSettlementVoucher(event)} 生成财务凭证。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} 确保门店日结主事务提交成功后才消费事件，
 *       避免主事务回滚后仍生成凭证导致数据不一致。</li>
 *   <li>{@code @Async} 异步执行，不阻塞主事务提交。</li>
 *   <li>异常 try-catch 隔离，仅 {@code log.error} 记录，不重抛，
 *       避免事件消费失败影响其他监听器或主流程。</li>
 *   <li>幂等性由 {@code AutoVoucherService} 内部通过
 *       {@code existsBySource(SOURCE_TYPE_STORE_SETTLEMENT, settlementId)} 保证，
 *       监听器不重复实现幂等逻辑。</li>
 * </ul>
 * </p>
 */
@Component
public class StoreDailySettlementEventListener {

    private static final Logger log = LoggerFactory.getLogger(StoreDailySettlementEventListener.class);

    private final AutoVoucherService autoVoucherService;

    /**
     * 构造函数注入 AutoVoucherService
     *
     * @param autoVoucherService 自动凭证生成Service
     */
    public StoreDailySettlementEventListener(AutoVoucherService autoVoucherService) {
        this.autoVoucherService = autoVoucherService;
    }

    /**
     * 处理门店日结完成事件
     *
     * <p>主事务提交后异步生成财务凭证。异常被隔离，不影响主流程。</p>
     *
     * @param event 门店日结完成事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(StoreDailySettlementCompletedEvent event) {
        log.info("处理门店日结完成事件：日结单ID={}，门店={}，收入={}，支出={}",
                event.getSettlementId(), event.getStoreName(),
                event.getTotalIncome(), event.getTotalExpense());

        try {
            autoVoucherService.generateStoreSettlementVoucher(event);
            log.info("门店日结完成事件处理成功：日结单ID={}", event.getSettlementId());
        } catch (Exception e) {
            log.error("处理门店日结完成事件失败：日结单ID={}，错误：{}",
                    event.getSettlementId(), e.getMessage(), e);
            // 异常隔离：不重抛，避免影响其他监听器或主流程
        }
    }
}
