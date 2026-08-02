package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1/cache")
@Tag(name = "缓存管理", description = "缓存管理相关接口")
public class CacheController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CacheController.class);
    private final CacheService cacheService;

    @PostMapping("/clear")
    @Operation(summary = "清理缓存")
    public Result<Void> clearCache(@Parameter(description = "缓存类型") @RequestParam(required = false) String cacheType) {
        return cacheService.clearCache(cacheType);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计")
    public Result<Map<String, Object>> getCacheStats() {
        try {
            log.info("获取缓存统计");
            return Result.success(Map.of("totalKeys", 0, "totalMemory", "0MB", "hitRate", "95%", "description", "缓存统计信息"), "获取缓存统计成功");
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return Result.error(500, "获取缓存统计失败：" + e.getMessage());
        }
    }

    public CacheController(final CacheService cacheService) {
        this.cacheService = cacheService;
    }
}
