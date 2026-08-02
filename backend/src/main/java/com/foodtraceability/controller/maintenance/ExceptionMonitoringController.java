package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.ExceptionMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常监控控制器
 * 提供异常统计信息的API接口
 */
@RestController
@RequestMapping("/v1/exception-monitoring")
public class ExceptionMonitoringController {


    public ExceptionMonitoringController(ExceptionMonitoringService exceptionMonitoringService) {
        this.exceptionMonitoringService = exceptionMonitoringService;
    }

    private final ExceptionMonitoringService exceptionMonitoringService;

    /**
     * 获取异常统计信息
     * @return 异常统计信息
     */
    @GetMapping("/statistics")
    public Result<?> getExceptionStatistics() {
        ConcurrentHashMap<String, AtomicInteger> exceptionCounts = exceptionMonitoringService.getExceptionCounts();
        
        // 转换为可序列化的Map
        Map<String, Integer> statistics = new java.util.HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : exceptionCounts.entrySet()) {
            statistics.put(entry.getKey(), entry.getValue().get());
        }
        
        return Result.success(statistics, "获取异常统计信息成功");
    }

    /**
     * 重置异常统计
     * @return 操作结果
     */
    @GetMapping("/reset")
    public Result<?> resetExceptionStatistics() {
        exceptionMonitoringService.resetExceptionCounts();
        return Result.success("重置异常统计成功");
    }
}
