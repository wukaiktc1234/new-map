package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.order.ReservationStatsVO;
import com.foodtraceability.entity.TableReservationNew;
import com.foodtraceability.mapper.TableReservationNewMapper;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TableReservationNewServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖点：预约创建（编码前缀/待确认状态）、分页筛选（状态/关键词 OR/日期区间与精确匹配/倒序）、
 * 状态机 confirm→arrive→cancel 的合法迁移与非法迁移拦截、统计口径与取消率计算、
 * 以及统计查询异常时的 0 值兜底。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TableReservationNewServiceImplTest {

    @Mock
    private TableReservationNewMapper tableReservationNewMapper;

    @InjectMocks
    private TableReservationNewServiceImpl service;

    @BeforeEach
    void setUp() {
        when(tableReservationNewMapper.insert(any(TableReservationNew.class))).thenReturn(1);
        when(tableReservationNewMapper.updateById(any(TableReservationNew.class))).thenReturn(1);
        when(tableReservationNewMapper.countByDate(any())).thenReturn(0L);
        when(tableReservationNewMapper.countPendingConfirm()).thenReturn(0L);
        when(tableReservationNewMapper.countArrived()).thenReturn(0L);
        when(tableReservationNewMapper.countByDateAndStatus(any(), any())).thenReturn(0L);
        when(tableReservationNewMapper.countAll()).thenReturn(0L);
        when(tableReservationNewMapper.countCancelled()).thenReturn(0L);
    }

    private static TableReservationNew reservation(Long id, int status) {
        TableReservationNew r = new TableReservationNew();
        r.setReservationId(id);
        r.setStatus(status);
        r.setCustomerName("张三");
        r.setCustomerPhone("13800000000");
        return r;
    }

    // ==================== 创建 / 查询 ====================

    @Test
    @DisplayName("create：生成 RES 前缀编码，状态置 1（待确认），并落库")
    void create_generatesCodeAndPendingStatus() {
        TableReservationNew input = new TableReservationNew();
        input.setCustomerName("李四");

        TableReservationNew result = service.create(input);

        assertSame(input, result);
        assertNotNull(result.getReservationCode());
        assertTrue(result.getReservationCode().startsWith("RES"));
        assertEquals(17, result.getReservationCode().length() - 3, "编码后缀为 yyyyMMddHHmmssSSS（17 位）");
        assertEquals(1, result.getStatus());
        verify(tableReservationNewMapper).insert(input);
    }

    @Test
    @DisplayName("update：更新后回查返回最新实体")
    void update_returnsReloaded() {
        TableReservationNew payload = reservation(3L, 1);
        TableReservationNew reloaded = reservation(3L, 1);
        when(tableReservationNewMapper.selectById(3L)).thenReturn(reloaded);

        assertSame(reloaded, service.update(payload));
        verify(tableReservationNewMapper).updateById(payload);
    }

    @Test
    @DisplayName("getById：不存在返回 null（由 Controller 决定错误响应，不在 Service 抛异常）")
    void getById_missing_returnsNull() {
        when(tableReservationNewMapper.selectById(404L)).thenReturn(null);
        assertNull(service.getById(404L));
    }

    @Test
    @DisplayName("queryPage：status + 关键词（姓名/手机 OR）+ 日期区间 + create_time 倒序")
    void queryPage_buildsFilters() {
        Page<TableReservationNew> page = new Page<>(1, 10);
        when(tableReservationNewMapper.selectPage(any(), any())).thenReturn(page);

        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 7);
        service.queryPage(page, 2, "  138  ", start, end);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<TableReservationNew>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(tableReservationNewMapper).selectPage(any(), captor.capture());
        QueryWrapper<TableReservationNew> wrapper = (QueryWrapper<TableReservationNew>) captor.getValue();
        String sql = wrapper.getSqlSegment();
        wrapper.getSqlSegment();
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());

        assertTrue(values.contains(2), "status 等值条件");
        assertTrue(values.contains("%138%"), "关键词应 trim 后两侧加通配符");
        assertTrue(values.contains(start) && values.contains(end), "日期区间两端都应入参");
        assertTrue(sql.toLowerCase().contains("order by"), "应按 create_time 倒序");
        assertTrue(sql.contains("customer_name") && sql.contains("customer_phone"), "关键词应同时匹配姓名与手机号");
    }

    @Test
    @DisplayName("queryPage：只有 date 时走精确匹配；全空时无筛选条件")
    void queryPage_singleDateExactAndNoFilter() {
        Page<TableReservationNew> page = new Page<>(1, 10);
        when(tableReservationNewMapper.selectPage(any(), any())).thenReturn(page);
        LocalDate date = LocalDate.of(2026, 10, 1);

        service.queryPage(page, null, "  ", date, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<TableReservationNew>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(tableReservationNewMapper).selectPage(any(), captor.capture());
        QueryWrapper<TableReservationNew> wrapper = (QueryWrapper<TableReservationNew>) captor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(date));
        assertEquals(1, wrapper.getParamNameValuePairs().size(), "只有日期精确匹配一个条件（空白关键词被忽略）");
    }

    @Test
    @DisplayName("listByDate：委托 mapper.selectByDate")
    void listByDate_delegates() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        List<TableReservationNew> rows = List.of(reservation(1L, 1));
        when(tableReservationNewMapper.selectByDate(date)).thenReturn(rows);

        assertSame(rows, service.listByDate(date));
    }

    // ==================== 状态机 ====================

    @Test
    @DisplayName("confirm：不存在 / 非待确认状态均拒绝")
    void confirm_guards() {
        when(tableReservationNewMapper.selectById(404L)).thenReturn(null);
        RuntimeException missing = assertThrows(RuntimeException.class, () -> service.confirm(404L, 1L));
        assertTrue(missing.getMessage().contains("预约记录不存在"));

        when(tableReservationNewMapper.selectById(2L)).thenReturn(reservation(2L, 2));
        RuntimeException invalid = assertThrows(RuntimeException.class, () -> service.confirm(2L, 1L));
        assertTrue(invalid.getMessage().contains("只有待确认"));
        verify(tableReservationNewMapper, never()).updateById(any(TableReservationNew.class));
    }

    @Test
    @DisplayName("confirm：待确认 → 已确认，写入确认时间与操作人")
    void confirm_success() {
        TableReservationNew r = reservation(1L, 1);
        when(tableReservationNewMapper.selectById(1L)).thenReturn(r);

        service.confirm(1L, 88L);

        assertEquals(2, r.getStatus());
        assertNotNull(r.getConfirmTime());
        assertEquals(88L, r.getConfirmOperatorId());
        verify(tableReservationNewMapper).updateById(r);
    }

    @Test
    @DisplayName("arrive：仅已确认可标记到店，成功后写入到店时间")
    void arrive_guardsAndSuccess() {
        when(tableReservationNewMapper.selectById(1L)).thenReturn(reservation(1L, 1));
        RuntimeException invalid = assertThrows(RuntimeException.class, () -> service.arrive(1L, 9L));
        assertTrue(invalid.getMessage().contains("只有已确认"));

        TableReservationNew confirmed = reservation(2L, 2);
        when(tableReservationNewMapper.selectById(2L)).thenReturn(confirmed);
        service.arrive(2L, 9L);

        assertEquals(3, confirmed.getStatus());
        assertNotNull(confirmed.getArriveTime());
        assertEquals(9L, confirmed.getArriveOperatorId());
    }

    @Test
    @DisplayName("cancel：已到店/已取消/已完成不可取消；待确认与已确认可取消")
    void cancel_guardsAndSuccess() {
        for (int blocked : new int[]{3, 4, 5}) {
            when(tableReservationNewMapper.selectById((long) blocked)).thenReturn(reservation((long) blocked, blocked));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.cancel((long) blocked, "客户临时有事", 1L));
            assertTrue(ex.getMessage().contains("不允许取消"));
        }

        TableReservationNew pending = reservation(10L, 1);
        when(tableReservationNewMapper.selectById(10L)).thenReturn(pending);
        service.cancel(10L, "客户临时有事", 66L);

        assertEquals(4, pending.getStatus());
        assertNotNull(pending.getCancelTime());
        assertEquals(66L, pending.getCancelOperatorId());
        verify(tableReservationNewMapper).updateById(pending);
    }

    // ==================== 统计 ====================

    @Test
    @DisplayName("getStats：取消率 = 已取消/总数，保留 2 位（25/200=12.5%）")
    void getStats_computesCancelRate() {
        when(tableReservationNewMapper.countByDate(any())).thenReturn(12L);
        when(tableReservationNewMapper.countPendingConfirm()).thenReturn(3L);
        when(tableReservationNewMapper.countArrived()).thenReturn(4L);
        when(tableReservationNewMapper.countByDateAndStatus(any(), eq(2))).thenReturn(5L);
        when(tableReservationNewMapper.countByDateAndStatus(any(), eq(4))).thenReturn(6L);
        when(tableReservationNewMapper.countAll()).thenReturn(200L);
        when(tableReservationNewMapper.countCancelled()).thenReturn(25L);

        ReservationStatsVO stats = service.getStats();

        assertEquals(12L, stats.getTodayReservations());
        assertEquals(3L, stats.getPendingConfirm());
        assertEquals(4L, stats.getArrived());
        assertEquals(5L, stats.getConfirmed());
        assertEquals(6L, stats.getCancelled());
        assertEquals(12.5, stats.getCancelRate());
    }

    @Test
    @DisplayName("getStats：总数为 0 时取消率为 0.0（不除零）")
    void getStats_zeroTotal_avoidsDivisionByZero() {
        ReservationStatsVO stats = service.getStats();
        assertEquals(0.0, stats.getCancelRate());
        assertEquals(0L, stats.getTodayReservations());
    }

    @Test
    @DisplayName("getStats：统计查询抛异常时逐项兜底为 0，不向上冒泡")
    void getStats_queryFailure_fallsBackToZero() {
        when(tableReservationNewMapper.countByDate(any())).thenThrow(new IllegalStateException("db down"));
        when(tableReservationNewMapper.countAll()).thenThrow(new IllegalStateException("db down"));
        when(tableReservationNewMapper.countCancelled()).thenThrow(new IllegalStateException("db down"));

        ReservationStatsVO stats = service.getStats();

        assertEquals(0L, stats.getTodayReservations());
        assertEquals(0.0, stats.getCancelRate());
        assertEquals(0L, stats.getConfirmed());
    }

    @Test
    @DisplayName("getStats：COUNT 返回 null 时按 0 处理")
    void getStats_nullCounts_treatedAsZero() {
        when(tableReservationNewMapper.countByDate(any())).thenReturn(null);
        when(tableReservationNewMapper.countAll()).thenReturn(null);
        when(tableReservationNewMapper.countCancelled()).thenReturn(null);

        ReservationStatsVO stats = service.getStats();

        assertEquals(0L, stats.getTodayReservations());
        assertEquals(0.0, stats.getCancelRate());
    }
}
