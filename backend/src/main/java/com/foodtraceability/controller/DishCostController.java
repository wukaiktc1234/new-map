package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.product.CostAlertRuleVO;
import com.foodtraceability.dto.product.CostSummaryVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import com.foodtraceability.dto.product.DishCostQueryDTO;
import com.foodtraceability.dto.product.DishCostSnapshotVO;
import com.foodtraceability.service.DishCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜品成本控制器
 * 提供菜品成本快照、成本趋势、BOM 明细、预警规则、汇总统计、报表导出等接口
 *
 * <p>路径前缀：/v1/product-center/dish-cost
 *
 * <p>接口分组：
 * <ul>
 *   <li>查询：GET /list、GET /summary</li>
 *   <li>操作：POST /recalculate、POST /report/export</li>
 *   <li>预警规则：GET /alert-rules、POST /alert-rules、PUT /alert-rules/{id}</li>
 *   <li>单菜品：GET /{dishId}、GET /{dishId}/trend、GET /{dishId}/bom、PUT /{dishId}/bom/{materialId}</li>
 * </ul>
 *
 * <p>注意：字面量路径（/list、/summary、/recalculate、/alert-rules、/report/export）
 * 需在 /{dishId} 之前定义，避免路径变量误匹配。
 */
@Tag(name = "菜品成本管理", description = "菜品成本快照、趋势、BOM明细、预警规则、汇总统计接口")
@RestController
@RequestMapping("/v1/product-center/dish-cost")
public class DishCostController {

    private final DishCostService dishCostService;

    public DishCostController(DishCostService dishCostService) {
        this.dishCostService = dishCostService;
    }

    // ==================== 字面量路径优先定义（避免被 /{dishId} 拦截） ====================

    /**
     * 分页查询菜品成本列表
     */
    @Operation(summary = "分页查询菜品成本", description = "支持分类、状态、关键字筛选和排序")
    @GetMapping("/list")
    public Result<PageResult<DishCostSnapshotVO>> getDishCostList(DishCostQueryDTO queryDto) {
        try {
            return Result.success(dishCostService.getDishCostList(queryDto));
        } catch (Exception e) {
            return Result.error("查询菜品成本列表失败");
        }
    }

    /**
     * 重新计算所有菜品成本
     */
    @Operation(summary = "重算所有菜品成本", description = "触发异步任务重新计算所有菜品的成本快照")
    @PostMapping("/recalculate")
    public Result<Map<String, Object>> recalculateAll() {
        try {
            return Result.success(dishCostService.recalculateAll());
        } catch (Exception e) {
            return Result.error("触发成本重算失败");
        }
    }

    /**
     * 获取成本汇总统计
     */
    @Operation(summary = "获取成本汇总", description = "获取指定日期范围内的成本汇总统计")
    @GetMapping("/summary")
    public Result<CostSummaryVO> getCostSummary(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        try {
            return Result.success(dishCostService.getCostSummary(startDate, endDate));
        } catch (Exception e) {
            return Result.error("获取成本汇总失败");
        }
    }

    /**
     * 获取成本预警规则列表
     */
    @Operation(summary = "获取成本预警规则", description = "获取所有成本预警规则")
    @GetMapping("/alert-rules")
    public Result<List<CostAlertRuleVO>> getAlertRules() {
        try {
            return Result.success(dishCostService.getAlertRules());
        } catch (Exception e) {
            return Result.error("获取成本预警规则失败");
        }
    }

    /**
     * 保存成本预警规则
     */
    @Operation(summary = "保存成本预警规则", description = "新增或更新成本预警规则")
    @PostMapping("/alert-rules")
    public Result<CostAlertRuleVO> saveAlertRule(@RequestBody CostAlertRuleVO rule) {
        try {
            return Result.success(dishCostService.saveAlertRule(rule));
        } catch (Exception e) {
            return Result.error("保存成本预警规则失败");
        }
    }

    /**
     * 切换成本预警规则启用状态
     */
    @Operation(summary = "切换预警规则状态", description = "启用或禁用指定的成本预警规则")
    @PutMapping("/alert-rules/{id}")
    public Result<Void> toggleAlertRule(
            @Parameter(description = "规则ID") @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            Boolean enabled = body == null ? null : (Boolean) body.get("enabled");
            dishCostService.toggleAlertRule(id, enabled);
            return Result.success();
        } catch (Exception e) {
            return Result.error("切换预警规则状态失败");
        }
    }

    /**
     * 导出成本报表
     */
    @Operation(summary = "导出成本报表", description = "按分类和状态筛选导出成本报表为 Excel/CSV 文件")
    @PostMapping("/report/export")
    public ResponseEntity<byte[]> exportCostReport(@RequestBody(required = false) Map<String, Object> params) {
        try {
            String category = params == null ? null : (String) params.get("category");
            String status = params == null ? null : (String) params.get("status");
            byte[] data = dishCostService.exportCostReport(category, status);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "cost-report.csv");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new byte[0]);
        }
    }

    // ==================== 单菜品路径变量 ====================

    /**
     * 获取菜品成本快照
     */
    @Operation(summary = "获取菜品成本快照", description = "获取指定菜品的当前成本构成和毛利信息")
    @GetMapping("/{dishId}")
    public Result<DishCostSnapshotVO> getDishCost(
            @Parameter(description = "菜品ID") @PathVariable String dishId) {
        try {
            return Result.success(dishCostService.getDishCost(dishId));
        } catch (Exception e) {
            return Result.error("获取菜品成本快照失败");
        }
    }

    /**
     * 获取菜品成本趋势
     */
    @Operation(summary = "获取菜品成本趋势", description = "获取指定菜品近 N 天的成本变化趋势")
    @GetMapping("/{dishId}/trend")
    public Result<List<CostTrendDataVO>> getCostTrend(
            @Parameter(description = "菜品ID") @PathVariable String dishId,
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") Integer days) {
        try {
            return Result.success(dishCostService.getCostTrend(dishId, days));
        } catch (Exception e) {
            return Result.error("获取菜品成本趋势失败");
        }
    }

    /**
     * 获取菜品 BOM 明细成本
     */
    @Operation(summary = "获取菜品BOM明细成本", description = "获取指定菜品的 BOM 配方明细及各项成本")
    @GetMapping("/{dishId}/bom")
    public Result<DishCostSnapshotVO> getBomDetail(
            @Parameter(description = "菜品ID") @PathVariable String dishId) {
        try {
            return Result.success(dishCostService.getBomDetail(dishId));
        } catch (Exception e) {
            return Result.error("获取菜品BOM明细失败");
        }
    }

    /**
     * 更新 BOM 项数量
     */
    @Operation(summary = "更新BOM项数量", description = "更新指定菜品 BOM 中某物料的标准用量")
    @PutMapping("/{dishId}/bom/{materialId}")
    public Result<Void> updateBomQuantity(
            @Parameter(description = "菜品ID") @PathVariable String dishId,
            @Parameter(description = "物料ID") @PathVariable String materialId,
            @RequestBody Map<String, Object> body) {
        try {
            Double quantity = body == null ? null : ((Number) body.get("quantity")).doubleValue();
            dishCostService.updateBomQuantity(dishId, materialId, quantity);
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新BOM项数量失败");
        }
    }
}
