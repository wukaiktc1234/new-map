package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.SelfPurchase;
import com.foodtraceability.entity.SelfPurchaseItem;
import com.foodtraceability.mapper.SelfPurchaseItemMapper;
import com.foodtraceability.mapper.SelfPurchaseMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.TraceCodeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SelfPurchaseServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：自采单创建（编号前缀 SP、pending/unreimbursed 初始态、明细指派父 ID 与状态、主表失败不落明细）、
 * 入库（仅 pending 可入库、逐条生成追溯码 + 库存增加 sourceType=QUICK_STOCKIN、明细置 completed）、
 * 报销（仅 completed 可报销）、取消（completed 不可取消）、以及查询委托。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelfPurchaseServiceImplTest {

    @Mock
    private SelfPurchaseMapper selfPurchaseMapper;

    @Mock
    private SelfPurchaseItemMapper selfPurchaseItemMapper;

    @Mock
    private TraceCodeService traceCodeService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private SelfPurchaseServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SelfPurchase.class);
        TableInfoHelper.initTableInfo(assistant, SelfPurchaseItem.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", selfPurchaseMapper);
        when(selfPurchaseMapper.insert(any(SelfPurchase.class))).thenReturn(1);
        when(selfPurchaseMapper.updateById(any(SelfPurchase.class))).thenReturn(1);
        when(selfPurchaseItemMapper.insert(any(SelfPurchaseItem.class))).thenReturn(1);
        when(selfPurchaseItemMapper.updateById(any(SelfPurchaseItem.class))).thenReturn(1);
        when(selfPurchaseItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(traceCodeService.generateTraceCodes(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
    }

    private static SelfPurchase purchase(String id, String status) {
        SelfPurchase p = new SelfPurchase();
        p.setSelfPurchaseId(id);
        p.setStatus(status);
        return p;
    }

    private static SelfPurchaseItem item(Long productId, String name, int qty) {
        SelfPurchaseItem i = new SelfPurchaseItem();
        i.setProductId(productId);
        i.setProductName(name);
        i.setQuantity(qty);
        return i;
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("createSelfPurchase：生成 SP 前缀编号，主表 pending/unreimbursed，明细挂父 ID 并置 pending")
    void create_setsHeaderAndItems() {
        SelfPurchase header = new SelfPurchase();
        SelfPurchaseItem i1 = item(100L, "土豆", 2);
        SelfPurchaseItem i2 = item(101L, "牛肉", 1);

        assertTrue(service.createSelfPurchase(header, List.of(i1, i2)));

        assertNotNull(header.getPurchaseNumber());
        assertTrue(header.getPurchaseNumber().startsWith("SP"));
        assertEquals(14, header.getPurchaseNumber().length() - 2, "编号后缀为 yyyyMMddHHmmss（14 位）");
        assertEquals("pending", header.getStatus());
        assertEquals("unreimbursed", header.getReimburseStatus());

        ArgumentCaptor<SelfPurchaseItem> captor = ArgumentCaptor.forClass(SelfPurchaseItem.class);
        verify(selfPurchaseItemMapper, times(2)).insert(captor.capture());
        for (SelfPurchaseItem saved : captor.getAllValues()) {
            assertEquals(header.getSelfPurchaseId(), saved.getSelfPurchaseId(), "明细必须挂到主表 ID 上");
            assertEquals("pending", saved.getStatus());
            assertEquals("pending", saved.getTraceCodeStatus());
        }
    }

    @Test
    @DisplayName("createSelfPurchase：主表保存失败时不写明细，返回 false")
    void create_headerSaveFails_skipsItems() {
        when(selfPurchaseMapper.insert(any(SelfPurchase.class))).thenReturn(0);

        assertFalse(service.createSelfPurchase(new SelfPurchase(), List.of(item(100L, "土豆", 1))));
        verify(selfPurchaseItemMapper, never()).insert(any(SelfPurchaseItem.class));
    }

    // ==================== 入库 ====================

    @Test
    @DisplayName("inboundSelfPurchase：单据不存在或非 pending 时返回 false，且不生成追溯码")
    void inbound_guards() {
        when(selfPurchaseMapper.selectById("SP404")).thenReturn(null);
        assertFalse(service.inboundSelfPurchase("SP404"));

        when(selfPurchaseMapper.selectById("SP1")).thenReturn(purchase("SP1", "completed"));
        assertFalse(service.inboundSelfPurchase("SP1"));

        verify(traceCodeService, never()).generateTraceCodes(any(), any(), any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).increaseInventory(any());
    }

    @Test
    @DisplayName("inboundSelfPurchase：逐条生成追溯码 + 库存增加（类型1/自采来源），明细与主表置 completed")
    void inbound_generatesTraceCodesAndIncreasesStock() {
        SelfPurchase header = purchase("SP1", "pending");
        when(selfPurchaseMapper.selectById("SP1")).thenReturn(header);
        when(selfPurchaseItemMapper.selectList(any())).thenReturn(List.of(
                item(100L, "土豆", 3),
                item(101L, "牛肉", 2)
        ));

        assertTrue(service.inboundSelfPurchase("SP1"));

        verify(traceCodeService).generateTraceCodes(eq(100L), eq("土豆"), eq(3), eq("self"), eq("SP1"),
                anyString(), eq(1L), eq("主仓库"));
        verify(traceCodeService).generateTraceCodes(eq(101L), eq("牛肉"), eq(2), eq("self"), eq("SP1"),
                anyString(), eq(1L), eq("主仓库"));

        ArgumentCaptor<InventoryIncreaseDTO> captor = ArgumentCaptor.forClass(InventoryIncreaseDTO.class);
        verify(inventoryService, times(2)).increaseInventory(captor.capture());
        InventoryIncreaseDTO first = captor.getAllValues().get(0);
        assertEquals(100L, first.getMaterialId());
        assertEquals(1L, first.getWarehouseId());
        assertEquals(0, new BigDecimal("3").compareTo(first.getQuantity()));
        assertEquals(1, first.getTransactionType());
        assertEquals("self_purchase", first.getReferenceType());
        assertEquals("SP1", first.getReferenceNo());
        assertEquals("QUICK_STOCKIN", first.getSourceType());

        ArgumentCaptor<SelfPurchaseItem> itemCaptor = ArgumentCaptor.forClass(SelfPurchaseItem.class);
        verify(selfPurchaseItemMapper, times(2)).updateById(itemCaptor.capture());
        assertTrue(itemCaptor.getAllValues().stream().allMatch(i -> "completed".equals(i.getStatus())));
        assertTrue(itemCaptor.getAllValues().stream().allMatch(i -> "completed".equals(i.getTraceCodeStatus())));

        assertEquals("completed", header.getStatus());
        assertNotNull(header.getPurchaseDate());
        verify(selfPurchaseMapper).updateById(header);
    }

    @Test
    @DisplayName("inboundSelfPurchase：仓库取自采单自身（多仓不再硬编码 1/主仓库）（P1-4）")
    void inbound_usesHeaderWarehouse() {
        SelfPurchase header = purchase("SP3", "pending");
        header.setWarehouseId(7L);
        header.setWarehouseName("二号仓");
        when(selfPurchaseMapper.selectById("SP3")).thenReturn(header);
        when(selfPurchaseItemMapper.selectList(any())).thenReturn(List.of(item(100L, "土豆", 1)));

        assertTrue(service.inboundSelfPurchase("SP3"));

        verify(traceCodeService).generateTraceCodes(eq(100L), eq("土豆"), eq(1), eq("self"), eq("SP3"),
                anyString(), eq(7L), eq("二号仓"));
        ArgumentCaptor<InventoryIncreaseDTO> captor = ArgumentCaptor.forClass(InventoryIncreaseDTO.class);
        verify(inventoryService).increaseInventory(captor.capture());
        assertEquals(7L, captor.getValue().getWarehouseId(), "入库仓库必须来自自采单");
    }

    @Test
    @DisplayName("inboundSelfPurchase：没有明细时也把主表置 completed")
    void inbound_noItems_stillCompletes() {
        SelfPurchase header = purchase("SP2", "pending");
        when(selfPurchaseMapper.selectById("SP2")).thenReturn(header);

        assertTrue(service.inboundSelfPurchase("SP2"));

        verify(traceCodeService, never()).generateTraceCodes(any(), any(), any(), any(), any(), any(), any(), any());
        assertEquals("completed", header.getStatus());
    }

    // ==================== 报销 / 取消 / 查询 ====================

    @Test
    @DisplayName("reimburseSelfPurchase：仅 completed 可报销；成功置 reimbursed")
    void reimburse_guardsAndSuccess() {
        when(selfPurchaseMapper.selectById("A")).thenReturn(null);
        assertFalse(service.reimburseSelfPurchase("A"));

        when(selfPurchaseMapper.selectById("B")).thenReturn(purchase("B", "pending"));
        assertFalse(service.reimburseSelfPurchase("B"));

        SelfPurchase completed = purchase("C", "completed");
        when(selfPurchaseMapper.selectById("C")).thenReturn(completed);
        assertTrue(service.reimburseSelfPurchase("C"));
        assertEquals("reimbursed", completed.getReimburseStatus());
    }

    @Test
    @DisplayName("cancelSelfPurchase：completed 不可取消；其余置 cancelled")
    void cancel_guardsAndSuccess() {
        when(selfPurchaseMapper.selectById("A")).thenReturn(null);
        assertFalse(service.cancelSelfPurchase("A"));

        when(selfPurchaseMapper.selectById("B")).thenReturn(purchase("B", "completed"));
        assertFalse(service.cancelSelfPurchase("B"));

        SelfPurchase pending = purchase("C", "pending");
        when(selfPurchaseMapper.selectById("C")).thenReturn(pending);
        assertTrue(service.cancelSelfPurchase("C"));
        assertEquals("cancelled", pending.getStatus());
    }

    @Test
    @DisplayName("getSelfPurchaseById / getSelfPurchaseItems 委托")
    void queries_delegate() {
        SelfPurchase stored = purchase("SP1", "pending");
        when(selfPurchaseMapper.selectById("SP1")).thenReturn(stored);
        List<SelfPurchaseItem> items = List.of(item(100L, "土豆", 1));
        when(selfPurchaseItemMapper.selectList(any())).thenReturn(items);

        assertSame(stored, service.getSelfPurchaseById("SP1"));
        assertSame(items, service.getSelfPurchaseItems("SP1"));
    }
}
