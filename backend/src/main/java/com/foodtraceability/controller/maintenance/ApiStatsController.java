package com.foodtraceability.controller.maintenance;

import com.foodtraceability.aspect.ApiRequestCountAspect;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API统计控制器（已移除 Redis 依赖）
 * 请求次数统计从 ApiRequestCountAspect 内存统计获取；
 * 响应时间分布、错误率、平均响应时间暂返回空数据（ApiResponseTimeAspect 未暴露公开统计方法）。
 */
@RestController
@RequestMapping("/v1/stats")
@Tag(name = "API统计管理")
@SuppressWarnings("null")
public class ApiStatsController {

    private final ApiRequestCountAspect apiRequestCountAspect;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param apiRequestCountAspect API请求次数统计切面（内存统计）
     */
    public ApiStatsController(ApiRequestCountAspect apiRequestCountAspect) {
        this.apiRequestCountAspect = apiRequestCountAspect;
    }

    /**
     * 查询API请求次数（从内存统计中读取）
     */
    @GetMapping("/api-counts")
    @Operation(summary = "查询API请求次数统计")
    @PreAuthorize("hasAuthority('system:stats:view') or hasAuthority('*')")
    public Map<String, Object> getApiCounts(
            @Parameter(description = "统计日期，格式：yyyy-MM-dd，默认当天") @RequestParam(required = false) String date,
            @Parameter(description = "HTTP方法，如GET、POST，默认所有方法") @RequestParam(required = false) String method,
            @Parameter(description = "API路径，如/v1/products，默认所有路径") @RequestParam(required = false) String uri) {

        String today = date != null ? date : LocalDate.now().format(DATE_FORMATTER);
        Map<String, Long> counts = new HashMap<>();

        // 构造前缀用于筛选：api:count:{date}:{method?}:
        StringBuilder prefixBuilder = new StringBuilder("api:count:").append(today).append(":");
        if (method != null && !method.isEmpty()) {
            prefixBuilder.append(method).append(":");
        }
        String prefix = prefixBuilder.toString();

        // 从内存统计中获取数据
        ConcurrentMap<String, AtomicLong> allCounts = apiRequestCountAspect.getAllCounts();
        for (Map.Entry<String, AtomicLong> entry : allCounts.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix)) {
                continue;
            }

            // 解析 key: api:count:{date}:{method}:{uri}
            // 限制分割次数为5，避免 uri 中的 ":" 导致分割异常
            String[] parts = key.split(":", 5);
            if (parts.length < 5) {
                continue;
            }

            String keyMethod = parts[3];
            String keyUri = parts[4];

            // uri 过滤
            if (uri != null && !uri.isEmpty() && !uri.equals(keyUri)) {
                continue;
            }

            String apiPath = String.format("%s:%s", keyMethod, keyUri);
            counts.put(apiPath, entry.getValue().get());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("counts", counts);
        result.put("total", counts.values().stream().mapToLong(Long::longValue).sum());

        return result;
    }

    /**
     * 查询指定API的历史请求趋势（从内存统计中读取）
     */
    @GetMapping("/api-trend")
    @Operation(summary = "查询API请求趋势")
    @PreAuthorize("hasAuthority('system:stats:view') or hasAuthority('*')")
    public Map<String, Object> getApiTrend(
            @Parameter(description = "API路径，如/v1/products") @RequestParam String uri,
            @Parameter(description = "HTTP方法，如GET、POST") @RequestParam String method,
            @Parameter(description = "查询天数，默认7天") @RequestParam(required = false, defaultValue = "7") int days) {

        Map<String, Long> trendData = new HashMap<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        ConcurrentMap<String, AtomicLong> allCounts = apiRequestCountAspect.getAllCounts();

        while (!startDate.isAfter(endDate)) {
            String dateStr = startDate.format(DATE_FORMATTER);
            String key = String.format("api:count:%s:%s:%s", dateStr, method, uri);
            AtomicLong count = allCounts.get(key);
            trendData.put(dateStr, count != null ? count.get() : 0L);
            startDate = startDate.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uri", uri);
        result.put("method", method);
        result.put("days", days);
        result.put("trend", trendData);

        return result;
    }

    /**
     * 查询API响应时间分布
     * 注：由于移除 Redis 依赖且 ApiResponseTimeAspect 未暴露公开统计方法，暂返回空数据
     */
    @GetMapping("/response-time-distribution")
    @Operation(summary = "查询API响应时间分布")
    @PreAuthorize("hasAuthority('system:stats:view') or hasAuthority('*')")
    public Map<String, Object> getResponseTimeDistribution(
            @Parameter(description = "统计日期，格式：yyyy-MM-dd，默认当天") @RequestParam(required = false) String date,
            @Parameter(description = "HTTP方法，如GET、POST") @RequestParam String method,
            @Parameter(description = "API路径，如/v1/products") @RequestParam String uri) {

        String today = date != null ? date : LocalDate.now().format(DATE_FORMATTER);
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("0-100ms", 0L);
        distribution.put("100-500ms", 0L);
        distribution.put("500-1000ms", 0L);
        distribution.put("1-3s", 0L);
        distribution.put("3-5s", 0L);
        distribution.put("5s+", 0L);

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("method", method);
        result.put("uri", uri);
        result.put("distribution", distribution);

        return result;
    }

    /**
     * 查询API错误率统计
     * 注：由于移除 Redis 依赖且 ApiResponseTimeAspect 未暴露公开统计方法，暂返回空数据
     */
    @GetMapping("/error-rate")
    @Operation(summary = "查询API错误率统计")
    @PreAuthorize("hasAuthority('system:stats:view')")
    public Map<String, Object> getErrorRate(
            @Parameter(description = "统计日期，格式：yyyy-MM-dd，默认当天") @RequestParam(required = false) String date,
            @Parameter(description = "HTTP方法，如GET、POST") @RequestParam(required = false) String method,
            @Parameter(description = "API路径，如/v1/products，默认所有路径") @RequestParam(required = false) String uri) {

        String today = date != null ? date : LocalDate.now().format(DATE_FORMATTER);

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("errorRateData", new HashMap<>());

        return result;
    }

    /**
     * 查询API平均响应时间
     * 注：由于移除 Redis 依赖且 ApiResponseTimeAspect 未暴露公开统计方法，暂返回空数据
     */
    @GetMapping("/avg-response-time")
    @Operation(summary = "查询API平均响应时间")
    @PreAuthorize("hasAuthority('system:stats:view')")
    public Map<String, Object> getAvgResponseTime(
            @Parameter(description = "统计日期，格式：yyyy-MM-dd，默认当天") @RequestParam(required = false) String date,
            @Parameter(description = "HTTP方法，如GET、POST") @RequestParam(required = false) String method,
            @Parameter(description = "API路径，如/v1/products，默认所有路径") @RequestParam(required = false) String uri) {

        String today = date != null ? date : LocalDate.now().format(DATE_FORMATTER);

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("avgResponseTime", new HashMap<>());

        return result;
    }
}
