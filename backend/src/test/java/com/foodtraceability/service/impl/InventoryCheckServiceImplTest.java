package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.dto.InventoryCheckCreateDTO;
import com.foodtraceability.entity.InventoryCheck;
import com.foodtraceability.entity.InventoryCheckItem;
import com.foodtraceability.mapper.InventoryCheckItemMapper;
import com.foodtraceability.mapper.InventoryCheckMapper;
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
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventoryCheckServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：盘点单创建（单号/初始状态/时间戳）、按仓库查询、草稿态更新与删除的状态闸门、
 * 盘点条目保存（清旧增新、差异数量自动计算、金额元→分、非法值容错）、
 * 以及各类「不存在 / 非草稿」异常分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryCheckServiceImplTest {

    @Mock
    private InventoryCheckMapper checkMapper;

    @Mock
    private InventoryCheckItemMapper checkItemMapper;

    @InjectMocks
    private InventoryCheckServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InventoryCheck.class);
        TableInfoHelper.initTableInfo(assistant, InventoryCheckItem.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", checkMapper);
        when(checkMapper.insert(any(InventoryCheck.class))).thenReturn(1);
        when(checkMapper.updateById(any(InventoryCheck.class))).thenReturn(1);
        when(checkMapper.deleteById(any(Long.class))).thenReturn(1);
        when(checkItemMapper.delete(any())).thenReturn(0);
        when(checkItemMapper.deleteById(any(Long.class))).thenReturn(1);
        when(checkItemMapper.insert(any(InventoryCheckItem.class))).thenReturn(1);
    }

    private static InventoryCheck check(Long id, int status) {
        InventoryCheck c = new InventoryCheck();
        c.setCheckId(id);
        c.setCheckCode("CK-EXIST");
        c.setWarehouseId(10L);
        c.setCheckType(1);
        c.setCheckStatus(status);
        c.setCheckDate(LocalDate.of(2026, 10, 1));
        c.setRemark("旧备注");
        return c;
    }

    private static InventoryCheckCreateDTO createDTO() {
        InventoryCheckCreateDTO dto = new InventoryCheckCreateDTO();
        dto.setWarehouseId(10L);
        dto.setCheckType(2);
        dto.setCheckDate(LocalDate.of(2026, 10, 1));
        dto.setRemark("月末盘点");
        return dto;
    }

    // ==================== 创建 / 查询 ====================

    @Test
    @DisplayName("createCheck：生成 CK 前缀单号，状态置 0（待盘点），补齐时间戳并落库")
    void createCheck_generatesCodeAndDraftStatus() {
        InventoryCheck result = service.createCheck(createDTO());

        assertNotNull(result.getCheckCode());
        assertTrue(result.getCheckCode().startsWith("CK"));
        assertEquals(10L, result.getWarehouseId());
        assertEquals(2, result.getCheckType());
        assertEquals(0, result.getCheckStatus());
        assertEquals(LocalDate.of(2026, 10, 1), result.getCheckDate());
        assertEquals("月末盘点", result.getRemark());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(checkMapper).insert(result);
    }

    @Test
    @DisplayName("getChecksByWarehouse：按 warehouse_id 过滤并按 create_time 倒序")
    void getChecksByWarehouse_filtersAndOrders() {
        List<InventoryCheck> rows = List.of(check(1L, 0));
        when(checkMapper.selectList(any())).thenReturn(rows);

        assertSame(rows, service.getChecksByWarehouse(10L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<InventoryCheck>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(checkMapper).selectList(captor.capture());
        LambdaQueryWrapper<InventoryCheck> wrapper = (LambdaQueryWrapper<InventoryCheck>) captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(10L), "应按 warehouse_id 过滤");
        assertTrue(sql.toLowerCase().contains("order by"), "应带排序，实际 SQL 段: " + sql);
    }

    // ==================== 更新闸门 ====================

    @Test
    @DisplayName("updateCheck：盘点单不存在抛异常且不写库")
    void updateCheck_missing_throws() {
        when(checkMapper.selectById(404L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateCheck(404L, createDTO()));
        assertTrue(ex.getMessage().contains("盘点单不存在"));
        verify(checkMapper, never()).updateById(any(InventoryCheck.class));
    }

    @Test
    @DisplayName("updateCheck：非草稿状态拒绝更新（防止覆盖已审批/已完成结果）")
    void updateCheck_nonDraft_rejected() {
        when(checkMapper.selectById(1L)).thenReturn(check(1L, 2));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateCheck(1L, createDTO()));
        assertTrue(ex.getMessage().contains("不允许更新"));
        verify(checkMapper, never()).updateById(any(InventoryCheck.class));
    }

    @Test
    @DisplayName("updateCheck：草稿态只覆盖非空字段，返回重新查询的实体")
    void updateCheck_draft_updatesOnlyProvidedFields() {
        InventoryCheck existing = check(1L, 0);
        when(checkMapper.selectById(1L)).thenReturn(existing, check(1L, 0));

        InventoryCheckCreateDTO dto = new InventoryCheckCreateDTO();
        dto.setWarehouseId(20L);
        dto.setRemark(null); // 不覆盖
        service.updateCheck(1L, dto);

        ArgumentCaptor<InventoryCheck> captor = ArgumentCaptor.forClass(InventoryCheck.class);
        verify(checkMapper).updateById(captor.capture());
        InventoryCheck saved = captor.getValue();
        assertEquals(20L, saved.getWarehouseId());
        assertEquals(1, saved.getCheckType(), "未传的 checkType 应保持原值");
        assertEquals("旧备注", saved.getRemark(), "remark 为 null 时不得清空");
        assertNotNull(saved.getUpdateTime());
    }

    // ==================== 删除闸门 ====================

    @Test
    @DisplayName("deleteCheck：不存在 / 非草稿一律拒绝，不删条目也不删主表")
    void deleteCheck_guards() {
        when(checkMapper.selectById(404L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.deleteCheck(404L));

        when(checkMapper.selectById(2L)).thenReturn(check(2L, 3));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteCheck(2L));
        assertTrue(ex.getMessage().contains("不允许删除"));

        verify(checkItemMapper, never()).delete(any());
        verify(checkMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteCheck：草稿态先逐条删子表，再逻辑删除主表")
    void deleteCheck_draft_removesItemsThenCheck() {
        when(checkMapper.selectById(1L)).thenReturn(check(1L, 0));
        InventoryCheckItem item1 = new InventoryCheckItem();
        item1.setCheckItemId(101L);
        InventoryCheckItem item2 = new InventoryCheckItem();
        item2.setCheckItemId(102L);
        when(checkItemMapper.selectList(any())).thenReturn(List.of(item1, item2));

        service.deleteCheck(1L);

        verify(checkItemMapper).deleteById(101L);
        verify(checkItemMapper).deleteById(102L);
        verify(checkMapper).deleteById(1L);
    }

    // ==================== 盘点条目保存 ====================

    @Test
    @DisplayName("saveCheckItems：盘点单不存在抛异常")
    void saveCheckItems_missing_throws() {
        when(checkMapper.selectById(404L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.saveCheckItems(404L, List.of()));
        verify(checkItemMapper, never()).insert(any(InventoryCheckItem.class));
    }

    @Test
    @DisplayName("saveCheckItems：先清空旧条目，再按 map 落库；差异数量自动=实盘-账面，金额元→分")
    void saveCheckItems_replacesItemsAndComputesDiff() {
        InventoryCheck c = check(1L, 0);
        when(checkMapper.selectById(1L)).thenReturn(c);

        Map<String, Object> item = new HashMap<>();
        item.put("inventoryId", "9527");
        item.put("bookQty", "10.5");
        item.put("actualQty", 8);
        item.put("diffAmount", "12.34");
        item.put("reason", "破损");

        service.saveCheckItems(1L, List.of(item));

        verify(checkItemMapper).delete(any());
        ArgumentCaptor<InventoryCheckItem> captor = ArgumentCaptor.forClass(InventoryCheckItem.class);
        verify(checkItemMapper).insert(captor.capture());
        InventoryCheckItem saved = captor.getValue();
        assertEquals(1L, saved.getCheckId());
        assertEquals(9527L, saved.getInventoryId());
        assertEquals(new BigDecimal("10.5"), saved.getBookQty());
        assertEquals(new BigDecimal("8"), saved.getActualQty());
        assertEquals(0, new BigDecimal("-2.5").compareTo(saved.getDiffQty()), "差异数量应为 实盘-账面");
        assertEquals(1234L, saved.getDiffAmount(), "12.34 元 = 1234 分");
        assertEquals("破损", saved.getReason());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());

        assertEquals(1, c.getCheckStatus(), "保存条目后盘点单进入「盘点中」");
        verify(checkMapper).updateById(c);
    }

    @Test
    @DisplayName("saveCheckItems：条目里显式传 diffQty 时以传入值为准")
    void saveCheckItems_explicitDiffQtyWins() {
        when(checkMapper.selectById(1L)).thenReturn(check(1L, 0));

        Map<String, Object> item = new HashMap<>();
        item.put("bookQty", "5");
        item.put("actualQty", "5");
        item.put("diffQty", "3");
        service.saveCheckItems(1L, List.of(item));

        ArgumentCaptor<InventoryCheckItem> captor = ArgumentCaptor.forClass(InventoryCheckItem.class);
        verify(checkItemMapper).insert(captor.capture());
        assertEquals(new BigDecimal("3"), captor.getValue().getDiffQty());
    }

    @Test
    @DisplayName("saveCheckItems：空条目列表只清空旧数据并把状态置为盘点中")
    void saveCheckItems_emptyItems_onlyClearsAndUpdatesStatus() {
        InventoryCheck c = check(1L, 0);
        when(checkMapper.selectById(1L)).thenReturn(c);

        service.saveCheckItems(1L, null);

        verify(checkItemMapper).delete(any());
        verify(checkItemMapper, never()).insert(any(InventoryCheckItem.class));
        assertEquals(1, c.getCheckStatus());
        verify(checkMapper).updateById(c);
    }

    @Test
    @DisplayName("saveCheckItems：非法 inventoryId / 非数字数量 / 非数字金额均不抛错")
    void saveCheckItems_toleratesIllegalValues() {
        when(checkMapper.selectById(1L)).thenReturn(check(1L, 0));

        Map<String, Object> item = new HashMap<>();
        item.put("inventoryId", "not-a-number");
        item.put("bookQty", "abc");
        item.put("actualQty", null);
        item.put("diffAmount", "not-a-number");
        item.put("reason", 12345); // 非字符串也走 toString

        service.saveCheckItems(1L, List.of(item));

        ArgumentCaptor<InventoryCheckItem> captor = ArgumentCaptor.forClass(InventoryCheckItem.class);
        verify(checkItemMapper).insert(captor.capture());
        InventoryCheckItem saved = captor.getValue();
        assertNull(saved.getInventoryId());
        assertNull(saved.getBookQty());
        assertNull(saved.getDiffAmount());
        assertEquals(new BigDecimal("0"), saved.getDiffQty(), "账面/实盘都缺失时差异按 0 处理");
        assertEquals("12345", saved.getReason());
    }

    @Test
    @DisplayName("saveCheckItems：多条条目逐条落库（不合并、不丢条）")
    void saveCheckItems_multipleItemsInsertedIndividually() {
        when(checkMapper.selectById(1L)).thenReturn(check(1L, 0));

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("bookQty", i);
            m.put("actualQty", i + 1);
            items.add(m);
        }

        service.saveCheckItems(1L, items);

        verify(checkItemMapper, times(3)).insert(any(InventoryCheckItem.class));
    }
}
