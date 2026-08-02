package com.foodtraceability.controller.maintenance;

import com.foodtraceability.aspect.PerformanceMonitorAspect;
import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供API性能统计信息查询接口
 */
@RestController
@RequestMapping("/v1/performance-monitor")
@Tag(name = "性能监控", description = "API性能监控和统计")
public class PerformanceMonitorController {
    

    public PerformanceMonitorController(PerformanceMonitorAspect performanceMonitorAspect) {
        this.performanceMonitorAspect = performanceMonitorAspect;
    }

    private final PerformanceMonitorAspect performanceMonitorAspect;
    
    /**
     * 获取API性能统计
     */
    @GetMapping("/api-stats")
    @Operation(summary = "获取API性能统计")
    public Result<List<Map<String, Object>>> getApiStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        
        performanceMonitorAspect.getApiStats().forEach((methodName, apiStats) -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("methodName", methodName);
            stat.put("totalCalls", apiStats.getTotalCalls());
            stat.put("successCalls", apiStats.getSuccessCalls());
            stat.put("failedCalls", apiStats.getFailedCalls());
            stat.put("averageTime", String.format("%.2f", apiStats.getAverageTime()));
            stat.put("maxTime", apiStats.getMaxTime());
            stat.put("minTime", apiStats.getMinTime());
            stat.put("successRate", String.format("%.2f%%", apiStats.getSuccessRate()));
            stats.add(stat);
        });
        
        stats.sort((a, b) -> Long.compare(
            (Long) b.get("totalCalls"),
            (Long) a.get("totalCalls")
        ));
        
        return Result.success(stats);
    }
    
    /**
     * 重置性能统计
     */
    @PostMapping("/reset-stats")
    @Operation(summary = "重置API性能统计")
    public Result<Void> resetStats() {
        performanceMonitorAspect.resetStats();
        return Result.success(null);
    }
}
