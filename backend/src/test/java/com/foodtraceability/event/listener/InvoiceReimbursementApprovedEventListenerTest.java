package com.foodtraceability.event.listener;

import com.foodtraceability.event.InvoiceReimbursementApprovedEvent;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * InvoiceReimbursementApprovedEventListener 单元测试
 *
 * <p>Sprint 3.1 P0 T-042（TDD）：验证审批通过事件监听器正常调用
 * {@code AutoVoucherService.generateReimbursementVoucher} 与
 * {@code CostRecordService.recordReimbursementCost}，且任一动作抛异常时
 * 被隔离不影响其他动作或主流程（ADR-004 事件隔离策略）。</p>
 *
 * <p>测试参考 {@code StoreDailySettlementEventListenerTest} 的结构：
 * 正常调用 / 异常隔离 / 异常不阻断后续动作。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoiceReimbursementApprovedEventListener 单元测试")
class InvoiceReimbursementApprovedEventListenerTest {

    @Mock
    private AutoVoucherService autoVoucherService;

    @Mock
    private CostRecordService costRecordService;

    @InjectMocks
    private InvoiceReimbursementApprovedEventListener listener;

    /** 构造一个最小可用的事件 */
    private InvoiceReimbursementApprovedEvent buildEvent() {
        return new InvoiceReimbursementApprovedEvent(
                this,
                7001L,                          // reimbursementId
                "RE20260626001",                // reimbursementNo
                1001L,                          // applicantId
                2001L,                          // departmentId
                "财务部",                        // departmentName
                "差旅费",                        // reimbursementType
                90000L,                         // approvedAmount（分）
                8001L,                          // approverId
                LocalDateTime.of(2026, 6, 26, 14, 30, 0)  // approveTime
        );
    }

    // ============================================================
    // 1. 正常调用：验证下游两个 Service 方法均被调用且参数正确
    // ============================================================
    @Test
    @DisplayName("正常调用：生成凭证 + 归集成本两个动作均执行且参数正确")
    void handleEvent_normalFlow_invokesBothServices() {
        // given
        InvoiceReimbursementApprovedEvent event = buildEvent();

        // when
        listener.handleEvent(event);

        // then：验证生成凭证调用
        verify(autoVoucherService, times(1)).generateReimbursementVoucher(event);

        // then：验证归集成本调用及其参数
        ArgumentCaptor<Long> reimbursementIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> departmentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDate> occurDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(costRecordService, times(1)).recordReimbursementCost(
                reimbursementIdCaptor.capture(),
                departmentIdCaptor.capture(),
                typeCaptor.capture(),
                amountCaptor.capture(),
                occurDateCaptor.capture()
        );
        assertAll("归集成本参数验证",
                () -> assertEquals(7001L, reimbursementIdCaptor.getValue()),
                () -> assertEquals(2001L, departmentIdCaptor.getValue()),
                () -> assertEquals("差旅费", typeCaptor.getValue()),
                () -> assertEquals(90000L, amountCaptor.getValue()),
                () -> assertEquals(LocalDate.of(2026, 6, 26), occurDateCaptor.getValue(),
                        "occurDate 应取自 approveTime.toLocalDate()")
        );
    }

    // ============================================================
    // 2. 异常隔离：凭证生成抛异常不重抛，不影响主流程
    // ============================================================
    @Test
    @DisplayName("异常隔离：生成凭证抛异常时被隔离，主流程不抛且成本归集仍执行")
    void handleEvent_voucherThrowsException_isolatedAndCostStillRuns() {
        // given
        InvoiceReimbursementApprovedEvent event = buildEvent();
        doThrow(new RuntimeException("凭证生成失败"))
                .when(autoVoucherService).generateReimbursementVoucher(event);

        // when & then：监听器不应抛异常
        assertDoesNotThrow(() -> listener.handleEvent(event));

        // 凭证生成抛异常后，成本归集仍应被执行（异常隔离独立处理）
        verify(costRecordService, times(1)).recordReimbursementCost(
                eq(7001L), eq(2001L), eq("差旅费"), eq(90000L), any(LocalDate.class));
    }

    // ============================================================
    // 3. 异常隔离：成本归集抛异常不重抛，不影响主流程
    // ============================================================
    @Test
    @DisplayName("异常隔离：成本归集抛异常时被隔离，主流程不抛")
    void handleEvent_costThrowsException_isolated() {
        // given
        InvoiceReimbursementApprovedEvent event = buildEvent();
        doThrow(new RuntimeException("成本归集失败"))
                .when(costRecordService).recordReimbursementCost(
                        anyLong(), anyLong(), anyString(), anyLong(), any(LocalDate.class));

        // when & then：监听器不应抛异常
        assertDoesNotThrow(() -> listener.handleEvent(event));

        // 凭证生成仍应被调用（先执行，与成本归集失败无关）
        verify(autoVoucherService, times(1)).generateReimbursementVoucher(event);
    }

    // ============================================================
    // 4. approveTime 为 null：occurDate 兜底为 LocalDate.now()
    // ============================================================
    @Test
    @DisplayName("边界：approveTime 为 null 时 occurDate 兜底取 LocalDate.now()")
    void handleEvent_nullApproveTime_fallbackToToday() {
        // given：构造 approveTime 为 null 的事件
        InvoiceReimbursementApprovedEvent event = new InvoiceReimbursementApprovedEvent(
                this, 7002L, "RE20260626002", 1001L, 2001L, "财务部", "办公费",
                50000L, 8001L, null
        );

        // when
        listener.handleEvent(event);

        // then：occurDate 应为 LocalDate.now()（无法精确断言日期，仅校验非 null 且调用发生）
        verify(costRecordService, times(1)).recordReimbursementCost(
                eq(7002L), eq(2001L), eq("办公费"), eq(50000L), any(LocalDate.class));
    }
}
