package com.foodtraceability.service.operations.impl;

import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.operations.LiveMonitorService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时监控服务实现类
 * 聚合订单、门店、排班数据，提供门店实时运营监控能力
 *
 * <p>金额单位说明：底层 Mapper 返回分（long），对外输出转换为元（double）以匹配前端期望。</p>
 */
@Service
public class LiveMonitorServiceImpl implements LiveMonitorService {

    private static final Logger log = LoggerFactory.getLogger(LiveMonitorServiceImpl.class);

    /** 分转元的除数 */
    private static final long FEN_TO_YUAN_DIVISOR = 100L;
    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 无权限时使用的占位门店ID（确保查询返回空结果，实现最严格权限隔离） */
    private static final long NO_ACCESS_STORE_ID = -1L;

    private final OrderNewMapper orderNewMapper;
    private final StoreNewMapper storeNewMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final DataPermissionService dataPermissionService;

    public LiveMonitorServiceImpl(OrderNewMapper orderNewMapper,
                                   StoreNewMapper storeNewMapper,
                                   ScheduleEntryMapper scheduleEntryMapper,
                                   DataPermissionService dataPermissionService) {
        this.orderNewMapper = orderNewMapper;
        this.storeNewMapper = storeNewMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    public Map<String, Object> getOverview() {
        log.info("获取实时监控总览数据");
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 解析当前用户门店数据权限：null=不限制（管理角色），非null=限制到该门店（店长），-1=无权限
        Long storeFilter = resolveStoreFilter();

        try {
            // 1. 门店总数与在线门店数（status=1 营业中），按门店权限过滤
            List<StoreNew> activeStores = filterStores(storeNewMapper.selectActiveStores(), storeFilter);
            int storeCount = activeStores.size();
            data.put("storeCount", storeCount);
            data.put("onlineStores", storeCount);

            // 2. 今日订单数与今日营收（按门店权限过滤）
            int todayOrders = storeFilter == null
                    ? safeCount(() -> orderNewMapper.countOrdersByDate(today))
                    : safeCount(() -> orderNewMapper.countOrdersByStoreAndDate(storeFilter, today));
            data.put("todayOrders", todayOrders);

            long todayRevenueFen = storeFilter == null
                    ? safeSum(() -> orderNewMapper.sumSalesByDate(today))
                    : safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeFilter, today));
            data.put("todayRevenue", fenToYuan(todayRevenueFen));

            // 3. 在岗总人数（按门店权限过滤）
            long totalStaff = storeFilter == null
                    ? safeCountLong(() -> scheduleEntryMapper.countOnDutyStaffByDate(today))
                    : safeCountLong(() -> scheduleEntryMapper.countOnDutyStaffByStoreAndDate(storeFilter, today));
            data.put("totalStaff", totalStaff);

            // 4. 平均翻台率（全门店堂食订单数 / 门店数，近似值）
            double avgTurnover = storeCount > 0
                    ? roundTwoDecimal((double) todayOrders / storeCount)
                    : 0.0;
            data.put("avgTurnover", avgTurnover);
        } catch (Exception e) {
            log.error("获取实时监控总览数据失败", e);
        }

