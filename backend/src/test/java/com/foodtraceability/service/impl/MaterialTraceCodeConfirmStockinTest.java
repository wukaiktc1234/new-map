package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.event.EventPublisher;
import com.foodtraceability.event.PurchaseStockInEvent;
import com.foodtraceability.event.listener.PurchaseStockInEventListener;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * DATA-007：扫码确认入库 → 落统一库存账 + 发事件写成本。
 *
 * <p>覆盖：正常落账、门店无 location 映射必须抛错且不写库存、物料ID非法必须抛错、
 * 扫码确认来源的事件监听跳过追溯码再生成只写成本。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialTraceCodeConfirmStockinTest {

    private static final long STORE_ID = 1L;
    private static final long LOCATION_ID = 100L;
    private static final long MATERIAL_ID = 5001L;
    private static final long STOCKIN_ID = 777L;

    @Mock private MaterialTraceCodeMapper materialTraceCodeMapper;
    @Mock private PrintService printService;
    @Mock private InventoryService inventoryService;
    @Mock private StoreInventoryLogService storeInventoryLogService;
    @Mock private PurchaseStockinService purchaseStockinService;
    @Mock private EventPublisher eventPublisher;
    @Mock private LocationIdMapMapper locationIdMapMapper;
    @Mock private MaterialArchiveMapper materialArchiveMapper;
    @Mock private PurchaseStockinItemMapper purchaseStockinItemMapper;

    /** 可测试子类：绕过 MyBatis-Plus 的 baseMapper（getByTraceCode / updateById 直接给出结果） */
    private MaterialTraceCode pendingCode;
    private MaterialTraceCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialTraceCodeServiceImpl(materialTraceCodeMapper, printService,
                inventoryService, storeInventoryLogService, purchaseStockinService, eventPublisher,
                materialArchiveMapper, purchaseStockinItemMapper) {
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
        pendingCode.setStoreId(String.valueOf(STORE_ID));
        pendingCode.setStoreName("测试门店A");
        pendingCode.setEntryType("purchase");
        pendingCode.setStatus("pending");

        // storeId → locationId 映射（stores_new 口径）
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, STORE_ID))
                .thenReturn(LOCATION_ID);
        new LocationIdBridge(locationIdMapMapper);   // 构造即设置静态 instance

        // 统一账当前库存（legacy 日志 before/after 用）
        Inventory existing = new Inventory();
        existing.setLocationId(LOCATION_ID);
        existing.setMaterialId(MATERIAL_ID);
        existing.setQuantity(new BigDecimal("10.000"));
        when(inventoryService.getByLocationAndMaterial(LOCATION_ID, MATERIAL_ID)).thenReturn(existing);

        // 入库单保存：模拟 MyBatis-Plus 回填主键
        when(purchaseStockinService.save(any(PurchaseStockin.class))).thenAnswer(inv -> {
            PurchaseStockin s = inv.getArgument(0);
            s.setStockinId(STOCKIN_ID);
            return true;
        });
    }

    @Test
    @DisplayName("a) 正常确认 → 调用 increaseStockAtLocation，参数与来源类型正确，并写 legacy 日志真实前后值")
    void confirmStockinWritesCanonicalLedger() {
        assertTrue(service.confirmStockin("MTC20261024TEST01", "1"));

        ArgumentCaptor<String> sourceRef = ArgumentCaptor.forClass(String.class);
        verify(inventoryService).increaseStockAtLocation(
                eq(LOCATION_ID),
                eq(MATERIAL_ID),
                eq("QA-土豆"),
                eq(new BigDecimal("2.500")),
                eq("kg"),
                isNull(),                       // unitCost 不臆造 0
                eq("BATCH-001"),
                eq("TRACE_CONFIRM_STOCKIN"),
                sourceRef.capture());

        assertNotNull(sourceRef.getValue(), "sourceRef 应为入库单号");
        assertTrue(sourceRef.getValue().startsWith("SI"), "sourceRef 应为入库单号(SI...)，实际=" + sourceRef.getValue());

        // legacy 审计日志：before=真实库存 10，after=12.5
        ArgumentCaptor<StoreInventoryLog> logCaptor = ArgumentCaptor.forClass(StoreInventoryLog.class);
        verify(storeInventoryLogService).createLog(logCaptor.capture());
        assertEquals(0, new BigDecimal("10.000").compareTo(logCaptor.getValue().getBeforeStock()));
        assertEquals(0, new BigDecimal("12.500").compareTo(logCaptor.getValue().getAfterStock()));

        // 入库单落库 + 事件发布（sourceType=TRACE_CONFIRM）
        verify(purchaseStockinService).save(any(PurchaseStockin.class));
        ArgumentCaptor<PurchaseStockInEvent> eventCaptor = ArgumentCaptor.forClass(PurchaseStockInEvent.class);
        verify(eventPublisher).publishPurchaseStockInEvent(eventCaptor.capture());
        PurchaseStockInEvent event = eventCaptor.getValue();
        assertEquals(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM, event.getSourceType());
        assertEquals(STOCKIN_ID, event.getStockinId());
        assertEquals(MATERIAL_ID, event.getProductId());
    }

    @Test
    @DisplayName("b) storeId 无 location 映射 → 抛 BusinessException 且不写任何库存")
    void confirmStockinFailsWhenLocationMissing() {
        when(locationIdMapMapper.selectLocationIdBySrcId(LocationIdBridge.SRC_STORES_NEW, STORE_ID)).thenReturn(null);
        new LocationIdBridge(locationIdMapMapper);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin("MTC20261024TEST01", "1"));
        assertTrue(ex.getMessage().contains("未映射"), "错误信息应说明位置未映射，实际=" + ex.getMessage());

        // 核心断言：没有任何库存写入（统一账 / legacy 日志）
        verifyNoInteractions(inventoryService);
        verify(storeInventoryLogService, never()).createLog(any());
        verify(eventPublisher, never()).publishPurchaseStockInEvent(any());
        // 注：入库单按 DATA-007 顺序在落账之前创建；异常抛在 @Transactional 内，
        // 由 confirmStockin 的事务整体回滚（此处不做「save 未调用」断言，那不是回滚语义）。
    }

    @Test
    @DisplayName("d) materialId 非法 → 抛错不静默，且不写库存")
    void confirmStockinFailsWhenMaterialIdInvalid() {
        pendingCode.setMaterialId("NOT_A_NUMBER");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmStockin("MTC20261024TEST01", "1"));
        assertTrue(ex.getMessage().contains("物料ID非法"), "实际=" + ex.getMessage());

        verify(inventoryService, never()).increaseStockAtLocation(anyLong(), anyLong(), anyString(),
                any(), anyString(), any(), any(), anyString(), anyString());
        verify(storeInventoryLogService, never()).createLog(any());
    }

    @Test
    @DisplayName("c) 扫码确认来源的事件：跳过追溯码再生成，只写采购成本（防递归）")
    void listenerSkipsTraceGenerationForTraceConfirm() {
        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        PurchaseStockInEventListener listener = new PurchaseStockInEventListener(traceCodeService, costRecordService);

        PurchaseStockInEvent event = new PurchaseStockInEvent();
        event.setSourceType(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM);
        event.setStockinId(STOCKIN_ID);
        event.setProductId(MATERIAL_ID);
        event.setProductName("QA-土豆");
        event.setQuantity(new BigDecimal("2.500"));
        event.setTotalAmount(new BigDecimal("8.75"));   // 有单价才写成本行；无价不写 0（见 f）

        listener.handlePurchaseStockInEvent(event);

        verify(traceCodeService, never()).generateBatch(any());
        verify(costRecordService, times(1)).create(any());
    }

    @Test
    @DisplayName("c2) 采购入库单来源（sourceType=null）：追溯码生成与成本写入都执行")
    void listenerGeneratesTraceCodesForNormalStockin() {
        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        PurchaseStockInEventListener listener = new PurchaseStockInEventListener(traceCodeService, costRecordService);

        PurchaseStockInEvent event = new PurchaseStockInEvent();
        event.setStockinId(STOCKIN_ID);
        event.setProductId(MATERIAL_ID);
        event.setProductName("QA-土豆");
        event.setTotalAmount(new BigDecimal("8.75"));

        listener.handlePurchaseStockInEvent(event);

        verify(traceCodeService, times(1)).generateBatch(any());
        verify(costRecordService, times(1)).create(any());
    }

    // ==================== e) / f) 单价解析链 ====================

    /** 捕获确认入库时发布的事件 */
    private PurchaseStockInEvent capturePublishedEvent() {
        ArgumentCaptor<PurchaseStockInEvent> captor = ArgumentCaptor.forClass(PurchaseStockInEvent.class);
        verify(eventPublisher).publishPurchaseStockInEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("e) 物料主档有参考价 → 事件金额=qty×参考价，成本行金额=875 分（非 0）")
    void costAmountComesFromMaterialReferencePrice() {
        // material_archives.reference_price = 350 分/单位；追溯码 weight=2.5
        MaterialArchive archive = new MaterialArchive();
        archive.setMaterialId(MATERIAL_ID);
        archive.setReferencePrice(350L);
        when(materialArchiveMapper.selectById(MATERIAL_ID)).thenReturn(archive);
        // 入库明细无单价（扫码确认只建表头）
        when(purchaseStockinItemMapper.selectList(any())).thenReturn(java.util.List.of());

        assertTrue(service.confirmStockin("MTC20261024TEST01", "1"));

        PurchaseStockInEvent event = capturePublishedEvent();
        assertEquals(0, new BigDecimal("3.50").compareTo(event.getUnitPrice()),
                "单价应为 350 分 = 3.50 元，实际=" + event.getUnitPrice());
        assertEquals(0, new BigDecimal("8.75").compareTo(event.getTotalAmount()),
                "金额应为 2.5 × 3.50 = 8.75 元，实际=" + event.getTotalAmount());

        // 事件交给监听器 → cost_records 金额 875 分
        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        new PurchaseStockInEventListener(traceCodeService, costRecordService).handlePurchaseStockInEvent(event);

        ArgumentCaptor<CostRecordCreateDTO> dtoCaptor = ArgumentCaptor.forClass(CostRecordCreateDTO.class);
        verify(costRecordService).create(dtoCaptor.capture());
        assertEquals(875L, dtoCaptor.getValue().getAmount(),
                "成本金额应为 875 分（非 0），实际=" + dtoCaptor.getValue().getAmount());
        assertEquals(7, dtoCaptor.getValue().getCostType());
        assertTrue(dtoCaptor.getValue().getRemark().contains("采购入库成本"));
    }

    @Test
    @DisplayName("f) 三处都没有单价 → 事件金额为 null，监听器不写 0（跳过成本行）")
    void costAmountIsNotZeroWhenPriceMissing() {
        // 物料主档不存在 + 入库明细无单价
        when(materialArchiveMapper.selectById(MATERIAL_ID)).thenReturn(null);
        when(purchaseStockinItemMapper.selectList(any())).thenReturn(java.util.List.of());

        assertTrue(service.confirmStockin("MTC20261024TEST01", "1"));

        PurchaseStockInEvent event = capturePublishedEvent();
        assertNull(event.getTotalAmount(), "无单价时金额必须为 null（不能落 0）");
        assertNull(event.getUnitPrice());

        MaterialTraceCodeService traceCodeService = org.mockito.Mockito.mock(MaterialTraceCodeService.class);
        CostRecordService costRecordService = org.mockito.Mockito.mock(CostRecordService.class);
        new PurchaseStockInEventListener(traceCodeService, costRecordService).handlePurchaseStockInEvent(event);

        // cost_records.amount 为 NOT NULL → 不写行（禁止金额 0 的错账），只留 WARN
        verify(costRecordService, never()).create(any());
    }
}
