package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.store.operation.DiningTableQueryDTO;
import com.foodtraceability.entity.DiningTableNew;
import com.foodtraceability.mapper.DiningTableNewMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DiningTableNewServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：创建时的桌台编码唯一性闸门、更新/查询/列表/状态变更的委托、
 * 按状态计数与按门店统计（未知状态码的容错）、分页参数透传、
 * 以及 {@code isTableCodeUnique} 的排除自身条件。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiningTableNewServiceImplTest {

    @Mock
    private DiningTableNewMapper diningTableNewMapper;

    @InjectMocks
    private DiningTableNewServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DiningTableNew.class);
    }

    @BeforeEach
    void setUp() {
        when(diningTableNewMapper.insert(any(DiningTableNew.class))).thenReturn(1);
        when(diningTableNewMapper.updateById(any(DiningTableNew.class))).thenReturn(1);
        when(diningTableNewMapper.updateStatus(anyLong(), any(), any())).thenReturn(1);
    }

    private static DiningTableNew table(Long id, String code, Long storeId, Integer status) {
        DiningTableNew t = new DiningTableNew();
        t.setTableId(id);
        t.setTableCode(code);
        t.setStoreId(storeId);
        t.setTableName("A" + id);
        t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("create：编码在同门店重复时抛 PARAM_ERROR(400)，且不写库")
    void create_duplicateCode_rejected() {
        when(diningTableNewMapper.selectByTableCode("T01", 10L)).thenReturn(table(1L, "T01", 10L, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(table(null, "T01", 10L, 1)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("桌台编码已存在"));
        verify(diningTableNewMapper, never()).insert(any(DiningTableNew.class));
    }

    @Test
    @DisplayName("create：编码唯一时落库并返回原实体")
    void create_uniqueCode_inserts() {
        when(diningTableNewMapper.selectByTableCode("T02", 10L)).thenReturn(null);
        DiningTableNew input = table(null, "T02", 10L, 1);

        assertSame(input, service.create(input));
        verify(diningTableNewMapper).insert(input);
    }

    @Test
    @DisplayName("update：更新后回查并返回最新实体")
    void update_returnsReloaded() {
        DiningTableNew payload = table(7L, "T07", 10L, 2);
        DiningTableNew reloaded = table(7L, "T07", 10L, 2);
        when(diningTableNewMapper.selectById(7L)).thenReturn(reloaded);

        assertSame(reloaded, service.update(payload));
        verify(diningTableNewMapper).updateById(payload);
    }

    @Test
    @DisplayName("getById：不存在抛 NOT_FOUND(404)，存在则返回")
    void getById_hitAndMiss() {
        when(diningTableNewMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(1L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());

        DiningTableNew found = table(2L, "T2", 10L, 1);
        when(diningTableNewMapper.selectById(2L)).thenReturn(found);
        assertSame(found, service.getById(2L));
    }

    @Test
    @DisplayName("getByCode：以 tableCode + null storeId 查询（全局唯一语义），未命中抛 404")
    void getByCode_queriesWithoutStoreScope() {
        DiningTableNew found = table(3L, "T3", 10L, 1);
        when(diningTableNewMapper.selectByTableCode("T3", null)).thenReturn(found);

        assertSame(found, service.getByCode("T3"));
        verify(diningTableNewMapper).selectByTableCode("T3", null);

        when(diningTableNewMapper.selectByTableCode("MISS", null)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getByCode("MISS"));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("listAll / listAvailable：分别委托全量查询与可用桌台查询")
    void listAllAndAvailable_delegate() {
        List<DiningTableNew> all = List.of(table(1L, "A", 10L, 1));
        List<DiningTableNew> available = List.of(table(2L, "B", 10L, 1));
        when(diningTableNewMapper.selectList(isNull())).thenReturn(all);
        when(diningTableNewMapper.selectAvailableTables()).thenReturn(available);

        assertSame(all, service.listAll());
        assertSame(available, service.listAvailable());
    }

    @Test
    @DisplayName("updateStatus：透传 tableId/status/currentOrderId；回查为空时不抛错")
    void updateStatus_delegatesAndToleratesMissingRow() {
        when(diningTableNewMapper.selectById(9L)).thenReturn(null);

        service.updateStatus(9L, 2, "ORDER-1");

        verify(diningTableNewMapper).updateStatus(9L, 2, "ORDER-1");
        verify(diningTableNewMapper).selectById(9L);
    }

    @Test
    @DisplayName("countByStatus：按状态聚合为 Map，空结果返回空 Map")
    void countByStatus_mapsAggregationRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("status", 1, "cnt", 12L));
        rows.add(row("status", 2, "cnt", 3L));
        when(diningTableNewMapper.countByStatus()).thenReturn(rows);

        Map<Integer, Long> result = service.countByStatus();

        assertEquals(2, result.size());
        assertEquals(12L, result.get(1));
        assertEquals(3L, result.get(2));

        when(diningTableNewMapper.countByStatus()).thenReturn(List.of());
        assertTrue(service.countByStatus().isEmpty());
    }

    @Test
    @DisplayName("listByStoreId：page/size 取自 query，status/tableType/keyword 原样透传")
    void listByStoreId_passesPaginationAndFilters() {
        IPage<DiningTableNew> page = new Page<>(2, 5);
        when(diningTableNewMapper.selectByStoreId(any(), eq(10L), eq(1), eq(2), eq("A")))
                .thenReturn(page);

        DiningTableQueryDTO query = new DiningTableQueryDTO();
        query.setPage(2);
        query.setSize(5);
        query.setStatus(1);
        query.setTableType(2);
        query.setKeyword("A");

        assertSame(page, service.listByStoreId(10L, query));

        ArgumentCaptor<IPage<DiningTableNew>> captor = ArgumentCaptor.forClass(IPage.class);
        verify(diningTableNewMapper).selectByStoreId(captor.capture(), eq(10L), eq(1), eq(2), eq("A"));
        assertEquals(2L, captor.getValue().getCurrent());
        assertEquals(5L, captor.getValue().getSize());
    }

    @Test
    @DisplayName("getStatsByStoreId：按状态分组汇总，未知状态只计总数不计分类")
    void getStatsByStoreId_aggregatesKnownStatuses() {
        when(diningTableNewMapper.countGroupByStatus(10L)).thenReturn(List.of(
                row("status", 1, "cnt", 4L),
                row("status", 2, "cnt", 2L),
                row("status", 3, "cnt", 1L),
                row("status", 4, "cnt", 1L),
                row("status", 5, "cnt", 1L),
                row("status", 99, "cnt", 7L)
        ));

        Map<String, Object> stats = service.getStatsByStoreId(10L);

        assertEquals(16L, stats.get("total"), "未知状态也要计入总数");
        assertEquals(4L, stats.get("idle"));
        assertEquals(2L, stats.get("dining"));
        assertEquals(1L, stats.get("reserved"));
        assertEquals(1L, stats.get("maintenance"));
        assertEquals(1L, stats.get("disabled"));
    }

    @Test
    @DisplayName("getStatsByStoreId：无数据时各项均为 0（不是 null）")
    void getStatsByStoreId_emptyStore_returnsZeros() {
        when(diningTableNewMapper.countGroupByStatus(10L)).thenReturn(List.of());

        Map<String, Object> stats = service.getStatsByStoreId(10L);

        assertEquals(0L, stats.get("total"));
        assertEquals(0L, stats.get("idle"));
        assertEquals(0L, stats.get("disabled"));
    }

    @Test
    @DisplayName("isTableCodeUnique：计数为 0 才唯一；excludeTableId 非空时追加「不等于自身」")
    void isTableCodeUnique_buildsExcludeCondition() {
        when(diningTableNewMapper.selectCount(any())).thenReturn(0L);
        assertTrue(service.isTableCodeUnique(10L, "T01", null));

        when(diningTableNewMapper.selectCount(any())).thenReturn(1L);
        assertFalse(service.isTableCodeUnique(10L, "T01", 5L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DiningTableNew>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(diningTableNewMapper, org.mockito.Mockito.times(2)).selectCount(captor.capture());
        LambdaQueryWrapper<DiningTableNew> withExclude = (LambdaQueryWrapper<DiningTableNew>) captor.getAllValues().get(1);
        withExclude.getSqlSegment();
        assertTrue(withExclude.getParamNameValuePairs().containsValue(5L), "应包含排除自身的主键条件");
    }

    private static Map<String, Object> row(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
