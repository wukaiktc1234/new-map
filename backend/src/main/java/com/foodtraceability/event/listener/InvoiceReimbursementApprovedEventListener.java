package com.foodtraceability.event.listener;

import com.foodtraceability.event.InvoiceReimbursementApprovedEvent;
import com.foodtraceability.service.finance.AutoVoucherService;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * 发票报销审批通过事件监听器（F-011 联动）
 *
 * <p>Sprint 3.1 P0 T-042：监听 {@link InvoiceReimbursementApprovedEvent}，
 * 在报销审批主事务提交后（AFTER_COMMIT）异步执行两项联动动作：</p>
 * <ol>
 *   <li>调用 {@code AutoVoucherService.generateReimbursementVoucher(event)} 生成财务凭证；</li>
 *   <li>调用 {@code CostRecordService.recordReimbursementCost(...)} 将报销金额按部门归集到成本记录表
 *       （costType=7 其他成本）。</li>
 * </ol>
 *
 * <p>设计要点（ADR-004 事件隔离策略）：</p>
 * <ul>
 *   <li>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} 确保审批主事务提交成功后才消费事件，
 *       避免主事务回滚后仍生成凭证/记录成本导致数据不一致。</li>
 *   <li>{@code @Async} 异步执行，不阻塞审批主事务提交。</li>
 *   <li>异常 try-catch 隔离，仅 {@code log.error} 记录，不重抛，
 *       避免事件消费失败影响其他监听器或主流程。</li>
 *   <li>幂等性由下游 Service 内部保证：
 *       <ul>
 *         <li>{@code AutoVoucherService} 通过 {@code existsBySource(SOURCE_TYPE_REIMBURSEMENT, reimbursementId)} 判重；</li>
 *         <li>{@code CostRecordService.recordReimbursementCost} 内部通过
 *             {@code relatedVoucherId == reimbursementId} 查询判重。</li>
 *       </ul>
 *   </li>
 *   <li>构造函数注入 {@code AutoVoucherService} + {@code CostRecordService}（finance 包下的新接口）。</li>
 * </ul>
 *
 * @see StoreDailySettlementEventListener 同类参考实现
 */
@Component
public class InvoiceReimbursementApprovedEventListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReimbursementApprovedEventListener.class);

    private final AutoVoucherService autoVoucherService;
    private final CostRecordService costRecordService;

    /**
     * 构造函数注入
     *
     * @param autoVoucherService 自动凭证生成Service（用于生成报销凭证）
     * @param costRecordService  成本记录Service（finance 包下的新接口，用于归集部门成本）
     */
    public InvoiceReimbursementApprovedEventListener(AutoVoucherService autoVoucherService,
                                                     CostRecordService costRecordService) {
        this.autoVoucherService = autoVoucherService;
        this.costRecordService = costRecordService;
    }

    /**
     * 处理发票报销审批通过事件
     *
     * <p>主事务提交后异步执行：生成财务凭证 + 归集部门成本。
     * 任意一步抛异常均被 try-catch 隔离，不影响主流程与其他监听器。</p>
     *
     * @param event 发票报销审批通过事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(InvoiceReimbursementApprovedEvent event) {
        log.info("处理发票报销审批通过事件：报销单ID={}，单号={}，部门={}，类型={}，金额={}分",
                event.getReimbursementId(), event.getReimbursementNo(),
                event.getDepartmentName(), event.getReimbursementType(),
                event.getApprovedAmount());

        // 1. 生成财务凭证（联动 F-001 → F-008 凭证体系）
        try {
            autoVoucherService.generateReimbursementVoucher(event);
            log.info("发票报销凭证生成成功：报销单ID={}", event.getReimbursementId());
        } catch (Exception e) {
            log.error("生成发票报销凭证失败：报销单ID={}，错误：{}",
                    event.getReimbursementId(), e.getMessage(), e);
            // 异常隔离：不重抛，继续执行后续联动动作
        }

        // 2. 归集部门成本（联动 F-001 → F-007 成本核算）
        try {
            LocalDate occurDate = event.getApproveTime() != null
                    ? event.getApproveTime().toLocalDate()
                    : LocalDate.now();
            costRecordService.recordReimbursementCost(
                    event.getReimbursementId(),
                    event.getDepartmentId(),
                    event.getReimbursementType(),
                    event.getApprovedAmount(),
                    occurDate
            );
            log.info("发票报销成本归集成功：报销单ID={}，部门ID={}",
                    event.getReimbursementId(), event.getDepartmentId());
        } catch (Exception e) {
            log.error("归集发票报销成本失败：报销单ID={}，错误：{}",
                    event.getReimbursementId(), e.getMessage(), e);
            // 异常隔离：不重抛，避免影响其他监听器或主流程
        }
    }
}
