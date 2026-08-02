package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.service.OperationsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * 运营数据中心控制器
 * 提供公司级数据分析与监控API
 *
 * <h2>权限说明</h2>
 * 仅 operations_director（运营总监）和 regional_manager（区域经理）可访问
 */
@RestController
@RequestMapping("/v1/operations")
@Tag(name = "运营数据中心", description = "公司级数据分析与监控")
public class OperationsDashboardController {

    private final OperationsDashboardService operationsDashboardService;

    /**
     * 构造函数注入
     *
     * @param operationsDashboardService 运营数据中心服务
     */
    public OperationsDashboardController(OperationsDashboardService operationsDashboardService) {
        this.operationsDashboardService = operationsDashboardService;
    }

    /**
     * 获取多店监控统计卡片数据
     * 包含活跃门店数、今日营收、订单总数、在岗人数等核心指标
     */
    @GetMapping("/dashboard/stats")
    @Operation(summary = "多店监控统计卡片", description = "获取门店运营核心指标数据")
    @PreAuthorize("hasAnyRole('ROLE_OPERATIONS_DIRECTOR', 'ROLE_REGIONAL_MANAGER', 'ROLE_ADMIN')")
    public Result<Map<String, Object>> getDashboardStats(Principal principal) {
        Map<String, Object> stats = operationsDashboardService.getDashboardStats(principal);
        return Result.success(stats);
    }

    /**
     * 获取门店绩效列表
     * 支持分页查询，区域经理仅可查看辖区内的门店
     */
    @GetMapping("/dashboard/stores")
    @Operation(summary = "门店绩效列表", description = "分页获取各门店的绩效数据")
    @PreAuthorize("hasAnyRole('ROLE_OPERATIONS_DIRECTOR', 'ROLE_REGIONAL_MANAGER', 'ROLE_ADMIN')")
    public Result<IPage<Map<String, Object>>> getStoreList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Principal principal) {
        IPage<Map<String, Object>> storePage = operationsDashboardService.getStorePerformanceList(
                page, size, principal);
        return Result.success(storePage);
    }
}
