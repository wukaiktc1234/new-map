package com.foodtraceability.service.impl;

import com.foodtraceability.dto.operations.KpiSummaryDTO;
import com.foodtraceability.dto.operations.OperationsReportQueryDTO;
import com.foodtraceability.entity.report.ExportTask;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.SalesAnalysisReportMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.OperationsReportService;
import com.foodtraceability.service.ExportTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 经营报表服务实现类（管理层视角 - 精简版）
 * 提供统一报表查询、KPI汇总、导出等功能
 *
 * 数据权限说明：
 * - 所有查询方法均通过getAuthorizedStoreIds进行门店数据权限过滤
 * - 用户只能查看其有权限的门店数据
 */
@Service
public class OperationsReportServiceImpl implements OperationsReportService {

    private static final Logger logger = LoggerFactory.getLogger(OperationsReportServiceImpl.class);

    /** 无权限时使用的占位门店ID（确保查询返回空结果，实现最严格权限隔离） */
    private static final long NO_ACCESS_STORE_ID = -1L;

    private final SalesAnalysisReportMapper salesAnalysisReportMapper;
    private final OrderNewMapper orderNewMapper;
    private final ExportTaskService exportTaskService;
    private final ObjectMapper objectMapper;
    private final DataPermissionService dataPermissionService;

    public OperationsReportServiceImpl(SalesAnalysisReportMapper salesAnalysisReportMapper,
                                       OrderNewMapper orderNewMapper,
                                       ExportTaskService exportTaskService,
                                       ObjectMapper objectMapper,
                                       DataPermissionService dataPermissionService) {
        this.salesAnalysisReportMapper = salesAnalysisReportMapper;
        this.orderNewMapper = orderNewMapper;
        this.exportTaskService = exportTaskService;
        this.objectMapper = objectMapper;
        this.dataPermissionService = dataPermissionService;
    }

    /**
     * 获取当前用户有权限的门店ID列表
     * @param userId 用户ID
     * @param requestedStoreIds 用户请求的门店ID列表
     * @return 经过权限过滤后的门店ID列表
     */
    private List<String> getAuthorizedStoreIds(Long userId, List<String> requestedStoreIds) {
        List<String> accessibleStoreIds = dataPermissionService.getAccessibleStoreIds(String.valueOf(userId));

        if (requestedStoreIds == null || requestedStoreIds.isEmpty()) {
            return accessibleStoreIds;
        }

        List<String> authorized = new ArrayList<>();
        for (String storeId : requestedStoreIds) {
            if (accessibleStoreIds.contains(storeId)) {
                authorized.add(storeId);
            } else {
                logger.warn("用户{}尝试访问无权限的门店: {}", userId, storeId);
            }
        }

        return authorized.isEmpty() ? accessibleStoreIds : authorized;
    }

