package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.event.EventPublisher;
import com.foodtraceability.event.PurchaseStockInEvent;
import com.foodtraceability.event.listener.PurchaseStockInEventListener;
import com.foodtraceability.mapper.LocationIdMapMapper;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.mapper.PurchaseStockinItemMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.MaterialTraceCodeService;
import com.foodtraceability.service.PrintService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.service.finance.CostRecordService;
import com.foodtraceability.service.purchase.StockinLocationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 扫码确认入库（DATA-007 + 整改 1）：落统一库存账 / 强制写采购入库明细 / 库位与单价必须解析成功。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialTraceCodeConfirmStockinTest {

    private static final long LOCATION_ID = 100L;
    private static final long MATERIAL_ID = 5001L;
    private static final long STOCKIN_ID = 777L;
    private static final long ORDER_ID = 66L;
    private static final long UNIT_PRICE_FEN = 350L;

    @Mock private MaterialTraceCodeMapper materialTraceCodeMapper;
    @Mock private PrintService printService;
    @Mock private InventoryService inventoryService;
    @Mock private StoreInventoryLogService storeInventoryLogService;
    @Mock private PurchaseStockinService purchaseStockinService;
    @Mock private EventPublisher eventPublisher;
    @Mock private LocationIdMapMapper locationIdMapMapper;
    @Mock private MaterialArchiveMapper materialArchiveMapper;
    @Mock private PurchaseStockinItemMapper purchaseStockinItemMapper;
    @Mock private StockinLocationResolver stockinLocationResolver;

    private MaterialTraceCode pendingCode;
    private MaterialTraceCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialTraceCodeServiceImpl(materialTraceCodeMapper, printService,
                inventoryService, storeInventoryLogService, purchaseStockinService, eventPublisher,
                materialArchiveMapper, purchaseStockinItemMapper, stockinLocationResolver) {
            @Override
            public MaterialTraceCode getByTraceCode(String traceCode) {
                return pendingCode;
            }

            @Override
            public boolean updateById(MaterialTraceCode entity) {
                return true;
            }
        };

        pendingCode = new MaterialTraceCode();
        pendingCode.setTraceCode("MTC20261024TEST01");
        pendingCode.setMaterialId(String.valueOf(MATERIAL_ID));
        pendingCode.setMaterialName("QA-土豆");
        pendingCode.setBatchNumber("BATCH-001");
        pendingCode.setWeight(new BigDecimal("2.500"));
        pendingCode.setWeightUnit("kg");
        pendingCode.setStoreId("1");
        pendingCode.setStoreName("测试门店A");
        pendingCode.setEntryType("purchase");
        pendingCode.setPurchaseOrderId(String.valueOf(ORDER_ID));
        pendingCode.setStatus("pending");

        when(stockinLocationResolver.resolveLocationId(any(), any(), any(), any())).thenReturn(LOCATION_ID);

        Inventory existing = new Inventory();
        existing.setLocationId(LOCATION_ID);
        existing.setMaterialId(MATERIAL_ID);
        existing.setQuantity(new BigDecimal("10.000"));
        when(inventoryService.getByLocationAndMaterial(LOCATION_ID, MATERIAL_ID)).thenReturn(existing);

        when(purchaseStockinService.save(any(PurchaseStockin.class))).thenAnswer(inv -> {
            PurchaseStockin s = inv.getArgument(0);
            s.setStockinId(STOCKIN_ID);
            return true;
        });
    }

    private PurchaseStockInEvent capturePublishedEvent() {
        ArgumentCaptor<PurchaseStockInEvent> captor = ArgumentCaptor.forClass(PurchaseStockInEvent.class);
        verify(eventPublisher).publishPurchaseStockInEvent(captor.capture());
        return captor.getValue();
    }

    private PurchaseStockinItem captureItem() {
        ArgumentCaptor<PurchaseStockinItem> captor = ArgumentCaptor.forClass(PurchaseStockinItem.class);
        verify(purchaseStockinItemMapper).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("a) 正常确认（显式单价）→ 落统一账 + 写明细 + 发事件")
    void confirmStockinWritesLedgerItemAndEvent() {
        assertTrue(service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));

        verify(inventoryService).increaseStockAtLocation(
                eq(LOCATION_ID), eq(MATERIAL_ID), eq("QA-土豆"),
                eq(new BigDecimal("2.500")), eq("kg"), eq(UNIT_PRICE_FEN),
                eq("BATCH-001"), eq("TRACE_CONFIRM_STOCKIN"), anyString());

        ArgumentCaptor<StoreInventoryLog> logCaptor = ArgumentCaptor.forClass(StoreInventoryLog.class);
        verify(storeInventoryLogService).createLog(logCaptor.capture());
        assertEquals(0, new BigDecimal("10.000").compareTo(logCaptor.getValue().getBeforeStock()));
        assertEquals(0, new BigDecimal("12.500").compareTo(logCaptor.getValue().getAfterStock()));

        PurchaseStockInEvent event = capturePublishedEvent();
        assertEquals(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM, event.getSourceType());
        assertEquals(STOCKIN_ID, event.getStockinId());
    }

    @Test
    @DisplayName("b) 库位解析失败 → 抛 BusinessException 且零库存/零明细写入")
    void confirmStockinFailsWhenLocationUnresolved() {
        when(stockinLocationResolver.resolveLocationId(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, StockinLocationResolver.RESOLVE_FAILED_MESSAGE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));
        assertTrue(ex.getMessage().contains("无法确定入库库位"), "实际=" + ex.getMessage());

        verifyNoInteractions(inventoryService);
        verify(purchaseStockinItemMapper, never()).insert(any());
        verify(purchaseStockinService, never()).save(any());
        verify(storeInventoryLogService, never()).createLog(any());
    }

    @Test
    @DisplayName("c) 入库明细字段：material_id/qty/unit_price/amount/location_id 全有值且 amount=qty×price")
    void stockinItemCarriesAllMandatoryFields() {
        assertTrue(service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));

        PurchaseStockinItem item = captureItem();
        assertEquals(STOCKIN_ID, item.getStockinId());
        assertEquals(MATERIAL_ID, item.getMaterialId());
        assertEquals("QA-土豆", item.getMaterialName());
        assertEquals("BATCH-001", item.getBatchNo());
        assertEquals(0, new BigDecimal("2.500").compareTo(item.getActualQuantity()));
        assertEquals("kg", item.getUnit());
        assertEquals(UNIT_PRICE_FEN, item.getUnitPrice(), "unit_price 必须落分");
        assertEquals(875L, item.getAmount(), "amount = 2.5 × 350 = 875 分");
        assertEquals(LOCATION_ID, item.getLocationId(), "items.location_id 必须非空");
        assertNull(item.getOrderItemId(), "扫码确认无采购订单行");
    }

    @Test
    @DisplayName("d) materialId 非法 → 抛错且不写库存/明细")
    void confirmStockinFailsWhenMaterialIdInvalid() {
        pendingCode.setMaterialId("NOT_A_NUMBER");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));
        assertTrue(ex.getMessage().contains("物料ID非法"), "实际=" + ex.getMessage());

        verify(inventoryService, never()).increaseStockAtLocation(anyLong(), anyLong(), anyString(),
                any(), anyString(), any(), any(), anyString(), anyString());
        verify(purchaseStockinItemMapper, never()).insert(any());
    }

    @Test
    @DisplayName("e) 单价来自物料主档参考价 → 事件 8.75 元，成本行 875 分")
    void unitPriceFromMaterialArchive() {
        MaterialArchive archive = new MaterialArchive();
        archive.setMaterialId(MATERIAL_ID);
        archive.setReferencePrice(UNIT_PRICE_FEN);
        when(materialArchiveMapper.selectById(MATERIAL_ID)).thenReturn(archive);

        assertTrue(service.confirmStockin("MTC20261024TEST01", "1"));

        PurchaseStockInEvent event = capturePublishedEvent();
        assertEquals(0, new BigDecimal("3.50").compareTo(event.getUnitPrice()));
        assertEquals(0, new BigDecimal("8.75").compareTo(event.getTotalAmount()));

        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        new PurchaseStockInEventListener(traceCodeService, costRecordService).handlePurchaseStockInEvent(event);

        ArgumentCaptor<CostRecordCreateDTO> dtoCaptor = ArgumentCaptor.forClass(CostRecordCreateDTO.class);
        verify(costRecordService).create(dtoCaptor.capture());
        assertEquals(875L, dtoCaptor.getValue().getAmount());
        assertEquals(7, dtoCaptor.getValue().getCostType());
    }

    @Test
    @DisplayName("f) 三处都无单价 → 抛错（不建单/不写明细/不落账/不发事件）")
    void confirmStockinFailsWhenPriceMissing() {
        when(materialArchiveMapper.selectById(MATERIAL_ID)).thenReturn(null);
        when(purchaseStockinItemMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin("MTC20261024TEST01", "1"));
        assertTrue(ex.getMessage().contains("无法确定入库单价"), "实际=" + ex.getMessage());

        verify(purchaseStockinService, never()).save(any());
        verify(purchaseStockinItemMapper, never()).insert(any());
        verifyNoInteractions(inventoryService);
        verify(eventPublisher, never()).publishPurchaseStockInEvent(any());
    }

    @Test
    @DisplayName("g) 显式 locationId 透传给解析器，并写入明细")
    void explicitLocationIdIsPassedToResolver() {
        when(stockinLocationResolver.resolveLocationId(eq(999L), isNull(), eq(ORDER_ID), eq("1")))
                .thenReturn(999L);

        assertTrue(service.confirmStockin("MTC20261024TEST01", "1", 999L, UNIT_PRICE_FEN));

        verify(stockinLocationResolver).resolveLocationId(eq(999L), isNull(), eq(ORDER_ID), eq("1"));
        assertEquals(999L, captureItem().getLocationId());
    }

    @Test
    @DisplayName("h) 入库表头写入 order_id 与 total_amount（分），明细 location_id 非空")
    void stockinHeaderCarriesOrderAndAmount() {
        assertTrue(service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));

        ArgumentCaptor<PurchaseStockin> captor = ArgumentCaptor.forClass(PurchaseStockin.class);
        verify(purchaseStockinService).save(captor.capture());
        PurchaseStockin stockin = captor.getValue();

        assertEquals(ORDER_ID, stockin.getOrderId(), "order_id 应由追溯码 purchaseOrderId 推出");
        assertEquals(LOCATION_ID, stockin.getLocationId(), "表头 location_id 必须非空（列 NOT NULL）");
        assertEquals(875L, stockin.getTotalAmount(), "total_amount = 2.5 × 350 = 875 分");
        assertEquals(0, new BigDecimal("2.500").compareTo(stockin.getTotalQuantity()));
        assertEquals(2, stockin.getStockinType());
        assertNotNull(captureItem().getLocationId(), "明细 location_id 必须非空");
    }

    @Test
    @DisplayName("i) 追溯码状态非 pending → 返回 false，不写任何数据")
    void confirmStockinRejectsNonPendingCode() {
        pendingCode.setStatus("in_stock");

        assertFalse(service.confirmStockin("MTC20261024TEST01", "1", null, UNIT_PRICE_FEN));
        verify(purchaseStockinService, never()).save(any());
        verify(purchaseStockinItemMapper, never()).insert(any());
    }

    @Test
    @DisplayName("j) 追溯码不存在 → 返回 false")
    void confirmStockinRejectsMissingCode() {
        pendingCode = null;
        assertFalse(service.confirmStockin("MTC_NOT_EXIST", "1", null, UNIT_PRICE_FEN));
        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("k) 扫码确认来源：跳过追溯码再生成，只写成本")
    void listenerSkipsTraceGenerationForTraceConfirm() {
        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        PurchaseStockInEventListener listener = new PurchaseStockInEventListener(traceCodeService, costRecordService);

        PurchaseStockInEvent event = new PurchaseStockInEvent();
        event.setSourceType(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM);
        event.setStockinId(STOCKIN_ID);
        event.setTotalAmount(new BigDecimal("8.75"));

        listener.handlePurchaseStockInEvent(event);

        verify(traceCodeService, never()).generateBatch(any());
        verify(costRecordService, times(1)).create(any());
    }

    @Test
    @DisplayName("l) 事件金额 null → 监听器不写成本行（禁止金额 0 的错账）")
    void listenerSkipsCostWhenAmountUnknown() {
        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        PurchaseStockInEventListener listener = new PurchaseStockInEventListener(traceCodeService, costRecordService);

        PurchaseStockInEvent event = new PurchaseStockInEvent();
        event.setSourceType(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM);
        event.setStockinId(STOCKIN_ID);

        listener.handlePurchaseStockInEvent(event);

        verify(costRecordService, never()).create(any());
    }
}
