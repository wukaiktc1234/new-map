package com.foodtraceability.event.listener;

import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.event.SalaryPaidEvent;
import com.foodtraceability.service.finance.AutoVoucherService;
import com.foodtraceability.service.finance.CostRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SalaryPaidEventListener 单元测试
 *
 * <p>Sprint 3.1 P0 T-044（TDD）：验证薪资发放事件监听器
 * {@code recordFinanceExpense()} 调用
 * {@code AutoVoucherService.generateSalaryVoucher()} 生成薪资凭证，
 * 且异常被隔离不影响主流程或其他动作（ADR-004 事件隔离策略）。</p>
 *
 * <p>修复前 {@code recordFinanceExpense()} 仅打 log 未调用任何 Service，
 * 修复后注入 {@link AutoVoucherService} 并调用 {@code generateSalaryVoucher}。</p>
 *
 * <p>金额转换：{@code SalaryPaidEvent.actualSalary} 为 BigDecimal（元），
 * {@code generateSalaryVoucher} 入参为 Long（分），需 {@code * 100} 转换。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SalaryPaidEventListener 单元测试 - T-044 薪资凭证联动")
class SalaryPaidEventListenerTest {

    @Mock
    private CostRecordService costRecordService;

    @Mock
    private AutoVoucherService autoVoucherService;

    @InjectMocks
    private SalaryPaidEventListener listener;

    /** 构造一个最小可用的薪资发放事件 */
    private SalaryPaidEvent buildEvent() {
        SalaryPaidEvent event = new SalaryPaidEvent();
        event.setEventId("EVT-20260626001");
        event.setSalaryRecordId(9001L);
        event.setEmployeeId(1001L);
        event.setEmployeeName("张三");
        event.setDepartmentId(2001L);
        event.setDepartmentName("财务部");
        event.setStoreId(3001L);
        event.setStoreName("总店");
        // actualSalary 以元为单位（BigDecimal），对应 5000.00 元
        event.setActualSalary(new BigDecimal("5000.00"));
        event.setPaymentTime(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
        return event;
    }

    // ============================================================
    // 1. 主流程：recordFinanceExpense 调用 generateSalaryVoucher
    // ============================================================
    @Test
    @DisplayName("主流程：薪资发放事件触发 generateSalaryVoucher 调用且参数正确")
    void handleSalaryPaidEvent_normalFlow_invokesGenerateSalaryVoucher() {
        // given
        SalaryPaidEvent event = buildEvent();

        // when
        listener.handleSalaryPaidEvent(event);

        // then：验证 generateSalaryVoucher 被调用且参数正确
        ArgumentCaptor<Long> salaryRecordIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(autoVoucherService, times(1)).generateSalaryVoucher(
                salaryRecordIdCaptor.capture(),
                amountCaptor.capture()
        );
        assertEquals(9001L, salaryRecordIdCaptor.getValue(), "salaryRecordId 应为事件中的工资记录ID");
        // 5000.00 元 → 500000 分
        assertEquals(500000L, amountCaptor.getValue(), "金额应为元转分后的值（5000.00 元 → 500000 分）");
    }

    // ============================================================
    // 2. 异常隔离：generateSalaryVoucher 抛异常不影响 recordLaborCost
    // ============================================================
    @Test
    @DisplayName("异常隔离：generateSalaryVoucher 抛异常时 recordLaborCost 仍被调用")
    void handleSalaryPaidEvent_voucherThrowsException_recordLaborCostStillCalled() {
        // given
        SalaryPaidEvent event = buildEvent();
        doThrow(new RuntimeException("凭证生成DB连接失败"))
                .when(autoVoucherService).generateSalaryVoucher(anyLong(), anyLong());

        // when & then：异常被隔离，主流程不抛出
        assertDoesNotThrow(() -> listener.handleSalaryPaidEvent(event));

        // then：尽管凭证生成失败，人工成本记录仍应被调用
        verify(costRecordService, times(1)).create(any(CostRecordCreateDTO.class));
    }

    // ============================================================
    // 3. 异常隔离：recordLaborCost 抛异常不影响 generateSalaryVoucher
    // ============================================================
    @Test
    @DisplayName("异常隔离：recordLaborCost 抛异常时 generateSalaryVoucher 仍被调用")
    void handleSalaryPaidEvent_laborCostThrowsException_voucherStillCalled() {
        // given
        SalaryPaidEvent event = buildEvent();
        doThrow(new RuntimeException("人工成本记录DB连接失败"))
                .when(costRecordService).create(any(CostRecordCreateDTO.class));

        // when & then：异常被隔离，主流程不抛出
        assertDoesNotThrow(() -> listener.handleSalaryPaidEvent(event));

        // then：尽管人工成本记录失败，凭证生成仍应被调用
        verify(autoVoucherService, times(1)).generateSalaryVoucher(anyLong(), anyLong());
    }

    // ============================================================
    // 4. 边界：actualSalary 为 null 时跳过凭证生成
    // ============================================================
    @Test
    @DisplayName("边界：actualSalary 为 null 时跳过凭证生成但不抛异常")
    void handleSalaryPaidEvent_nullActualSalary_skipsVoucherGeneration() {
        // given
        SalaryPaidEvent event = buildEvent();
        event.setActualSalary(null);

        // when & then：不抛异常
        assertDoesNotThrow(() -> listener.handleSalaryPaidEvent(event));

        // then：actualSalary 为 null，不应调用 generateSalaryVoucher
        verify(autoVoucherService, never()).generateSalaryVoucher(anyLong(), anyLong());
    }

    // ============================================================
    // 5. 边界：salaryRecordId 为 null 时跳过凭证生成
    // ============================================================
    @Test
    @DisplayName("边界：salaryRecordId 为 null 时跳过凭证生成但不抛异常")
    void handleSalaryPaidEvent_nullSalaryRecordId_skipsVoucherGeneration() {
        // given
        SalaryPaidEvent event = buildEvent();
        event.setSalaryRecordId(null);

        // when & then：不抛异常
        assertDoesNotThrow(() -> listener.handleSalaryPaidEvent(event));

        // then：salaryRecordId 为 null，不应调用 generateSalaryVoucher
        verify(autoVoucherService, never()).generateSalaryVoucher(anyLong(), anyLong());
    }
}