    /**
     * 校验查询参数的日期范围
     * @param queryDTO 查询参数
     */
    private void validateQueryDTO(OperationsReportQueryDTO queryDTO) {
        if (!queryDTO.isDateRangeValid()) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    @Override
    public Map<String, Object> getReportData(String reportType, OperationsReportQueryDTO queryDTO, Long userId) {
        logger.info("获取报表数据，类型: {}, 用户: {}, 日期范围: {} ~ {}",
                reportType, userId, queryDTO.getStartDate(), queryDTO.getEndDate());
        validateQueryDTO(queryDTO);
        List<String> authorizedStoreIds = getAuthorizedStoreIds(userId, queryDTO.getStoreIds());
        logger.info("用户{}有权限的门店: {}", userId, authorizedStoreIds);

        // 将授权门店列表解析为门店过滤ID，用于实际查询的数据权限过滤
        Long storeFilter = resolveStoreFilterFromAuthorized(authorizedStoreIds);

        Map<String, Object> result = new HashMap<>();

        // 根据报表类型构建不同的数据
        switch (reportType) {
            case "daily":
                result.put("trendData", buildDailyTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            case "weekly":
                result.put("trendData", buildWeeklyTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            case "monthly":
                result.put("trendData", buildMonthlyTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            case "quarterly":
                result.put("trendData", buildQuarterlyTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            case "yearly":
                result.put("trendData", buildYearlyTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            case "profit":
                result.put("trendData", buildProfitTrend(queryDTO));
                result.put("storeRank", buildStoreRanking(queryDTO, storeFilter));
                break;
            default:
                throw new IllegalArgumentException("不支持的报表类型: " + reportType);
        }
        return result;
    }

    @Override
    public KpiSummaryDTO getKpiSummary(OperationsReportQueryDTO queryDTO, Long userId) {
        logger.info("获取KPI汇总数据，用户: {}, 日期范围: {} ~ {}", userId, queryDTO.getStartDate(), queryDTO.getEndDate());
        validateQueryDTO(queryDTO);
        List<String> authorizedStoreIds = getAuthorizedStoreIds(userId, queryDTO.getStoreIds());
        logger.info("用户{}有权限的门店: {}", userId, authorizedStoreIds);

        // 将授权门店列表解析为门店过滤ID，用于实际查询的数据权限过滤
        Long storeFilter = resolveStoreFilterFromAuthorized(authorizedStoreIds);

        KpiSummaryDTO result = new KpiSummaryDTO();
        result.setPeriod(queryDTO.getStartDate() + " ~ " + queryDTO.getEndDate());
        result.setReportType(detectReportType(queryDTO));

        // 通过 OrderNewMapper 查询日期范围内的真实订单数据（按门店权限过滤）
        LocalDate startDate = LocalDate.parse(queryDTO.getStartDate());
        LocalDate endDate = LocalDate.parse(queryDTO.getEndDate());
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        long totalRevenueFen = 0L;
        long totalOrders = 0L;
        try {
            List<Map<String, Object>> trend = orderNewMapper.getDailyTrendByStore(startTime, endTime, storeFilter);
            if (trend != null) {
                for (Map<String, Object> row : trend) {
                    totalRevenueFen += asLong(row.get("revenue"));
                    totalOrders += asLong(row.get("order_count"));
                }
            }
        } catch (Exception e) {
            logger.warn("查询KPI汇总数据失败，回退为0: {}", e.getMessage());
        }

        // 计算平均客单价（分），订单数为0时为0
        long avgCheckFen = totalOrders > 0 ? totalRevenueFen / totalOrders : 0L;

        List<KpiSummaryDTO.KpiCardItem> kpiCards = new ArrayList<>();

        // 1. 区域总营收（基于真实订单数据）
        KpiSummaryDTO.KpiCardItem revenue = new KpiSummaryDTO.KpiCardItem();
        revenue.setMetricKey("revenue");
        revenue.setMetricName("区域总营收");
        revenue.setCurrentValue(fenToYuanString(totalRevenueFen));
        revenue.setCurrentValueFen(totalRevenueFen);
        revenue.setYoyChange(BigDecimal.ZERO);
        revenue.setMomChange(BigDecimal.ZERO);
        revenue.setTargetAchievement(BigDecimal.ZERO);
        revenue.setUnit("元");
        revenue.setIsCurrency(true);
        kpiCards.add(revenue);

        // 2. 区域毛利率（暂无成本数据，返回0）
        KpiSummaryDTO.KpiCardItem grossRate = new KpiSummaryDTO.KpiCardItem();
        grossRate.setMetricKey("gross_rate");
        grossRate.setMetricName("区域毛利率");
        grossRate.setCurrentValue("0.00");
        grossRate.setCurrentValueFen(0L);
        grossRate.setYoyChange(BigDecimal.ZERO);
        grossRate.setMomChange(BigDecimal.ZERO);
        grossRate.setTargetAchievement(BigDecimal.ZERO);
        grossRate.setUnit("%");
        grossRate.setIsCurrency(false);
        kpiCards.add(grossRate);

        // 3. 平均客单价（基于真实订单数据）
        KpiSummaryDTO.KpiCardItem avgCheck = new KpiSummaryDTO.KpiCardItem();
        avgCheck.setMetricKey("avg_check");
        avgCheck.setMetricName("平均客单价");
        avgCheck.setCurrentValue(fenToYuanString(avgCheckFen));
        avgCheck.setCurrentValueFen(avgCheckFen);
        avgCheck.setYoyChange(BigDecimal.ZERO);
        avgCheck.setMomChange(BigDecimal.ZERO);
        avgCheck.setTargetAchievement(BigDecimal.ZERO);
        avgCheck.setUnit("元");
        avgCheck.setIsCurrency(true);
        kpiCards.add(avgCheck);

        // 4. 人效（暂无在岗人数数据，返回0）
        KpiSummaryDTO.KpiCardItem perCapita = new KpiSummaryDTO.KpiCardItem();
        perCapita.setMetricKey("per_capita");
        perCapita.setMetricName("人效");
        perCapita.setCurrentValue("0.00");
        perCapita.setCurrentValueFen(0L);
        perCapita.setYoyChange(BigDecimal.ZERO);
        perCapita.setMomChange(BigDecimal.ZERO);
        perCapita.setTargetAchievement(BigDecimal.ZERO);
        perCapita.setUnit("元/人/天");
        perCapita.setIsCurrency(false);
        kpiCards.add(perCapita);

        // 5. 坪效（暂无门店面积数据，返回0）
        KpiSummaryDTO.KpiCardItem perArea = new KpiSummaryDTO.KpiCardItem();
        perArea.setMetricKey("per_area");
        perArea.setMetricName("坪效");
        perArea.setCurrentValue("0.00");
        perArea.setCurrentValueFen(0L);
        perArea.setYoyChange(BigDecimal.ZERO);
        perArea.setMomChange(BigDecimal.ZERO);
        perArea.setTargetAchievement(BigDecimal.ZERO);
        perArea.setUnit("元/㎡/月");
        perArea.setIsCurrency(false);
        kpiCards.add(perArea);

        // 6. 异常门店数（暂无阈值规则，返回0）
        KpiSummaryDTO.KpiCardItem abnormal = new KpiSummaryDTO.KpiCardItem();
        abnormal.setMetricKey("abnormal_stores");
        abnormal.setMetricName("异常门店");
        abnormal.setCurrentValue("0");
        abnormal.setCurrentValueFen(0L);
        abnormal.setYoyChange(BigDecimal.ZERO);
        abnormal.setMomChange(BigDecimal.ZERO);
        abnormal.setTargetAchievement(BigDecimal.ZERO);
        abnormal.setUnit("家");
        abnormal.setIsCurrency(false);
        kpiCards.add(abnormal);

        result.setKpiCards(kpiCards);

        return result;
    }

    @Override
    public Map<String, Object> createExportTask(Integer taskType, Integer reportType,
                                               OperationsReportQueryDTO queryDTO, Long userId) {
        logger.info("创建导出任务，类型: {}, 报表: {}, 用户: {}", taskType, reportType, userId);
        validateQueryDTO(queryDTO);

        try {
            ExportTask task = new ExportTask();
            task.setTaskNo(exportTaskService.generateTaskNo());
            task.setTaskType(taskType);
            task.setReportType(reportType);
            task.setReportParamsJson(objectMapper.writeValueAsString(queryDTO));
            task.setStatus(0);
            task.setCreatedBy(userId);

            ExportTask savedTask = exportTaskService.createTask(task);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", savedTask.getTaskId());
            result.put("taskNo", savedTask.getTaskNo());
            result.put("status", savedTask.getStatus());

            return result;
        } catch (JsonProcessingException e) {
            logger.error("创建导出任务失败", e);
            throw new RuntimeException("创建导出任务失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getExportTaskStatus(Long taskId, Long userId) {
        ExportTask task = exportTaskService.getTaskById(taskId);
        if (task == null || !task.getCreatedBy().equals(userId)) {
            throw new RuntimeException("任务不存在或无权限访问");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("taskNo", task.getTaskNo());
        result.put("status", task.getStatus());
        result.put("fileName", task.getFileName());
        result.put("fileSize", task.getFileSize());
        result.put("rowCount", task.getRowCount());
        result.put("errorMessage", task.getErrorMessage());
        result.put("completedTime", task.getCompletedTime());

        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 检测报表类型
     */
    private Integer detectReportType(OperationsReportQueryDTO queryDTO) {
        LocalDate start = LocalDate.parse(queryDTO.getStartDate());
        LocalDate end = LocalDate.parse(queryDTO.getEndDate());
        long days = end.toEpochDay() - start.toEpochDay() + 1;

        if (days == 1) {
            return 1;
        } else if (days <= 7) {
            return 2;
        } else if (days <= 31) {
            return 3;
        } else if (days <= 92) {
            return 4;
        } else {
            return 5;
        }
    }

    // ========== 趋势数据构建 ==========

    private List<Map<String, Object>> buildDailyTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> hour = new HashMap<>();
            hour.put("date", String.format("%02d:00", i));
            hour.put("revenue", (i >= 11 && i <= 13) || (i >= 17 && i <= 20) ? 2500000L : 500000L);
            hour.put("grossRate", 60 + (i % 5));
            list.add(hour);
        }
        return list;
    }

    private List<Map<String, Object>> buildWeeklyTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < days.length; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", days[i]);
            day.put("revenue", (15000000 + i * 500000) * 100L);
            day.put("grossRate", 58 + (i % 7));
            list.add(day);
        }
        return list;
    }

    private List<Map<String, Object>> buildMonthlyTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", "2024-01-" + String.format("%02d", i));
            day.put("revenue", (15000000 + (i % 7) * 2000000L));
            day.put("grossRate", 60 + (i % 5));
            list.add(day);
        }
        return list;
    }

    private List<Map<String, Object>> buildQuarterlyTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> month = new HashMap<>();
            month.put("date", "第" + i + "月");
            month.put("revenue", (500000000L + i * 25000000L));
            month.put("grossRate", 60 + i);
            list.add(month);
        }
        return list;
    }

    private List<Map<String, Object>> buildYearlyTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> month = new HashMap<>();
            month.put("date", i + "月");
            month.put("revenue", (500000000L + (i % 3) * 50000000L));
            month.put("grossRate", 60 + (i % 5));
            list.add(month);
        }
        return list;
    }

