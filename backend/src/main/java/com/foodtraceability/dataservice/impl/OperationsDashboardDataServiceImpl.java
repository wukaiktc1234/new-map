package com.foodtraceability.dataservice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dataservice.OperationsDashboardDataService;
import com.foodtraceability.entity.DiningTableNew;
import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.mapper.DiningTableNewMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营数据中心数据服务实现类
 * 提供门店运营统计数据的缓存和聚合查询功能
 * 用于运营Dashboard的多店监控、绩效分析等场景
 *
 * <h2>数据来源（跨模块联动）</h2>
 * <ul>
 *   <li>{@link OrderNewMapper}：今日订单数、今日营收、翻台率（订单中心）</li>
 *   <li>{@link StoreNewMapper}：活跃门店数（门店管理）</li>
 *   <li>{@link ScheduleEntryMapper}：在岗人数（排班管理）</li>
 *   <li>{@link DiningTableNewMapper}：桌台使用率（堂食管理）</li>
 * </ul>
 *
 * <p>缓存键格式: operations_dashboard:stats:{storeId}:{date}</p>
 */
@Service
public class OperationsDashboardDataServiceImpl implements OperationsDashboardDataService {

    private static final Logger log = LoggerFactory.getLogger(OperationsDashboardDataServiceImpl.class);

    /** 缓存名称 */
    private static final String CACHE_NAME = "operationsDashboard";
    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 分转元的除数 */
    private static final long FEN_TO_YUAN_DIVISOR = 100L;
    /** 金额保留小数位 */
    private static final int AMOUNT_SCALE = 2;
    /** 无权限时使用的占位门店ID（确保查询返回空结果，实现最严格权限隔离） */
    private static final long NO_ACCESS_STORE_ID = -1L;

    private final OrderNewMapper orderNewMapper;
    /** POS订单Mapper（orders_legacy 表），用于聚合 POS 端订单数据到运营总览 */
    private final OrderMapper posOrderMapper;
    private final StoreNewMapper storeNewMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final DiningTableNewMapper diningTableNewMapper;
    private final DataPermissionService dataPermissionService;

