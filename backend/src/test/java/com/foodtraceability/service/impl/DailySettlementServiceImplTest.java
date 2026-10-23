package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.DailySettlementDataService;
import com.foodtraceability.dto.SettlementQueryDTO;
import com.foodtraceability.dto.store.operation.DailySettlementCreateDTO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.DailySettlement;
import com.foodtraceability.entity.User;
import com.foodtraceability.event.StoreDailySettlementCompletedEvent;
import com.foodtraceability.mapper.DailySettlementMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.service.NotificationService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DailySettlementServiceImpl} 纯 Mockito 单元测试（不连数据库、不起 Spring 上下文）。
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>查询链路：分页列表 / 详情（含班次明细挂载）/ 班次明细委托 / 班次订单委托</li>
 *   <li>创建草稿：参数校验、同店同日唯一性、元→分换算、插入失败</li>
 *   <li>确认对账：状态机（draft/pending 可确认，其余拒绝）、事件发布、缓存失效</li>
 *   <li>异常上报：状态置 abnormal、缓存失效</li>
 *   <li>日终聚合核心：按支付方式聚合、营收/单量/客单价、退款/作废/优惠、已完成未支付告警、幂等更新</li>
 *   <li>批量生成：单日失败不拖垮整批、全区间失败必须显式抛错（不返回空列表冒充成功）</li>
 * </ul>
 *
 * <p>本类只新增测试，不修改任何生产代码；断言以当前实现行为为准（特征化测试）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailySettlementServiceImplTest {

    private static final String ROLE_STORE_MANAGER = "store_manager";

    @Mock
    private DailySettlementMapper dailySettlementMapper;

    @Mock
    private DailySettlementDataService dailySettlementDataService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private DailySettlementServiceImpl service;

    /**
     * 纯单测没有 MyBatis 容器，LambdaQueryWrapper 解析列名需要先注册实体元数据，
     * 否则 {@code getSqlSegment()} 会抛 "can not find lambda cache"。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DailySettlement.class);
    }

    @BeforeEach
    void setUp() {
        // 默认：按门店+日期查不到历史日结记录（走新建分支）
        when(dailySettlementMapper.selectByStoreAndDate(anyString(), any(LocalDate.class))).thenReturn(null);
        // 默认：所有写操作成功
        when(dailySettlementMapper.insert(any(DailySettlement.class))).thenReturn(1);
        when(dailySettlementMapper.updateById(any(DailySettlement.class))).thenReturn(1);
        // 默认：订单聚合无数据
        when(orderMapper.aggregateByPaymentMethod(anyString(), any(), any())).thenReturn(new ArrayList<>());
        when(orderMapper.aggregateRefunds(anyString(), any(), any())).thenReturn(new HashMap<>());
        when(orderMapper.aggregateCancelled(anyString(), any(), any())).thenReturn(new HashMap<>());
        when(orderMapper.sumDiscountAmount(anyString(), any(), any())).thenReturn(0L);
        when(orderMapper.countCompletedUnpaid(anyString(), any(), any())).thenReturn(0L);
        when(orderMapper.sumCompletedUnpaidAmount(anyString(), any(), any())).thenReturn(0L);
    }

    // ==================== 构造辅助 ====================

    private static DailySettlement settlement(String id, String storeId, String status, LocalDate date) {
        DailySettlement s = new DailySettlement();
        s.setSettlementId(id);
        s.setStoreId(storeId);
        s.setStatus(status);
        s.setSettlementDate(date);
        return s;
    }

    private static DailySettlementVO vo(String id, String storeId) {
        DailySettlementVO v = new DailySettlementVO();
        v.setSettlementId(id);
        v.setStoreId(storeId);
        v.setStatus("draft");
        return v;
    }

    private static User userWithLocation(Long locationId) {
        User u = new User();
        u.setId(1L);
        u.setLocationId(locationId);
        return u;
    }

    // ==================== 查询链路 ====================

    @Test
    @DisplayName("getSettlementList：分页记录逐条经 DataService 转换（角色固定传 store_manager）")
    void getSettlementList_convertsEachRecordThroughDataService() {
        DailySettlement s1 = settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1));
        DailySettlement s2 = settlement("S2", "10", "approved", LocalDate.of(2026, 10, 2));
        Page<DailySettlement> page = new Page<>(1, 10);
        page.setRecords(List.of(s1, s2));
        when(dailySettlementMapper.selectPage(any(), any())).thenReturn(page);
        when(dailySettlementDataService.getSettlementBasicInfo("S1", ROLE_STORE_MANAGER)).thenReturn(vo("S1", "10"));
        when(dailySettlementDataService.getSettlementBasicInfo("S2", ROLE_STORE_MANAGER)).thenReturn(vo("S2", "10"));

        SettlementQueryDTO query = new SettlementQueryDTO();
        IPage<DailySettlementVO> result = service.getSettlementList(query, userWithLocation(1001L));

        assertEquals(2, result.getRecords().size());
        assertEquals("S1", result.getRecords().get(0).getSettlementId());
        assertEquals("S2", result.getRecords().get(1).getSettlementId());
        verify(dailySettlementDataService, times(2)).getSettlementBasicInfo(anyString(), eq(ROLE_STORE_MANAGER));
    }

    @Test
    @DisplayName("getSettlementList：currentUser 为空时跳过门店收敛但保留其它筛选条件（P0-3 已修）")
    void getSettlementList_nullUser_keepsFiltersWithoutStoreNarrowing() {
        Page<DailySettlement> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(dailySettlementMapper.selectPage(any(), any())).thenReturn(page);

        SettlementQueryDTO query = new SettlementQueryDTO();
        query.setStatus("approved");
        query.setStartDate(LocalDate.of(2026, 10, 1));

        IPage<DailySettlementVO> result = service.getSettlementList(query, null);

        assertEquals(0, result.getRecords().size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DailySettlement>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dailySettlementMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue() instanceof LambdaQueryWrapper);
        LambdaQueryWrapper<DailySettlement> wrapper = (LambdaQueryWrapper<DailySettlement>) captor.getValue();
        wrapper.getSqlSegment(); // 触发参数装配
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());
        // P0-3：无用户只跳过门店收敛，状态与日期条件必须照样生效，防止退化成全表扫描
        assertTrue(values.contains("approved"), "currentUser 为 null 时状态条件仍应生效，实际参数: " + values);
        assertTrue(values.contains(LocalDate.of(2026, 10, 1)), "currentUser 为 null 时日期条件仍应生效，实际参数: " + values);
        assertEquals(2, values.size(), "无用户时不应产生门店参数，实际参数: " + values);
    }

    @Test
    @DisplayName("getSettlementList：日期范围 + 状态 + 门店筛选参数进入 wrapper")
    void getSettlementList_appliesDateStatusAndStoreFilters() {
        Page<DailySettlement> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(dailySettlementMapper.selectPage(any(), any())).thenReturn(page);

        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 20);
        SettlementQueryDTO query = new SettlementQueryDTO();
        query.setStartDate(start);
        query.setEndDate(end);
        query.setStatus("approved");
        query.setStoreId("10");

        service.getSettlementList(query, userWithLocation(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DailySettlement>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dailySettlementMapper).selectPage(any(), captor.capture());
        LambdaQueryWrapper<DailySettlement> wrapper = (LambdaQueryWrapper<DailySettlement>) captor.getValue();
        wrapper.getSqlSegment(); // 触发参数装配
        List<Object> values = new ArrayList<>(wrapper.getParamNameValuePairs().values());

        assertTrue(values.contains(start), "应包含起始日期条件");
        assertTrue(values.contains(end), "应包含结束日期条件");
        assertTrue(values.contains("approved"), "应包含状态条件");
        assertTrue(values.contains("10"), "应包含门店条件");
    }

    @Test
    @DisplayName("getSettlementWithPermission：记录不存在抛 NOT_FOUND(404)")
    void getSettlementWithPermission_notFound() {
        when(dailySettlementMapper.selectById("S404")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSettlementWithPermission("S404", userWithLocation(1L)));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        assertEquals("日结记录不存在", ex.getMessage());
        verify(dailySettlementDataService, never()).getSettlementBasicInfo(anyString(), anyString());
    }

    @Test
    @DisplayName("getSettlementWithPermission：返回 VO 并挂载班次明细")
    void getSettlementWithPermission_attachesShifts() {
        DailySettlement s = settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1));
        when(dailySettlementMapper.selectById("S1")).thenReturn(s);
        DailySettlementVO detail = vo("S1", "10");
        when(dailySettlementDataService.getSettlementBasicInfo("S1", ROLE_STORE_MANAGER)).thenReturn(detail);
        List<DailySettlementShiftVO> shifts = List.of(new DailySettlementShiftVO());
        when(dailySettlementDataService.getShiftsBySettlementId("S1")).thenReturn(shifts);

        DailySettlementVO result = service.getSettlementWithPermission("S1", userWithLocation(1L));

        assertSame(detail, result);
        assertEquals(1, result.getShifts().size());
        verify(dailySettlementDataService).getShiftsBySettlementId("S1");
    }

    @Test
    @DisplayName("getShiftsBySettlementId：直接委托 DataService")
    void getShiftsBySettlementId_delegates() {
        List<DailySettlementShiftVO> shifts = Collections.singletonList(new DailySettlementShiftVO());
        when(dailySettlementDataService.getShiftsBySettlementId("S1")).thenReturn(shifts);

        assertSame(shifts, service.getShiftsBySettlementId("S1"));
    }

    @Test
    @DisplayName("getOrdersByShiftId：空/空白 shiftId 抛 PARAM_ERROR(400)，否则委托 DataService")
    void getOrdersByShiftId_validatesAndDelegates() {
        BusinessException nullEx = assertThrows(BusinessException.class, () -> service.getOrdersByShiftId(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), nullEx.getCode());

        BusinessException blankEx = assertThrows(BusinessException.class, () -> service.getOrdersByShiftId(""));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), blankEx.getCode());

        List<Object> orders = List.of(new Object());
        when(dailySettlementDataService.getOrdersByShiftId("SH1")).thenReturn(orders);
        assertSame(orders, service.getOrdersByShiftId("SH1"));
    }

    // ==================== 创建草稿 ====================

    @Test
    @DisplayName("createSettlement：storeId 为空抛 PARAM_ERROR(400)")
    void createSettlement_blankStoreId_rejected() {
        DailySettlementCreateDTO dto = new DailySettlementCreateDTO();
        dto.setStoreId("");
        dto.setSettlementDate(LocalDate.now().minusDays(1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSettlement(dto, "U1"));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(dailySettlementMapper, never()).insert(any(DailySettlement.class));
    }

    @Test
    @DisplayName("createSettlement：结算日期晚于今日抛 PARAM_ERROR(400)")
    void createSettlement_futureDate_rejected() {
        DailySettlementCreateDTO dto = new DailySettlementCreateDTO();
        dto.setStoreId("10");
        dto.setSettlementDate(LocalDate.now().plusDays(1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSettlement(dto, "U1"));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createSettlement：同店同日已存在抛 OPERATION_NOT_ALLOWED(7001)")
    void createSettlement_duplicateRejected() {
        when(dailySettlementMapper.selectCount(any())).thenReturn(1L);
        DailySettlementCreateDTO dto = new DailySettlementCreateDTO();
        dto.setStoreId("10");
        dto.setSettlementDate(LocalDate.now().minusDays(1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSettlement(dto, "U1"));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已存在日结记录"));
        verify(dailySettlementMapper, never()).insert(any(DailySettlement.class));
    }

    @Test
    @DisplayName("createSettlement：元→分换算、状态置 draft、返回 store_manager 视图")
    void createSettlement_convertsYuanToFenAndReturnsVo() {
        when(dailySettlementMapper.selectCount(any())).thenReturn(0L);
        DailySettlementCreateDTO dto = new DailySettlementCreateDTO();
        dto.setStoreId("10");
        dto.setSettlementDate(LocalDate.now().minusDays(1));
        dto.setTotalRevenue("15800.50");
        dto.setTotalCost("8000.00");
        dto.setNetProfit("7800.50");
        dto.setGrossProfitRate("45.5");
        dto.setOrderCount(156);
        dto.setAvgOrderValue("101.28");
        dto.setTableUsageRate("85.0");
        dto.setDifferenceAmount("0.00");
        dto.setRemark("今日营业正常");
        when(dailySettlementDataService.getSettlementBasicInfo(any(), eq(ROLE_STORE_MANAGER)))
                .thenReturn(vo("S1", "10"));

        service.createSettlement(dto, "U1");

        ArgumentCaptor<DailySettlement> captor = ArgumentCaptor.forClass(DailySettlement.class);
        verify(dailySettlementMapper).insert(captor.capture());
        DailySettlement saved = captor.getValue();
        assertEquals("10", saved.getStoreId());
        assertEquals(1580050L, saved.getTotalRevenue());
        assertEquals(800000L, saved.getTotalCost());
        assertEquals(780050L, saved.getNetProfit());
        assertEquals(new BigDecimal("45.5"), saved.getGrossProfitRate());
        assertEquals(156, saved.getOrderCount());
        assertEquals(10128L, saved.getAvgOrderValue());
        assertEquals(new BigDecimal("85.0"), saved.getTableUsageRate());
        assertEquals(0L, saved.getDifferenceAmount());
        assertEquals("draft", saved.getStatus());
        assertEquals("今日营业正常", saved.getRemark());
    }

    @Test
    @DisplayName("createSettlement：插入影响行数为 0 时抛 INTERNAL_SERVER_ERROR(500)")
    void createSettlement_insertFailure_throws() {
        when(dailySettlementMapper.selectCount(any())).thenReturn(0L);
        when(dailySettlementMapper.insert(any(DailySettlement.class))).thenReturn(0);
        DailySettlementCreateDTO dto = new DailySettlementCreateDTO();
        dto.setStoreId("10");
        dto.setSettlementDate(LocalDate.now().minusDays(1));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSettlement(dto, "U1"));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
        verify(dailySettlementDataService, never()).getSettlementBasicInfo(anyString(), anyString());
    }

    // ==================== 确认对账 ====================

    @Test
    @DisplayName("confirmSettlement：记录不存在抛 NOT_FOUND(404)")
    void confirmSettlement_notFound() {
        when(dailySettlementMapper.selectById("S404")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmSettlement("S404", "U1"));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("confirmSettlement：approved 状态不允许再次确认（7001），且不写库/不发事件")
    void confirmSettlement_invalidStatus_rejected() {
        when(dailySettlementMapper.selectById("S1"))
                .thenReturn(settlement("S1", "10", "approved", LocalDate.of(2026, 10, 1)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmSettlement("S1", "U1"));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("approved"));
        verify(dailySettlementMapper, never()).updateById(any(DailySettlement.class));
        verify(applicationEventPublisher, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    @DisplayName("confirmSettlement：draft → approved，写审核人、清缓存、发布日结完成事件")
    void confirmSettlement_success_publishesEvent() {
        DailySettlement s = settlement("2110437068670001154", "576", "draft", LocalDate.of(2026, 10, 1));
        s.setTotalRevenue(123400L);
        s.setTotalCost(50000L);
        s.setNetProfit(73400L);
        when(dailySettlementMapper.selectById("2110437068670001154")).thenReturn(s);

        service.confirmSettlement("2110437068670001154", "9527");

        ArgumentCaptor<DailySettlement> updateCaptor = ArgumentCaptor.forClass(DailySettlement.class);
        verify(dailySettlementMapper).updateById(updateCaptor.capture());
        assertEquals("approved", updateCaptor.getValue().getStatus());
        assertEquals("9527", updateCaptor.getValue().getAuditorId());
        assertNotNull(updateCaptor.getValue().getUpdateTime());

        verify(dailySettlementDataService).clearSettlementCache("2110437068670001154");

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        StoreDailySettlementCompletedEvent event = (StoreDailySettlementCompletedEvent) eventCaptor.getValue();
        assertEquals(2110437068670001154L, event.getSettlementId());
        assertEquals(576L, event.getStoreId());
        assertEquals(123400L, event.getTotalIncome());
        assertEquals(50000L, event.getTotalExpense());
        assertEquals(73400L, event.getNetProfit());
        assertEquals(9527L, event.getOperatorId());
        assertEquals(LocalDate.of(2026, 10, 1), event.getSettlementDate());
    }

    @Test
    @DisplayName("confirmSettlement：非数字 ID 降级为 null（事件仍发布，不阻断确认）")
    void confirmSettlement_nonNumericIds_publishEventWithNullIds() {
        DailySettlement s = settlement("S-NON-NUM", "STORE-X", "pending", LocalDate.of(2026, 10, 1));
        when(dailySettlementMapper.selectById("S-NON-NUM")).thenReturn(s);

        service.confirmSettlement("S-NON-NUM", "operator-x");

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        StoreDailySettlementCompletedEvent event = (StoreDailySettlementCompletedEvent) eventCaptor.getValue();
        assertNull(event.getSettlementId());
        assertNull(event.getStoreId());
        assertNull(event.getOperatorId());
        assertEquals("approved", s.getStatus());
    }

    @Test
    @DisplayName("confirmSettlement：更新影响行数为 0 抛 500，且不发布事件")
    void confirmSettlement_updateFailure_throwsWithoutEvent() {
        when(dailySettlementMapper.selectById("S1"))
                .thenReturn(settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1)));
        when(dailySettlementMapper.updateById(any(DailySettlement.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirmSettlement("S1", "U1"));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
        verify(applicationEventPublisher, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    @DisplayName("confirmSettlement：事件发布抛异常被吞掉，不影响确认结果")
    void confirmSettlement_eventPublishFailure_swallowed() {
        when(dailySettlementMapper.selectById("1"))
                .thenReturn(settlement("1", "10", "draft", LocalDate.of(2026, 10, 1)));
        org.mockito.Mockito.doThrow(new IllegalStateException("publisher down"))
                .when(applicationEventPublisher).publishEvent(any(ApplicationEvent.class));

        service.confirmSettlement("1", "U1");

        verify(dailySettlementMapper).updateById(any(DailySettlement.class));
        verify(dailySettlementDataService).clearSettlementCache("1");
    }

    // ==================== 异常上报 ====================

    @Test
    @DisplayName("reportIssue：记录不存在抛 NOT_FOUND(404)")
    void reportIssue_notFound() {
        when(dailySettlementMapper.selectById("S404")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reportIssue("S404", new Object(), "U1"));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reportIssue：状态置 abnormal、清缓存、更新失败抛 500")
    void reportIssue_successAndFailure() {
        DailySettlement s = settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1));
        when(dailySettlementMapper.selectById("S1")).thenReturn(s);

        service.reportIssue("S1", new Object(), "U1");

        assertEquals("abnormal", s.getStatus());
        assertNotNull(s.getUpdateTime());
        verify(dailySettlementDataService).clearSettlementCache("S1");

        when(dailySettlementMapper.updateById(any(DailySettlement.class))).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.reportIssue("S1", null, "U1"));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
    }

    // ==================== 日终聚合 ====================

    @Test
    @DisplayName("generateDailySettlement：新记录按支付方式聚合，营收/单量/客单价/退款/作废/优惠全部落字段")
    void generateDailySettlement_newRecord_aggregatesAllMetrics() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(orderMapper.aggregateByPaymentMethod(eq("10"), any(), any())).thenReturn(List.of(
                row(1, 10000L, 2),   // 现金
                row(2, 5000L, 1),    // 微信
                row(99, 777L, 9)     // 未知支付方式：被忽略
        ));
        when(orderMapper.sumDiscountAmount(eq("10"), any(), any())).thenReturn(300L);
        when(orderMapper.aggregateRefunds(eq("10"), any(), any()))
                .thenReturn(Map.of("total_refund_amount", 500L, "refund_count", 1));
        when(orderMapper.aggregateCancelled(eq("10"), any(), any()))
                .thenReturn(Map.of("total_cancelled_amount", 100L, "cancelled_count", 2));
        when(orderMapper.countCompletedUnpaid(eq("10"), any(), any())).thenReturn(3L);
        when(orderMapper.sumCompletedUnpaidAmount(eq("10"), any(), any())).thenReturn(250L);

        DailySettlement generated = service.generateDailySettlement("10", date);

        assertEquals("10", generated.getStoreId());
        assertEquals(date, generated.getSettlementDate());
        assertEquals(15000L, generated.getTotalRevenue(), "未知支付方式不计入营收");
        assertEquals(3, generated.getOrderCount());
        assertEquals(5000L, generated.getAvgOrderValue());
        assertEquals(300L, generated.getTotalDiscountAmount());
        assertEquals(500L, generated.getRefundAmount());
        assertEquals(1, generated.getRefundCount());
        assertEquals(100L, generated.getCancelledAmount());
        assertEquals(2, generated.getCancelledCount());
        assertEquals("draft", generated.getStatus());
        assertNotNull(generated.getPaymentBreakdown());
        assertTrue(generated.getPaymentBreakdown().contains("\"cash\""));
        assertTrue(generated.getPaymentBreakdown().contains("\"wechat\""));
        assertTrue(generated.getPaymentBreakdown().contains("\"alipay\""));
        assertTrue(generated.getPaymentBreakdown().contains("\"mixed\""));
        assertFalse(generated.getPaymentBreakdown().contains("unknown"));
        verify(dailySettlementMapper).insert(generated);
    }

    @Test
    @DisplayName("generateDailySettlement：无订单时营收 0、客单价 0（不出现除零）")
    void generateDailySettlement_noOrders_avoidsDivisionByZero() {
        DailySettlement generated = service.generateDailySettlement("10", LocalDate.of(2026, 10, 1));

        assertEquals(0L, generated.getTotalRevenue());
        assertEquals(0, generated.getOrderCount());
        assertEquals(0L, generated.getAvgOrderValue());
        assertEquals(0L, generated.getRefundAmount());
        assertEquals(0, generated.getRefundCount());
        assertEquals(0L, generated.getCancelledAmount());
        assertEquals(0, generated.getCancelledCount());
    }

    @Test
    @DisplayName("generateDailySettlement：已存在记录走更新分支，不 insert")
    void generateDailySettlement_existing_updatesInsteadOfInsert() {
        DailySettlement existing = settlement("S1", "10", "pending", LocalDate.of(2026, 10, 1));
        when(dailySettlementMapper.selectByStoreAndDate("10", LocalDate.of(2026, 10, 1))).thenReturn(existing);
        when(orderMapper.aggregateByPaymentMethod(eq("10"), any(), any())).thenReturn(List.of(row(3, 8888L, 4)));

        DailySettlement result = service.generateDailySettlement("10", LocalDate.of(2026, 10, 1));

        assertSame(existing, result);
        assertEquals(8888L, result.getTotalRevenue());
        assertEquals(4, result.getOrderCount());
        assertEquals(2222L, result.getAvgOrderValue());
        assertNotNull(result.getUpdateTime());
        verify(dailySettlementMapper, never()).insert(any(DailySettlement.class));
        verify(dailySettlementMapper).updateById(existing);
    }

    @Test
    @DisplayName("generateDailySettlement：insert 失败抛 INTERNAL_SERVER_ERROR(500)")
    void generateDailySettlement_insertFailure_throws() {
        when(dailySettlementMapper.insert(any(DailySettlement.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generateDailySettlement("10", LocalDate.of(2026, 10, 1)));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getOrGenerateSettlement：已存在直接返回，不触发聚合")
    void getOrGenerateSettlement_existing_returnsWithoutAggregation() {
        DailySettlement existing = settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1));
        when(dailySettlementMapper.selectByStoreAndDate("10", LocalDate.of(2026, 10, 1))).thenReturn(existing);

        assertSame(existing, service.getOrGenerateSettlement("10", LocalDate.of(2026, 10, 1)));
        verify(orderMapper, never()).aggregateByPaymentMethod(anyString(), any(), any());
        verify(dailySettlementMapper, never()).insert(any(DailySettlement.class));
    }

    @Test
    @DisplayName("getOrGenerateSettlement：不存在时实时聚合生成")
    void getOrGenerateSettlement_missing_generates() {
        DailySettlement generated = service.getOrGenerateSettlement("10", LocalDate.of(2026, 10, 1));

        assertNotNull(generated);
        verify(dailySettlementMapper).insert(generated);
    }

    @Test
    @DisplayName("getOrGenerateSettlements：单日失败不影响其他日期，返回成功部分")
    void getOrGenerateSettlements_partialFailure_returnsSuccessfulDays() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 3);
        when(dailySettlementMapper.selectByStoreAndDate("10", LocalDate.of(2026, 10, 1)))
                .thenReturn(settlement("S1", "10", "draft", LocalDate.of(2026, 10, 1)));
        when(dailySettlementMapper.selectByStoreAndDate("10", LocalDate.of(2026, 10, 2)))
                .thenThrow(new IllegalStateException("db down"));
        when(dailySettlementMapper.selectByStoreAndDate("10", LocalDate.of(2026, 10, 3)))
                .thenReturn(settlement("S3", "10", "draft", LocalDate.of(2026, 10, 3)));

        List<DailySettlement> result = service.getOrGenerateSettlements("10", start, end);

        assertEquals(2, result.size());
        assertEquals("S1", result.get(0).getSettlementId());
        assertEquals("S3", result.get(1).getSettlementId());
    }

    @Test
    @DisplayName("getOrGenerateSettlements：全区间失败必须显式抛 500，不返回空列表冒充成功")
    void getOrGenerateSettlements_allFailed_throws() {
        when(dailySettlementMapper.selectByStoreAndDate(anyString(), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("db down"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOrGenerateSettlements("10", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("日结对账生成失败"));
    }

    private static Map<String, Object> row(int paymentMethod, long amount, int count) {
        Map<String, Object> m = new HashMap<>();
        m.put("payment_method", paymentMethod);
        m.put("total_amount", amount);
        m.put("order_count", count);
        return m;
    }
}
