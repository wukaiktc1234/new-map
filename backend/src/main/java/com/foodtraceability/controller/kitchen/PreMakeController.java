package com.foodtraceability.controller.kitchen;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PreMakeRequestDTO;
import com.foodtraceability.service.PreMakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/kitchen/pre-make")
@Tag(name = "提前制作管理", description = "高峰期提前制作模式API")
public class PreMakeController {

    private final PreMakeService preMakeService;

    public PreMakeController(PreMakeService preMakeService) {
        this.preMakeService = preMakeService;
    }

    @PostMapping("/create")
    @Operation(summary = "提前制作：生成食品追溯码")
    public Result<Map<String, Object>> preMakeFood(@Valid @RequestBody PreMakeRequestDTO request) {
        Map<String, Object> result = preMakeService.preMakeFood(request);
        if (result.containsKey("error")) {
            return Result.error((String) result.get("error"));
        }
        return Result.success(result);
    }

    @PostMapping("/scan-serve")
    @Operation(summary = "扫描食品码出餐（匹配待出餐订单）")
    public Result<Map<String, Object>> scanServe(
            @RequestParam String traceCode,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorName) {
        Map<String, Object> result = preMakeService.scanServe(traceCode, orderNumber, operatorId, operatorName);
        if (result.containsKey("error")) {
            return Result.error((String) result.get("error"));
        }
        return Result.success(result);
    }

    @GetMapping("/ready-foods")
    @Operation(summary = "获取已提前制作待出餐的食品列表")
    public Result<List<Map<String, Object>>> getReadyFoods() {
        List<Map<String, Object>> readyFoods = preMakeService.getReadyFoods();
        return Result.success(readyFoods);
    }
}