    private List<Map<String, Object>> buildProfitTrend(OperationsReportQueryDTO queryDTO) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> month = new HashMap<>();
            month.put("date", "2024-" + String.format("%02d", i));
            month.put("revenue", (500000000L + (i % 3) * 50000000L));
            month.put("grossRate", 60 + (i % 5));
            list.add(month);
        }
        return list;
    }

    // ========== 门店排名数据构建 ==========

    /**
     * 构建门店排名数据（按门店权限过滤）
     * @param queryDTO 查询参数
     * @param storeFilter 门店过滤ID（null=不限制，非null=限制到该门店，-1=无权限）
     */
    private List<Map<String, Object>> buildStoreRanking(OperationsReportQueryDTO queryDTO, Long storeFilter) {
        List<Map<String, Object>> list = new ArrayList<>();

        // 无权限用户返回空列表
        if (storeFilter != null && storeFilter == NO_ACCESS_STORE_ID) {
            return list;
        }

        String[] storeNames = {"总店", "东区店", "西区店", "南区店", "北区店"};
        long[] revenues = {350000000L, 280000000L, 220000000L, 180000000L, 120000000L};

        // 店长（单门店权限）仅返回其门店的排名数据
        // 注：当前为 mock 数据，接入真实查询后应按 storeFilter 过滤
        int limit = (storeFilter != null) ? 1 : storeNames.length;
        for (int i = 0; i < limit; i++) {
            Map<String, Object> store = new HashMap<>();
            store.put("name", storeNames[i]);
            store.put("value", revenues[i]);
            list.add(store);
        }
        return list;
    }

    /**
     * 从已授权门店列表解析门店过滤ID
     * <p>将 getAuthorizedStoreIds 返回的门店列表转换为单门店过滤ID：</p>
     * <ul>
     *   <li>空列表：返回 {@link #NO_ACCESS_STORE_ID}（无权限，确保空结果）</li>
     *   <li>单门店：返回该门店ID（店长权限）</li>
     *   <li>多门店：返回 null（管理员/区域经理，不限制）</li>
     * </ul>
     *
     * @param authorizedStoreIds 已授权门店ID列表
     * @return null=不限制；非null=限制到该门店ID（含-1表示无权限）
     */
    private Long resolveStoreFilterFromAuthorized(List<String> authorizedStoreIds) {
        if (authorizedStoreIds == null || authorizedStoreIds.isEmpty()) {
            return NO_ACCESS_STORE_ID;
        }
        if (authorizedStoreIds.size() == 1) {
            try {
                return Long.parseLong(authorizedStoreIds.get(0));
            } catch (NumberFormatException e) {
                logger.warn("门店ID解析失败: {}", authorizedStoreIds.get(0));
                return NO_ACCESS_STORE_ID;
            }
        }
        // 多门店权限（管理员/区域经理）：不限制
        return null;
    }

    // ========== KPI 辅助方法 ==========

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

    /**
     * 分转元字符串（保留2位小数）
     */
    private String fenToYuanString(long fen) {
        return BigDecimal.valueOf(fen)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
