package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.order.DailyStatsVO;
import com.foodtraceability.service.OrderNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单趋势控制器
 * 提供订单趋势数据的API接口
 *
 * <p>业务逻辑通过 OrderNewService 实现，Controller 仅负责路由与参数透传
 */
@RestController
@RequestMapping("/v1/orders/trends")
@Tag(name = "订单趋势管理")
public class OrderTrendsController {

    private final OrderNewService orderNewService;

    public OrderTrendsController(OrderNewService orderNewService) {
        this.orderNewService = orderNewService;
    }

    /**
     * 获取订单趋势数据
     * @param startDate 开始日期（yyyy-MM-dd），为空时默认近7天起始
     * @param endDate 结束日期（yyyy-MM-dd），为空时默认今天
     * @param granularity 统计粒度：day-按天, week-按周, month-按月（当前仅支持 day）
     * @return 订单趋势数据，包含订单数、销售额等时间序列数据
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "获取订单趋势数据")
    public Result<Map<String, Object>> getOrderTrends(
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "统计粒度: day/week/month") @RequestParam(defaultValue = "day") String granularity) {
        return Result.success(orderNewService.getOrderTrends(startDate, endDate, granularity));
    }

    /**
     * 按日期分组统计
     * 返回按日期分组的营业额、成本、利润数据
     */
    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "按日期分组统计", description = "获取指定日期范围内按日期分组的营业额、成本、利润数据")
    public Result<List<DailyStatsVO>> getDailyStats(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false) String endDate,
            @Parameter(description = "门店名称") @RequestParam(required = false) String storeName) {
        try {
            List<DailyStatsVO> stats = orderNewService.getDailyStats(startDate, endDate, storeName);
            return Result.success(stats, "获取按日期分组统计成功");
        } catch (Exception e) {
            return Result.error("获取按日期分组统计失败");
        }
    }
}
