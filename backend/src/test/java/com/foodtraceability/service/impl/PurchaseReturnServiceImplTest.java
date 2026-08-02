package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.PurchaseReturnApproveDTO;
import com.foodtraceability.dto.PurchaseReturnCreateDTO;
import com.foodtraceability.dto.PurchaseReturnItemCreateDTO;
import com.foodtraceability.dto.PurchaseReturnVO;
import com.foodtraceability.dto.finance.PayableVO;
import com.foodtraceability.entity.PurchaseReturn;
import com.foodtraceability.entity.PurchaseReturnItem;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.entity.finance.SupplierRefundRequest;
import com.foodtraceability.mapper.PurchaseReturnItemMapper;
import com.foodtraceability.mapper.PurchaseReturnMapper;
import com.foodtraceability.mapper.finance.SupplierRefundRequestMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.SupplierService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PurchaseReturnServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PurchaseReturnServiceImpl 单元测试")
class PurchaseReturnServiceImplTest {

    @Mock
    private PurchaseReturnMapper purchaseReturnMapper;

    @Mock
    private PurchaseReturnItemMapper purchaseReturnItemMapper;

    @Mock
    private SupplierRefundRequestMapper supplierRefundRequestMapper;

    @Mock
    private PurchaseStockinService purchaseStockinService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StoreInventoryService storeInventoryService;

