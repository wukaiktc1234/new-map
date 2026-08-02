package com.foodtraceability.event.listener;

import com.foodtraceability.event.StoreDailySettlementCompletedEvent;
import com.foodtraceability.service.finance.AutoVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StoreDailySettlementEventListener 单元测试
 *
 * <p>Sprint 3.1 P0 T-036（TDD）：验证监听器收到门店日结完成事件后
 * 调用 {@code autoVoucherService.generateStoreSettlementVoucher(event)}，
 * 且异常被 try-catch 隔离不向外抛出。</p>
 *
 * <p>幂等性由 {@code AutoVoucherService} 内部保证，监听器不重复实现。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreDailySettlementEventListenerTest {

    @Mock
    private AutoVoucherService autoVoucherService;

    private StoreDailySettlementEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new StoreDailySettlementEventListener(autoVoucherService);
    }

    /**
     * 构建门店日结完成事件
     */
    private StoreDailySettlementCompletedEvent buildEvent(Long settlementId,
                                                          Long totalIncome,
                                                          Long totalExpense) {
        return new StoreDailySettlementCompletedEvent(
                this,
                settlementId,
                2001L,
                "测试门店",
                LocalDate.of(2026, 6, 26),
                totalIncome,
                totalExpense,
                totalIncome - totalExpense,
                3001L,
                LocalDateTime.of(2026, 6, 26, 22, 0, 0)
        );
    }

    @Test
    @DisplayName("T-036 监听器收到事件后调用 autoVoucherService.generateStoreSettlementVoucher")
    void handleEvent_normal_callsGenerateStoreSettlementVoucher() {
        StoreDailySettlementCompletedEvent event = buildEvent(1001L, 10000L, 5000L);

        listener.handleEvent(event);

        verify(autoVoucherService).generateStoreSettlementVoucher(event);
    }

    @Test
    @DisplayName("T-036 autoVoucherService 抛异常时监听器不重抛（异常隔离）")
    void handleEvent_serviceThrowsException_doesNotRethrow() {
        StoreDailySettlementCompletedEvent event = buildEvent(1002L, 10000L, 5000L);
        when(autoVoucherService.generateStoreSettlementVoucher(event))
                .thenThrow(new RuntimeException("模拟凭证生成失败"));

        // 监听器应吞掉异常，不向外抛出
        assertDoesNotThrow(() -> listener.handleEvent(event));

        verify(autoVoucherService).generateStoreSettlementVoucher(event);
    }

    @Test
    @DisplayName("T-036 监听器对每个事件都调用一次凭证生成（幂等由Service保证）")
    void handleEvent_eachEventTriggersOneCall() {
        StoreDailySettlementCompletedEvent event = buildEvent(1003L, 8000L, 3000L);

        listener.handleEvent(event);

        verify(autoVoucherService, never()).generateReimbursementVoucher(org.mockito.ArgumentMatchers.any());
        verify(autoVoucherService).generateStoreSettlementVoucher(event);
    }
}
