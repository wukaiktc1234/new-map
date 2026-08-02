package com.foodtraceability.event.listener;

import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.event.SalaryPaidEvent;
import com.foodtraceability.service.finance.AutoVoucherService;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资发放事件监听器
 * 处理薪资发放后的数据联动
 *
 * <p>ADR-004 事件隔离策略：使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * 确保主事务（薪资发放）提交后才触发联动处理，避免主事务回滚后联动副作用残留。
 * 配合 {@code @Async} 在独立线程池执行，不阻塞主事务。</p>
 */
@Component
public class SalaryPaidEventListener {

    private static final Logger log = LoggerFactory.getLogger(SalaryPaidEventListener.class);

    /** 元转分的乘数（SalaryPaidEvent.actualSalary 为 BigDecimal 元，需转为 Long 分） */
    private static final BigDecimal YUAN_TO_FEN = BigDecimal.valueOf(100L);

    public SalaryPaidEventListener(CostRecordService costRecordService, AutoVoucherService autoVoucherService) {
        this.costRecordService = costRecordService;
        this.autoVoucherService = autoVoucherService;
    }

    private final CostRecordService costRecordService;

    private final AutoVoucherService autoVoucherService;

    /**
     * 监听薪资发放事件
     * 1. 记录财务支出（生成薪资凭证）
     * 2. 记录人工成本
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSalaryPaidEvent(SalaryPaidEvent event) {
        log.info("处理薪资发放事件: employeeId={}, employeeName={}, amount={}",
                event.getEmployeeId(), event.getEmployeeName(), event.getActualSalary());

        try {
            recordFinanceExpense(event);

            recordLaborCost(event);

            log.info("薪资发放事件处理完成: employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("处理薪资发放事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录财务支出（T-044 修复：调用 AutoVoucherService 生成薪资凭证）
     *
     * <p>Sprint 3.1 P0 T-044：原实现仅打 log 未调用任何 Service，
     * 现注入 {@link AutoVoucherService} 并调用
     * {@code generateSalaryVoucher(salaryRecordId, amountInCents)} 生成薪资凭证。</p>
     *
     * <p>金额转换：{@code SalaryPaidEvent.actualSalary} 为 BigDecimal（元），
     * {@code generateSalaryVoucher} 入参为 Long（分），通过 {@code * 100} 转换。</p>
     *
     * <p>异常隔离（ADR-004）：本方法内部 try-catch，仅 log.error 不重抛，
     * 不影响 {@link #recordLaborCost(SalaryPaidEvent)} 的执行。</p>
     *
     * @param event 薪资发放事件
     */
    private void recordFinanceExpense(SalaryPaidEvent event) {
        log.info("记录财务支出: employeeId={}, salaryRecordId={}, amount={}",
                event.getEmployeeId(), event.getSalaryRecordId(), event.getActualSalary());

        // 边界保护：salaryRecordId 或 actualSalary 为 null 时跳过
        if (event.getSalaryRecordId() == null) {
            log.warn("salaryRecordId 为空，跳过薪资凭证生成，employeeId={}", event.getEmployeeId());
            return;
        }
        if (event.getActualSalary() == null) {
            log.warn("actualSalary 为空，跳过薪资凭证生成，employeeId={}", event.getEmployeeId());
            return;
        }

        try {
            // 元（BigDecimal）→ 分（Long）转换
            Long amountInCents = event.getActualSalary().multiply(YUAN_TO_FEN).longValue();
            autoVoucherService.generateSalaryVoucher(event.getSalaryRecordId(), amountInCents);
            log.info("财务支出（薪资凭证）记录成功: employeeId={}, salaryRecordId={}, amountFen={}",
                    event.getEmployeeId(), event.getSalaryRecordId(), amountInCents);
        } catch (Exception e) {
            // 异常隔离：仅记录日志，不重抛，不影响后续 recordLaborCost 执行
            log.error("记录财务支出（薪资凭证）失败: employeeId={}, salaryRecordId={}, 错误={}",
                    event.getEmployeeId(), event.getSalaryRecordId(), e.getMessage(), e);
        }
    }

    /**
     * 记录人工成本
     */
    private void recordLaborCost(SalaryPaidEvent event) {
        log.info("记录人工成本: employeeId={}, amount={}",
                event.getEmployeeId(), event.getActualSalary());

        try {
            CostRecordCreateDTO dto = new CostRecordCreateDTO();
            dto.setCostType(2); // 2-人工成本
            dto.setCostCenterId(event.getStoreId());
            // 金额：元（BigDecimal）转分（Long）
            BigDecimal amountYuan = event.getActualSalary() != null
                    ? event.getActualSalary() : BigDecimal.ZERO;
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            // 成本归属期间：取支付时间的 yyyy-MM
            LocalDate costDate = event.getPaymentTime() != null
                    ? event.getPaymentTime().toLocalDate() : LocalDate.now();
            dto.setPeriod(costDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            // 备注：员工姓名 + 部门
            String remark = "人工成本 - 员工：" + event.getEmployeeName();
            if (event.getDepartmentName() != null) {
                remark += "，部门：" + event.getDepartmentName();
            }
            dto.setRemark(remark);
            dto.setCalculationMethod(1); // 1-实际发生

            costRecordService.create(dto);

            log.info("人工成本记录成功: employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("记录人工成本失败: {}", e.getMessage(), e);
        }
    }
}
