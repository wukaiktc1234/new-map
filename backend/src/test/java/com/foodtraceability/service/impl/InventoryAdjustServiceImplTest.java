package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryAdjustCreateDTO;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.InventoryAdjust;
import com.foodtraceability.entity.InventoryAdjustItem;
import com.foodtraceability.mapper.InventoryAdjustItemMapper;
import com.foodtraceability.mapper.InventoryAdjustMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryAdjustServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：调整单创建（AD 单号、pending、总数量/总金额、明细元→分与调整后数量）、
 * 单号查询（明细 + 审批历史三步）、草稿态更新/删除闸门、审批（approve/reject + 安全上下文审批人）、
 * 执行调整（正数入库/负数出库、类型映射、source_type=ADJUST、非法 ID 与零量跳过）。</p>
 *
 * <p>另以特征化断言固化两处已知口径问题（只报不改）：</p>
 * <ul>
 *   <li>创建时表头 totalAdjustAmount 直接累加传入值（按"元"），而明细 adjustAmount 已 ×100 存"分" —— 同一单内两种口径；</li>
 *   <li>更新时表头改用明细的分值累加（与创建口径相反）。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryAdjustServiceImplTest {

    @Mock
    private InventoryAdjustMapper inventoryAdjustMapper;

    @Mock
    private InventoryAdjustItemMapper inventoryAdjustItemMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryAdjustServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryAdjust.class);
        TableInfoHelper.initTableInfo(assistant, InventoryAdjustItem.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", inventoryAdjustMapper);
        SecurityContextHolder.clearContext();
        when(inventoryAdjustMapper.insert(any(InventoryAdjust.class))).thenReturn(1);
        when(inventoryAdjustMapper.updateById(any(InventoryAdjust.class))).thenReturn(1);
        when(inventoryAdjustMapper.deleteById(any(Long.class))).thenReturn(1);
        when(inventoryAdjustItemMapper.insert(any(InventoryAdjustItem.class))).thenReturn(1);
        when(inventoryAdjustItemMapper.delete(any())).thenReturn(0);
        when(inventoryAdjustItemMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubAdjust(InventoryAdjust a) {
        when(inventoryAdjustMapper.selectOne(any())).thenReturn(a);
        when(inventoryAdjustMapper.selectOne(any(), anyBoolean())).thenReturn(a);
    }

    private static InventoryAdjust adjust(String code, String status, String type, String warehouseId) {
        InventoryAdjust a = new InventoryAdjust();
        a.setAdjustId(1L);
        a.setAdjustCode(code);
        a.setStatus(status);
        a.setAdjustType(type);
        a.setWarehouseId(warehouseId);
        a.setParticipatingDepts("[\"采购部\",\"仓储部\"]");
        return a;
    }

    private static InventoryAdjustItem item(String materialId, String qty, Long unitCost) {
        InventoryAdjustItem i = new InventoryAdjustItem();
        i.setAdjustItemId(11L);
        i.setAdjustId(1L);
        i.setMaterialId(materialId);
        i.setAdjustQuantity(new BigDecimal(qty));
        i.setUnitCost(unitCost);
        i.setBatchNo("B1");
        i.setReason("盘点差异");
        return i;
    }

    private static Map<String, Object> itemMap(String materialId, String before, String adjust, String unitCost, String amount) {
        Map<String, Object> m = new HashMap<>();
        m.put("materialId", materialId);
        m.put("materialName", "物料" + materialId);
        m.put("specification", "规格");
        m.put("unit", "kg");
        if (before != null) m.put("beforeQuantity", before);
        m.put("adjustQuantity", adjust);
        if (unitCost != null) m.put("unitCost", unitCost);
        if (amount != null) m.put("adjustAmount", amount);
        m.put("batchNo", "B1");
        m.put("reason", "盘点差异");
        return m;
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("getAdjustPage：实体转 Map（业务编号作 ID、类型名/状态名、参与部门解析、空时间空串）")
    void getAdjustPage_convertsToMap() {
        Page<InventoryAdjust> page = new Page<>(1, 10);
        InventoryAdjust a = adjust("AD1", "pending", "gain", "10");
        page.setRecords(List.of(a));
        when(inventoryAdjustMapper.selectAdjustPage(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        IPage<Map<String, Object>> result = service.getAdjustPage(page, "gain", "pending", "10", "kw", "2026-10-01", "2026-10-31");

        Map<String, Object> row = result.getRecords().get(0);
        assertEquals("AD1", row.get("adjustId"));
        assertEquals("盘盈调整", row.get("adjustTypeName"));
        assertEquals("待审批", row.get("statusName"));
        assertEquals("", row.get("approveTime"));
        @SuppressWarnings("unchecked")
        List<String> depts = (List<String>) row.get("participatingDepts");
        assertEquals(List.of("采购部", "仓储部"), depts);
    }

    @Test
    @DisplayName("getAdjustByCode：不存在返回 null；存在则装配明细与审批历史（提交一步）")
    void getAdjustByCode_assemblesDetail() {
        stubAdjust(null);
        assertNull(service.getAdjustByCode("MISS"));

        InventoryAdjust a = adjust("AD1", "pending", "loss", "10");
        a.setApplyTime(LocalDateTime.of(2026, 10, 1, 9, 0));
        stubAdjust(a);
        when(inventoryAdjustItemMapper.selectList(any())).thenReturn(List.of(item("100", "-2", 500L)));

        Map<String, Object> result = service.getAdjustByCode("AD1");

        assertEquals("AD1", result.get("adjustCode"));
        assertEquals("盘亏调整", result.get("adjustTypeName"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("11", items.get(0).get("adjustItemId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) result.get("approvalHistory");
        assertEquals(1, history.size());
        assertEquals("submit", history.get(0).get("step"));
        assertEquals("2026-10-01 09:00:00", history.get(0).get("time"));
    }

    @Test
    @DisplayName("getAdjustByCode：已审批/已完成时审批历史追加 approve 与 execute；驳回时为 reject")
    void getAdjustByCode_buildsHistorySteps() {
        InventoryAdjust approved = adjust("AD2", "completed", "gain", "10");
        approved.setApproveTime(LocalDateTime.of(2026, 10, 1, 10, 0));
        approved.setCompleteTime(LocalDateTime.of(2026, 10, 1, 11, 0));
        stubAdjust(approved);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history =
                (List<Map<String, Object>>) service.getAdjustByCode("AD2").get("approvalHistory");
        assertEquals(3, history.size());
        assertEquals("approve", history.get(1).get("step"));
        assertEquals("execute", history.get(2).get("step"));

        InventoryAdjust rejected = adjust("AD3", "rejected", "gain", "10");
        rejected.setApproveTime(LocalDateTime.of(2026, 10, 1, 10, 0));
        stubAdjust(rejected);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rejectedHistory =
                (List<Map<String, Object>>) service.getAdjustByCode("AD3").get("approvalHistory");
        assertEquals("reject", rejectedHistory.get(1).get("step"));
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("createAdjust：AD 单号、pending、总数量累加；明细元→分且调整后数量=调整前+调整量")
    void createAdjust_setsHeaderAndItems() {
        InventoryAdjust header = new InventoryAdjust();
        header.setAdjustId(1L);

        InventoryAdjust result = service.createAdjust(header, List.of(
                itemMap("100", "10", "2", "5.50", "11"),
                itemMap("101", null, "-1", null, null)
        ));

        assertSame(header, result);
        assertTrue(result.getAdjustCode().startsWith("AD"));
        assertEquals(16, result.getAdjustCode().length());
        assertEquals("pending", result.getStatus());
        assertNotNull(result.getApplyTime());
        assertEquals(0, BigDecimal.ONE.compareTo(result.getTotalAdjustQuantity()), "2 + (-1) = 1");
        assertEquals(11L, result.getTotalAdjustAmount(), "特征化：表头按传入值(元)直接累加，未×100");

        ArgumentCaptor<InventoryAdjustItem> captor = ArgumentCaptor.forClass(InventoryAdjustItem.class);
        verify(inventoryAdjustItemMapper, times(2)).insert(captor.capture());
        InventoryAdjustItem first = captor.getAllValues().get(0);
        assertEquals("100", first.getMaterialId());
        assertEquals(0, new BigDecimal("10").compareTo(first.getBeforeQuantity()));
        assertEquals(0, new BigDecimal("12").compareTo(first.getAfterQuantity()));
        assertEquals(550L, first.getUnitCost(), "5.50 元 = 550 分");
        assertEquals(1100L, first.getAdjustAmount(), "11 元 = 1100 分（明细按分，与表头口径不一致）");
        assertEquals("盘点差异", first.getReason());
        assertNull(captor.getAllValues().get(1).getAfterQuantity(), "调整前为空时不计算调整后数量");
    }

    @Test
    @DisplayName("createAdjust：无金额时表头总金额为 0")
    void createAdjust_noAmount_headerZero() {
        InventoryAdjust result = service.createAdjust(new InventoryAdjust(), List.of(itemMap("100", "1", "1", null, null)));
        assertEquals(0L, result.getTotalAdjustAmount());
    }

    // ==================== 更新 / 删除 ====================

    @Test
    @DisplayName("updateAdjust：不存在/非 pending 拒绝；成功重写明细并把表头金额按分累加")
    void updateAdjust_guardsAndRewrite() {
        stubAdjust(null);
        assertThrows(RuntimeException.class, () -> service.updateAdjust("MISS", new InventoryAdjustCreateDTO()));

        stubAdjust(adjust("AD1", "approved", "gain", "10"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateAdjust("AD1", new InventoryAdjustCreateDTO()));
        assertTrue(ex.getMessage().contains("不允许更新"));

        InventoryAdjust draft = adjust("AD2", "pending", "gain", "10");
        draft.setAdjustId(2L);
        stubAdjust(draft);
        InventoryAdjustCreateDTO dto = new InventoryAdjustCreateDTO();
        dto.setAdjustType("loss");
        dto.setWarehouseId("20");
        dto.setParticipatingDepts(List.of("采购部"));
        InventoryAdjustCreateDTO.AdjustItemDTO i1 = new InventoryAdjustCreateDTO.AdjustItemDTO();
        i1.setMaterialId("100");
        i1.setBeforeQuantity(new BigDecimal("10"));
        i1.setAdjustQuantity(new BigDecimal("-2"));
        i1.setUnitCost("5.00");
        dto.setItems(List.of(i1));

        InventoryAdjust result = service.updateAdjust("AD2", dto);

        assertEquals("loss", result.getAdjustType());
        assertEquals("20", result.getWarehouseId());
        assertEquals("[采购部]", result.getParticipatingDepts());
        assertEquals(0, new BigDecimal("-2").compareTo(result.getTotalAdjustQuantity()));
        assertEquals(-1000L, result.getTotalAdjustAmount(),
                "特征化：更新按明细分累加（-2 × 5.00 元 = -1000 分），与创建口径（按元累加）不一致");
        verify(inventoryAdjustItemMapper).delete(any());
        verify(inventoryAdjustItemMapper).insert(any(InventoryAdjustItem.class));
    }

    @Test
    @DisplayName("updateAdjust：参与部门为空时置 null")
    void updateAdjust_emptyDepts_null() {
        InventoryAdjust draft = adjust("AD2", "pending", "gain", "10");
        stubAdjust(draft);
        InventoryAdjustCreateDTO dto = new InventoryAdjustCreateDTO();
        dto.setParticipatingDepts(new ArrayList<>());
        dto.setItems(new ArrayList<>());

        service.updateAdjust("AD2", dto);

        assertNull(draft.getParticipatingDepts());
    }

    @Test
    @DisplayName("deleteAdjust：不存在/非 pending 拒绝；草稿先删明细再逻辑删除")
    void deleteAdjust_guardsAndCascade() {
        stubAdjust(null);
        assertThrows(RuntimeException.class, () -> service.deleteAdjust("MISS"));

        stubAdjust(adjust("AD1", "completed", "gain", "10"));
        assertThrows(RuntimeException.class, () -> service.deleteAdjust("AD1"));

        stubAdjust(adjust("AD2", "pending", "gain", "10"));
        service.deleteAdjust("AD2");
        verify(inventoryAdjustItemMapper).delete(any());
        verify(inventoryAdjustMapper).deleteById(1L);
    }

    // ==================== 审批 ====================

    @Test
    @DisplayName("approveAdjust：通过/驳回 + 审批人取自安全上下文，非 pending 拒绝")
    void approveAdjust_transitions() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor", null));
        stubAdjust(adjust("AD1", "pending", "gain", "10"));
        InventoryAdjust approved = service.approveAdjust("AD1", true, "同意");
        assertEquals("approved", approved.getStatus());
        assertEquals("auditor", approved.getApproveUserName());
        assertNotNull(approved.getApproveTime());

        stubAdjust(adjust("AD2", "pending", "gain", "10"));
        assertEquals("rejected", service.approveAdjust("AD2", false, "不同意").getStatus());

        stubAdjust(adjust("AD3", "completed", "gain", "10"));
        assertThrows(RuntimeException.class, () -> service.approveAdjust("AD3", true, null));
    }

    @Test
    @DisplayName("approveAdjust：未登录时审批人写空串")
    void approveAdjust_noAuth_emptyApprover() {
        stubAdjust(adjust("AD1", "pending", "gain", "10"));
        assertEquals("", service.approveAdjust("AD1", true, null).getApproveUserName());
    }

    // ==================== 执行 ====================

    @Test
    @DisplayName("executeAdjust：不存在/非 approved 拒绝")
    void executeAdjust_guards() {
        stubAdjust(null);
        assertThrows(RuntimeException.class, () -> service.executeAdjust("MISS"));

        stubAdjust(adjust("AD1", "pending", "gain", "10"));
        assertThrows(RuntimeException.class, () -> service.executeAdjust("AD1"));
        verify(inventoryService, never()).increaseInventory(any());
        verify(inventoryService, never()).decreaseInventory(any());
    }

    @Test
    @DisplayName("executeAdjust：正数走入库（gain→类型3、other→5、source=ADJUST），并置 completed")
    void executeAdjust_positive_usesIncrease() {
        stubAdjust(adjust("AD1", "approved", "gain", "10"));
        when(inventoryAdjustItemMapper.selectList(any())).thenReturn(List.of(item("100", "2", 500L)));

        InventoryAdjust result = service.executeAdjust("AD1");

        ArgumentCaptor<InventoryIncreaseDTO> captor = ArgumentCaptor.forClass(InventoryIncreaseDTO.class);
        verify(inventoryService).increaseInventory(captor.capture());
        InventoryIncreaseDTO dto = captor.getValue();
        assertEquals(100L, dto.getMaterialId());
        assertEquals(10L, dto.getWarehouseId());
        assertEquals(0, new BigDecimal("2").compareTo(dto.getQuantity()));
        assertEquals(3, dto.getTransactionType(), "盘盈 → 3");
        assertEquals("AD1", dto.getReferenceNo());
        assertEquals("inventory_adjust", dto.getReferenceType());
        assertEquals("ADJUST", dto.getSourceType());
        assertEquals(500L, dto.getUnitCost());
        assertEquals("completed", result.getStatus());
        assertNotNull(result.getCompleteTime());
    }

    @Test
    @DisplayName("executeAdjust：负数走出库并取反；loss→4、temp_loss→5、weight_diff→4、other→6")
    void executeAdjust_negative_usesDecreaseWithTypeMapping() {
        stubAdjust(adjust("AD1", "approved", "loss", "10"));
        when(inventoryAdjustItemMapper.selectList(any())).thenReturn(List.of(item("100", "-3", null)));

        service.executeAdjust("AD1");

        ArgumentCaptor<InventoryDecreaseDTO> captor = ArgumentCaptor.forClass(InventoryDecreaseDTO.class);
        verify(inventoryService).decreaseInventory(captor.capture());
        assertEquals(0, new BigDecimal("3").compareTo(captor.getValue().getQuantity()), "负数取反后按正数扣减");
        assertEquals(4, captor.getValue().getTransactionType());
        assertEquals("ADJUST", captor.getValue().getSourceType());
    }

    @Test
    @DisplayName("executeAdjust：类型映射矩阵（temp_loss→5、weight_diff→4、other→6、null→6）")
    void executeAdjust_decreaseTypeMatrix() {
        String[][] cases = {{"temp_loss", "5"}, {"weight_diff", "4"}, {"other", "6"}, {null, "6"}};
        for (String[] c : cases) {
            stubAdjust(adjust("AD1", "approved", c[0], "10"));
            when(inventoryAdjustItemMapper.selectList(any())).thenReturn(List.of(item("100", "-1", null)));
            service.executeAdjust("AD1");
        }

        ArgumentCaptor<InventoryDecreaseDTO> captor = ArgumentCaptor.forClass(InventoryDecreaseDTO.class);
        verify(inventoryService, times(4)).decreaseInventory(captor.capture());
        List<InventoryDecreaseDTO> all = captor.getAllValues();
        assertEquals(5, all.get(0).getTransactionType());
        assertEquals(4, all.get(1).getTransactionType());
        assertEquals(6, all.get(2).getTransactionType());
        assertEquals(6, all.get(3).getTransactionType());
    }

    @Test
    @DisplayName("executeAdjust：零量/空量/非法物料ID/非法仓库ID 全部跳过，但状态仍 completed")
    void executeAdjust_skipsInvalidItems() {
        stubAdjust(adjust("AD1", "approved", "gain", "not-a-number"));
        when(inventoryAdjustItemMapper.selectList(any())).thenReturn(List.of(
                item("100", "0", null),
                item(null, "1", null),
                item("MAT-X", "1", null)
        ));

        InventoryAdjust result = service.executeAdjust("AD1");

        verify(inventoryService, never()).increaseInventory(any());
        verify(inventoryService, never()).decreaseInventory(any());
        assertEquals("completed", result.getStatus());
    }
}