    public OperationsDashboardDataServiceImpl(
            OrderNewMapper orderNewMapper,
            OrderMapper posOrderMapper,
            StoreNewMapper storeNewMapper,
            ScheduleEntryMapper scheduleEntryMapper,
            DiningTableNewMapper diningTableNewMapper,
            DataPermissionService dataPermissionService) {
        this.orderNewMapper = orderNewMapper;
        this.posOrderMapper = posOrderMapper;
        this.storeNewMapper = storeNewMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.diningTableNewMapper = diningTableNewMapper;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    @Cacheable(value = CACHE_NAME,
            key = "(#storeIds != null ? #storeIds.hashCode() : 'all') + ':' + #date + ':' + T(com.foodtraceability.utils.SecurityUtils).getCurrentUserId()",
            unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> getDashboardStats(List<String> storeIds, String date) {
        log.info("查询运营统计数据: storeIds={}, date={}", storeIds, date);

        Map<String, Object> stats = new HashMap<>();

        // 统计日期默认为今日
        String queryDate = (date != null && !date.isEmpty()) ? date : LocalDate.now().format(DATE_FORMATTER);
        LocalDate queryLocalDate = LocalDate.parse(queryDate, DATE_FORMATTER);

        // 防御性权限过滤：当调用方未传门店范围时，按当前用户门店权限过滤
        // 防止绕过上层 Service 权限检查直接调用 DataService 导致数据越权
        if (storeIds == null || storeIds.isEmpty()) {
            Long userStoreFilter = resolveUserStoreFilter();
            if (userStoreFilter != null) {
                // 店长或无权限用户：限制到其门店（-1表示无权限，查询返回空结果）
                storeIds = Collections.singletonList(String.valueOf(userStoreFilter));
            }
            // userStoreFilter == null 表示管理角色（多门店权限），不限制
        }

        // 单店模式下 storeIds 暂时统一使用默认门店1聚合（跨店支持预留）
        // 当前系统只有一家门店 ID=1，多店场景待门店账号体系完善后启用
        Long storeIdFilter = resolveStoreIdFilter(storeIds);

        // 1. 活跃门店数（status=1 营业中）
        long activeStores = countActiveStores(storeIds);
        stats.put("activeStores", activeStores);

        // 2. 营收（按查询日期聚合，支持历史日期）
        long todayRevenueFen = sumRevenueByDate(storeIdFilter, queryLocalDate);
        stats.put("todayRevenue", fenToYuanString(todayRevenueFen));

        // 3. 订单数（按查询日期聚合，支持历史日期）
        long todayOrderCount = countOrdersByDate(storeIdFilter, queryLocalDate);
        stats.put("todayOrderCount", todayOrderCount);

        // 4. 在岗人数（基于排班表数据，支持历史日期）
        long onDutyStaff = countOnDutyStaff(storeIdFilter, queryLocalDate);
        stats.put("onDutyStaff", onDutyStaff);

        // 5. 平均翻台率（堂食订单数 / 总桌台数）
        String avgTableTurnover = calculateAvgTableTurnover(storeIdFilter, queryLocalDate);
        stats.put("avgTableTurnover", avgTableTurnover);

        // 6. 客户满意度（暂无评价数据，保留字段）
        stats.put("customerSatisfaction", 0);

        stats.put("queryDate", queryDate);

        log.info("运营统计数据聚合完成: {}", stats);
        return stats;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'performance:' + #storeId + ':' + #date",
            unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> getStorePerformance(String storeId, String date) {
        if (storeId == null || storeId.isEmpty()) {
            return new HashMap<>();
        }

        // 防御性权限校验：店长只能查看自己门店的绩效数据，防止越权访问其他门店
        Long userStoreFilter = resolveUserStoreFilter();
        if (userStoreFilter != null) {
            Long requestedStoreId = parseStoreId(storeId);
            if (requestedStoreId == null || !requestedStoreId.equals(userStoreFilter)) {
                log.warn("用户无权限访问该门店绩效数据: requestedStoreId={}, userStoreFilter={}", storeId, userStoreFilter);
                return new HashMap<>();
            }
        }

        log.info("查询单店绩效数据: storeId={}, date={}", storeId, date);

        Map<String, Object> performance = new HashMap<>();

        // 统计日期默认为今日
        String queryDate = (date != null && !date.isEmpty()) ? date : LocalDate.now().format(DATE_FORMATTER);
        LocalDate queryLocalDate = LocalDate.parse(queryDate, DATE_FORMATTER);

        // 查询门店基本信息
        Long storeIdLong = parseStoreId(storeId);
        StoreNew store = storeIdLong != null ? storeNewMapper.selectById(storeIdLong) : null;
        String storeName = store != null ? store.getStoreName() : "";

        // 1. 总营收（按查询日期聚合，支持历史日期）
        long totalRevenueFen = sumRevenueByDate(storeIdLong, queryLocalDate);
        performance.put("totalRevenue", fenToYuanString(totalRevenueFen));

        // 2. 订单总数（按查询日期聚合，支持历史日期）
        long orderCount = countOrdersByDate(storeIdLong, queryLocalDate);
        performance.put("orderCount", orderCount);

        // 3. 平均客单价（元）
        String avgOrderValue = orderCount > 0
                ? fenToYuanString(totalRevenueFen / orderCount)
                : fenToYuanString(0L);
        performance.put("avgOrderValue", avgOrderValue);

        // 4. 桌台使用率（status=2 用餐中 / 总桌台数）
        double tableUsageRate = calculateTableUsageRate(storeIdLong);
        performance.put("tableUsageRate", tableUsageRate);

        // 5. 人效（营收 / 在岗人数）
        long onDutyStaff = countOnDutyStaff(storeIdLong, queryLocalDate);
        double staffEfficiency = onDutyStaff > 0
                ? BigDecimal.valueOf(totalRevenueFen)
                    .divide(BigDecimal.valueOf(FEN_TO_YUAN_DIVISOR), AMOUNT_SCALE, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(onDutyStaff), AMOUNT_SCALE, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0.0;
        performance.put("staffEfficiency", staffEfficiency);

        // 6. 食材成本率（暂无成本数据，保留0）
        performance.put("foodCostRate", 0.0);

        // 7. 客户评分（暂无评价数据，保留0）
        performance.put("customerReviewScore", 0.0);

        performance.put("storeId", storeId);
        performance.put("storeName", storeName);
        performance.put("queryDate", queryDate);

        return performance;
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearStoreOperationsCache(String storeId) {
        log.debug("清除门店运营数据缓存: storeId={}", storeId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析当前用户的数据过滤门店ID（防御性权限过滤）
     * <p>用于 DataService 层的防御性权限校验，防止绕过上层 Service 直接调用导致越权。</p>
     * <p>权限过滤策略（保守原则）：</p>
     * <ul>
     *   <li>管理角色（多门店权限）：返回 null 表示不限制</li>
     *   <li>店长（单门店权限）：返回该门店ID</li>
     *   <li>无法确定用户或无权限：返回 {@link #NO_ACCESS_STORE_ID} 确保空结果</li>
     * </ul>
     *
     * @return null=不限制；非null=限制到该门店ID（含-1表示无权限）
     */
    private Long resolveUserStoreFilter() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("无法获取当前用户ID，权限过滤按最严格处理");
            return NO_ACCESS_STORE_ID;
        }
        try {
            List<String> accessible = dataPermissionService.getAccessibleStoreIds(String.valueOf(userId));
            // 关键区分：null 表示管理员不限制；empty 表示无权限
            if (accessible == null) {
                // 管理员（多门店权限）：不限制
                return null;
            }
            if (accessible.isEmpty()) {
                // 无可访问门店：返回 -1 确保查询返回空结果
                return NO_ACCESS_STORE_ID;
            }
            if (accessible.size() == 1) {
                Long parsed = parseStoreId(accessible.get(0));
                return parsed != null ? parsed : NO_ACCESS_STORE_ID;
            }
            // 多门店权限（区域经理）：不限制
            return null;
        } catch (Exception e) {
            log.warn("获取用户可访问门店失败，按最严格处理: {}", e.getMessage());
            return NO_ACCESS_STORE_ID;
        }
    }

    /**
     * 解析 storeIds 列表为单一门店 ID 过滤值
     * 当前为单店系统，storeIds 为空或为 null 时返回 null（表示全部）
     * storeIds 仅 1 个时返回该门店 ID
     */
    private Long resolveStoreIdFilter(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return null;
        }
        if (storeIds.size() == 1) {
            return parseStoreId(storeIds.get(0));
        }
        // 多门店场景暂不支持精确过滤，返回 null 表示全部聚合
        // 后续扩展时可改为 IN 查询
        return null;
    }

    /**
     * 安全解析门店 ID 字符串
     */
    private Long parseStoreId(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(storeId);
        } catch (NumberFormatException e) {
            log.warn("门店 ID 解析失败: {}", storeId);
            return null;
        }
    }

    /**
     * 统计活跃门店数
     */
    private long countActiveStores(List<String> storeIds) {
        try {
            if (storeIds == null || storeIds.isEmpty()) {
                // 全部门店：统计 status=1 的门店数
                LambdaQueryWrapper<StoreNew> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(StoreNew::getStatus, 1);
                return storeNewMapper.selectCount(wrapper);
            } else {
                // 指定门店列表：仅统计 status=1 的
                List<Long> storeIdLongs = storeIds.stream()
                        .map(this::parseStoreId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                if (storeIdLongs.isEmpty()) {
                    return 0L;
                }
                LambdaQueryWrapper<StoreNew> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(StoreNew::getStoreId, storeIdLongs)
                        .eq(StoreNew::getStatus, 1);
                return storeNewMapper.selectCount(wrapper);
            }
        } catch (Exception e) {
            log.warn("统计活跃门店数失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 统计指定日期营收（分，支持历史日期）
     * @param storeId 门店ID（null 表示全部门店聚合）
     * @param date 查询日期
     */
    private long sumRevenueByDate(Long storeId, LocalDate date) {
        try {
            // 聚合 orders 表（管理端订单）+ orders_legacy 表（POS端订单，主要数据来源）
            // POS端产生的交易行为是管理端获取订单数据的主要来源
            long adminRevenue = storeId != null
                    ? orderNewMapper.sumSalesByStoreAndDate(storeId, date)
                    : orderNewMapper.sumSalesByDate(date);
            long posRevenue = posOrderMapper.sumPosSalesByDate(date);
            return adminRevenue + posRevenue;
        } catch (Exception e) {
            log.warn("统计指定日期营收失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 统计指定日期订单数（支持历史日期）
     * @param storeId 门店ID（null 表示全部门店聚合）
     * @param date 查询日期
     */
    private long countOrdersByDate(Long storeId, LocalDate date) {
        try {
            // 聚合 orders 表（管理端订单）+ orders_legacy 表（POS端订单，主要数据来源）
            long adminOrders = storeId != null
                    ? orderNewMapper.countOrdersByStoreAndDate(storeId, date)
                    : orderNewMapper.countOrdersByDate(date);
            long posOrders = posOrderMapper.countPosOrdersByDate(date);
            return adminOrders + posOrders;
        } catch (Exception e) {
            log.warn("统计指定日期订单数失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 统计在岗人数
     */
    private long countOnDutyStaff(Long storeId, LocalDate workDate) {
        try {
            if (storeId != null) {
                return scheduleEntryMapper.countOnDutyStaffByStoreAndDate(storeId, workDate);
            }
            return scheduleEntryMapper.countOnDutyStaffByDate(workDate);
        } catch (Exception e) {
            log.warn("统计在岗人数失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 计算平均翻台率
     * 翻台率 = 指定日期堂食订单数 / 总桌台数
     * @param storeId 门店ID（null 表示全部）
     * @param date 查询日期
     */
    private String calculateAvgTableTurnover(Long storeId, LocalDate date) {
        try {
            // 指定日期堂食订单数
            long completedDineInOrders;
            if (storeId != null) {
                completedDineInOrders = orderNewMapper.countDineInOrdersByStoreAndDate(storeId, date);
            } else {
                // 全部门店：通过订单总数近似（暂无全门店堂食订单按日期查询方法）
                completedDineInOrders = orderNewMapper.countOrdersByDate(date);
            }
            // 总桌台数
            long totalTables;
            LambdaQueryWrapper<DiningTableNew> wrapper = new LambdaQueryWrapper<>();
            if (storeId != null) {
                wrapper.eq(DiningTableNew::getStoreId, storeId);
            }
            totalTables = diningTableNewMapper.selectCount(wrapper);

            if (totalTables == 0) {
                return "0.00";
            }
            BigDecimal rate = BigDecimal.valueOf(completedDineInOrders)
                    .divide(BigDecimal.valueOf(totalTables), AMOUNT_SCALE, RoundingMode.HALF_UP);
            return rate.toPlainString();
        } catch (Exception e) {
            log.warn("计算翻台率失败: {}", e.getMessage());
            return "0.00";
        }
    }

    /**
     * 计算桌台使用率（status=2 用餐中 / 总桌台数）
     */
    private double calculateTableUsageRate(Long storeId) {
        try {
            LambdaQueryWrapper<DiningTableNew> totalWrapper = new LambdaQueryWrapper<>();
            if (storeId != null) {
                totalWrapper.eq(DiningTableNew::getStoreId, storeId);
            }
            long totalTables = diningTableNewMapper.selectCount(totalWrapper);
            if (totalTables == 0) {
                return 0.0;
            }

            LambdaQueryWrapper<DiningTableNew> usedWrapper = new LambdaQueryWrapper<>();
            if (storeId != null) {
                usedWrapper.eq(DiningTableNew::getStoreId, storeId);
            }
            usedWrapper.eq(DiningTableNew::getStatus, 2);
            long usedTables = diningTableNewMapper.selectCount(usedWrapper);

            return BigDecimal.valueOf(usedTables)
                    .divide(BigDecimal.valueOf(totalTables), AMOUNT_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (Exception e) {
            log.warn("计算桌台使用率失败: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 分转元字符串
     */
    private String fenToYuanString(long fen) {
        BigDecimal yuan = BigDecimal.valueOf(fen)
                .divide(BigDecimal.valueOf(FEN_TO_YUAN_DIVISOR), AMOUNT_SCALE, RoundingMode.HALF_UP);
        return yuan.toPlainString();
    }
}
