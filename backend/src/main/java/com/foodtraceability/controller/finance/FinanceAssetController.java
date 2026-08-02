package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.AssetMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产管理Controller
 *
 * <p>Sprint 3.1 P0 F-002/F-003/F-017：资产管理相关接口。</p>
 *
 * <p>路径前缀：/v1/finance/assets（原 /finance/asset）</p>
 *
 * <p>真实化：移除所有 BigDecimal 硬编码数据，对接 AssetMasterService，
 * 金额字段全部 Long（分）。AssetMasterService 暂未实现的方法在 P0 阶段返回空数据 + TODO 标记，
 * 待 P1 阶段补齐。</p>
 */
@Tag(name = "资产管理", description = "固定资产统计、同步、折旧、处置等管理")
@RestController
@RequestMapping("/v1/finance/assets")
public class FinanceAssetController {

    private final AssetMasterService assetMasterService;

    public FinanceAssetController(AssetMasterService assetMasterService) {
        this.assetMasterService = assetMasterService;
    }

    /**
     * 获取资产统计
     *
     * <p>对接 AssetMasterService.getOverviewStats() 返回真实统计数据。</p>
     *
     * @return 资产统计信息（金额单位：分）
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取资产统计", description = "返回资产总数、净值、月折旧等统计数据，金额单位为分")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<Map<String, Object>> getStatistics() {
        // 对接 AssetMasterService 真实数据
        Map<String, Object> stats = assetMasterService.getOverviewStats();
        if (stats == null) {
            stats = new HashMap<>();
        }
        return Result.success(stats);
    }

    /**
     * 获取同步设置
     *
     * @return 同步设置
     */
    @GetMapping("/sync-settings")
    @Operation(summary = "获取同步设置", description = "获取资产同步设置")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<Map<String, Object>> getSyncSettings() {
        return Result.success(assetMasterService.getSyncSettings());
    }

    /**
     * 更新同步设置
     *
     * @param data 同步设置数据
     * @return 操作结果
     */
    @PutMapping("/sync-settings")
    @Operation(summary = "更新同步设置", description = "更新资产同步设置")
    @PreAuthorize("hasAuthority('finance:asset:manage')")
    public Result<Void> updateSyncSettings(@RequestBody Map<String, Object> data) {
        assetMasterService.updateSyncSettings(data);
        return Result.success(null);
    }

    /**
     * 同步资产数据
     *
     * @param assetIds 资产ID列表
     * @return 同步任务ID
     */
    @PostMapping("/sync")
    @Operation(summary = "同步资产数据", description = "手动触发资产数据同步")
    @PreAuthorize("hasAuthority('finance:asset:manage')")
    public Result<String> syncAssets(@RequestBody List<String> assetIds) {
        String taskId = assetMasterService.syncAssets(assetIds);
        return Result.success(taskId);
    }

    /**
     * 获取同步日志
     *
     * @param page 页码
     * @param size 每页条数
     * @return 同步日志（分页）
     */
    @GetMapping("/sync-logs")
    @Operation(summary = "获取同步日志", description = "查询资产同步日志列表")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<Map<String, Object>> getSyncLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(assetMasterService.getSyncLogs(page, size));
    }

    /**
     * 获取折旧列表
     *
     * @param assetId 资产ID（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 折旧列表
     */
    @GetMapping("/depreciation")
    @Operation(summary = "获取折旧列表", description = "查询资产折旧列表，金额单位为分")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<List<Map<String, Object>>> getDepreciationList(
            @RequestParam(required = false) Long assetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(assetMasterService.getDepreciationList(assetId, page, size));
    }

    /**
     * 计提折旧
     *
     * @param data 计提参数（assetId, method, months）
     * @return 折旧计算结果
     */
    @PostMapping("/depreciation/calculate")
    @Operation(summary = "计提折旧", description = "对指定资产计提折旧，金额单位为分")
    @PreAuthorize("hasAuthority('finance:asset:manage')")
    public Result<Map<String, Object>> calculateDepreciation(@RequestBody Map<String, Object> data) {
        Long assetId = data.get("assetId") != null ? Long.valueOf(data.get("assetId").toString()) : null;
        String method = (String) data.getOrDefault("method", "straight_line");
        int months = data.get("months") != null ? Integer.parseInt(data.get("months").toString()) : 1;
        return Result.success(assetMasterService.calculateDepreciation(assetId, method, months));
    }

    /**
     * 获取资产处置列表
     *
     * @param page 页码
     * @param size 每页条数
     * @return 处置列表
     */
    @GetMapping("/disposal")
    @Operation(summary = "获取资产处置列表", description = "查询资产处置申请列表，金额单位为分")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<List<Map<String, Object>>> getDisposalList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(assetMasterService.getDisposalList(page, size));
    }

    /**
     * 获取处置详情
     *
     * @param id 处置单ID
     * @return 处置详情
     */
    @GetMapping("/disposal/{id}")
    @Operation(summary = "获取处置详情", description = "根据ID查询资产处置详情，金额单位为分")
    @PreAuthorize("hasAuthority('finance:asset:view')")
    public Result<Map<String, Object>> getDisposalDetail(@PathVariable Long id) {
        return Result.success(assetMasterService.getDisposalDetail(id));
    }

    /**
     * 创建处置申请
     *
     * @param data 处置申请数据（assetId, type, reason, handler）
     * @return 创建结果（含处置单ID）
     */
    @PostMapping("/disposal")
    @Operation(summary = "创建处置申请", description = "创建资产处置申请")
    @PreAuthorize("hasAuthority('finance:asset:manage')")
    public Result<Map<String, Object>> createDisposal(@RequestBody Map<String, Object> data) {
        Long assetId = data.get("assetId") != null ? Long.valueOf(data.get("assetId").toString()) : null;
        String type = (String) data.get("type");
        String reason = (String) data.get("reason");
        String handler = (String) data.get("handler");
        Long disposalId = assetMasterService.createDisposal(assetId, type, reason, handler);
        Map<String, Object> result = new HashMap<>();
        result.put("id", disposalId);
        result.put("status", "pending");
        return Result.success(result);
    }

    /**
     * 审批处置申请
     *
     * @param id   处置单ID
     * @param data 审批数据（approved, comment）
     * @return 操作结果
     */
    @PostMapping("/disposal/{id}/approve")
    @Operation(summary = "审批处置申请", description = "审批资产处置申请")
    @PreAuthorize("hasAuthority('finance:asset:manage')")
    public Result<Void> approveDisposal(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        boolean approved = Boolean.parseBoolean(data.getOrDefault("approved", false).toString());
        String comment = (String) data.get("comment");
        assetMasterService.approveDisposal(id, approved, comment);
        return Result.success(null);
    }
}