    @Mock
    private PayableService payableService;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private PurchaseReturnServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", purchaseReturnMapper);
    }

    // ============================================================
    // 1. 创建退货单
    // ============================================================
    @Test
    @DisplayName("创建退货单：正确计算总数量、总金额并保存明细")
    void createPurchaseReturn_success() {
        PurchaseStockin stockin = buildStockin();
        PurchaseReturnCreateDTO dto = buildCreateDTO();

        when(purchaseStockinService.getStockinDetail(100L)).thenReturn(stockin);
        when(purchaseReturnMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            PurchaseReturn entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(purchaseReturnMapper).insert(any(PurchaseReturn.class));
        when(purchaseReturnItemMapper.insert(any(PurchaseReturnItem.class))).thenReturn(1);
        when(purchaseReturnMapper.selectById(1L)).thenReturn(buildReturnEntity(1L, stockin));
        when(purchaseReturnItemMapper.selectList(any())).thenReturn(
                Collections.singletonList(buildReturnItem(1L, 1L)));

        PurchaseReturnVO vo = service.createPurchaseReturn(dto);

        assertAll("创建结果校验",
                () -> assertNotNull(vo),
                () -> assertEquals(1L, vo.getId()),
                () -> assertEquals(new BigDecimal("5.000"), vo.getTotalQuantity()),
                () -> assertEquals(1500L, vo.getTotalAmount()),
                () -> assertEquals("pending", vo.getStatus())
        );

        ArgumentCaptor<PurchaseReturn> captor = ArgumentCaptor.forClass(PurchaseReturn.class);
        verify(purchaseReturnMapper, times(1)).insert(captor.capture());
        PurchaseReturn saved = captor.getValue();
        assertEquals("offset", saved.getRefundMethod());
    }

    // ============================================================
    // 2. 审批通过：冲抵 + 原单未付款
    // ============================================================
    @Test
    @DisplayName("审批通过（冲抵/未付款）：扣减库存并生成红字应付单")
    void approvePurchaseReturn_offsetUnpaid_success() {
        PurchaseReturn returnOrder = buildReturnEntity(1L, buildStockin());
        returnOrder.setItems(Collections.singletonList(buildReturnItem(1L, 1L)));

        Payable originalPayable = buildOriginalPayable(1, 3000L);
        PayableVO redVo = buildRedPayableVO(99L);

        when(purchaseReturnMapper.selectById(1L)).thenReturn(returnOrder);
        when(purchaseReturnItemMapper.selectList(any())).thenReturn(returnOrder.getItems());
        mockOriginalPayable(originalPayable);
        when(payableService.createRedPayableForReturn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(redVo);

        PurchaseReturnApproveDTO dto = new PurchaseReturnApproveDTO();
        dto.setStatus("approved");
        dto.setRemark("同意");

        when(purchaseReturnMapper.updateById(any(PurchaseReturn.class))).thenReturn(1);
        when(purchaseReturnMapper.selectById(1L)).thenReturn(returnOrder);

        PurchaseReturnVO vo = service.approvePurchaseReturn(1L, dto);

        verify(inventoryService, times(1)).decreaseInventory(any());
        verify(storeInventoryService, times(1)).decreaseStock(any(), any(), any());
        verify(payableService, times(1)).createRedPayableForReturn(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(supplierService, never()).updateById(any(Supplier.class));
        assertEquals("approved", vo.getStatus());
    }

    // ============================================================
    // 3. 审批通过：冲抵 + 原单已付款
    // ============================================================
    @Test
    @DisplayName("审批通过（冲抵/已付款）：扣减库存并增加供应商可抵扣余额")
    void approvePurchaseReturn_offsetPaid_addCreditBalance() {
        PurchaseReturn returnOrder = buildReturnEntity(1L, buildStockin());
        returnOrder.setItems(Collections.singletonList(buildReturnItem(1L, 1L)));

        Payable originalPayable = buildOriginalPayable(3, 3000L);
        Supplier supplier = new Supplier();
        supplier.setSupplierId(200L);
        supplier.setReturnCreditBalance(100L);

        when(purchaseReturnMapper.selectById(1L)).thenReturn(returnOrder);
        when(purchaseReturnItemMapper.selectList(any())).thenReturn(returnOrder.getItems());
        mockOriginalPayable(originalPayable);
        when(payableService.createRedPayableForReturn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(buildRedPayableVO(99L));
        when(supplierService.getById(200L)).thenReturn(supplier);
        when(supplierService.updateById(any(Supplier.class))).thenReturn(true);
        when(purchaseReturnMapper.updateById(any(PurchaseReturn.class))).thenReturn(1);

        PurchaseReturnApproveDTO dto = new PurchaseReturnApproveDTO();
        dto.setStatus("approved");

        service.approvePurchaseReturn(1L, dto);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierService, times(1)).updateById(supplierCaptor.capture());
        assertEquals(1600L, supplierCaptor.getValue().getReturnCreditBalance());
    }

    // ============================================================
    // 4. 审批通过：现金退款
    // ============================================================
    @Test
    @DisplayName("审批通过（现金退款）：生成供应商退款申请单")
    void approvePurchaseReturn_cash_createRefundRequest() {
        PurchaseReturn returnOrder = buildReturnEntity(1L, buildStockin());
        returnOrder.setRefundMethod("cash");
        returnOrder.setItems(Collections.singletonList(buildReturnItem(1L, 1L)));

        Payable originalPayable = buildOriginalPayable(1, 3000L);

        when(purchaseReturnMapper.selectById(1L)).thenReturn(returnOrder);
        when(purchaseReturnItemMapper.selectList(any())).thenReturn(returnOrder.getItems());
        mockOriginalPayable(originalPayable);
        when(payableService.createRedPayableForReturn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(buildRedPayableVO(99L));
        when(supplierRefundRequestMapper.insert(any(SupplierRefundRequest.class))).thenReturn(1);
        when(purchaseReturnMapper.updateById(any(PurchaseReturn.class))).thenReturn(1);

        PurchaseReturnApproveDTO dto = new PurchaseReturnApproveDTO();
        dto.setStatus("approved");

        service.approvePurchaseReturn(1L, dto);

        ArgumentCaptor<SupplierRefundRequest> captor = ArgumentCaptor.forClass(SupplierRefundRequest.class);
        verify(supplierRefundRequestMapper, times(1)).insert(captor.capture());
        assertAll("退款申请校验",
                () -> assertEquals(1L, captor.getValue().getReturnId()),
                () -> assertEquals(1500L, captor.getValue().getAmount()),
                () -> assertEquals(SupplierRefundRequest.STATUS_PENDING, captor.getValue().getStatus())
        );
    }

    // ============================================================
    // 5. 非待审批状态不可审批
    // ============================================================
    @Test
    @DisplayName("已审批的退货单再次审批应抛出业务异常")
    void approvePurchaseReturn_notPending_throws() {
        PurchaseReturn returnOrder = buildReturnEntity(1L, buildStockin());
        returnOrder.setStatus("approved");

        when(purchaseReturnMapper.selectById(1L)).thenReturn(returnOrder);

        PurchaseReturnApproveDTO dto = new PurchaseReturnApproveDTO();
        dto.setStatus("approved");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approvePurchaseReturn(1L, dto));
        assertEquals("只有待审批状态的退货单可审批", ex.getMessage());
    }

    // ============================================================
    // 辅助方法
    // ============================================================
    private PurchaseStockin buildStockin() {
        PurchaseStockin stockin = new PurchaseStockin();
        stockin.setStockinId(100L);
        stockin.setStockinCode("SI20260720001");
        stockin.setOrderId(500L);
        stockin.setOrderNo("PO20260720001");
        stockin.setSupplierId(200L);
        stockin.setSupplierName("测试供应商");
        stockin.setWarehouseId(10L);
        stockin.setStatus(1);

        PurchaseStockinItem item = new PurchaseStockinItem();
        item.setStockinItemId(1000L);
        item.setStockinId(100L);
        item.setMaterialId(300L);
        item.setMaterialName("土豆");
        item.setActualQuantity(new BigDecimal("10.000"));
        item.setUnit("斤");
        item.setUnitPrice(300L);
        item.setBatchNo("B20260720001");

        stockin.setItems(Collections.singletonList(item));
        return stockin;
    }

    private PurchaseReturnCreateDTO buildCreateDTO() {
        PurchaseReturnCreateDTO dto = new PurchaseReturnCreateDTO();
        dto.setStockinId(100L);
        dto.setReturnDate(LocalDate.of(2026, 7, 20));
        dto.setRefundMethod("offset");
        dto.setRemark("质量问题");

        PurchaseReturnItemCreateDTO itemDto = new PurchaseReturnItemCreateDTO();
        itemDto.setStockinItemId(1000L);
        itemDto.setMaterialId(300L);
        itemDto.setQuantity(new BigDecimal("5.000"));
        itemDto.setReturnReason("破损");

        dto.setItems(Collections.singletonList(itemDto));
        return dto;
    }

    private PurchaseReturn buildReturnEntity(Long id, PurchaseStockin stockin) {
        PurchaseReturn entity = new PurchaseReturn();
        entity.setId(id);
        entity.setReturnNo("PR202607200001");
        entity.setStockinId(stockin.getStockinId());
        entity.setStockinNo(stockin.getStockinCode());
        entity.setOrderId(stockin.getOrderId());
        entity.setOrderNo(stockin.getOrderNo());
        entity.setSupplierId(stockin.getSupplierId());
        entity.setSupplierName(stockin.getSupplierName());
        entity.setWarehouseId(stockin.getWarehouseId());
        entity.setReturnDate(LocalDate.of(2026, 7, 20));
        entity.setTotalQuantity(new BigDecimal("5.000"));
        entity.setTotalAmount(1500L);
        entity.setRefundMethod("offset");
        entity.setStatus("pending");
        return entity;
    }

    private PurchaseReturnItem buildReturnItem(Long id, Long returnId) {
        PurchaseReturnItem item = new PurchaseReturnItem();
        item.setId(id);
        item.setReturnId(returnId);
        item.setStockinItemId(1000L);
        item.setMaterialId(300L);
        item.setMaterialName("土豆");
        item.setQuantity(new BigDecimal("5.000"));
        item.setUnitPrice(300L);
        item.setTotalAmount(1500L);
        item.setReturnReason("破损");
        return item;
    }

    private Payable buildOriginalPayable(int status, long amount) {
        Payable payable = new Payable();
        payable.setPayableId(10L);
        payable.setPayableNo("AP0000000100");
        payable.setStockinId(100L);
        payable.setSupplierId(200L);
        payable.setOriginalAmount(amount);
        payable.setBalanceAmount(amount);
        payable.setStatus(status);
        return payable;
    }

    private PayableVO buildRedPayableVO(Long payableId) {
        PayableVO vo = new PayableVO();
        vo.setPayableId(payableId);
        vo.setPayableNo("YR202607200001");
        vo.setOriginalAmount(-1500L);
        vo.setBalanceAmount(-1500L);
        vo.setStatus(1);
        return vo;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockOriginalPayable(Payable payable) {
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper wrapper =
                org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class);
        when(payableService.lambdaQuery()).thenReturn(wrapper);
        when(wrapper.eq(any(java.util.function.Function.class), any())).thenReturn(wrapper);
        when(wrapper.one()).thenReturn(payable);
    }
}
