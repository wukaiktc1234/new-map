package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseOrderItem;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseStockinItemMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.finance.PayableService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * PurchaseStockinServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-038（TDD）：验证 confirmStockin() 在状态更新为"已入库"后
 * 同事务调用 {@code PayableService.createForStockin()} 创建应付账款，金额以分为单位，
 * 到期日为入库日 + 30 天，且应付创建失败时整个收货事务回滚（强一致性）。</p>
 *
 * <p>测试位于 service.impl 包下。由于 PurchaseStockinServiceImpl
 * 继承 ServiceImpl，baseMapper 字段需通过 ReflectionTestUtils 注入。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PurchaseStockinServiceImpl 单元测试 - T-038 应付账款联动")
class PurchaseStockinServiceImplTest {

    @Mock
    private PurchaseStockinMapper purchaseStockinMapper;

    @Mock
    private PurchaseStockinItemMapper purchaseStockinItemMapper;

    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    @Mock
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StoreInventoryService storeInventoryService;

    @Mock
    private PayableService payableService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PurchaseStockinServiceImpl service;

    /** 入库状态常量 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_STOCKED = 1;
    /** 质检结果常量 */
    private static final int QC_PASSED = 1;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类 protected 字段，@InjectMocks 无法注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", purchaseStockinMapper);
        ReflectionTestUtils.setField(service, "purchaseOrderItemMapper", purchaseOrderItemMapper);
        ReflectionTestUtils.setField(service, "supplierMapper", supplierMapper);
        ReflectionTestUtils.setField(service, "storeInventoryService", storeInventoryService);
        ReflectionTestUtils.setField(service, "applicationEventPublisher", applicationEventPublisher);
    }

    /** 构造一个可确认入库的 PurchaseStockin（待入库 + 质检合格 + 有效金额） */
    private PurchaseStockin buildConfirmableStockin(Long stockinId, Long amount) {
        PurchaseStockin stockin = new PurchaseStockin();
        stockin.setStockinId(stockinId);
        stockin.setStockinCode("SI20260626001");
        stockin.setOrderId(5001L);
        stockin.setSupplierId(3001L);
        stockin.setWarehouseId(1001L);
        stockin.setStockinDate(LocalDate.of(2026, 6, 26));
        stockin.setTotalAmount(amount);
        stockin.setQualityCheckResult(QC_PASSED);
        stockin.setStatus(STATUS_PENDING);
        return stockin;
    }

    /** stub confirmStockin 中的辅助调用（明细查询、订单状态更新等） */
    private void stubConfirmStockinAux(PurchaseStockin stockin) {
        // 入库明细查询返回空列表（跳过库存增加循环）
        when(purchaseStockinItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 订单状态更新：返回一个已审核订单
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(stockin.getOrderId());
        order.setOrderCode("PO20260626001");
        order.setOrderStatus(2);
        when(purchaseOrderMapper.selectById(stockin.getOrderId())).thenReturn(order);
        when(purchaseOrderMapper.updateById(any())).thenReturn(1);
        // 入库单查询返回自身（用于 updateOrderStockStatus）
        when(purchaseStockinMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(stockin));
    }

    // ============================================================
    // 1. 主流程：confirmStockin 调用 createForStockin 创建应付账款
    // ============================================================
    @Test
    @DisplayName("主流程：确认入库后调用 payableService.createForStockin 创建应付账款")
    void confirmStockin_success_callsCreateForStockin() {
        // given
        Long stockinId = 6001L;
        Long amount = 500000L; // 5000元
        PurchaseStockin stockin = buildConfirmableStockin(stockinId, amount);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        stubConfirmStockinAux(stockin);

        // when
        service.confirmStockin(stockinId);

        // then：验证 payableService.createForStockin 被调用且参数正确
        ArgumentCaptor<Long> stockinIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> stockinNoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> supplierIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> supplierNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> orderNoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDate> stockinDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(payableService, times(1)).createForStockin(
                stockinIdCaptor.capture(),
                stockinNoCaptor.capture(),
                supplierIdCaptor.capture(),
                supplierNameCaptor.capture(),
                orderIdCaptor.capture(),
                orderNoCaptor.capture(),
                amountCaptor.capture(),
                stockinDateCaptor.capture()
        );
        assertAll("应付账款参数验证",
                () -> assertEquals(stockinId, stockinIdCaptor.getValue()),
                () -> assertEquals("SI20260626001", stockinNoCaptor.getValue()),
                () -> assertEquals(3001L, supplierIdCaptor.getValue()),
                () -> assertEquals("供应商3001", supplierNameCaptor.getValue()),
                () -> assertEquals(5001L, orderIdCaptor.getValue()),
                () -> assertEquals("PO20260626001", orderNoCaptor.getValue()),
                () -> assertEquals(500000L, amountCaptor.getValue(), "金额应为入库单总额（分）"),
                () -> assertEquals(LocalDate.of(2026, 6, 26), stockinDateCaptor.getValue())
        );
    }

    // ============================================================
    // 2. 强一致性：应付创建失败则整个收货事务回滚
    // ============================================================
    @Test
    @DisplayName("强一致性：payableService.createForStockin 抛异常时 confirmStockin 抛 BusinessException")
    void confirmStockin_payableCreateFails_throwsBusinessException() {
        // given
        Long stockinId = 6002L;
        Long amount = 300000L;
        PurchaseStockin stockin = buildConfirmableStockin(stockinId, amount);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        stubConfirmStockinAux(stockin);

        // 模拟应付创建失败
        doThrow(new RuntimeException("应付账款DB连接失败"))
                .when(payableService).createForStockin(
                        anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), any(LocalDate.class));

        // when & then：confirmStockin 应抛 BusinessException（事务回滚）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin(stockinId));
        assertTrue(ex.getMessage().contains("创建应付账款失败"),
                "异常信息应说明应付账款创建失败");
    }

    // ============================================================
    // 3. 边界：入库单金额无效（0 或 null）跳过应付创建
    // ============================================================
    @Test
    @DisplayName("边界：入库单金额为0时跳过应付账款创建")
    void confirmStockin_zeroAmount_skipsPayableCreation() {
        // given
        Long stockinId = 6003L;
        PurchaseStockin stockin = buildConfirmableStockin(stockinId, 0L);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        stubConfirmStockinAux(stockin);

        // when
        service.confirmStockin(stockinId);

        // then：金额为0，不应调用 createForStockin
        verify(payableService, never()).createForStockin(
                anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), any(LocalDate.class));
    }

    @Test
    @DisplayName("边界：入库单金额为null时跳过应付账款创建")
    void confirmStockin_nullAmount_skipsPayableCreation() {
        // given
        Long stockinId = 6004L;
        PurchaseStockin stockin = buildConfirmableStockin(stockinId, null);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        stubConfirmStockinAux(stockin);

        // when
        service.confirmStockin(stockinId);

        // then：金额为null，不应调用 createForStockin
        verify(payableService, never()).createForStockin(
                anyLong(), anyString(), anyLong(), anyString(), anyLong(), anyString(), anyLong(), any(LocalDate.class));
    }

    // ============================================================
    // 4. 幂等性：confirmStockin 调用 createForStockin 由 PayableService 保证幂等
    //    此测试验证 PurchaseStockinServiceImpl 总是调用 createForStockin
    //    （PayableService 内部通过确定性 payableNo 保证幂等，不在此层验证）
    // ============================================================
    @Test
    @DisplayName("幂等性：confirmStockin 始终调用 createForStockin，幂等由 PayableService 保证")
    void confirmStockin_alwaysCallsCreateForStockin_idempotencyByPayableService() {
        // given
        Long stockinId = 6005L;
        Long amount = 800000L;
        PurchaseStockin stockin = buildConfirmableStockin(stockinId, amount);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        stubConfirmStockinAux(stockin);

        // when
        service.confirmStockin(stockinId);

        // then：PurchaseStockinServiceImpl 层只负责调用，幂等由 PayableService 保证
        verify(payableService, times(1)).createForStockin(
                eq(stockinId), anyString(), anyLong(), anyString(), anyLong(), anyString(), eq(amount), any(LocalDate.class));
    }

    // ============================================================
    // 5. 作废入库单（作废非撤销）
    // ============================================================

    /** 构造一个已入库且带明细的入库单 */
    private PurchaseStockin buildVoidableStockin(Long stockinId) {
        PurchaseStockin stockin = new PurchaseStockin();
        stockin.setStockinId(stockinId);
        stockin.setStockinCode("SI20260626001");
        stockin.setOrderId(5001L);
        stockin.setSupplierId(3001L);
        stockin.setWarehouseId(1001L);
        stockin.setStatus(STATUS_STOCKED);
        return stockin;
    }

    private PurchaseStockinItem buildStockinItem(Long orderItemId, Long materialId, BigDecimal quantity) {
        PurchaseStockinItem item = new PurchaseStockinItem();
        item.setOrderItemId(orderItemId);
        item.setMaterialId(materialId);
        item.setMaterialName("测试物料");
        item.setActualQuantity(quantity);
        item.setUnit("kg");
        return item;
    }

    private void stubVoidAux(PurchaseStockin stockin, List<PurchaseStockinItem> items) {
        when(purchaseStockinItemMapper.selectList(any())).thenReturn(items);
        when(purchaseStockinMapper.updateById(any())).thenReturn(1);
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(stockin.getOrderId());
        order.setOrderCode("PO20260626001");
        order.setOrderStatus(2);
        when(purchaseOrderMapper.selectById(stockin.getOrderId())).thenReturn(order);
        when(purchaseOrderMapper.updateById(any())).thenReturn(1);
        when(purchaseOrderItemMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PurchaseOrderItem oi = new PurchaseOrderItem();
            oi.setItemId(id);
            oi.setReceivedQuantity(new BigDecimal("10.000"));
            return oi;
        });
        when(purchaseOrderItemMapper.updateById(any())).thenReturn(1);
        when(purchaseStockinMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("作废：已入库单作废后状态为9，并回滚库存与订单已收货数量")
    void voidStockin_success_statusVoidedAndInventoryRestored() {
        // given
        Long stockinId = 7001L;
        PurchaseStockin stockin = buildVoidableStockin(stockinId);
        PurchaseStockinItem item = buildStockinItem(8001L, 9001L, new BigDecimal("5.000"));
        List<PurchaseStockinItem> items = Collections.singletonList(item);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);
        stubVoidAux(stockin, items);

        // when
        PurchaseStockin result = service.voidStockin(stockinId, "测试作废", 1001L);

        // then
        assertEquals(9, result.getStatus(), "入库单状态应为已作废");
        assertEquals("测试作废", result.getVoidRemark());
        assertEquals(1001L, result.getVoidBy());
        verify(payableService, times(1)).voidPayableByStockinId(stockinId);
        verify(inventoryService, times(1)).decreaseInventory(any(InventoryDecreaseDTO.class));
        verify(storeInventoryService, times(1)).decreaseStock(anyString(), eq(9001L), eq(new BigDecimal("5.000")));
    }

    @Test
    @DisplayName("作废：非已入库状态禁止作废")
    void voidStockin_notStocked_throwsException() {
        // given
        Long stockinId = 7002L;
        PurchaseStockin stockin = buildVoidableStockin(stockinId);
        stockin.setStatus(STATUS_PENDING);
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.voidStockin(stockinId, "测试", 1001L));
        assertTrue(ex.getMessage().contains("仅已入库状态的入库单可作废"));
    }

    @Test
    @DisplayName("作废：已作废的入库单禁止重复作废")
    void voidStockin_alreadyVoided_throwsException() {
        // given
        Long stockinId = 7003L;
        PurchaseStockin stockin = buildVoidableStockin(stockinId);
        stockin.setVoidTime(LocalDateTime.now());
        when(purchaseStockinMapper.selectById(stockinId)).thenReturn(stockin);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.voidStockin(stockinId, "测试", 1001L));
        assertTrue(ex.getMessage().contains("已作废"));
    }
}