        return data;
    }

    @Override
    public List<Map<String, Object>> getStores() {
        log.info("获取门店实时状态列表");
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 按当前用户门店权限过滤可见门店
        Long storeFilter = resolveStoreFilter();
        List<StoreNew> activeStores = filterStores(storeNewMapper.selectActiveStores(), storeFilter);
        if (activeStores.isEmpty()) {
            return list;
        }

        for (StoreNew store : activeStores) {
            Long storeId = store.getStoreId();
            Map<String, Object> item = new HashMap<>();
            item.put("storeId", storeId);
            item.put("storeName", store.getStoreName());

            // 今日/昨日营收（元）
            long todayRevenueFen = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeId, today));
            long yesterdayRevenueFen = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeId, yesterday));
            item.put("todayRevenue", fenToYuan(todayRevenueFen));
            item.put("yesterdayRevenue", fenToYuan(yesterdayRevenueFen));

            // 今日订单数及分布
            int orderCount = safeCount(() -> orderNewMapper.countOrdersByStoreAndDate(storeId, today));
            int dineIn = safeCount(() -> orderNewMapper.countDineInOrdersByStoreAndDate(storeId, today));
            item.put("orderCount", orderCount);
            item.put("dineIn", dineIn);
            // 外卖/自提暂无独立查询方法，用订单总数减堂食作为外卖+自提示意值
            item.put("takeaway", Math.max(0, orderCount - dineIn));
            item.put("selfPickup", 0);

            // 翻台率（堂食订单数近似，无桌台数据时用堂食订单数）
            item.put("turnoverRate", roundTwoDecimal(dineIn));

            // 在岗人数
            long staffOnDuty = safeCountLong(() -> scheduleEntryMapper.countOnDutyStaffByStoreAndDate(storeId, today));
            item.put("staffOnDuty", staffOnDuty);

            // 门店状态（1-营业中 2-装修中 3-暂停营业 4-已关闭）
            item.put("status", store.getStatus() != null ? store.getStatus() : 1);

            list.add(item);
        }

        return list;
    }

    @Override
    public Map<String, Object> getTrend(String dimension) {
        log.info("获取趋势数据，维度: {}", dimension);
        Map<String, Object> data = new HashMap<>();
        data.put("dimension", dimension);

        // 根据 dimension 计算时间范围
        LocalDate endDate = LocalDate.now().plusDays(1);
        LocalDate startDate;
        switch (dimension == null ? "24h" : dimension) {
            case "7d":
                startDate = LocalDate.now().minusDays(6);
                break;
            case "30d":
                startDate = LocalDate.now().minusDays(29);
                break;
            case "24h":
            default:
                // 24小时维度：按日返回今日数据（与7d/30d保持一致的聚合粒度）
                startDate = LocalDate.now();
                break;
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atStartOfDay();

        // 按当前用户门店权限过滤趋势数据
        Long storeFilter = resolveStoreFilter();

        List<String> dates = new ArrayList<>();
        List<Number> revenue = new ArrayList<>();
        List<Number> orders = new ArrayList<>();

        try {
            List<Map<String, Object>> trend = orderNewMapper.getDailyTrendByStore(startTime, endTime, storeFilter);
            if (trend != null) {
                for (Map<String, Object> row : trend) {
                    Object dateObj = row.get("date");
                    String dateStr = dateObj != null ? dateObj.toString() : "";
                    dates.add(dateStr);
                    revenue.add(fenToYuan(asLong(row.get("revenue"))));
                    orders.add(asLong(row.get("order_count")));
                }
            }
        } catch (Exception e) {
            log.warn("获取趋势数据失败: {}", e.getMessage());
        }

        data.put("dates", dates);
        data.put("revenue", revenue);
        data.put("orders", orders);
        return data;
    }

    @Override
    public List<Map<String, Object>> getAlerts() {
        log.info("获取实时告警列表");
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try {
            // 按当前用户门店权限过滤可见门店
            Long storeFilter = resolveStoreFilter();
            List<StoreNew> activeStores = filterStores(storeNewMapper.selectActiveStores(), storeFilter);
            if (activeStores.isEmpty()) {
                return alerts;
            }

            // 计算今日门店平均营收，营收低于均值50%的门店生成 warning 告警
            long totalRevenue = 0L;
            int validStoreCount = 0;
            Map<Long, Long> storeRevenueMap = new HashMap<>();
            for (StoreNew store : activeStores) {
                long revenue = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(store.getStoreId(), today));
                storeRevenueMap.put(store.getStoreId(), revenue);
                if (revenue > 0) {
                    totalRevenue += revenue;
                    validStoreCount++;
                }
            }

            if (validStoreCount == 0) {
                return alerts;
            }

            double avgRevenue = (double) totalRevenue / validStoreCount;
            double threshold = avgRevenue * 0.5;

            int alertId = 1;
            for (StoreNew store : activeStores) {
                Long revenue = storeRevenueMap.get(store.getStoreId());
                if (revenue != null && revenue > 0 && revenue < threshold) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("id", alertId++);
                    alert.put("level", "warning");
                    alert.put("storeName", store.getStoreName());
                    alert.put("message", String.format("今日营收 %.2f 元，低于门店均值 %.2f 元的50%%，建议关注",
                            fenToYuan(revenue), fenToYuan((long) avgRevenue)));
                    alert.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    alerts.add(alert);
                }
            }
        } catch (Exception e) {
            log.error("获取实时告警列表失败", e);
        }

        return alerts;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析当前用户的数据过滤门店ID
     * <p>权限过滤策略（保守原则）：</p>
     * <ul>
     *   <li>管理角色（all/company/region scope，多门店权限）：返回 null 表示不限制</li>
     *   <li>店长（store scope，单门店权限）：返回该门店ID</li>
     *   <li>无法确定用户或无权限：返回 {@link #NO_ACCESS_STORE_ID} 确保空结果</li>
     * </ul>
     *
     * @return null=不限制；非null=限制到该门店ID（含-1表示无权限）
     */
    private Long resolveStoreFilter() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("无法获取当前用户ID，权限过滤按最严格处理");
            return NO_ACCESS_STORE_ID;
        }
        try {
            List<String> accessible = dataPermissionService.getAccessibleStoreIds(String.valueOf(userId));
            if (accessible == null || accessible.isEmpty()) {
                return NO_ACCESS_STORE_ID;
            }
            if (accessible.size() == 1) {
                return parseStoreId(accessible.get(0));
            }
            // 多门店权限（管理员/区域经理）：不限制
            return null;
        } catch (Exception e) {
            log.warn("获取用户可访问门店失败，按最严格处理: {}", e.getMessage());
            return NO_ACCESS_STORE_ID;
        }
    }

    /**
     * 按门店过滤条件过滤门店列表
     * @param stores 原始门店列表
     * @param storeFilter 门店过滤ID（null=不过滤，非null=仅保留匹配的门店）
     * @return 过滤后的门店列表
     */
    private List<StoreNew> filterStores(List<StoreNew> stores, Long storeFilter) {
        if (storeFilter == null) {
            return stores != null ? stores : new ArrayList<>();
        }
        List<StoreNew> filtered = new ArrayList<>();
        if (stores != null) {
            for (StoreNew store : stores) {
                if (storeFilter.equals(store.getStoreId())) {
                    filtered.add(store);
                }
            }
        }
        return filtered;
    }

    /**
     * 安全解析门店ID字符串为Long
     */
    private Long parseStoreId(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            return NO_ACCESS_STORE_ID;
        }
        try {
            return Long.parseLong(storeId);
        } catch (NumberFormatException e) {
            log.warn("门店ID解析失败: {}", storeId);
            return NO_ACCESS_STORE_ID;
        }
    }

    /**
     * 安全执行订单数统计，异常时返回0
     */
    private int safeCount(java.util.function.Supplier<Integer> supplier) {
        try {
            Integer result = supplier.get();
            return result != null ? result : 0;
        } catch (Exception e) {
            log.warn("订单数统计失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 安全执行计数统计（long 返回值），异常时返回0
     */
    private long safeCountLong(java.util.function.Supplier<Long> supplier) {
        try {
            Long result = supplier.get();
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("计数统计失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 安全执行销售额统计，异常时返回0
     */
    private long safeSum(java.util.function.Supplier<Long> supplier) {
        try {
            Long result = supplier.get();
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("销售额统计失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 分转元（保留2位小数）
     */
    private double fenToYuan(long fen) {
        return roundTwoDecimal((double) fen / FEN_TO_YUAN_DIVISOR);
    }

    /**
     * 保留两位小数
     */
    private double roundTwoDecimal(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 安全转换为 long
     */
    private long asLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
