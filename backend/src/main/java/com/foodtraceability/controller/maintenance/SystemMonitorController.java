package com.foodtraceability.controller.maintenance;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控控制器
 * @author example
 * @since 2026-01-08
 */
@RestController
@RequestMapping("/v1/system-monitor")
public class SystemMonitorController {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param healthEndpoint 健康端点，可选依赖
     * @param metricsEndpoint 指标端点，可选依赖
     */
    public SystemMonitorController(@Nullable HealthEndpoint healthEndpoint, @Nullable MetricsEndpoint metricsEndpoint) {
        this.healthEndpoint = healthEndpoint;
        this.metricsEndpoint = metricsEndpoint;
    }

    /**
     * 获取系统综合状态
     * @return 系统状态
     */
    @GetMapping("/system-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取健康状态
            result.put("health", healthEndpoint.health());

            // 获取系统信息
            result.put("system", getSystemInfo());

            // 获取内存使用情况
            result.put("memory", getMemoryInfo());

            // 获取线程信息
            result.put("threads", getThreadInfo());

            // 获取CPU使用情况
            result.put("cpu", getCpuInfo());

            // 获取关键指标
            result.put("metrics", getKeyMetrics());

            result.put("success", true);
            result.put("message", "获取系统状态成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取系统状态失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取系统信息
     * @return 系统信息
     */
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        systemInfo.put("osName", osBean.getName());
        systemInfo.put("osVersion", osBean.getVersion());
        systemInfo.put("osArch", osBean.getArch());
        systemInfo.put("availableProcessors", osBean.getAvailableProcessors());
        
        // 获取运行时间
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        systemInfo.put("uptime", uptime);
        systemInfo.put("uptimeFormatted", formatUptime(uptime));
        
        return systemInfo;
    }

    /**
     * 获取内存使用情况
     * @return 内存使用情况
     */
    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        memoryInfo.put("heap", Map.of(
            "init", heapMemory.getInit(),
            "used", heapMemory.getUsed(),
            "committed", heapMemory.getCommitted(),
            "max", heapMemory.getMax(),
            "usedPercent", calculateMemoryUsagePercent(heapMemory)
        ));
        
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        memoryInfo.put("nonHeap", Map.of(
            "init", nonHeapMemory.getInit(),
            "used", nonHeapMemory.getUsed(),
            "committed", nonHeapMemory.getCommitted(),
            "max", nonHeapMemory.getMax(),
            "usedPercent", calculateMemoryUsagePercent(nonHeapMemory)
        ));
        
        memoryInfo.put("totalUsed", heapMemory.getUsed() + nonHeapMemory.getUsed());
        
        return memoryInfo;
    }

    /**
     * 获取线程信息
     * @return 线程信息
     */
    private Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        threadInfo.put("totalThreads", threadBean.getThreadCount());
        threadInfo.put("daemonThreads", threadBean.getDaemonThreadCount());
        threadInfo.put("peakThreads", threadBean.getPeakThreadCount());
        threadInfo.put("startedThreads", threadBean.getTotalStartedThreadCount());
        threadInfo.put("deadlockedThreads", threadBean.findDeadlockedThreads() != null ? 
            threadBean.findDeadlockedThreads().length : 0);
        
        return threadInfo;
    }

    /**
     * 获取CPU使用情况
     * @return CPU使用情况
     */
    private Map<String, Object> getCpuInfo() {
        Map<String, Object> cpuInfo = new HashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // 注意：Java 8 及以上可以获取系统负载
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            cpuInfo.put("systemLoadAverage", sunOsBean.getSystemLoadAverage());
            cpuInfo.put("processCpuLoad", sunOsBean.getProcessCpuLoad());
        }
        
        cpuInfo.put("availableProcessors", osBean.getAvailableProcessors());
        
        return cpuInfo;
    }

    /**
     * 获取关键指标
     * @return 关键指标
     */
    private Map<String, Object> getKeyMetrics() {
        Map<String, Object> keyMetrics = new HashMap<>();
        
        try {
            // 简化指标获取，避免使用可能不存在的类
            keyMetrics.put("httpRequests", "N/A");
            keyMetrics.put("jvmMemoryUsed", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
            keyMetrics.put("jvmThreadsLive", ManagementFactory.getThreadMXBean().getThreadCount());
            
            // 获取系统指标
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            keyMetrics.put("availableProcessors", osBean.getAvailableProcessors());
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                keyMetrics.put("systemLoadAverage", sunOsBean.getSystemLoadAverage());
                keyMetrics.put("processCpuLoad", sunOsBean.getProcessCpuLoad());
            }
            
        } catch (Exception e) {
            // 忽略指标获取错误
        }
        
        return keyMetrics;
    }

    /**
     * 计算内存使用百分比
     */
    private double calculateMemoryUsagePercent(MemoryUsage memoryUsage) {
        if (memoryUsage.getMax() > 0) {
            return (double) memoryUsage.getUsed() / memoryUsage.getMax() * 100;
        }
        return 0;
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptime) {
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%d天 %d小时 %d分钟 %d秒", days, hours % 24, minutes % 60, seconds % 60);
    }

    /**
     * 获取应用状态概览
     * @return 应用状态概览
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简单的状态检查
            result.put("status", "UP");
            result.put("message", "系统运行正常");
            result.put("timestamp", System.currentTimeMillis());
            
            // 获取运行时间
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            result.put("uptime", uptime);
            result.put("uptimeFormatted", formatUptime(uptime));
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取状态失败: " + e.getMessage());
        }
        return result;
    }
}
