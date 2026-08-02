package com.foodtraceability.controller;

import com.foodtraceability.service.DashboardService;
import com.foodtraceability.dto.DashboardConfigVO;
import com.foodtraceability.dto.DashboardConfigQueryDTO;
import com.foodtraceability.dto.DashboardConfigCreateDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.common.Result;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 经营看板控制器
 * 提供经营看板相关的API接口
 */
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取看板概览数据
     * @param dashboardType 看板类型（1总览 2销售 3库存 4财务 5会员）
     * @return 看板概览数据
     */
    @GetMapping("/overview/{dashboardType}")
    public Result<Map<String, Object>> getDashboardOverview(@PathVariable Integer dashboardType) {
        try {
            Map<String, Object> data = dashboardService.getDashboardOverview(dashboardType);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取看板概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日销售概览
     * @return 今日销售数据
     */
    @GetMapping("/today-sales")
    public Result<Map<String, Object>> getTodaySalesOverview() {
        try {
            Map<String, Object> data = dashboardService.getTodaySalesOverview();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取今日销售概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取库存预警信息
     * @return 库存预警列表
     */
    @GetMapping("/inventory-alerts")
    public Result<Map<String, Object>> getInventoryAlerts() {
        try {
            Map<String, Object> data = dashboardService.getInventoryAlerts();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取库存预警信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取会员增长趋势
     * @param days 最近天数
     * @return 会员增长趋势
     */
    @GetMapping("/member-growth")
    public Result<Map<String, Object>> getMemberGrowthTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        try {
            Map<String, Object> data = dashboardService.getMemberGrowthTrend(days);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取会员增长趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取财务概况
     * @return 财务关键指标
     */
    @GetMapping("/finance-summary")
    public Result<Map<String, Object>> getFinanceSummary() {
        try {
            Map<String, Object> data = dashboardService.getFinanceSummary();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取财务概况失败: " + e.getMessage());
        }
    }

    /**
     * 保存看板配置
     * @param dto 配置信息
     * @return 配置ID
     */
    @PostMapping("/config")
    public Result<Long> saveDashboardConfig(@RequestBody DashboardConfigCreateDTO dto) {
        try {
            Long configId = dashboardService.saveDashboardConfig(dto);
            return Result.success(configId, "保存成功");
        } catch (Exception e) {
            return Result.error("保存看板配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户看板配置
     * @param userId 用户ID
     * @param dashboardType 看板类型
     * @return 配置信息
     */
    @GetMapping("/config/user/{userId}/type/{dashboardType}")
    public Result<DashboardConfigVO> getUserDashboardConfig(
            @PathVariable Long userId,
            @PathVariable Integer dashboardType) {
        try {
            DashboardConfigVO vo = dashboardService.getUserDashboardConfig(userId, dashboardType);
            if (vo == null) {
                return Result.success(null, "暂无配置");
            }
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取看板配置失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询看板配置
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/config/query")
    public Result<PageResult<DashboardConfigVO>> queryDashboardConfigs(@RequestBody DashboardConfigQueryDTO queryDTO) {
        try {
            PageResult<DashboardConfigVO> result = dashboardService.queryDashboardConfigs(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询看板配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除看板配置
     * @param configId 配置ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/config/{configId}")
    public Result<Boolean> deleteDashboardConfig(
            @PathVariable Long configId,
            @RequestParam Long userId) {
        try {
            boolean result = dashboardService.deleteDashboardConfig(configId, userId);
            if (result) {
                return Result.success(true, "删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除看板配置失败: " + e.getMessage());
        }
    }
}
