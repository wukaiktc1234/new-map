package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.mapper.InventoryCodeMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.SalesOrderMapper;
import com.foodtraceability.service.FinanceVoucherService;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.finance.ReceivableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SalesOrderServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-039（TDD）：验证 confirmDelivery() 在订单状态从 completed
 * 流转为 delivered 后，同事务调用 {@code ReceivableService.createForOrder()}
 * 创建应收账款。金额以分为单位（OICBE-B2-001 T-3 类型对齐后 SalesOrder.actualAmount
 * 为 Long，直接供 ReceivableService 使用），到期日为订单日 + 30 天。
 * 应收创建失败时整个出库事务回滚（强一致性，ADR-004）。</p>
 *
 * <p>测试位于 service.impl 包下。由于 SalesOrderServiceImpl
 * 继承 ServiceImpl，baseMapper 字段需通过 ReflectionTestUtils 注入。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SalesOrderServiceImpl 单元测试 - T-039 应收账款联动")
class SalesOrderServiceImplTest {

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryCodeMapper inventoryCodeMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private FinanceVoucherService financeVoucherService;

    @Mock
    private ReceivableService receivableService;

    @InjectMocks
    private SalesOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类 protected 字段，@InjectMocks 无法注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", salesOrderMapper);
    }

    /**
     * 构造一个可确认出库的 SalesOrder（状态 completed + 有效金额）
     * OICBE-B2-001/003：主键随表 String（T-1，setId(String)）、金额 Long（T-3）；
     * status 为 B-1 BLOCKED 字段（exist=false，仅内存断言，不参与 SQL），状态机映射待 PD-017 决策。
     */
    private SalesOrder buildDeliverableOrder(Long orderId, Long actualAmount) {
        SalesOrder order = new SalesOrder();
        order.setId(String.valueOf(orderId));
        order.setOrderNo("SO20260626001");
        order.setCustomerId(7001L);
        order.setCustomerName("张三");
        order.setTotalAmount(actualAmount);
        order.setActualAmount(actualAmount);
        order.setStatus("completed");
        order.setOrderTime(new Date(1769856000000L)); // 2026-06-26 00:00:00 UTC 固定值
        return order;
    }

    // ============================================================
    // 1. 主流程：confirmDelivery 调用 createForOrder 创建应收账款
    // ============================================================
    @Test
    @DisplayName("主流程：确认出库后调用 receivableService.createForOrder 创建应收账款")
    void confirmDelivery_success_callsCreateForOrder() {
        // given
        Long orderId = 8001L;
        Long actualAmount = 50000L; // 500元（单位：分）
        SalesOrder order = buildDeliverableOrder(orderId, actualAmount);
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);
        when(salesOrderMapper.updateById(any())).thenReturn(1);

        // when
        Boolean result = service.confirmDelivery(orderId);

        // then
        assertTrue(result, "确认出库应返回 true");

        // 验证状态流转为 delivered（B-1 状态机语义待 PD-017 决策；本断言仅验证 ServiceImpl 现状字符串状态流转逻辑不变）
        assertEquals("delivered", order.getStatus(), "订单状态应流转为 delivered（待 PD-017 状态机映射决策）");

        // 验证 receivableService.createForOrder 被调用且参数正确
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> customerIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> customerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Date> orderDateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(receivableService, times(1)).createForOrder(
                orderIdCaptor.capture(),
                customerIdCaptor.capture(),
                customerNameCaptor.capture(),
                amountCaptor.capture(),
                orderDateCaptor.capture()
        );
        assertAll("应收账款参数验证",
                () -> assertEquals(orderId, orderIdCaptor.getValue()),
                () -> assertEquals(7001L, customerIdCaptor.getValue()),
                () -> assertEquals("张三", customerNameCaptor.getValue()),
                () -> assertEquals(50000L, amountCaptor.getValue(), "金额应为订单实际金额（分）"),
                () -> assertEquals(order.getOrderTime(), orderDateCaptor.getValue())
        );
    }

    // ============================================================
    // 2. 强一致性：应收创建失败则整个出库事务回滚
    // ============================================================
    @Test
    @DisplayName("强一致性：receivableService.createForOrder 抛异常时 confirmDelivery 抛 BusinessException")
    void confirmDelivery_receivableCreateFails_throwsBusinessException() {
        // given
        Long orderId = 8002L;
        Long actualAmount = 30000L;
        SalesOrder order = buildDeliverableOrder(orderId, actualAmount);
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);
        when(salesOrderMapper.updateById(any())).thenReturn(1);

        // 模拟应收创建失败
        doThrow(new RuntimeException("应收账款DB连接失败"))
                .when(receivableService).createForOrder(
                        anyLong(), anyLong(), anyString(), anyLong(), any(Date.class));

        // when & then：confirmDelivery 应抛 BusinessException（事务回滚）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmDelivery(orderId));
        assertTrue(ex.getMessage().contains("创建应收账款失败"),
                "异常信息应说明应收账款创建失败");
    }

    // ============================================================
    // 3. 边界：订单金额无效（0 或 null）跳过应收创建
    // ============================================================
    @Test
    @DisplayName("边界：订单实际金额为0时跳过应收账款创建")
    void confirmDelivery_zeroAmount_skipsReceivableCreation() {
        // given
        Long orderId = 8003L;
        SalesOrder order = buildDeliverableOrder(orderId, 0L);
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);
        when(salesOrderMapper.updateById(any())).thenReturn(1);

        // when
        Boolean result = service.confirmDelivery(orderId);

        // then：金额为0，不应调用 createForOrder
        assertTrue(result, "出库仍应成功（金额无效仅跳过应收创建）");
        verify(receivableService, never()).createForOrder(
                anyLong(), anyLong(), anyString(), anyLong(), any(Date.class));
    }

    @Test
    @DisplayName("边界：订单实际金额为null时跳过应收账款创建")
    void confirmDelivery_nullAmount_skipsReceivableCreation() {
        // given
        Long orderId = 8004L;
        SalesOrder order = buildDeliverableOrder(orderId, null);
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);
        when(salesOrderMapper.updateById(any())).thenReturn(1);

        // when
        Boolean result = service.confirmDelivery(orderId);

        // then：金额为null，不应调用 createForOrder
        assertTrue(result, "出库仍应成功（金额无效仅跳过应收创建）");
        verify(receivableService, never()).createForOrder(
                anyLong(), anyLong(), anyString(), anyLong(), any(Date.class));
    }

    // ============================================================
    // 4. 订单不存在 / 状态非 completed 直接返回 false
    // ============================================================
    @Test
    @DisplayName("订单不存在：返回 false 且不调用应收创建")
    void confirmDelivery_orderNotFound_returnsFalse() {
        // given
        Long orderId = 8005L;
        when(salesOrderMapper.selectById(orderId)).thenReturn(null);

        // when
        Boolean result = service.confirmDelivery(orderId);

        // then
        assertFalse(result, "订单不存在时应返回 false");
        verify(receivableService, never()).createForOrder(
                anyLong(), anyLong(), anyString(), anyLong(), any(Date.class));
    }

    @Test
    @DisplayName("状态非 completed：返回 false 且不调用应收创建")
    void confirmDelivery_invalidStatus_returnsFalse() {
        // given
        Long orderId = 8006L;
        SalesOrder order = buildDeliverableOrder(orderId, 50000L);
        order.setStatus("preparing"); // 非 completed（B-1 状态机语义待 PD-017 决策，此处仅验证 ServiceImpl 现状判断逻辑）
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);

        // when
        Boolean result = service.confirmDelivery(orderId);

        // then
        assertFalse(result, "状态非 completed 时应返回 false");
        verify(receivableService, never()).createForOrder(
                anyLong(), anyLong(), anyString(), anyLong(), any(Date.class));
    }

    // ============================================================
    // 5. 幂等性：confirmDelivery 调用 createForOrder 由 ReceivableService 保证幂等
    //    此测试验证 SalesOrderServiceImpl 总是调用 createForOrder
    //    （ReceivableService 内部通过确定性 receivableNo 保证幂等，不在此层验证）
    // ============================================================
    @Test
    @DisplayName("幂等性：confirmDelivery 始终调用 createForOrder，幂等由 ReceivableService 保证")
    void confirmDelivery_alwaysCallsCreateForOrder_idempotencyByReceivableService() {
        // given
        Long orderId = 8007L;
        Long actualAmount = 80000L;
        SalesOrder order = buildDeliverableOrder(orderId, actualAmount);
        when(salesOrderMapper.selectById(orderId)).thenReturn(order);
        when(salesOrderMapper.updateById(any())).thenReturn(1);

        // when
        service.confirmDelivery(orderId);

        // then：SalesOrderServiceImpl 层只负责调用，幂等由 ReceivableService 保证
        verify(receivableService, times(1)).createForOrder(
                eq(orderId), anyLong(), anyString(), anyLong(), any(Date.class));
    }
}
