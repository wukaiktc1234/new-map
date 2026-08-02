package com.foodtraceability.service.finance;

import com.foodtraceability.event.finance.BudgetExceededEvent;
import com.foodtraceability.event.finance.PaymentCompletedEvent;
import com.foodtraceability.event.finance.VoucherPostedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 财务事件发布器（F-022）
 *
 * <p>封装财务模块业务事件发布逻辑，统一通过 Spring {@link ApplicationEventPublisher} 发布。
 * 事件发布使用 try-catch 隔离，确保事件发布失败不影响主事务。
 */
@Component
public class FinanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FinanceEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 构造函数注入
     *
     * @param applicationEventPublisher Spring 事件发布器
     */
    public FinanceEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布凭证过账完成事件
     *
     * @param voucherId   凭证ID
     * @param voucherNo   凭证号
     * @param totalAmount 合计金额（单位：分）
     */
    public void publishVoucherPosted(Long voucherId, String voucherNo, Long totalAmount) {
        try {
            VoucherPostedEvent event = new VoucherPostedEvent(voucherId, voucherNo, totalAmount);
            applicationEventPublisher.publishEvent(event);
            log.info("发布凭证过账事件：voucherId={}, voucherNo={}, totalAmount={}",
                    voucherId, voucherNo, totalAmount);
        } catch (Exception e) {
            // 事件发布失败不影响主事务
            log.error("发布凭证过账事件失败：voucherId={}, voucherNo={}", voucherId, voucherNo, e);
        }
    }

    /**
     * 发布付款完成事件
     *
     * @param paymentId  付款单ID
     * @param amount     付款金额（单位：分）
     * @param payeeType  收款方类型：supplier/employee
     * @param payeeId    收款方ID
     */
    public void publishPaymentCompleted(Long paymentId, Long amount, String payeeType, Long payeeId) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(paymentId, amount, payeeType, payeeId);
            applicationEventPublisher.publishEvent(event);
            log.info("发布付款完成事件：paymentId={}, amount={}, payeeType={}, payeeId={}",
                    paymentId, amount, payeeType, payeeId);
        } catch (Exception e) {
            // 事件发布失败不影响主事务
            log.error("发布付款完成事件失败：paymentId={}, payeeType={}, payeeId={}",
                    paymentId, payeeType, payeeId, e);
        }
    }

    /**
     * 发布预算超支事件
     *
     * @param budgetId     预算ID
     * @param budgetAmount 预算金额（单位：分）
     * @param actualAmount 实际金额（单位：分）
     * @param exceedRate   超支比例
     */
    public void publishBudgetExceeded(Long budgetId, Long budgetAmount, Long actualAmount, Double exceedRate) {
        try {
            BudgetExceededEvent event = new BudgetExceededEvent(budgetId, budgetAmount, actualAmount, exceedRate);
            applicationEventPublisher.publishEvent(event);
            log.info("发布预算超支事件：budgetId={}, budgetAmount={}, actualAmount={}, exceedRate={}",
                    budgetId, budgetAmount, actualAmount, exceedRate);
        } catch (Exception e) {
            // 事件发布失败不影响主事务
            log.error("发布预算超支事件失败：budgetId={}", budgetId, e);
        }
    }
}
