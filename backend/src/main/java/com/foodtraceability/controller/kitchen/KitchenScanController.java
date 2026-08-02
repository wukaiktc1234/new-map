package com.foodtraceability.controller.kitchen;

import com.foodtraceability.common.Result;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.dto.ScanMatchResultDTO;
import com.foodtraceability.dto.KitchenStatsDTO;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.entity.FoodTraceCode;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.MaterialUsageRecord;
import com.foodtraceability.entity.OrderMaterialRequirement;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.mapper.CallRecordMapper;
import com.foodtraceability.mapper.FoodTraceCodeMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.mapper.MaterialUsageRecordMapper;
import com.foodtraceability.mapper.OrderMaterialRequirementMapper;
import com.foodtraceability.service.KitchenOrderService;
import com.foodtraceability.service.KitchenScanService;
import com.foodtraceability.service.OrderMaterialRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/kitchen/scan")
@Tag(name = "后厨扫码终端", description = "后厨扫码制作相关接口")
public class KitchenScanController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KitchenScanController.class);
    private final KitchenScanService kitchenScanService;
    private final OrderMaterialRequirementService requirementService;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final MaterialTraceCodeMapper traceCodeMapper;
    private final MaterialUsageRecordMapper usageRecordMapper;
    private final OrderMaterialRequirementMapper requirementMapper;
    private final KitchenOrderService kitchenOrderService;
    private final OrderWebSocketController orderWebSocketController;
    private final ApplicationEventPublisher eventPublisher;
    private final FoodTraceCodeMapper foodTraceCodeMapper;
    private final CallRecordMapper callRecordMapper;

    @PostMapping("")
    @Operation(summary = "扫描追溯码")
    public Result<ScanMatchResultDTO> scanTraceCode(@RequestParam String traceCode, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        log.info("后厨扫描追溯码: {}", traceCode);
        try {
            ScanMatchResultDTO result = kitchenScanService.scanAndMatch(traceCode, operatorId, operatorName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("扫描追溯码失败", e);
            return Result.error("扫描失败: " + e.getMessage());
        }
    }

    @PostMapping("/scan-with-quantity")
    @Operation(summary = "扫描追溯码并指定使用数量")
    public Result<ScanMatchResultDTO> scanTraceCodeWithQuantity(@RequestParam String traceCode, @RequestParam java.math.BigDecimal quantity, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        log.info("后厨扫描追溯码: {}, 数量: {}", traceCode, quantity);
        try {
            ScanMatchResultDTO result = kitchenScanService.scanAndMatchWithQuantity(traceCode, quantity, operatorId, operatorName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("扫描追溯码失败", e);
            return Result.error("扫描失败: " + e.getMessage());
        }
    }

    @PostMapping("/scan-food-trace-code")
    @Operation(summary = "扫描食品追溯码出餐确认")
    public Result<Map<String, Object>> scanFoodTraceCode(@RequestParam String traceCode, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        log.info("扫描食品追溯码出餐确认: {}", traceCode);
        try {
            FoodTraceCode foodTraceCode = foodTraceCodeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FoodTraceCode>().eq(FoodTraceCode::getTraceCode, traceCode));
            if (foodTraceCode == null) {
                return Result.error("食品追溯码不存在: " + traceCode);
            }
            if ("served".equals(foodTraceCode.getStatus())) {
                return Result.error("该餐品已出餐，请勿重复操作");
            }
            Long kitchenOrderIdLong = foodTraceCode.getKitchenOrderId();
            KitchenOrder order = kitchenOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getId, kitchenOrderIdLong));
            if (order == null) {
                return Result.error("关联订单不存在");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("foodTraceCode", foodTraceCode);
            result.put("kitchenOrder", order);
            if (!"completed".equals(order.getStatus())) {
                result.put("message", "订单尚未完成制作，请先完成制作");
                result.put("action", "not_ready");
                return Result.error("订单尚未完成制作，无法出餐");
            }
            String previousStatus = order.getStatus();
            foodTraceCode.setStatus("served");
            foodTraceCode.setServeTime(java.time.LocalDateTime.now());
            foodTraceCodeMapper.updateById(foodTraceCode);
            order.setStatus("served");
            order.setServeTime(java.time.LocalDateTime.now());
            kitchenOrderMapper.updateById(order);
            createCallRecord(order);
            orderWebSocketController.pushOrderStatusChange(order, previousStatus);
            result.put("message", "出餐成功: " + order.getOrderNumber());
            result.put("action", "served");
            result.put("serveTime", foodTraceCode.getServeTime());
            log.info("食品追溯码 {} 出餐确认成功，订单: {}, 出餐时间: {}", traceCode, order.getOrderNumber(), foodTraceCode.getServeTime());
            return Result.success(result);
        } catch (Exception e) {
            log.error("扫描食品追溯码失败", e);
            return Result.error("扫描失败: " + e.getMessage());
        }
    }

    private void createCallRecord(KitchenOrder order) {
        try {
            CallRecord existing = callRecordMapper.findByOrderId(order.getOrderId());
            if (existing != null) {
                return;
            }
            CallRecord record = new CallRecord();
            record.setOrderId(order.getOrderId());
            record.setOrderNumber(order.getOrderNumber());
            record.setTableNumber(order.getTableNumber());
            record.setOrderType(getOrderTypeName(order.getOrderType()));
            record.setItemCount(order.getTotalDishes() != null ? order.getTotalDishes() : 0);
            record.setStatus("pending");
            record.setCallCount(0);
            record.setCreateTime(java.time.LocalDateTime.now());
            callRecordMapper.insert(record);
            log.info("已创建待取餐记录: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.warn("创建待取餐记录失败: {}", e.getMessage());
        }
    }

    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "堂食";
        return switch (orderType) {
            case 0 -> "堂食";
            case 1 -> "外卖";
            case 2 -> "自提";
            default -> "堂食";
        };
    }

    @GetMapping("/pending-orders")
    @Operation(summary = "获取待制作订单列表")
    public Result<List<KitchenOrder>> getPendingOrders() {
        List<KitchenOrder> orders = kitchenOrderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KitchenOrder>().in(KitchenOrder::getStatus, java.util.Arrays.asList("pending", "received")).orderByAsc(KitchenOrder::getCreateTime));
        return Result.success(orders);
    }

    @GetMapping("/making-orders")
    @Operation(summary = "获取制作中订单列表")
    public Result<List<KitchenOrder>> getMakingOrders() {
        List<KitchenOrder> orders = kitchenOrderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getStatus, "making").orderByAsc(KitchenOrder::getMakeStartTime));
        return Result.success(orders);
    }

    @GetMapping("/order-requirements/{kitchenOrderId}")
    @Operation(summary = "获取订单原料需求")
    public Result<List<OrderMaterialRequirement>> getOrderRequirements(@PathVariable String kitchenOrderId) {
        List<OrderMaterialRequirement> requirements = requirementMapper.findByKitchenOrderId(kitchenOrderId);
        return Result.success(requirements);
    }

    @GetMapping("/pending-requirements/{materialName}")
    @Operation(summary = "获取某原料的待处理需求")
    public Result<List<OrderMaterialRequirement>> getPendingRequirementsByMaterial(@PathVariable String materialName) {
        List<OrderMaterialRequirement> requirements = requirementMapper.findPendingByMaterialName(materialName);
        return Result.success(requirements);
    }

    @GetMapping("/trace-code-info/{traceCode}")
    @Operation(summary = "查询追溯码信息")
    public Result<Map<String, Object>> getTraceCodeInfo(@PathVariable String traceCode) {
        MaterialTraceCode traceCodeEntity = traceCodeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialTraceCode>().eq(MaterialTraceCode::getTraceCode, traceCode));
        if (traceCodeEntity == null) {
            return Result.error("追溯码不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("traceCode", traceCodeEntity);
        List<OrderMaterialRequirement> pendingRequirements = requirementMapper.findPendingByMaterialName(traceCodeEntity.getMaterialName());
        result.put("pendingOrders", pendingRequirements);
        return Result.success(result);
    }

    @GetMapping("/usage-records/{kitchenOrderId}")
    @Operation(summary = "获取订单的原料使用记录")
    public Result<List<MaterialUsageRecord>> getUsageRecords(@PathVariable String kitchenOrderId) {
        List<MaterialUsageRecord> records = usageRecordMapper.findByKitchenOrderId(kitchenOrderId);
        return Result.success(records);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取厨房统计面板数据")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<KitchenStatsDTO> getKitchenStats(
            @io.swagger.v3.oas.annotations.Parameter(description = "日期: today或yyyy-MM-dd格式") @RequestParam(defaultValue = "today") String date) {
        try {
            KitchenStatsDTO stats = kitchenOrderService.getKitchenStats(date);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取厨房统计数据失败", e);
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/orders/overdue")
    @Operation(summary = "获取超时预警订单列表")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<List<KitchenOrder>> getOverdueOrders(
            @io.swagger.v3.oas.annotations.Parameter(description = "超时阈值（分钟）") @RequestParam(defaultValue = "15") Integer thresholdMinutes) {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getOverdueOrders(thresholdMinutes);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("查询超时订单失败", e);
            return Result.error("查询超时订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/complete-order/{kitchenOrderId}")
    @Operation(summary = "完成订单制作")
    public Result<KitchenOrder> completeOrder(@PathVariable String kitchenOrderId) {
        KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!"making".equals(order.getStatus())) {
            return Result.error("只有制作中的订单可以完成");
        }
        String previousStatus = order.getStatus();
        boolean result = kitchenOrderService.completeMake(kitchenOrderId);
        if (!result) {
            return Result.error("完成制作失败");
        }
        order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
        orderWebSocketController.pushOrderStatusChange(order, previousStatus);
        orderWebSocketController.pushReadyForPickup(order);
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setEventId("EVT" + System.currentTimeMillis());
        event.setOrderId(order.getOrderId());
        event.setOrderNumber(order.getOrderNumber());
        event.setOrderType(order.getOrderType());
        event.setStoreId(order.getStoreId());
        event.setStoreName(order.getStoreName());
        event.setTableNumber(order.getTableNumber());
        event.setCompleteTime(java.time.LocalDateTime.now());
        event.setActualAmount(order.getTotalAmount());
        event.setOrderAmount(order.getTotalAmount());
        eventPublisher.publishEvent(event);
        log.info("订单 {} 制作完成，已生成餐品追溯码并通知收银端", order.getOrderNumber());
        return Result.success(order);
    }

    public KitchenScanController(final KitchenScanService kitchenScanService, final OrderMaterialRequirementService requirementService, final KitchenOrderMapper kitchenOrderMapper, final MaterialTraceCodeMapper traceCodeMapper, final MaterialUsageRecordMapper usageRecordMapper, final OrderMaterialRequirementMapper requirementMapper, final @Lazy KitchenOrderService kitchenOrderService, final OrderWebSocketController orderWebSocketController, final ApplicationEventPublisher eventPublisher, final FoodTraceCodeMapper foodTraceCodeMapper, final CallRecordMapper callRecordMapper) {
        this.kitchenScanService = kitchenScanService;
        this.requirementService = requirementService;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.traceCodeMapper = traceCodeMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.requirementMapper = requirementMapper;
        this.kitchenOrderService = kitchenOrderService;
        this.orderWebSocketController = orderWebSocketController;
        this.eventPublisher = eventPublisher;
        this.foodTraceCodeMapper = foodTraceCodeMapper;
        this.callRecordMapper = callRecordMapper;
    }
}
