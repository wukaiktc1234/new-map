package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PositionBasicInfo;
import com.foodtraceability.entity.Position;
import com.foodtraceability.service.PositionDataService;
import com.foodtraceability.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 职位管理控制器
 */
@Tag(name = "职位管理", description = "职位相关接口")
@RestController
@RequestMapping("/v1/positions")
public class PositionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PositionController.class);
    private final PositionService positionService;
    private final PositionDataService positionDataService;

    public PositionController(PositionService positionService, PositionDataService positionDataService) {
        this.positionService = positionService;
        this.positionDataService = positionDataService;
    }

    @Operation(summary = "生成职位编码", description = "根据部门和职位名称生成完整的职位编码")
    @GetMapping("/code/generate")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<String> generatePositionCode(@Parameter(description = "部门ID") @RequestParam(required = false) String departmentId, @Parameter(description = "职位名称") @RequestParam String positionName) {
        try {
            String code = positionService.generatePositionCode(departmentId, positionName);
            return Result.success(code);
        } catch (Exception e) {
            log.error("生成职位编码失败", e);
            return Result.error("生成职位编码失败: " + e.getMessage());
        }
    }

    @Operation(summary = "生成职位编码序号", description = "根据部门和职位名称生成职位编码的序号部分")
    @GetMapping("/seq/generate")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Integer> generatePositionCodeSeq(@Parameter(description = "部门ID") @RequestParam(required = false) String departmentId, @Parameter(description = "职位名称") @RequestParam String positionName) {
        try {
            String positionPrefix = positionService.getPositionCodePrefix(positionName);
            String departmentCode = positionService.getDepartmentCode(departmentId);
            String codePrefix = departmentCode + "-" + positionPrefix;
            Integer seq = positionService.getNextPositionSeq(codePrefix);
            return Result.success(seq);
        } catch (Exception e) {
            log.error("生成职位编码序号失败", e);
            return Result.error("生成职位编码序号失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取职位列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<String, Object>> getPositions(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = positionService.getPositions(current, size, departmentId, status, keyword);
        return Result.success(result);
    }

    /**
     * 获取所有职位（不分页）
     * 注意：此端点必须显式声明 @GetMapping("/all")，避免被 @GetMapping("/{id}") 拦截
     * 路径冲突 BUG 修复（G3-01）：原 /v1/positions/all 被误匹配到 /{id} 路径变量
     */
    @Operation(summary = "获取所有职位（不分页）", description = "返回所有职位列表，不进行分页")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<List<Position>> getAllPositions() {
        List<Position> positions = positionService.getAllPositions();
        return Result.success(positions);
    }

    @Operation(summary = "获取职位详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Position> getPosition(@PathVariable Long id) {
        Position position = positionService.getPositionById(id);
        if (position == null) {
            return Result.error("职位不存在");
        }
        return Result.success(position);
    }

    @Operation(summary = "创建职位")
    @PostMapping
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Position> createPosition(@RequestBody Position position) {
        Position created = positionService.createPosition(position);
        log.info("创建职位成功，职位ID：{}", created.getId());
        return Result.success(created);
    }

    @Operation(summary = "更新职位")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Position> updatePosition(@PathVariable Long id, @RequestBody Position position) {
        Position updated = positionService.updatePosition(id, position);
        log.info("更新职位成功，职位ID：{}", id);
        return Result.success(updated);
    }

    @Operation(summary = "删除职位")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
        log.info("删除职位成功，职位ID：{}", id);
        return Result.success();
    }

    @Operation(summary = "按编码前缀清理职位", description = "物理删除指定编码前缀且无员工关联的职位（默认清理 POS_ 前缀）")
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole(\'admin\')")
    public Result<Map<String, Object>> cleanupPositionsByPrefix(
            @RequestParam(required = false, defaultValue = "POS_") String prefix) {
        int cleanedCount = positionService.cleanupPositionsByPrefix(prefix);
        Map<String, Object> result = new HashMap<>();
        result.put("prefix", prefix);
        result.put("cleanedCount", cleanedCount);
        log.info("按前缀 [{}] 清理职位完成，共清理 {} 个", prefix, cleanedCount);
        return Result.success(result);
    }

    @Operation(summary = "更新职位状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Void> updatePositionStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Boolean statusValue = "active".equals(status);
        positionService.updatePositionStatus(id, statusValue);
        log.info("更新职位状态成功，职位ID：{}，状态：{}", id, status);
        return Result.success();
    }

    @Operation(summary = "根据部门获取职位列表")
    @GetMapping("/departments/{departmentId}/positions")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<List<Position>> getPositionsByDepartment(@PathVariable Long departmentId) {
        List<Position> positions = positionService.getPositionsByDepartment(departmentId);
        return Result.success(positions);
    }

    @Operation(summary = "清空所有职位数据")
    @DeleteMapping("/all")
    @PreAuthorize("hasRole(\'admin\')")
    public Result<Void> clearAllPositions() {
        positionService.clearAllPositions();
        log.warn("清空所有职位数据，操作人：admin");
        return Result.success();
    }

    @Operation(summary = "批量获取职位基本信息", description = "根据职位ID列表批量获取职位基本信息")
    @PostMapping("/batch/basic-info")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<Map<Long, PositionBasicInfo>> batchGetPositionBasicInfo(@Parameter(description = "职位ID列表") @RequestBody List<String> positionIds) {
        try {
            List<Long> longIds = positionIds.stream().map(id -> {
                try {
                    return Long.parseLong(id);
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            Map<Long, PositionBasicInfo> result = positionDataService.batchGetPositionBasicInfo(longIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量获取职位基本信息失败", e);
            return Result.error("批量获取职位基本信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取职位基本信息", description = "根据职位ID获取职位基本信息")
    @GetMapping("/basic-info/{positionId}")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\', \'store_manager\')")
    public Result<PositionBasicInfo> getPositionBasicInfo(@Parameter(description = "职位ID") @PathVariable Long positionId) {
        try {
            PositionBasicInfo result = positionDataService.getPositionBasicInfo(positionId);
            if (result == null) {
                return Result.error("职位不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取职位基本信息失败", e);
            return Result.error("获取职位基本信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量更新职位编码", description = "根据当前的全局编码规则批量更新所有职位的编码")
    @PostMapping("/batch-update-codes")
    @PreAuthorize("hasAnyRole(\'admin\', \'hr\')")
    public Result<Map<String, Object>> batchUpdatePositionCodes() {
        try {
            Map<String, Object> result = positionService.batchUpdatePositionCodes();
            log.info("批量更新职位编码完成");
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量更新职位编码失败", e);
            return Result.error("批量更新职位编码失败: " + e.getMessage());
        }
    }
}
