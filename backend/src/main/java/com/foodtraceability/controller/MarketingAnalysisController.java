package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.MarketingStatisticsVO;
import com.foodtraceability.service.MarketingAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营销分析控制器
 * 提供RFM模型、客户分群、流失预警、营销效果分析等接口
 */
@RestController
@RequestMapping("/v1/marketing/analysis")
@Tag(name = "营销分析", description = "RFM客户分群、流失预警、营销统计、趋势分析")
public class MarketingAnalysisController {

    private final MarketingAnalysisService analysisService;

    public MarketingAnalysisController(MarketingAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/overview")
    @Operation(summary = "获取营销统计概览（仪表盘）")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<MarketingStatisticsVO> getOverview() {
        MarketingStatisticsVO vo = analysisService.getStatisticsOverview();
        return Result.success(vo);
    }

    // ========== RFM相关 ==========

    @PostMapping("/rfm/calculate")
    @Operation(summary = "手动触发RFM计算（管理员）")
    @PreAuthorize("hasAuthority('member:analysis:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> calculateRFM() {
        Map<String, Object> result = analysisService.calculateRFM();
        return Result.success(result);
    }

    @GetMapping("/rfm/distribution")
    @Operation(summary = "获取RFM客户分布")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<Map<String, Long>> getRFMDistribution() {
        Map<String, Long> distribution = analysisService.getRFMDistribution();
        return Result.success(distribution);
    }

    // ========== 客户列表 ==========

    @GetMapping("/churn-risk")
    @Operation(summary = "获取流失风险会员列表")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getChurnRiskMembers(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> members = analysisService.getChurnRiskMembers(days, limit);
        return Result.success(members);
    }

    @GetMapping("/high-value")
    @Operation(summary = "获取高价值会员列表")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getHighValueMembers(
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> members = analysisService.getHighValueMembers(limit);
        return Result.success(members);
    }

    @GetMapping("/clv-ranking")
    @Operation(summary = "获取客户生命周期价值排名")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getCLVRanking(
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> ranking = analysisService.getCLVRanking(limit);
        return Result.success(ranking);
    }

    // ========== 趋势数据 ==========

    @GetMapping("/trend")
    @Operation(summary = "获取近N天趋势数据")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getTrendData(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> trend = analysisService.getTrendData(days);
        return Result.success(trend);
    }

    // ========== 流失预警 ==========

    @PostMapping("/churn-warning/process")
    @Operation(summary = "执行流失预警处理（发送提醒/打标签）")
    @PreAuthorize("hasAuthority('member:analysis:manage') or hasAuthority('*')")
    public Result<Integer> processChurnWarning(
            @RequestParam(defaultValue = "30") int days) {
        int processed = analysisService.processChurnWarning(days);
        return Result.success(processed, "已处理" + processed + "个流失风险会员");
    }
}
