package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.FoodTraceCodeGenerateDTO;
import com.foodtraceability.entity.FoodTraceCode;
import com.foodtraceability.service.FoodTraceCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 食品追溯码控制器
 */
@RestController
@RequestMapping("/v1/food-trace-code")
@Tag(name = "食品追溯码管理", description = "食品追溯码的生成、查询、出餐等接口")
public class FoodTraceCodeController {


    public FoodTraceCodeController(FoodTraceCodeService foodTraceCodeService) {
        this.foodTraceCodeService = foodTraceCodeService;
    }

    private final FoodTraceCodeService foodTraceCodeService;

    @PostMapping("/generate")
    @Operation(summary = "生成食品追溯码")
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<FoodTraceCode> generate(@RequestBody FoodTraceCodeGenerateDTO dto) {
        try {
            FoodTraceCode code = foodTraceCodeService.generate(dto);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error("生成食品追溯码失败: " + e.getMessage());
        }
    }

    @GetMapping("/query/{traceCode}")
    @Operation(summary = "根据追溯码查询")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<FoodTraceCode> queryByTraceCode(
            @Parameter(description = "追溯码") @PathVariable String traceCode) {
        FoodTraceCode code = foodTraceCodeService.getByTraceCode(traceCode);
        if (code == null) {
            return Result.error("追溯码不存在");
        }
        return Result.success(code);
    }

    @GetMapping("/trace-info/{traceCode}")
    @Operation(summary = "消费者查询追溯信息")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Object> getTraceInfo(
            @Parameter(description = "追溯码") @PathVariable String traceCode) {
        Object traceInfo = foodTraceCodeService.getTraceInfo(traceCode);
        if (traceInfo == null) {
            return Result.error("追溯码不存在");
        }
        return Result.success(traceInfo);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询食品追溯码列表")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<FoodTraceCode>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "订单ID") @RequestParam(required = false) String orderId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "制作状态") @RequestParam(required = false) String makeStatus,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {

        Page<FoodTraceCode> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FoodTraceCode> wrapper = new LambdaQueryWrapper<>();

        if (orderId != null && !orderId.isEmpty()) {
            wrapper.eq(FoodTraceCode::getOrderId, orderId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FoodTraceCode::getStatus, status);
        }
        if (makeStatus != null && !makeStatus.isEmpty()) {
            wrapper.eq(FoodTraceCode::getMakeStatus, makeStatus);
        }
        if (storeId != null) {
            wrapper.eq(FoodTraceCode::getStoreId, storeId);
        }

        wrapper.orderByDesc(FoodTraceCode::getCreateTime);

        IPage<FoodTraceCode> result = foodTraceCodeService.page(pageParam, wrapper);
        return Result.success(result);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID查询")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<FoodTraceCode>> listByOrderId(
            @Parameter(description = "订单ID") @PathVariable String orderId) {
        List<FoodTraceCode> codes = foodTraceCodeService.getByOrderId(orderId);
        return Result.success(codes);
    }

    @PutMapping("/{traceCodeId}/make-status")
    @Operation(summary = "更新制作状态")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> updateMakeStatus(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId,
            @Parameter(description = "制作状态") @RequestParam String makeStatus) {
        try {
            boolean result = foodTraceCodeService.updateMakeStatus(traceCodeId, makeStatus);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/{traceCodeId}/start-make")
    @Operation(summary = "开始制作")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> startMake(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId,
            @Parameter(description = "厨师ID") @RequestParam Long chefId,
            @Parameter(description = "厨师姓名") @RequestParam String chefName) {
        try {
            boolean result = foodTraceCodeService.startMake(traceCodeId, chefId, chefName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("开始制作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{traceCodeId}/complete-make")
    @Operation(summary = "完成制作")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> completeMake(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId) {
        try {
            boolean result = foodTraceCodeService.completeMake(traceCodeId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("完成制作失败: " + e.getMessage());
        }
    }

    @PostMapping("/serve")
    @Operation(summary = "扫码出餐")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> serve(
            @Parameter(description = "追溯码") @RequestParam String traceCode) {
        try {
            boolean result = foodTraceCodeService.serve(traceCode);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("出餐失败: " + e.getMessage());
        }
    }

    @PostMapping("/{traceCodeId}/print")
    @Operation(summary = "打印追溯码标签")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> printLabel(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId,
            @Parameter(description = "打印机ID") @RequestParam(required = false) Long printerId) {
        try {
            boolean result = foodTraceCodeService.printLabel(traceCodeId, printerId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("打印失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-print")
    @Operation(summary = "批量打印追溯码")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> batchPrint(
            @RequestBody List<String> traceCodeIds,
            @Parameter(description = "打印机ID") @RequestParam(required = false) Long printerId) {
        try {
            boolean result = foodTraceCodeService.batchPrint(traceCodeIds, printerId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("打印失败: " + e.getMessage());
        }
    }

    @GetMapping("/order-cost/{orderId}")
    @Operation(summary = "计算订单成本")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<BigDecimal> calculateOrderCost(
            @Parameter(description = "订单ID") @PathVariable String orderId) {
        BigDecimal cost = foodTraceCodeService.calculateOrderCost(orderId);
        return Result.success(cost);
    }

    @GetMapping("/qr/{traceCodeId}")
    @Operation(summary = "生成二维码URL")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<String> generateQrCode(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId) {
        String qrCodeUrl = foodTraceCodeService.generateQrCode(traceCodeId);
        return Result.success(qrCodeUrl);
    }

    @GetMapping("/statistics")
    @Operation(summary = "统计各状态数量")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Map<String, Integer>> statistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("created", foodTraceCodeService.countByStatus("created"));
        stats.put("printed", foodTraceCodeService.countByStatus("printed"));
        stats.put("served", foodTraceCodeService.countByStatus("served"));
        stats.put("expired", foodTraceCodeService.countByStatus("expired"));
        return Result.success(stats);
    }
}
