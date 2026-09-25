package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.PurchaseOrderItemMapper;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.dto.PurchaseOrderCreateDTO;
import com.foodtraceability.dto.PurchaseOrderItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

/**
 * P1-PURCHASE-SUPPLIER-BINDING-001：采购订单供应商绑定语义单测
 * 覆盖 createOrder 两个分支：表头供应商优先（修复 F4）/ 未指定表头时按物料档案拆分
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderSupplierBindingTest {

    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private SupplierMapper supplierMapper;
    @Mock private MaterialArchiveMapper materialArchiveMapper;
    @Mock private UserMapper userMapper;

    private PurchaseOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderServiceImpl(purchaseOrderMapper, purchaseOrderItemMapper,
                productMapper, supplierMapper, materialArchiveMapper, userMapper);
        // generateOrderCode 的 max 查询：无历史单 → 序号 0001
        lenient().when(purchaseOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        java.util.concurrent.atomic.AtomicReference<PurchaseOrder> saved =
                new java.util.concurrent.atomic.AtomicReference<>();
        lenient().when(purchaseOrderMapper.insert(any(PurchaseOrder.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0));
            return 1;
        });
        // buildAndSaveOrder 尾部回读 getOrderDetail(orderId)：单测无 DB，回放刚保存的实体
        lenient().when(purchaseOrderMapper.selectById(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> saved.get());
        lenient().when(purchaseOrderItemMapper.insert(any())).thenReturn(1);
    }

    private MaterialArchive archive(long materialId, long supplierId) {
        MaterialArchive a = new MaterialArchive();
        a.setMaterialId(materialId);
        a.setSupplierId(supplierId);
        return a;
    }

    private PurchaseOrderCreateDTO dto(Long headerSupplierId, long... materialIds) {
        PurchaseOrderCreateDTO dto = new PurchaseOrderCreateDTO();
        dto.setSupplierId(headerSupplierId);
        dto.setItems(new java.util.ArrayList<>());
        dto.setWarehouseId(1L);
        for (long mid : materialIds) {
            PurchaseOrderItemDTO item = new PurchaseOrderItemDTO();
            item.setMaterialId(mid);
            item.setMaterialName("M" + mid);
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(100L);
            item.setUnit("斤");
            dto.getItems().add(item);
        }
        return dto;
    }

    /** 分支一（F4 修复）：表头供应商存在时优先于物料档案绑定 */
    @Test
    void createOrder_headerSupplierPriority_overridesArchiveBinding() {
        // 物料 3 档案绑定供应商 1；表头声明供应商 11 → 必须按表头落库
        lenient().when(materialArchiveMapper.selectById(3L)).thenReturn(archive(3, 1L));

        List<PurchaseOrder> orders = service.createOrder(dto(11L, 3L));

        assertEquals(1, orders.size());
        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        org.mockito.Mockito.verify(purchaseOrderMapper, times(1)).insert(captor.capture());
        assertEquals(11L, captor.getValue().getSupplierId());
    }

    /** 分支二（保留行为）：未指定表头供应商时按物料档案主供应商自动拆分 */
    @Test
    void createOrder_splitsByArchiveSupplier_whenHeaderAbsent() {
        lenient().when(materialArchiveMapper.selectById(3L)).thenReturn(archive(3, 1L));
        lenient().when(materialArchiveMapper.selectById(4L)).thenReturn(archive(4, 2L));

        List<PurchaseOrder> orders = service.createOrder(dto(null, 3L, 4L));

        assertEquals(2, orders.size());
        org.mockito.Mockito.verify(purchaseOrderMapper, times(2)).insert(any(PurchaseOrder.class));
        List<Long> supplierIds = Arrays.asList(orders.get(0).getSupplierId(), orders.get(1).getSupplierId());
        assertTrue(supplierIds.contains(1L) && supplierIds.contains(2L), "两张单应分别绑定档案供应商 1 与 2");
    }
}
