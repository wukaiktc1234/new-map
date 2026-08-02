package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 摄像头控制器
 * <p>托盘出餐状态变更时联动摄像头拍照留底</p>
 */
@RestController
@RequestMapping("/v1/camera")
@Tag(name = "摄像头管理", description = "托盘出餐摄像头联动拍照")
public class CameraController {

    private final CameraService cameraService;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @PostMapping("/snapshot")
    @Operation(summary = "通知摄像头拍照", description = "托盘出餐状态变更时调用，联动摄像头拍照留底")
    public Result<Map<String, Object>> notifySnapshot(
            @Parameter(description = "托盘码") @RequestParam(required = false) String trayCode,
            @Parameter(description = "后厨订单ID") @RequestParam(required = false) String kitchenOrderId,
            @Parameter(description = "扫码类型：KITCHEN_IN/KITCHEN_OUT/SERVE") @RequestParam String scanType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        Map<String, Object> result = cameraService.notifySnapshot(trayCode, kitchenOrderId, scanType, storeId);
        return Result.success(result);
    }

    @GetMapping("/snapshot-history")
    @Operation(summary = "查询摄像头拍照历史", description = "按托盘码或后厨订单查询拍照历史")
    public Result<Map<String, Object>> getSnapshotHistory(
            @Parameter(description = "托盘码") @RequestParam(required = false) String trayCode,
            @Parameter(description = "后厨订单ID") @RequestParam(required = false) String kitchenOrderId,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> result = cameraService.getSnapshotHistory(trayCode, kitchenOrderId, limit);
        return Result.success(result);
    }
}
