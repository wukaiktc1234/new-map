package com.foodtraceability.service.operations.impl;

import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.operations.DecisionBoardService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经营分析决策看板服务实现类
 * 基于订单、门店数据生成经营决策支持信息
 *
 * <p>金额单位说明：底层 Mapper 返回分（long），对外输出转换为元（double）以匹配前端期望。</p>
 */
@Service
public class DecisionBoardServiceImpl implements DecisionBoardService {

    private static final Logger log = LoggerFactory.getLogger(DecisionBoardServiceImpl.class);

    /** 分转元的除数 */
    private static final long FEN_TO_YUAN_DIVISOR = 100L;
    /** 无权限时使用的占位门店ID（确保查询返回空结果，实现最严格权限隔离） */
    private static final long NO_ACCESS_STORE_ID = -1L;

    private final OrderNewMapper orderNewMapper;
    private final StoreNewMapper storeNewMapper;
    private final DataPermissionService dataPermissionService;

    public DecisionBoardServiceImpl(OrderNewMapper orderNewMapper,
                                     StoreNewMapper storeNewMapper,
                                     DataPermissionService dataPermissionService) {
        this.orderNewMapper = orderNewMapper;
        this.storeNewMapper = storeNewMapper;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    public List<Map<String, Object>> getHealthScores() {
        log.info("获取门店健康度评分");
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 解析当前用户门店数据权限
        Long storeFilter = resolveStoreFilter();

        try {
            long todayRevenueFen = storeFilter == null
                    ? safeSum(() -> orderNewMapper.sumSalesByDate(today))
                    : safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeFilter, today));
            long yesterdayRevenueFen = storeFilter == null
                    ? safeSum(() -> orderNewMapper.sumSalesByDate(yesterday))
                    : safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeFilter, yesterday));
            int todayOrders = storeFilter == null
                    ? safeCount(() -> orderNewMapper.countOrdersByDate(today))
                    : safeCount(() -> orderNewMapper.countOrdersByStoreAndDate(storeFilter, today));

            // 1. 整体经营评分（0-100）：基于订单数和营收综合评估
            // 评分 = min(100, 订单数 * 2 + 营收(元) / 100)，上限100
            double revenueYuan = fenToYuan(todayRevenueFen);
            int overallScore = (int) Math.min(100, Math.round(todayOrders * 2.0 + revenueYuan / 100.0));
            list.add(buildHealthCard("整体经营评分", Math.max(0, overallScore), "Shop"));

            // 2. 营收达成率（%）：今日营收 / 昨日营收 * 100（昨日营收作为基准目标）
            double achievementRate = yesterdayRevenueFen > 0
                    ? roundTwoDecimal((double) todayRevenueFen / yesterdayRevenueFen * 100)
                    : 0.0;
            list.add(buildHealthCard("营收达成率", achievementRate, "TrendCharts"));

            // 3. 成本控制率（%）：暂无成本数据，返回0
            list.add(buildHealthCard("成本控制率", 0.0, "Wallet"));

            // 4. 顾客满意度（0-100）：暂无评价数据，返回0
            list.add(buildHealthCard("顾客满意度", 0.0, "User"));
        } catch (Exception e) {
            log.error("获取门店健康度评分失败", e);
        }

        return list;
    }

    @Override
    public List<Map<String, Object>> getStoreRanking() {
        log.info("获取门店排名数据");
        List<Map<String, Object>> list = new ArrayList<>();

        // 解析当前用户门店数据权限
        Long storeFilter = resolveStoreFilter();

        try {
            List<Map<String, Object>> ranking = orderNewMapper.getStoreRankingToday();
            if (ranking == null || ranking.isEmpty()) {
                return list;
            }

            // 按门店权限过滤排名数据（店长仅可见自己门店）
            if (storeFilter != null) {
                List<Map<String, Object>> filtered = new ArrayList<>();
                for (Map<String, Object> row : ranking) {
                    if (storeFilter.equals(asLong(row.get("store_id")))) {
                        filtered.add(row);
                    }
                }
                ranking = filtered;
                if (ranking.isEmpty()) {
                    return list;
                }
            }

            // 计算总营收用于健康度评分
            long totalRevenue = 0L;
            for (Map<String, Object> row : ranking) {
                totalRevenue += asLong(row.get("today_revenue"));
            }

            int rank = 1;
            for (Map<String, Object> row : ranking) {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", rank++);
                item.put("storeId", row.get("store_id"));
                item.put("storeName", row.get("store_name"));

                long revenueFen = asLong(row.get("today_revenue"));
                double revenueYuan = fenToYuan(revenueFen);
                item.put("revenue", revenueYuan);

                // 成本暂无数据，用0
                double costYuan = 0.0;
                item.put("cost", costYuan);

                // 利润 = 营收 - 成本
                double profitYuan = revenueYuan - costYuan;
                item.put("profit", roundTwoDecimal(profitYuan));

                // 毛利率 = 利润 / 营收 * 100
                double profitMargin = revenueYuan > 0
                        ? roundTwoDecimal(profitYuan / revenueYuan * 100)
                        : 0.0;
                item.put("profitMargin", profitMargin);

                // 环比增长暂无数据，用0
                item.put("growth", 0.0);

                // 健康度评分（0-100）：基于营收占比，营收越高分数越高
                double healthScore = totalRevenue > 0
                        ? roundTwoDecimal((double) revenueFen / totalRevenue * 100)
                        : 0.0;
                item.put("healthScore", healthScore);

                item.put("orderCount", asLong(row.get("order_count")));
                list.add(item);
            }
        } catch (Exception e) {
            log.error("获取门店排名数据失败", e);
        }

        return list;
    }

    @Override
    public List<Map<String, Object>> getCategorySales() {
        log.info("获取品类销售分析数据");
        // 暂无 order_items 与菜品分类的关联查询，返回空列表
        // 后续接入 order_items + foods + food_categories 表后可按分类聚合销售
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getInsights() {
        log.info("获取 AI 洞察建议");
        List<Map<String, Object>> insights = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        try {
            // 按当前用户门店权限过滤可见门店
            Long storeFilter = resolveStoreFilter();
            List<StoreNew> activeStores = filterStores(storeNewMapper.selectActiveStores(), storeFilter);
            if (activeStores.isEmpty()) {
                return insights;
            }

            // 计算今日/昨日营收，用于生成洞察
            long totalTodayRevenue = 0L;
            long totalYesterdayRevenue = 0L;
            int lowRevenueStoreCount = 0;
            String lowRevenueStoreName = null;
            long minRevenue = Long.MAX_VALUE;

            for (StoreNew store : activeStores) {
                Long storeId = store.getStoreId();
                long todayRev = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeId, today));
                long yesterdayRev = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(storeId, yesterday));
                totalTodayRevenue += todayRev;
                totalYesterdayRevenue += yesterdayRev;

                if (todayRev < minRevenue) {
                    minRevenue = todayRev;
                    lowRevenueStoreName = store.getStoreName();
                }
            }

            double avgRevenue = activeStores.size() > 0
                    ? (double) totalTodayRevenue / activeStores.size()
                    : 0.0;

            // 统计低于均值的门店数
            for (StoreNew store : activeStores) {
                long todayRev = safeSum(() -> orderNewMapper.sumSalesByStoreAndDate(store.getStoreId(), today));
                if (todayRev < avgRevenue * 0.5) {
                    lowRevenueStoreCount++;
                }
            }

            int insightId = 1;

            // 洞察1：整体营收环比
            if (totalYesterdayRevenue > 0) {
                double growth = roundTwoDecimal((double) (totalTodayRevenue - totalYesterdayRevenue) / totalYesterdayRevenue * 100);
                if (growth < 0) {
                    insights.add(buildInsight(insightId++, "TrendCharts", "营收环比下降",
                            String.format("今日营收 %.2f 元，较昨日下降 %.2f%%，建议分析下降原因并制定提升方案",
                                    fenToYuan(totalTodayRevenue), Math.abs(growth)), "high"));
                } else {
                    insights.add(buildInsight(insightId++, "TrendCharts", "营收环比增长",
                            String.format("今日营收 %.2f 元，较昨日增长 %.2f%%，经营状况良好",
                                    fenToYuan(totalTodayRevenue), growth), "low"));
                }
            }

            // 洞察2：低营收门店提醒
            if (lowRevenueStoreName != null && minRevenue < avgRevenue * 0.5) {
                insights.add(buildInsight(insightId++, "Warning", "低营收门店预警",
                        String.format("门店「%s」今日营收 %.2f 元，低于门店均值 %.2f 元的50%%，建议重点关注",
                                lowRevenueStoreName, fenToYuan(minRevenue), fenToYuan((long) avgRevenue)), "medium"));
            }

            // 洞察3：门店经营均衡度
            if (lowRevenueStoreCount > 0) {
                insights.add(buildInsight(insightId++, "DataAnalysis", "门店经营均衡度",
                        String.format("共有 %d 家门店营收低于均值50%%，建议优化资源配置，提升整体经营水平",
                                lowRevenueStoreCount), "medium"));
            }

            // 洞察4：经营状态良好
            if (insights.isEmpty()) {
                insights.add(buildInsight(insightId++, "CircleCheck", "经营状态良好",
                        "所有门店营收均达到或超过均值50%，整体经营平稳", "low"));
            }
        } catch (Exception e) {
            log.error("获取 AI 洞察建议失败", e);
        }

        return insights;
    }

    @Override
    public Map<String, Object> getRevenueTrend(String period) {
        log.info("获取营收-成本-利润趋势数据, period={}", period);
        Map<String, Object> data = new HashMap<>();

        // 根据 period 计算时间范围
        LocalDate endDate = LocalDate.now().plusDays(1);
        LocalDate startDate;
        switch (period == null ? "7d" : period) {
            case "4w":
                // 近4周（28天）
                startDate = LocalDate.now().minusDays(27);
                break;
            case "6m":
                // 近6月（180天）
                startDate = LocalDate.now().minusDays(179);
                break;
            case "7d":
            default:
                // 近7天
                startDate = LocalDate.now().minusDays(6);
                break;
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atStartOfDay();

        // 按当前用户门店权限过滤趋势数据
        Long storeFilter = resolveStoreFilter();

        List<String> dates = new ArrayList<>();
        List<Number> revenue = new ArrayList<>();
        List<Number> cost = new ArrayList<>();
        List<Number> profit = new ArrayList<>();

        try {
            List<Map<String, Object>> trend = orderNewMapper.getDailyTrendByStore(startTime, endTime, storeFilter);
            if (trend != null) {
                for (Map<String, Object> row : trend) {
                    Object dateObj = row.get("date");
                    dates.add(dateObj != null ? dateObj.toString() : "");

                    double revenueYuan = fenToYuan(asLong(row.get("revenue")));
                    // 成本暂无数据，用0
                    double costYuan = 0.0;
                    // 利润 = 营收 - 成本
                    double profitYuan = roundTwoDecimal(revenueYuan - costYuan);

                    revenue.add(revenueYuan);
                    cost.add(costYuan);
                    profit.add(profitYuan);
                }
            }
        } catch (Exception e) {
            log.warn("获取营收趋势数据失败: {}", e.getMessage());
        }

        data.put("dates", dates);
        data.put("revenue", revenue);
        data.put("cost", cost);
        data.put("profit", profit);
        return data;
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
     * 构建健康度评分卡片
     */
    private Map<String, Object> buildHealthCard(String label, double score, String icon) {
        Map<String, Object> card = new HashMap<>();
        card.put("label", label);
        card.put("score", score);
        card.put("icon", icon);
        return card;
    }

    /**
     * 构建 AI 洞察建议
     */
    private Map<String, Object> buildInsight(int id, String icon, String title, String description, String severity) {
        Map<String, Object> insight = new HashMap<>();
        insight.put("id", id);
        insight.put("icon", icon);
        insight.put("title", title);
        insight.put("description", description);
        insight.put("severity", severity);
        return insight;
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
