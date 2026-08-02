package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.ExpiryAlertQueryDTO;
import com.foodtraceability.dto.trace.ExpiryAlertVO;
import com.foodtraceability.dto.trace.ExpiryDashboardVO;
import com.foodtraceability.dto.trace.ExpiryStatisticsVO;
import com.foodtraceability.service.trace.ExpiryAlertService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临期预警管理控制器
 * 提供临期预警看板、临期/已过期列表查询、一键报损/退货和预警统计功能
 */
@Tag(name = "临期预警管理", description = "临期预警看板、报损、退货")
@RestController
@RequestMapping("/v1/expiry-alerts")
public class ExpiryAlertController {

    private final ExpiryAlertService expiryAlertService;

    public ExpiryAlertController(ExpiryAlertService expiryAlertService) {
        this.expiryAlertService = expiryAlertService;
    }

    /** 临期预警看板聚合数据 */
    @Operation(summary = "临期预警看板", description = "获取临期预警看板聚合数据")
    @GetMapping("/dashboard")
    public Result<ExpiryDashboardVO> dashboard() {
        ExpiryDashboardVO vo = expiryAlertService.getDashboard();
        return Result.success(vo);
    }

    /** 临期列表（按剩余天数排序） */
    @Operation(summary = "临期列表", description = "查询临期列表，按剩余天数升序排序")
    @GetMapping("/expiring-soon")
    public Result<IPage<ExpiryAlertVO>> expiringSoon(ExpiryAlertQueryDTO queryDTO) {
        IPage<ExpiryAlertVO> page = expiryAlertService.queryExpiringSoon(queryDTO);
        return Result.success(page);
    }

    /** 已过期列表 */
    @Operation(summary = "已过期列表", description = "查询已过期列表")
    @GetMapping("/expired")
    public Result<IPage<ExpiryAlertVO>> expired(ExpiryAlertQueryDTO queryDTO) {
        IPage<ExpiryAlertVO> page = expiryAlertService.queryExpired(queryDTO);
        return Result.success(page);
    }

    /** 一键报损 */
    @Operation(summary = "一键报损", description = "将指定追溯码对应的物料标记为报损")
    @PostMapping("/{traceCodeId}/scrap")
    public Result<Boolean> scrap(@PathVariable Long traceCodeId,
                                  @RequestParam(required = false) String remark) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        boolean result = expiryAlertService.scrap(traceCodeId, operatorId, operatorName, remark);
        return Result.success(result, result ? "报损操作成功" : "报损操作失败");
    }

    /** 一键退货 */
    @Operation(summary = "一键退货", description = "将指定追溯码对应的物料标记为退货")
    @PostMapping("/{traceCodeId}/return")
    public Result<Boolean> returnGoods(@PathVariable Long traceCodeId,
                                        @RequestParam(required = false) String remark) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        boolean result = expiryAlertService.returnGoods(traceCodeId, operatorId, operatorName, remark);
        return Result.success(result, result ? "退货操作成功" : "退货操作失败");
    }

    /** 预警统计 */
    @Operation(summary = "预警统计", description = "获取临期预警统计数据")
    @GetMapping("/statistics")
    public Result<ExpiryStatisticsVO> statistics() {
        ExpiryStatisticsVO vo = expiryAlertService.getStatistics();
        return Result.success(vo);
    }
}
