package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器（已移除 Redis 健康检查，仅保留 PostgreSQL 数据库检查）
 */
@Tag(name = "健康检查", description = "系统健康状态检查端点")
@RestController
@RequestMapping("/v1/health/check")
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param jdbcTemplate JDBC模板，可选依赖
     */
    public HealthCheckController(@Nullable JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "综合健康检查（负载均衡器探测用）")
    @GetMapping
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        boolean overallHealthy = true;

        try {
            result.put("status", "UP");
            result.put("timestamp", LocalDateTime.now().toString());

            // 数据库连接检查
            boolean dbHealthy = checkDatabase();
            result.put("database", dbHealthy ? "UP" : "DOWN");
            if (!dbHealthy) overallHealthy = false;

            // 磁盘空间检查
            long freeSpace = getDiskFreeSpace();
            result.put("diskSpaceBytes", freeSpace);
            result.put("diskStatus", freeSpace > 1073741824 ? "OK" : "WARNING"); // 1GB阈值

            // JVM内存检查
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            result.put("jvmMemoryUsagePercent", Math.round(memoryUsage * 100.0) / 100.0);
            result.put("jvmMemoryStatus", memoryUsage < 85 ? "OK" : "WARNING");

            if (!overallHealthy) {
                result.put("status", "DEGRADED");
                return Result.error(503, "服务降级", result);
            }

            return Result.success(result);
        } catch (Exception e) {
            logger.error("健康检查失败: {}", e.getMessage(), e);
            result.put("status", "DOWN");
            return Result.error(503, "服务不可用", result);
        }
    }

    @Operation(summary = "就绪探针（Kubernetes readinessProbe）")
    @GetMapping("/ready")
    public Result<Map<String, String>> readinessProbe() {
        Map<String, String> result = new HashMap<>();
        try {
            boolean dbOk = checkDatabase();

            if (dbOk) {
                result.put("status", "READY");
                return Result.success(result);
            }

            result.put("status", "NOT_READY");
            result.put("database", dbOk ? "UP" : "DOWN");
            return Result.error(503, "未就绪", result);
        } catch (Exception e) {
            result.put("status", "NOT_READY");
            return Result.error(503, "未就绪", result);
        }
    }

    @Operation(summary = "存活探针（Kubernetes livenessProbe）")
    @GetMapping("/live")
    public Result<Map<String, String>> livenessProbe() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ALIVE");
        result.put("timestamp", LocalDateTime.now().toString());
        return Result.success(result);
    }

    private boolean checkDatabase() {
        if (jdbcTemplate == null) {
            return true;
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            logger.warn("数据库健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private long getDiskFreeSpace() {
        try {
            java.io.File file = new java.io.File(".");
            return file.getUsableSpace();
        } catch (Exception e) {
            return -1;
        }
    }
}
