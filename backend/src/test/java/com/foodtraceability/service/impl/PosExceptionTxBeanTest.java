package com.foodtraceability.service.impl;

import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.mapper.ExceptionOpsMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.PosCashAccountResolver;
import com.foodtraceability.service.pos.PosExceptionService;
import com.foodtraceability.service.pos.ResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RT-3 / RT-4 [P1] 回归：异常单作废的状态机、并发、联动完整性。
 *
 * <p>三条被 R7 实测打穿的路径：</p>
 * <ol>
 *   <li>二次作废 → 唯一索引 {@code uk_exception_ops_order} 冲突 → 500 + 裸 SQL；</li>
 *   <li>厨房单取消失败被 try-warn 吞掉 → 订单已取消但后厨还在做；</li>
 *   <li>库存回补被跳过 → {@code foods.stock} 永久虚减。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class PosExceptionTxBeanTest {

    @Mock private OrderNewMapper orderNewMapper;
    @Mock private OrderPaymentRecordNewMapper paymentRecordMapper;
    @Mock private ExceptionOpsMapper exceptionOpsMapper;
    @Mock private PosExceptionOpsAttempt opsAttempt;
    @Mock private PosCashAccountResolver cashAccountResolver;
    @Mock private FundFlowService fundFlowService;
    @Mock private PosExceptionService self;
    @Mock private KitchenOrderMapper kitchenOrderMapper;
    @Mock private PosOrderStockRestorer stockRestorer;

    private PosExceptionTxBean txBean;

    @BeforeEach
    void setUp() {
        txBean = new PosExceptionTxBean(orderNewMapper, paymentRecordMapper, exceptionOpsMapper, opsAttempt,
                cashAccountResolver, fundFlowService, self, kitchenOrderMapper, stockRestorer);
    }

    private OrderNew pendingOrder() {
        OrderNew o = new OrderNew();
        o.setOrderId("O-T1");
        o.setDeleted(0);
        o.setShiftId(7L);
        o.setFinalAmount(1800L);
        return o;
    }

    @Test
    @DisplayName("RT-3 二次作废（casCancel=0 且当前状态=已取消）→ 409 文案，且不碰厨房/库存")
    void secondVoidReturnsCancelledMessage() {
        when(orderNewMapper.selectOne(any())).thenReturn(pendingOrder());
        when(exceptionOpsMapper.paymentStatusOf("O-T1")).thenReturn(0);
        when(exceptionOpsMapper.casCancel("O-T1")).thenReturn(0);
        when(exceptionOpsMapper.orderStatusOf("O-T1")).thenReturn(3);

        ResolveResult r = txBean.executeVoid("O-T1", "重复点击", null, "k-2", "1");

        assertEquals(ResolveResult.Status.ALREADY_HANDLED, r.getStatus(), "CAS 0 行必须判为已处理（→ HTTP 409）");
        assertEquals("订单已是【已取消】状态，无需重复作废", r.getMessage());
        verifyNoInteractions(kitchenOrderMapper);
        verifyNoInteractions(stockRestorer);
        verify(opsAttempt, never()).insertWithinSavepoint(any());
    }

    @Test
    @DisplayName("RT-4 作废成功 → 厨房单同事务取消 + 库存回补各一次")
    void voidCancelsKitchenAndRestoresStock() {
        when(orderNewMapper.selectOne(any())).thenReturn(pendingOrder());
        when(exceptionOpsMapper.paymentStatusOf("O-T1")).thenReturn(0);
        when(exceptionOpsMapper.casCancel("O-T1")).thenReturn(1);

        KitchenOrder ko = new KitchenOrder();
        ko.setId(11L);
        ko.setKitchenOrderId("KO-1");
        ko.setStatus("pending");
        when(kitchenOrderMapper.selectOne(any())).thenReturn(ko);
        when(kitchenOrderMapper.update(any(KitchenOrder.class), any())).thenReturn(1);
        when(stockRestorer.restoreForOrder("O-T1", "[EXC-VOID]")).thenReturn(2);

        ResolveResult r = txBean.executeVoid("O-T1", "顾客取消", "note", "k-3", "1");

        assertEquals(ResolveResult.Status.OK, r.getStatus());
        verify(kitchenOrderMapper, times(1)).update(any(KitchenOrder.class), any());
        verify(stockRestorer, times(1)).restoreForOrder("O-T1", "[EXC-VOID]");
        verify(opsAttempt, times(1)).insertWithinSavepoint(any());
    }

    @Test
    @DisplayName("RT-4 厨房单取消失败 → 抛 409 回滚整单（禁止 try-warn 吞掉）")
    void kitchenCancelFailureAbortsWholeVoid() {
        when(orderNewMapper.selectOne(any())).thenReturn(pendingOrder());
        when(exceptionOpsMapper.paymentStatusOf("O-T1")).thenReturn(0);
        when(exceptionOpsMapper.casCancel("O-T1")).thenReturn(1);

        KitchenOrder ko = new KitchenOrder();
        ko.setId(12L);
        ko.setKitchenOrderId("KO-2");
        ko.setStatus("making");
        when(kitchenOrderMapper.selectOne(any())).thenReturn(ko);
        when(kitchenOrderMapper.update(any(KitchenOrder.class), any())).thenReturn(0);
        when(kitchenOrderMapper.selectById(12L)).thenReturn(ko);   // 仍是 making：真失败

        com.foodtraceability.common.exception.BusinessException ex = assertThrows(
                com.foodtraceability.common.exception.BusinessException.class,
                () -> txBean.executeVoid("O-T1", "顾客取消", null, "k-4", "1"));

        assertEquals(409, ex.getHttpStatus(), "厨房联动失败必须是 409 业务冲突");
        assertTrue(ex.getMessage().contains("已回滚"), "文案要告诉用户本次作废未生效");
        verify(stockRestorer, never()).restoreForOrder(anyString(), anyString());
        verify(opsAttempt, never()).insertWithinSavepoint(any());
    }

    @Test
    @DisplayName("RT-3 唯一键冲突 → 规范业务 409，文案不含底层 SQL")
    void uniqueKeyConflictBecomesBusiness409() {
        when(orderNewMapper.selectOne(any())).thenReturn(pendingOrder());
        when(exceptionOpsMapper.paymentStatusOf("O-T1")).thenReturn(0);
        when(exceptionOpsMapper.casCancel("O-T1")).thenReturn(1);
        when(kitchenOrderMapper.selectOne(any())).thenReturn(null);
        when(stockRestorer.restoreForOrder(eq("O-T1"), anyString())).thenReturn(0);
        doThrow(new DataIntegrityViolationException(
                "### Error updating database. Cause: 重复键违反唯一约束 uk_exception_ops_order"))
                .when(opsAttempt).insertWithinSavepoint(any());

        com.foodtraceability.common.exception.BusinessException ex = assertThrows(
                com.foodtraceability.common.exception.BusinessException.class,
                () -> txBean.executeVoid("O-T1", "顾客取消", null, "k-5", "1"));

        assertEquals(409, ex.getHttpStatus());
        assertFalse(ex.getMessage().contains("uk_exception_ops_order"), "底层约束名不得透出到用户");
        assertFalse(ex.getMessage().contains("INSERT INTO"), "底层 SQL 不得透出到用户");
    }
}
