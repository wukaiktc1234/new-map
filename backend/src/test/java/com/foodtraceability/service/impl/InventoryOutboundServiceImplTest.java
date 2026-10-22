package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryOutboundCreateDTO;
import com.foodtraceability.entity.InventoryOutbound;
import com.foodtraceability.entity.InventoryOutboundItem;
import com.foodtraceability.mapper.InventoryOutboundItemMapper;
import com.foodtraceability.mapper.InventoryOutboundMapper;
import com.foodtraceability.service.InventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryOutboundServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：出库单创建（OB 单号、pending、总数量累加、明细零实发）、单号查询（含明细装配与字段映射）、
 * 草稿态更新/删除闸门、审批（approve/reject + 审批人取安全上下文）、执行出库
 * （实发=申请、按类型映射 transactionType、sourceType=OTHER、金额=单价×数量）、
 * 以及仓库/物料 ID 非法时跳过扣减的容错。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryOutboundServiceImplTest {

    @Mock
    private InventoryOutboundMapper inventoryOutboundMapper;

    @Mock
    private InventoryOutboundItemMapper inventoryOutboundItemMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryOutboundServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryOutbound.class);
        TableInfoHelper.initTableInfo(assistant, InventoryOutboundItem.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", inventoryOutboundMapper);
        SecurityContextHolder.clearContext();
        when(inventoryOutboundMapper.insert(any(InventoryOutbound.class))).thenReturn(1);
        when(inventoryOutboundMapper.updateById(any(InventoryOutbound.class))).thenReturn(1);
        when(inventoryOutboundMapper.deleteById(any(Long.class))).thenReturn(1);
        when(inventoryOutboundItemMapper.insert(any(InventoryOutboundItem.class))).thenReturn(1);
        when(inventoryOutboundItemMapper.updateById(any(InventoryOutboundItem.class))).thenReturn(1);
        when(inventoryOutboundItemMapper.delete(any())).thenReturn(0);
        when(inventoryOutboundItemMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * MyBatis-Plus 的 {@code ServiceImpl.getOne()} 会走 {@code selectOne(wrapper, throwEx)} 重载，
     * 单参默认方法在 Mock 上不会自动转发，因此两个重载都要打桩。
     */
    private void stubOutboundReturning(InventoryOutbound o) {
        when(inventoryOutboundMapper.selectOne(any())).thenReturn(o);
        when(inventoryOutboundMapper.selectOne(any(), anyBoolean())).thenReturn(o);
    }

    private static InventoryOutbound outbound(String code, String status, String type, String warehouseId) {
        InventoryOutbound o = new InventoryOutbound();
        o.setOutboundId(1L);
        o.setOutboundCode(code);
        o.setStatus(status);
        o.setOutboundType(type);
        o.setWarehouseId(warehouseId);
        o.setOutboundDate(LocalDate.of(2026, 10, 1));
        o.setTotalQuantity(new BigDecimal("3"));
        return o;
    }

    private static InventoryOutboundItem item(String materialId, String qty, Long unitCost) {
        InventoryOutboundItem i = new InventoryOutboundItem();
        i.setOutboundItemId(11L);
        i.setOutboundId(1L);
        i.setMaterialId(materialId);
        i.setMaterialName("物料" + materialId);
        i.setRequestQuantity(new BigDecimal(qty));
        i.setUnitCost(unitCost);
        return i;
    }

    private static Map<String, Object> itemMap(String materialId, String qty) {
        Map<String, Object> m = new HashMap<>();
        m.put("materialId", materialId);
        m.put("materialName", "物料" + materialId);
        m.put("specification", "规格");
        m.put("unit", "kg");
        m.put("requestQuantity", qty);
        m.put("batchNo", "B1");
        m.put("remark", "备注");
        return m;
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("getOutboundPage：分页实体转 Map，含类型名/状态名/日期字符串")
    void getOutboundPage_convertsToMap() {
        Page<InventoryOutbound> page = new Page<>(1, 10);
        InventoryOutbound o = outbound("OB1", "pending", "requisition", "10");
        o.setApplyTime(LocalDateTime.of(2026, 10, 1, 9, 30, 0));
        page.setRecords(List.of(o));
        when(inventoryOutboundMapper.selectOutboundPage(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        IPage<Map<String, Object>> result = service.getOutboundPage(page, "requisition", "pending", "10", "kw", "2026-10-01", "2026-10-31");

        assertEquals(1, result.getRecords().size());
        Map<String, Object> row = result.getRecords().get(0);
        assertEquals("OB1", row.get("outboundId"), "前端用业务编号作为 ID");
        assertEquals("领料出库", row.get("outboundTypeName"));
        assertEquals("待审批", row.get("statusName"));
        assertEquals("2026-10-01", row.get("outboundDate"));
        assertEquals("2026-10-01 09:30:00", row.get("applyTime"));
        assertEquals("", row.get("approveTime"), "空时间返回空串而非 null");
    }

    @Test
    @DisplayName("getOutboundByCode：不存在返回 null；存在则装配明细列表")
    void getOutboundByCode_assemblesItems() {
        stubOutboundReturning(null);
        assertNull(service.getOutboundByCode("MISS"));

        InventoryOutbound o = outbound("OB1", "pending", "sale", "10");
        stubOutboundReturning(o);
        when(inventoryOutboundItemMapper.selectList(any())).thenReturn(List.of(item("100", "2", 500L)));

        Map<String, Object> result = service.getOutboundByCode("OB1");

        assertEquals("OB1", result.get("outboundCode"));
        assertEquals("销售出库", result.get("outboundTypeName"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("100", items.get(0).get("materialId"));
        assertEquals("11", items.get(0).get("outboundItemId"), "明细 ID 以字符串返回");
        assertEquals(500L, items.get(0).get("unitCost"));
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("createOutbound：OB+时间戳单号、pending、applyTime、总数量累加、总金额 0、明细零实发")
    void createOutbound_setsHeaderAndItems() {
        InventoryOutbound header = new InventoryOutbound();
        header.setOutboundId(1L);

        InventoryOutbound saved = service.createOutbound(header, List.of(
                itemMap("100", "1.5"),
                itemMap("101", "2.5")
        ));

        assertSame(header, saved);
        assertTrue(saved.getOutboundCode().startsWith("OB"));
        assertEquals(16, saved.getOutboundCode().length(), "OB + yyyyMMddHHmmss（14 位）");
        assertEquals("pending", saved.getStatus());
        assertEquals(0, new BigDecimal("4.0").compareTo(saved.getTotalQuantity()));
        assertEquals(0L, saved.getTotalAmount());
        assertNotNull(saved.getApplyTime());
        verify(inventoryOutboundMapper).insert(header);

        ArgumentCaptor<InventoryOutboundItem> captor = ArgumentCaptor.forClass(InventoryOutboundItem.class);
        verify(inventoryOutboundItemMapper, times(2)).insert(captor.capture());
        InventoryOutboundItem first = captor.getAllValues().get(0);
        assertEquals(1L, first.getOutboundId());
        assertEquals("100", first.getMaterialId());
        assertEquals("kg", first.getUnit());
        assertEquals("B1", first.getBatchNo());
        assertEquals(0, new BigDecimal("1.5").compareTo(first.getRequestQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(first.getActualQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getAllValues().get(1).getActualQuantity()));
    }

    @Test
    @DisplayName("createOutbound：明细缺少 requestQuantity 时不累加总数量")
    void createOutbound_missingQuantity_notCounted() {
        Map<String, Object> m = new HashMap<>();
        m.put("materialId", "100");

        InventoryOutbound saved = service.createOutbound(new InventoryOutbound(), List.of(m));

        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getTotalQuantity()));
        ArgumentCaptor<InventoryOutboundItem> captor = ArgumentCaptor.forClass(InventoryOutboundItem.class);
        verify(inventoryOutboundItemMapper).insert(captor.capture());
        assertNull(captor.getValue().getRequestQuantity());
        assertNull(captor.getValue().getMaterialName());
    }

    // ==================== 更新 / 删除 ====================

    @Test
    @DisplayName("updateOutbound：不存在 / 非 pending 拒绝；成功后删旧明细重算总数量")
    void updateOutbound_guardsAndRewriteItems() {
        stubOutboundReturning(null);
        assertThrows(RuntimeException.class, () -> service.updateOutbound("MISS", new InventoryOutboundCreateDTO()));

        stubOutboundReturning(outbound("OB1", "approved", "sale", "10"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateOutbound("OB1", new InventoryOutboundCreateDTO()));
        assertTrue(ex.getMessage().contains("不允许更新"));

        InventoryOutbound draft = outbound("OB2", "pending", "sale", "10");
        stubOutboundReturning(draft);
        InventoryOutboundCreateDTO dto = new InventoryOutboundCreateDTO();
        dto.setOutboundType("requisition");
        dto.setWarehouseId("20");
        dto.setTargetName("后厨");
        dto.setOutboundDate("2026-10-05");
        dto.setRemark("改单");
        InventoryOutboundCreateDTO.OutboundItemDTO i1 = new InventoryOutboundCreateDTO.OutboundItemDTO();
        i1.setMaterialId("100");
        i1.setRequestQuantity(new BigDecimal("1"));
        InventoryOutboundCreateDTO.OutboundItemDTO i2 = new InventoryOutboundCreateDTO.OutboundItemDTO();
        i2.setMaterialId("101");
        i2.setRequestQuantity(new BigDecimal("2"));
        dto.setItems(List.of(i1, i2));

        InventoryOutbound result = service.updateOutbound("OB2", dto);

        assertEquals("requisition", result.getOutboundType());
        assertEquals("20", result.getWarehouseId());
        assertEquals("后厨", result.getTargetName());
        assertEquals(LocalDate.of(2026, 10, 5), result.getOutboundDate());
        assertEquals(0, new BigDecimal("3").compareTo(result.getTotalQuantity()));
        verify(inventoryOutboundItemMapper).delete(any());
        verify(inventoryOutboundItemMapper, times(2)).insert(any(InventoryOutboundItem.class));
        verify(inventoryOutboundMapper).updateById(draft);
    }

    @Test
    @DisplayName("deleteOutbound：不存在 / 非 pending 拒绝；草稿删除先删明细再逻辑删主表")
    void deleteOutbound_guardsAndCascade() {
        stubOutboundReturning(null);
        assertThrows(RuntimeException.class, () -> service.deleteOutbound("MISS"));

        stubOutboundReturning(outbound("OB1", "completed", "sale", "10"));
        assertThrows(RuntimeException.class, () -> service.deleteOutbound("OB1"));

        InventoryOutbound draft = outbound("OB2", "pending", "sale", "10");
        stubOutboundReturning(draft);
        service.deleteOutbound("OB2");

        verify(inventoryOutboundItemMapper).delete(any());
        verify(inventoryOutboundMapper).deleteById(1L);
    }

    // ==================== 审批 ====================

    @Test
    @DisplayName("approveOutbound：通过/驳回写状态与审批时间，审批人取安全上下文用户名")
    void approveOutbound_mapsDecisionAndApprover() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null));
        stubOutboundReturning(outbound("OB1", "pending", "sale", "10"));

        InventoryOutbound approved = service.approveOutbound("OB1", true, "同意");

        assertEquals("approved", approved.getStatus());
        assertEquals("alice", approved.getApproveUserId());
        assertEquals("alice", approved.getApproveUserName());
        assertNotNull(approved.getApproveTime());

        InventoryOutbound pendingAgain = outbound("OB2", "pending", "sale", "10");
        stubOutboundReturning(pendingAgain);
        assertEquals("rejected", service.approveOutbound("OB2", false, "不同意").getStatus());
    }

    @Test
    @DisplayName("approveOutbound：未登录时审批人写空串；非 pending 拒绝审批")
    void approveOutbound_noAuthAndStatusGuard() {
        stubOutboundReturning(outbound("OB1", "pending", "sale", "10"));
        assertEquals("", service.approveOutbound("OB1", true, null).getApproveUserName());

        stubOutboundReturning(outbound("OB2", "completed", "sale", "10"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.approveOutbound("OB2", true, null));
        assertTrue(ex.getMessage().contains("不允许审批"));
    }

    // ==================== 执行出库 ====================

    @Test
    @DisplayName("executeOutbound：非 approved 拒绝；未找到拒绝")
    void executeOutbound_guards() {
        stubOutboundReturning(null);
        assertThrows(RuntimeException.class, () -> service.executeOutbound("MISS"));

        stubOutboundReturning(outbound("OB1", "pending", "sale", "10"));
        assertThrows(RuntimeException.class, () -> service.executeOutbound("OB1"));
        verify(inventoryService, never()).decreaseInventory(any());
    }

    @Test
    @DisplayName("executeOutbound：实发=申请、按类型映射交易类型、金额=单价×数量、状态 completed")
    void executeOutbound_deductsAndComputesAmount() {
        InventoryOutbound o = outbound("OB1", "approved", "requisition", "10");
        stubOutboundReturning(o);
        when(inventoryOutboundItemMapper.selectList(any())).thenReturn(List.of(
                item("100", "2", 500L),
                item("101", "3", null)
        ));

        InventoryOutbound result = service.executeOutbound("OB1");

        assertEquals("completed", result.getStatus());
        assertNotNull(result.getCompleteTime());
        assertEquals(1000L, result.getTotalAmount(), "500×2；第二条无单价不计");

        ArgumentCaptor<InventoryDecreaseDTO> captor = ArgumentCaptor.forClass(InventoryDecreaseDTO.class);
        verify(inventoryService, times(2)).decreaseInventory(captor.capture());
        InventoryDecreaseDTO first = captor.getAllValues().get(0);
        assertEquals(100L, first.getMaterialId());
        assertEquals(10L, first.getWarehouseId());
        assertEquals(0, new BigDecimal("2").compareTo(first.getQuantity()));
        assertEquals(2, first.getTransactionType(), "领料出库 → 2");
        assertEquals("OB1", first.getReferenceNo());
        assertEquals("inventory_outbound", first.getReferenceType());
        assertEquals("OTHER", first.getSourceType());

        ArgumentCaptor<InventoryOutboundItem> itemCaptor = ArgumentCaptor.forClass(InventoryOutboundItem.class);
        verify(inventoryOutboundItemMapper, times(3)).updateById(itemCaptor.capture());
        assertTrue(itemCaptor.getAllValues().stream()
                .allMatch(i -> i.getActualQuantity() != null
                        && i.getActualQuantity().compareTo(i.getRequestQuantity() == null ? BigDecimal.ZERO : i.getRequestQuantity()) == 0));
    }

    @Test
    @DisplayName("executeOutbound：销售出库映射交易类型 1；仓库 ID 非法时跳过扣减但仍置实发")
    void executeOutbound_invalidWarehouse_skipsDeduction() {
        InventoryOutbound o = outbound("OB2", "approved", "sale", "not-a-number");
        stubOutboundReturning(o);
        when(inventoryOutboundItemMapper.selectList(any())).thenReturn(List.of(item("100", "1", 100L)));

        service.executeOutbound("OB2");

        verify(inventoryService, never()).decreaseInventory(any());
        ArgumentCaptor<InventoryOutboundItem> captor = ArgumentCaptor.forClass(InventoryOutboundItem.class);
        verify(inventoryOutboundItemMapper, org.mockito.Mockito.atLeast(1)).updateById(captor.capture());
        assertEquals(0, BigDecimal.ONE.compareTo(captor.getAllValues().get(0).getActualQuantity()));
    }

    @Test
    @DisplayName("executeOutbound：物料 ID 非数字时跳过扣减（不抛错）")
    void executeOutbound_invalidMaterialId_skipsDeduction() {
        InventoryOutbound o = outbound("OB3", "approved", "other", "10");
        stubOutboundReturning(o);
        when(inventoryOutboundItemMapper.selectList(any())).thenReturn(List.of(item("MAT-X", "1", null)));

        service.executeOutbound("OB3");

        verify(inventoryService, never()).decreaseInventory(any());
        assertEquals(0L, o.getTotalAmount());
    }

}
