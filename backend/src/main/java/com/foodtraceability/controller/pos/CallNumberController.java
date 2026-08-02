package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.CallNumberCallRequest;
import com.foodtraceability.dto.CallNumberCreateFromOrderRequest;
import com.foodtraceability.dto.CallNumberHardwareDisplayRequest;
import com.foodtraceability.dto.CallNumberHardwareVoiceRequest;
import com.foodtraceability.dto.CallNumberPickupRequest;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.service.CallNumberHardwareService;
import com.foodtraceability.service.CallNumberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/call-number")
@Tag(name = "叫号管理", description = "取餐叫号系统API")
public class CallNumberController {

    private final CallNumberService callNumberService;
    private final CallNumberHardwareService hardwareService;

    public CallNumberController(CallNumberService callNumberService,
                                 CallNumberHardwareService hardwareService) {
        this.callNumberService = callNumberService;
        this.hardwareService = hardwareService;
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待叫号订单列表")
    public Result<List<CallRecord>> getPendingOrders() {
        List<CallRecord> records = callNumberService.getPendingOrders();
        return Result.success(records);
    }

    @GetMapping("/history")
    @Operation(summary = "获取今日叫号历史")
    public Result<List<CallRecord>> getTodayHistory() {
        List<CallRecord> history = callNumberService.getHistoryByDate(LocalDate.now());
        return Result.success(history);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取叫号统计")
    public Result<Map<String, Integer>> getStats() {
        Map<String, Integer> stats = callNumberService.getStats();
        return Result.success(stats);
    }

    @PostMapping("/call")
    @Operation(summary = "叫号")
    public Result<CallRecord> callNumber(@RequestBody CallNumberCallRequest request) {
        CallRecord record = callNumberService.callNumber(
                request.getOrderId(), request.getOrderNumber(),
                request.getTableNumber(), request.getOrderType(),
                request.getItemCount());
        return Result.success(record);
    }

    @PostMapping("/recall")
    @Operation(summary = "重叫")
    public Result<CallRecord> recallNumber(@RequestBody CallNumberCallRequest request) {
        CallRecord record = callNumberService.recallNumber(request.getOrderId());
        if (record == null) {
            return Result.error("订单不存在");
        }
        return Result.success(record);
    }

    @PostMapping("/pickup")
    @Operation(summary = "标记已取餐")
    public Result<CallRecord> markPicked(@RequestBody CallNumberPickupRequest request) {
        CallRecord record = callNumberService.markPicked(request.getOrderId());
        if (record == null) {
            return Result.error("订单不存在");
        }
        return Result.success(record);
    }

    @PostMapping("/sync-from-kitchen")
    @Operation(summary = "从后厨订单同步待取餐订单")
    public Result<Void> syncFromKitchen() {
        callNumberService.syncFromKitchen();
        return Result.success();
    }

    @PostMapping("/create-from-order")
    @Operation(summary = "从订单创建待取餐记录")
    public Result<CallRecord> createFromOrder(@RequestBody CallNumberCreateFromOrderRequest request) {
        CallRecord record = callNumberService.createFromOrder(
                request.getOrderId(), request.getOrderNumber(),
                request.getTableNumber(), request.getOrderType(),
                request.getItemCount());
        return Result.success(record);
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "清理历史记录")
    public Result<Integer> cleanupOldRecords(@RequestParam(defaultValue = "30") int days) {
        int deleted = callNumberService.cleanupOldRecords(days);
        return Result.success(deleted);
    }

    // ==================== 硬件集成端点 ====================

    @PostMapping("/hardware/voice")
    @Operation(summary = "硬件语音播报叫号信息")
    public Result<Map<String, Object>> hardwareVoiceBroadcast(@RequestBody CallNumberHardwareVoiceRequest request) {
        return hardwareService.broadcastVoice(request.getOrderNumber(), request.getOrderType());
    }

    @PostMapping("/hardware/display")
    @Operation(summary = "向LED显示屏发送叫号信息")
    public Result<Map<String, Object>> sendToDisplay(@RequestBody CallNumberHardwareDisplayRequest request) {
        return hardwareService.sendToDisplay(request.getOrderNumber(), request.getOrderType(), request.getTableNumber());
    }

    @GetMapping("/hardware/status")
    @Operation(summary = "获取硬件连接状态")
    public Result<Map<String, Object>> getHardwareStatus() {
        return hardwareService.getHardwareStatus();
    }
}
