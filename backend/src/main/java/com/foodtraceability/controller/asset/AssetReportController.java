package com.foodtraceability.controller.asset;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.AssetMasterNewService;
import com.foodtraceability.service.AssetMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产报表管理控制器
 * 提供资产概览、状态分布、类型分布、门店分布、折旧汇总等报表接口
 */
@RestController
@RequestMapping("/v1/asset/report")
@Tag(name = "资产报表管理", description = "资产概览、分布统计及折旧汇总报表API")
public class AssetReportController {

    private final AssetMasterService assetMasterService;
    private final AssetMasterNewService assetMasterNewService;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetReportController(AssetMasterService assetMasterService,
                                 AssetMasterNewService assetMasterNewService) {
        this.assetMasterService = assetMasterService;
        this.assetMasterNewService = assetMasterNewService;
    }

    /**
     * 资产概览报表
     * @return 概览统计数据
     */
    @GetMapping("/overview")
    @Operation(summary = "资产概览报表", description = "返回资产总数、状态统计、类型统计、门店统计等概览数据")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> overview = new HashMap<>();
        // 状态统计
        List<Map<String, Object>> statusStats = assetMasterService.countByStatus();
        // 类型统计
        List<Map<String, Object>> typeStats = assetMasterService.countByType();
        // 门店统计
        List<Map<String, Object>> storeStats = assetMasterService.countByStore();
        // 汇总总数
        int total = 0;
        for (Map<String, Object> stat : statusStats) {
            Object countObj = stat.get("count");
            if (countObj instanceof Number) {
                total += ((Number) countObj).intValue();
            }
        }
        overview.put("total", total);
        overview.put("statusStats", statusStats);
        overview.put("typeStats", typeStats);
        overview.put("storeStats", storeStats);
        return Result.success(overview);
    }

    /**
     * 状态分布统计
     * @return 各状态资产数量
     */
    @GetMapping("/status-distribution")
    @Operation(summary = "状态分布统计", description = "返回按资产状态分组的数量统计")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> statusDistribution() {
        List<Map<String, Object>> stats = assetMasterService.countByStatus();
        return Result.success(stats);
    }

    /**
     * 类型分布统计
     * @return 各类型资产数量
     */
    @GetMapping("/type-distribution")
    @Operation(summary = "类型分布统计", description = "返回按资产类型分组的数量统计")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> typeDistribution() {
        List<Map<String, Object>> stats = assetMasterService.countByType();
        return Result.success(stats);
    }

    /**
     * 门店分布统计
     * @return 各门店资产数量
     */
    @GetMapping("/store-distribution")
    @Operation(summary = "门店分布统计", description = "返回按门店分组的资产数量统计")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> storeDistribution() {
        List<Map<String, Object>> stats = assetMasterService.countByStore();
        return Result.success(stats);
    }

    /**
     * 折旧汇总报表
     * @return 折旧汇总数据
     */
    @GetMapping("/depreciation-summary")
    @Operation(summary = "折旧汇总报表", description = "返回资产原值、累计折旧、净值等汇总数据")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> depreciationSummary() {
        Map<String, Object> summary = assetMasterNewService.getOverviewStats();
        return Result.success(summary);
    }
}
