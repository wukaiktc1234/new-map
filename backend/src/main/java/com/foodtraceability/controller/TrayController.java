package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.TrayBindOrderRequest;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.Tray;
import com.foodtraceability.security.filter.KdsTokenFilter;
import com.foodtraceability.service.TrayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Operation(summary = "获取空闲托盘列表",
            description = "KDS 设备凭证调用时自动收敛为本店托盘；管理端可通过 storeId 指定门店，缺省返回全部")
    public Result<List<Tray>> getIdleTrays(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "门店ID（管理端可选；KDS 调用时以凭证门店为准，忽略此参数）")
            @RequestParam(required = false) Long storeId) {
        // 2026-10-23 修复（设计缺陷）：原实现直接返回全库空闲托盘，
        // 门店1的 KDS 会看到门店2的托盘，选中后绑定必被「门店不匹配」拒绝 —— 交互陷阱。
        // 现按调用方身份收敛：KDS 凭证强制用凭证门店（防止客户端伪造 storeId 越权查询）。
        Long effectiveStoreId = storeId;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KdsTokenFilter.KdsPrincipal kdsPrincipal) {
            try {
                effectiveStoreId = Long.parseLong(kdsPrincipal.getStoreId());
            } catch (NumberFormatException e) {
                log.warn("KDS token store_id 非法，无法收敛托盘列表: {}", kdsPrincipal.getStoreId());
                return Result.error("KDS token store_id 格式无效");
            }
        }
        List<Tray> trays = trayService.findIdleTraysByStore(effectiveStoreId, limit);
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
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Tray> createTray(@RequestBody Tray tray) {
        Tray created = trayService.createTray(tray);
        return Result.success(created);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建托盘")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<String> batchCreateTrays(@Parameter(description = "数量") @RequestParam int count, @Parameter(description = "编码前缀") @RequestParam(required = false, defaultValue = "TRAY") String prefix, @Parameter(description = "托盘类型") @RequestParam(required = false, defaultValue = "standard") String trayType, @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId, @Parameter(description = "门店名称") @RequestParam(required = false) String storeName) {
        trayService.batchCreateTrays(count, prefix, trayType, storeId, storeName);
        return Result.success("成功创建 " + count + " 个托盘");
    }

    @PostMapping("/bind-order")
    @Operation(summary = "扫描托盘码绑定订单（POS端，托盘 idle → bound）")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Tray> bindOrder(@RequestBody TrayBindOrderRequest request) {
        log.info("绑定托盘订单: trayCode={}, orderId={}, kitchenOrderId={}", request.getTrayCode(), request.getOrderId(), request.getKitchenOrderId());

        // KDS token store_id 隔离校验：KDS 凭据绑定的门店必须与目标托盘所属门店一致
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KdsTokenFilter.KdsPrincipal kdsPrincipal) {
            Long kdsStoreId;
            try {
                kdsStoreId = Long.parseLong(kdsPrincipal.getStoreId());
            } catch (NumberFormatException e) {
                return Result.error("KDS token store_id 格式无效");
            }
            // 查托盘，校验门店归属
            Tray tray = trayService.findByTrayCode(request.getTrayCode());
            if (tray == null) {
                return Result.error("托盘不存在: " + request.getTrayCode());
            }
            if (tray.getStoreId() == null) {
                return Result.error("托盘 " + request.getTrayCode() + " 未分配门店，KDS 无法校验归属。请在管理端为该托盘分配门店后再使用");
            }
            if (!kdsStoreId.equals(tray.getStoreId())) {
                log.warn("KDS 门店不匹配: kdsStoreId={}, trayStoreId={}, trayCode={}", kdsStoreId, tray.getStoreId(), request.getTrayCode());
                return Result.error("KDS 凭据门店(" + kdsStoreId + ")与托盘 " + request.getTrayCode() + " 所属门店(" + tray.getStoreId() + ")不一致，拒绝操作。请选择本店托盘");
            }
        }

        return trayService.bindOrder(request.getTrayCode(), request.getOrderId(), request.getKitchenOrderId());
    }

    @PostMapping("/scan-kitchen-in")
    @Operation(summary = "后厨一次扫码（托盘 bound → making，防抖≥3s）")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
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
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
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
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<KitchenOrder> scanServe(@Parameter(description = "托盘码") @RequestParam String trayCode, @Parameter(description = "操作员ID") @RequestParam(required = false) String operatorId, @Parameter(description = "操作员姓名") @RequestParam(required = false) String operatorName) {
        log.info("出餐口扫描托盘确认出餐: trayCode={}", trayCode);
        return trayService.scanServe(trayCode, operatorId, operatorName);
    }

    @PostMapping("/release")
    @Operation(summary = "释放托盘")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Tray> releaseTray(@Parameter(description = "托盘码") @RequestParam String trayCode) {
        Tray tray = trayService.releaseTray(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    @PostMapping("/start-cleaning")
    @Operation(summary = "开始清洁托盘")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Tray> startCleaning(@Parameter(description = "托盘码") @RequestParam String trayCode) {
        Tray tray = trayService.startCleaning(trayCode);
        if (tray == null) {
            return Result.error("托盘不存在");
        }
        return Result.success(tray);
    }

    @PostMapping("/finish-cleaning")
    @Operation(summary = "完成清洁托盘")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
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
