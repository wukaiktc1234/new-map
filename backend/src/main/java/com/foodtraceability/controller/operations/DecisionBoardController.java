package com.foodtraceability.controller.operations;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.operations.DecisionBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 经营分析决策看板控制器
 * 提供门店健康度评分、门店排名、品类销售分析、AI 洞察建议等接口
 */
@RestController
@RequestMapping("/v1/operations/decision-board")
@Tag(name = "经营分析", description = "经营分析决策看板接口")
public class DecisionBoardController {

    private static final Logger logger = LoggerFactory.getLogger(DecisionBoardController.class);

    private final DecisionBoardService decisionBoardService;

    public DecisionBoardController(DecisionBoardService decisionBoardService) {
        this.decisionBoardService = decisionBoardService;
    }

    /**
     * 门店健康度评分
     * 返回整体经营评分、营收达成率、成本控制率、顾客满意度等指标
     *
     * @return 健康度评分列表
     */
    @GetMapping("/health-scores")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "门店健康度评分", description = "返回整体经营评分、营收达成率、成本控制率、顾客满意度等指标")
    public Result<List<Map<String, Object>>> healthScores() {
        try {
            logger.info("获取门店健康度评分");
            List<Map<String, Object>> list = decisionBoardService.getHealthScores();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取门店健康度评分失败", e);
            return Result.error("获取门店健康度评分失败");
        }
    }

    /**
     * 门店排名
     * 返回各门店的营收、成本、利润、毛利率、环比增长、健康度等排名数据
     *
     * @return 门店排名列表
     */
    @GetMapping("/store-ranking")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "门店排名", description = "返回各门店的营收、成本、利润、毛利率、环比增长、健康度等排名数据")
    public Result<List<Map<String, Object>>> storeRanking() {
        try {
            logger.info("获取门店排名数据");
            List<Map<String, Object>> list = decisionBoardService.getStoreRanking();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取门店排名数据失败", e);
            return Result.error("获取门店排名数据失败");
        }
    }

    /**
     * 品类销售分析
     * 返回各品类的销售额及环比增长数据
     *
     * @return 品类销售列表
     */
    @GetMapping("/category-sales")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "品类销售分析", description = "返回各品类的销售额及环比增长数据")
    public Result<List<Map<String, Object>>> categorySales() {
        try {
            logger.info("获取品类销售分析数据");
            List<Map<String, Object>> list = decisionBoardService.getCategorySales();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取品类销售分析数据失败", e);
            return Result.error("获取品类销售分析数据失败");
        }
    }

    /**
     * AI 洞察建议
     * 返回基于经营数据自动生成的策略建议
     *
     * @return 洞察建议列表
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "AI 洞察建议", description = "返回基于经营数据自动生成的策略建议")
    public Result<List<Map<String, Object>>> insights() {
        try {
            logger.info("获取 AI 洞察建议");
            List<Map<String, Object>> list = decisionBoardService.getInsights();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取 AI 洞察建议失败", e);
            return Result.error("获取 AI 洞察建议失败");
        }
    }

    /**
     * 营收-成本-利润趋势
     * 返回营收、成本、利润的趋势数据（按时间维度聚合）
     *
     * @param period 时间维度：7d（近7天）/ 4w（近4周）/ 6m（近6月）
     * @return 趋势数据
     */
    @GetMapping("/revenue-trend")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "营收-成本-利润趋势", description = "返回营收、成本、利润的趋势数据")
    public Result<Map<String, Object>> revenueTrend(
            @RequestParam(defaultValue = "7d") String period) {
        try {
            logger.info("获取营收-成本-利润趋势数据, period={}", period);
            Map<String, Object> data = decisionBoardService.getRevenueTrend(period);
            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取营收-成本-利润趋势数据失败", e);
            return Result.error("获取营收-成本-利润趋势数据失败");
        }
    }
}
