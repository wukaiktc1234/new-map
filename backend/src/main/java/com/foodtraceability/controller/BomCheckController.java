package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.AlternativeDishVO;
import com.foodtraceability.dto.product.BatchCheckRequestDTO;
import com.foodtraceability.dto.product.BatchCheckResultVO;
import com.foodtraceability.dto.product.BomCheckResultVO;
import com.foodtraceability.dto.product.BomCheckStatsVO;
import com.foodtraceability.dto.product.BomStockWarningConfigVO;
import com.foodtraceability.dto.product.StockForecastVO;
import com.foodtraceability.service.BomCheckService;
import com.foodtraceability.service.StockForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BOM 库存联动检查控制器
 * 提供菜品 BOM 配方与库存的联动检查、预警配置、替代推荐、统计等接口
 *
 * <p>路径前缀：/v1/product-center/bom-check
 *
 * <p>接口分组：
 * <ul>
 *   <li>检查：GET /{dishId}、POST /batch、POST /pre-order</li>
 *   <li>预警配置：GET /warning-config、POST /warning-config</li>
 *   <li>替代推荐：GET /alternatives/{dishId}</li>
 *   <li>统计：GET /stats</li>
 * </ul>
 *
 * <p>注意：字面量路径（/batch、/pre-order、/warning-config、/alternatives、/stats）
 * 需在 /{dishId} 之前定义，避免路径变量误匹配。
 */
@Tag(name = "BOM库存联动检查", description = "菜品BOM与库存联动检查、预警、替代推荐接口")
@RestController
@RequestMapping("/v1/product-center/bom-check")
public class BomCheckController {

    private final BomCheckService bomCheckService;
    private final StockForecastService stockForecastService;

    public BomCheckController(BomCheckService bomCheckService,
                              StockForecastService stockForecastService) {
        this.bomCheckService = bomCheckService;
        this.stockForecastService = stockForecastService;
    }

    // ==================== 字面量路径优先定义（避免被 /{dishId} 拦截） ====================

    /**
     * 批量检查多个菜品的 BOM 库存
     */
    @Operation(summary = "批量检查BOM库存", description = "批量检查多个菜品的BOM配方所需物料库存情况")
    @PostMapping("/batch")
    public Result<BatchCheckResultVO> batchCheck(@RequestBody BatchCheckRequestDTO request) {
        try {
            return Result.success(bomCheckService.batchCheck(request));
        } catch (Exception e) {
            return Result.error("批量检查BOM库存失败");
        }
    }

    /**
     * 下单前预检查（识别阻塞和警告级问题）
     */
    @Operation(summary = "下单前预检查", description = "下单前批量预检查菜品BOM库存，识别阻塞和警告级问题")
    @PostMapping("/pre-order")
    public Result<Map<String, Object>> preOrderCheck(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> dishIds = body == null ? null : (List<String>) body.get("dishIds");
            return Result.success(bomCheckService.preOrderCheck(dishIds));
        } catch (Exception e) {
            return Result.error("下单前预检查失败");
        }
    }

    /**
     * 获取 BOM 预警配置
     */
    @Operation(summary = "获取BOM预警配置", description = "获取BOM库存预警配置信息")
    @GetMapping("/warning-config")
    public Result<BomStockWarningConfigVO> getWarningConfig() {
        try {
            return Result.success(bomCheckService.getWarningConfig());
        } catch (Exception e) {
            return Result.error("获取BOM预警配置失败");
        }
    }

    /**
     * 保存 BOM 预警配置
     */
    @Operation(summary = "保存BOM预警配置", description = "保存BOM库存预警配置信息")
    @PostMapping("/warning-config")
    public Result<BomStockWarningConfigVO> saveWarningConfig(@RequestBody BomStockWarningConfigVO config) {
        try {
            return Result.success(bomCheckService.saveWarningConfig(config));
        } catch (Exception e) {
            return Result.error("保存BOM预警配置失败");
        }
    }

    /**
     * 获取替代菜品推荐
     */
    @Operation(summary = "获取替代菜品推荐", description = "获取指定菜品的可替代菜品推荐列表")
    @GetMapping("/alternatives/{dishId}")
    public Result<List<AlternativeDishVO>> getAlternatives(
            @Parameter(description = "原菜品ID") @PathVariable String dishId,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "5") Integer limit) {
        try {
            return Result.success(bomCheckService.getAlternatives(dishId, limit));
        } catch (Exception e) {
            return Result.error("获取替代菜品推荐失败");
        }
    }

    /**
     * 获取 BOM 检查统计数据
     */
    @Operation(summary = "获取BOM检查统计", description = "获取指定日期范围内的BOM检查统计数据")
    @GetMapping("/stats")
    public Result<BomCheckStatsVO> getStats(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        try {
            return Result.success(bomCheckService.getStats(startDate, endDate));
        } catch (Exception e) {
            return Result.error("获取BOM检查统计失败");
        }
    }

    /**
     * 原料消耗差异分析（方案E）
     */
    @Operation(summary = "原料消耗差异分析", description = "近N天各原料理论消耗 vs 实际消耗（含损耗）差异，揪出超耗/浪费")
    @GetMapping("/variance-analysis")
    public Result<List<Map<String, Object>>> varianceAnalysis(
            @Parameter(description = "数据窗口（天，默认7）") @RequestParam(defaultValue = "7") Integer days,
            @Parameter(description = "返回条数，默认20") @RequestParam(defaultValue = "20") Integer limit) {
        try {
            return Result.success(stockForecastService.varianceAnalysis(days, limit));
        } catch (Exception e) {
            return Result.error("差异分析失败：" + e.getMessage());
        }
    }

    // ==================== 路径变量放最后 ====================

    /**
     * 菜品可做份数预测（方案D+，含损耗自校准/售罄时间/补货量）
     */
    @Operation(summary = "菜品可做份数预测", description = "基于近N天订单销量+库存流水自校准实际每份用量（含损耗），预测可做份数/售罄时间/补货量")
    @GetMapping("/{dishId}/forecast")
    public Result<StockForecastVO> forecast(
            @Parameter(description = "菜品ID") @PathVariable Long dishId,
            @Parameter(description = "数据窗口（天，默认7）") @RequestParam(defaultValue = "7") Integer days) {
        try {
            return Result.success(stockForecastService.forecast(dishId, days));
        } catch (Exception e) {
            return Result.error("可做份数预测失败：" + e.getMessage());
        }
    }

    /**
     * 检查单个菜品的 BOM 库存
     */
    @Operation(summary = "检查菜品BOM库存", description = "检查指定菜品的BOM配方所需物料库存是否充足")
    @GetMapping("/{dishId}")
    public Result<BomCheckResultVO> checkDish(
            @Parameter(description = "菜品ID") @PathVariable String dishId,
            @Parameter(description = "制作数量") @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            return Result.success(bomCheckService.checkDish(dishId, quantity));
        } catch (Exception e) {
            return Result.error("检查菜品BOM库存失败");
        }
    }
}
