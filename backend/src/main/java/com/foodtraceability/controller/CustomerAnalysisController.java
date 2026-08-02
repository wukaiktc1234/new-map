package com.foodtraceability.controller;

import com.foodtraceability.service.CustomerAnalysisService;
import com.foodtraceability.dto.CustomerAnalysisVO;
import com.foodtraceability.dto.CustomerAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 客户分析控制器
 * 提供客户（会员）数据分析的API接口
 */
@Tag(name = "客户分析", description = "会员客户分析、RFM分群、留存分析、CLV预测")
@RestController
@RequestMapping("/v1/analytics/customer")
public class CustomerAnalysisController {

    private final CustomerAnalysisService customerAnalysisService;

    public CustomerAnalysisController(CustomerAnalysisService customerAnalysisService) {
        this.customerAnalysisService = customerAnalysisService;
    }

    /**
     * 获取会员增长趋势
     * @param days 最近天数
     * @return 会员增长趋势
     */
    @Operation(summary = "会员增长趋势", description = "查询指定天数内的会员增长趋势")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/member-growth-trend")
    public Result<Map<String, Object>> getMemberGrowthTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        try {
            Map<String, Object> data = customerAnalysisService.getMemberGrowthTrend(days);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取会员增长趋势失败: " + e.getMessage());
        }
    }

    /**
     * 生成客户分析报表
     * @param period 统计周期
     * @return 客户分析报表
     */
    @Operation(summary = "生成客户分析报告", description = "按指定周期生成客户分析报告")
    @PreAuthorize("hasAuthority('member:analysis:manage') or hasAuthority('*')")
    @PostMapping("/report")
    public Result<CustomerAnalysisVO> generateCustomerReport(@RequestParam String period) {
        try {
            CustomerAnalysisVO vo = customerAnalysisService.generateCustomerReport(period);
            return Result.success(vo, "报表生成成功");
        } catch (Exception e) {
            return Result.error("生成客户报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取留存队列分析
     * @return 留存队列数据
     */
    @Operation(summary = "留存同期群分析", description = "查询会员留存同期群分析数据")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/retention-cohort")
    public Result<Map<String, Object>> getRetentionCohortAnalysis() {
        try {
            Map<String, Object> data = customerAnalysisService.getRetentionCohortAnalysis();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取留存队列分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取RFM分布快照
     * @return RFM分布数据
     */
    @Operation(summary = "RFM分布快照", description = "查询当前RFM客户分群分布")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/rfm-distribution")
    public Result<Map<String, Object>> getRFMDistributionSnapshot() {
        try {
            Map<String, Object> data = customerAnalysisService.getRFMDistributionSnapshot();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取RFM分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取分群洞察
     * @param segmentType 分群类型
     * @return 分群洞察数据
     */
    @Operation(summary = "客户分群洞察", description = "按分群类型查询客户洞察数据")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/segment-insight")
    public Result<Map<String, Object>> getMemberSegmentInsight(@RequestParam String segmentType) {
        try {
            Map<String, Object> data = customerAnalysisService.getMemberSegmentInsight(segmentType);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取分群洞察失败: " + e.getMessage());
        }
    }

    /**
     * 获取CLV预测
     * @param months 未来月数
     * @return CLV预测数据
     */
    @Operation(summary = "CLV预测", description = "客户生命周期价值预测")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/clv-forecast")
    public Result<Map<String, Object>> getCLVForecast(@RequestParam(defaultValue = "6") Integer months) {
        try {
            Map<String, Object> data = customerAnalysisService.getCLVForecast(months);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取CLV预测失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询客户报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询分析报告", description = "按条件分页查询已生成的客户分析报告")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @PostMapping("/reports/query")
    public Result<PageResult<CustomerAnalysisVO>> queryReports(@RequestBody CustomerAnalysisQueryDTO queryDTO) {
        try {
            PageResult<CustomerAnalysisVO> result = customerAnalysisService.queryCustomerReports(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询客户报表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    @Operation(summary = "获取报告详情", description = "根据报告ID获取客户分析报告详情")
    @PreAuthorize("hasAuthority('member:analysis:view') or hasAuthority('*')")
    @GetMapping("/reports/{reportId}")
    public Result<CustomerAnalysisVO> getReportById(@PathVariable Long reportId) {
        try {
            CustomerAnalysisVO vo = customerAnalysisService.getReportById(reportId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取报表详情失败: " + e.getMessage());
        }
    }
}
