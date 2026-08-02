package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.TrayBindOrderRequest;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.Tray;
import com.foodtraceability.service.TrayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tray")
@Tag(name = "托盘管理", description = "托盘绑定与出餐确认API")
public class TrayController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrayController.class);
    private final TrayService trayService;

    @GetMapping("/list")
    @Operation(summary = "获取所有托盘列表")
    public Result<List<Tray>> getAllTrays() {
        List<Tray> trays = trayService.findAll();
        return Result.success(trays);
    }

    @GetMapping("/idle")
    @Operation(summary = "获取空闲托盘列表")
    public Result<List<Tray>> getIdleTrays(@Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        List<Tray> trays = trayService.findIdleTrays(limit);
        return Result.success(trays);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取托盘统计（5状态机）")
    public Result<Map<String, Integer>> getStats() {
        int idle = trayService.countByStatus("idle");
        int bound = trayService.countByStatus("bound");
        int making = trayService.countByStatus("making");
        int ready = trayService.countByStatus("ready");
        int served = trayService.countByStatus("served");
        int cleaning = trayService.countByStatus("cleaning");
        int damaged = trayService.countByStatus("damaged");
        int total = idle + bound + making + ready + served + cleaning + damaged;
        Map<String, Integer> stats = new HashMap<>();
        stats.put("idle", idle);
        stats.put("bound", bound);
        stats.put("making", making);
        stats.put("ready", ready);
        stats.put("served", served);
        stats.put("cleaning", cleaning);
        stats.put("damaged", damaged);
        stats.put("total", total);
        return Result.success(stats);
    }

    @GetMapping("/{trayCode}")
    @Operation(summary = "根据托盘码查询托盘")
    public Result<Tray> getByTrayCode(@Parameter(description = "托盘码") @PathVariable String trayCode) {
        Tray tray = trayService.findByTrayCode(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    @PostMapping("/create")
    @Operation(summary = "创建托盘")
    public Result<Tray> createTray(@RequestBody Tray tray) {
        Tray created = trayService.createTray(tray);
        return Result.success(created);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建托盘")
    public Result<String> batchCreateTrays(@Parameter(description = "数量") @RequestParam int count, @Parameter(description = "编码前缀") @RequestParam(required = false, defaultValue = "TRAY") String prefix, @Parameter(description = "托盘类型") @RequestParam(required = false, defaultValue = "standard") String trayType, @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId, @Parameter(description = "门店名称") @RequestParam(required = false) String storeName) {
        trayService.batchCreateTrays(count, prefix, trayType, storeId, storeName);
        return Result.success("成功创建 " + count + " 个托盘");
    }

    @PostMapping("/bind-order")
    @Operation(summary = "扫描托盘码绑定订单（POS端，托盘 idle → bound）")
    public Result<Tray> bindOrder(@RequestBody TrayBindOrderRequest request) {
        log.info("绑定托盘订单: trayCode={}, orderId={}, kitchenOrderId={}", request.getTrayCode(), request.getOrderId(), request.getKitchenOrderId());
        return trayService.bindOrder(request.getTrayCode(), request.getOrderId(), request.getKitchenOrderId());
    }

    @PostMapping("/scan-kitchen-in")
    @Operation(summary = "后厨一次扫码（托盘 bound → making，防抖≥3s）")
    public Result<Tray> scanKitchenIn(
            @Parameter(description = "托盘码") @RequestParam String trayCode,
            @Parameter(description = "扫码设备ID") @RequestParam(required = false) Long scanDeviceId,
            @Parameter(description = "扫码设备编码") @RequestParam(required = false) String scanDeviceCode,
            @Parameter(description = "操作员ID") @RequestParam(required = false) String operatorId,
            @Parameter(description = "操作员姓名") @RequestParam(required = false) String operatorName) {
        log.info("后厨一次扫码: trayCode={}, scanDeviceId={}", trayCode, scanDeviceId);
        return trayService.scanKitchenIn(trayCode, scanDeviceId, scanDeviceCode, operatorId, operatorName);
    }

    @PostMapping("/scan-kitchen-out")
    @Operation(summary = "后厨二次扫码（托盘 making → ready，防抖≥5s，联动摄像头+YOLO）")
    public Result<Tray> scanKitchenOut(
            @Parameter(description = "托盘码") @RequestParam String trayCode,
            @Parameter(description = "扫码设备ID") @RequestParam(required = false) Long scanDeviceId,
            @Parameter(description = "扫码设备编码") @RequestParam(required = false) String scanDeviceCode,
            @Parameter(description = "操作员ID") @RequestParam(required = false) String operatorId,
            @Parameter(description = "操作员姓名") @RequestParam(required = false) String operatorName) {
        log.info("后厨二次扫码: trayCode={}, scanDeviceId={}", trayCode, scanDeviceId);
        return trayService.scanKitchenOut(trayCode, scanDeviceId, scanDeviceCode, operatorId, operatorName);
    }

    @PostMapping("/scan-serve")
    @Operation(summary = "取餐口确认出餐（托盘 ready → served → idle，防抖≥2s）")
    public Result<KitchenOrder> scanServe(@Parameter(description = "托盘码") @RequestParam String trayCode, @Parameter(description = "操作员ID") @RequestParam(required = false) String operatorId, @Parameter(description = "操作员姓名") @RequestParam(required = false) String operatorName) {
        log.info("出餐口扫描托盘确认出餐: trayCode={}", trayCode);
        return trayService.scanServe(trayCode, operatorId, operatorName);
    }

    @PostMapping("/release")
    @Operation(summary = "释放托盘")
    public Result<Tray> releaseTray(@Parameter(description = "托盘码") @RequestParam String trayCode) {
        Tray tray = trayService.releaseTray(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    @PostMapping("/start-cleaning")
    @Operation(summary = "开始清洁托盘")
    public Result<Tray> startCleaning(@Parameter(description = "托盘码") @RequestParam String trayCode) {
        Tray tray = trayService.startCleaning(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    @PostMapping("/finish-cleaning")
    @Operation(summary = "完成清洁托盘")
    public Result<Tray> finishCleaning(@Parameter(description = "托盘码") @RequestParam String trayCode) {
        Tray tray = trayService.finishCleaning(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    public TrayController(final TrayService trayService) {
        this.trayService = trayService;
    }
}
