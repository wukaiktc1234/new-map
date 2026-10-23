package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.CustomerOrderDTO;
import com.foodtraceability.dto.OrderDetailDTO;
import com.foodtraceability.dto.OrderQueryDTO;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.Order;
import com.foodtraceability.entity.OrderItem;
import com.foodtraceability.entity.OrderItemNew;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderItemMapper;
import com.foodtraceability.mapper.OrderItemNewMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.service.ReceiptPrinterService;
import com.foodtraceability.service.SensitiveDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PosOrderQueryServiceImpl 单元测试（POS 主读路径：orders 新表优先 + orders_legacy 兜底）。
 *
 * <p>背景：POS 快速下单写入 {@code orders}（OrderNew），而查询曾固定读 {@code orders_legacy}，
 * 导致「下的单查不到」。本类固化改造后的两条路径：新表命中、旧表兜底，以及金额「分→元」换算。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosOrderQueryServiceImplTest {

    @Mock private KitchenOrderMapper kitchenOrderMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderNewMapper orderNewMapper;
    @Mock private OrderItemNewMapper orderItemNewMapper;
    @Mock private OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    @Mock private SensitiveDataService sensitiveDataService;
    @Mock private ReceiptPrinterService receiptPrinterService;

    @InjectMocks
    private PosOrderQueryServiceImpl service;

    // ==================== fixtures ====================

    private static OrderNew newOrder(String orderId, String orderNumber) {
        OrderNew o = new OrderNew();
        o.setOrderId(orderId);
        o.setOrderCode("ORD" + orderId);
        o.setOrderNumber(orderNumber);
        o.setOrderStatus(1);
        o.setPaymentStatus(2);
        o.setTotalAmount(1800L);
        o.setFinalAmount(1800L);
        o.setPaidAmount(1800L);
        o.setCreateTime(LocalDateTime.of(2026, 10, 23, 11, 0));
        return o;
    }

    private static OrderItemNew newItem(String orderId) {
        OrderItemNew i = new OrderItemNew();
        i.setItemId("IT1");
        i.setOrderId(orderId);
        i.setProductName("QA-红烧土豆");
        i.setUnitPrice(1800L);
        i.setQuantity(1);
        return i;
    }

    private static OrderPaymentRecordNew payment(String orderId) {
        OrderPaymentRecordNew p = new OrderPaymentRecordNew();
        p.setPaymentId(1L);
        p.setOrderId(orderId);
        p.setPaymentMethod(2); // 新表支付方式编码：2=现金（与旧表不同，经 convertPaymentMethodToString 输出「现金」）
        p.setPaymentAmount(1800L);
        p.setTransactionNo("TX-1");
        p.setPaymentTime(LocalDateTime.of(2026, 10, 23, 11, 1));
        return p;
    }

    private static Order legacyOrder(String orderId, String orderNumber) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setOrderNumber(orderNumber);
        o.setOrderStatus(1);
        o.setActualAmount(new BigDecimal("18.00"));
        o.setCreateTime(LocalDateTime.of(2026, 9, 1, 9, 0));
        return o;
    }

    // ==================== getOrderDetail ====================

    @Test
    @DisplayName("getOrderDetail：命中 orders 新表 —— 金额分转元、明细与支付方式来自新表")
    void getOrderDetail_newTable() {
        OrderNew order = newOrder("O1", "T20261023001");
        when(orderNewMapper.selectOne(any())).thenReturn(order);
        when(orderPaymentRecordNewMapper.selectByOrderId("O1")).thenReturn(List.of(payment("O1")));
        when(orderItemNewMapper.selectList(any())).thenReturn(List.of(newItem("O1")));
        when(kitchenOrderMapper.selectOne(any())).thenReturn(null);

        Result<OrderDetailDTO> result = service.getOrderDetail("T20261023001");

        assertEquals(0, result.getCode());
        OrderDetailDTO dto = result.getData();
        assertNotNull(dto);
        assertEquals("T20261023001", dto.getOrderNumber());
        assertEquals(0, new BigDecimal("18.0").compareTo(dto.getTotalAmount()), "1800 分 → 18.00 元");
        assertEquals("confirmed", dto.getStatus(), "新表 order_status=1 表示已确认");
        assertEquals("现金", dto.getPaymentMethod());
        assertEquals("TX-1", dto.getTransactionId());
        assertEquals(1, dto.getItems().size());
        assertEquals("QA-红烧土豆", dto.getItems().get(0).getName());
        assertEquals(0, new BigDecimal("18.0").compareTo(dto.getItems().get(0).getPrice()));
        verify(orderMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("getOrderDetail：新表没有该单时回落 orders_legacy（历史单仍可查）")
    void getOrderDetail_fallsBackToLegacy() {
        when(orderNewMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectOne(any())).thenReturn(legacyOrder("L1", "T20260901001"));
        OrderItem legacyItem = new OrderItem();
        legacyItem.setFoodName("旧菜品");
        legacyItem.setUnitPrice(new BigDecimal("9.90"));
        legacyItem.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(legacyItem));
        when(kitchenOrderMapper.selectOne(any())).thenReturn(null);

        Result<OrderDetailDTO> result = service.getOrderDetail("T20260901001");

        assertEquals(0, result.getCode());
        assertEquals("T20260901001", result.getData().getOrderNumber());
        assertEquals("paid", result.getData().getStatus(), "旧表 order_status=1 语义是已支付");
        assertEquals(1, result.getData().getItems().size());
    }

    @Test
    @DisplayName("getOrderDetail：两边都查不到返回「订单不存在」")
    void getOrderDetail_notFound() {
        when(orderNewMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectOne(any())).thenReturn(null);

        Result<OrderDetailDTO> result = service.getOrderDetail("NOPE");

        assertNotEquals(0, result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }

    @Test
    @DisplayName("getOrderDetail：空入参直接走「订单不存在」，不炸")
    void getOrderDetail_nullKey() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        Result<OrderDetailDTO> result = service.getOrderDetail(null);
        assertEquals("订单不存在", result.getMessage());
    }

    // ==================== getOrders ====================

    @Test
    @DisplayName("getOrders：新表有数据时只查新表，旧表零查询")
    void getOrders_newTableOnly() {
        when(orderNewMapper.selectList(any())).thenReturn(List.of(newOrder("O1", "T1")));

        Result<List<OrderQueryDTO>> result = service.getOrders(null, null, null);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("T1", result.getData().get(0).getOrderNumber());
        assertEquals(0, new BigDecimal("18.0").compareTo(result.getData().get(0).getTotalAmount()));
        verify(orderMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getOrders：新表为空时回落旧表")
    void getOrders_fallsBackToLegacy() {
        when(orderNewMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(orderMapper.selectList(any())).thenReturn(List.of(legacyOrder("L1", "T20260901001")));

        Result<List<OrderQueryDTO>> result = service.getOrders("2026-09-01", "2026-09-30", "T2026");

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("T20260901001", result.getData().get(0).getOrderNumber());
        verify(orderMapper).selectList(any());
    }

    // ==================== getOrdersByTable ====================

    @Test
    @DisplayName("getOrdersByTable：桌台无后厨单时返回空列表，不查订单表")
    void getOrdersByTable_noKitchenOrders() {
        when(kitchenOrderMapper.selectList(any())).thenReturn(new ArrayList<>());

        Result<List<CustomerOrderDTO>> result = service.getOrdersByTable("1");

        assertEquals(0, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(orderNewMapper, never()).selectList(any());
        verify(orderMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("getOrdersByTable：新表命中在途单")
    void getOrdersByTable_newTable() {
        KitchenOrder ko = new KitchenOrder();
        ko.setOrderNumber("T1");
        ko.setTableNumber("1");
        ko.setTotalDishes(2);
        when(kitchenOrderMapper.selectList(any())).thenReturn(List.of(ko));
        when(orderNewMapper.selectList(any())).thenReturn(List.of(newOrder("O1", "T1")));
        when(orderItemNewMapper.selectList(any())).thenReturn(List.of(newItem("O1")));

        Result<List<CustomerOrderDTO>> result = service.getOrdersByTable("1");

        assertEquals(1, result.getData().size());
        assertEquals("T1", result.getData().get(0).getOrderNumber());
        assertEquals(1, result.getData().get(0).getItems().size());
    }

    // ==================== reprintReceipt ====================

    @Test
    @DisplayName("reprintReceipt：新表订单未支付（payment_status!=2）拒绝重打")
    void reprint_newOrderUnpaid_rejected() {
        OrderNew order = newOrder("O1", "T1");
        order.setPaymentStatus(1);
        when(orderNewMapper.selectOne(any())).thenReturn(order);

        Result<Boolean> result = service.reprintReceipt("O1");

        assertNotEquals(0, result.getCode());
        assertEquals("仅支持重打已支付的订单小票", result.getMessage());
        verify(receiptPrinterService, never()).reprintReceipt(any());
    }

    @Test
    @DisplayName("reprintReceipt：新表已支付订单走打印并返回打印机结果")
    void reprint_newOrderPaid_prints() {
        OrderNew order = newOrder("O1", "T1");
        when(orderNewMapper.selectOne(any())).thenReturn(order);
        when(kitchenOrderMapper.selectOne(any())).thenReturn(null);
        when(orderItemNewMapper.selectList(any())).thenReturn(List.of(newItem("O1")));
        when(receiptPrinterService.reprintReceipt(any())).thenReturn(true);

        Result<Boolean> result = service.reprintReceipt("O1");

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(receiptPrinterService).reprintReceipt(any());
    }

    @Test
    @DisplayName("reprintReceipt：新表没有该单、旧表也没有 → 订单不存在")
    void reprint_notFound() {
        when(orderNewMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectOne(any())).thenReturn(null);

        Result<Boolean> result = service.reprintReceipt("NOPE");

        assertNotEquals(0, result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }
}
