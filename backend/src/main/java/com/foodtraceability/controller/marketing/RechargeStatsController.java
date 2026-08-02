package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.RechargeStatsOverviewVO;
import com.foodtraceability.service.marketing.RechargeStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 储值统计控制器
 * 对应前端 API 路径 /v1/recharge-stats
 *
 * 端点说明：
 * 1. GET /v1/recharge-stats/overview - 获取储值统计概览
 *
 * 字面量路径 /overview 优先匹配（无 {id} 路径，但仍保持规范）
 */
@RestController
@RequestMapping("/v1/recharge-stats")
@Tag(name = "储值统计", description = "储值仪表盘统计数据")
public class RechargeStatsController {

    private final RechargeStatsService rechargeStatsService;

    public RechargeStatsController(RechargeStatsService rechargeStatsService) {
        this.rechargeStatsService = rechargeStatsService;
    }

    /**
     * 获取储值统计概览
     * 包含：余额统计、本月统计、今日统计、方案使用分布、近7天趋势、预警数据
     */
    @GetMapping("/overview")
    @Operation(summary = "获取储值统计概览")
    public Result<RechargeStatsOverviewVO> getOverview() {
        try {
            RechargeStatsOverviewVO vo = rechargeStatsService.getOverview();
            return Result.success(vo, "获取储值统计概览成功");
        } catch (Exception e) {
            return Result.error(500, "获取储值统计概览失败：" + e.getMessage());
        }
    }
}
