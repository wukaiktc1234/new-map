package com.foodtraceability.controller.kitchen;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.dto.BatchStatusUpdateDTO;
import com.foodtraceability.dto.FoodGroupedOrderDTO;
import com.foodtraceability.dto.KitchenOrderCreateDTO;
import com.foodtraceability.dto.KitchenOrderFullDTO;
import com.foodtraceability.dto.KitchenOrderWithWaitTimeDTO;
import com.foodtraceability.dto.KitchenStatsDTO;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.mapper.CallRecordMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.service.KitchenOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/kitchen/orders")
@Tag(name = "后厨订单管理", description = "后厨订单的创建、接单、制作、出餐等接口")
public class KitchenOrderController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KitchenOrderController.class);

    public KitchenOrderController(@Lazy KitchenOrderService kitchenOrderService, KitchenOrderMapper kitchenOrderMapper, OrderWebSocketController orderWebSocketController, ApplicationEventPublisher eventPublisher, JdbcTemplate jdbcTemplate, CallRecordMapper callRecordMapper, SimpMessagingTemplate messagingTemplate) {
        this.kitchenOrderService = kitchenOrderService;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderWebSocketController = orderWebSocketController;
        this.eventPublisher = eventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.callRecordMapper = callRecordMapper;
        this.messagingTemplate = messagingTemplate;
    }

    private final KitchenOrderService kitchenOrderService;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderWebSocketController orderWebSocketController;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final CallRecordMapper callRecordMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create")
    @Operation(summary = "创建后厨订单")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<KitchenOrder> create(@RequestBody KitchenOrderCreateDTO dto) {
        try {
            KitchenOrder order = kitchenOrderService.create(dto);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("创建后厨订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询后厨订单列表")
    public Result<IPage<KitchenOrder>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page, @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size, @Parameter(description = "状态") @RequestParam(required = false) String status, @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId, @Parameter(description = "厨师ID") @RequestParam(required = false) Long chefId) {
        Page<KitchenOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KitchenOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(KitchenOrder::getStatus, status);
        }
        if (storeId != null) {
            wrapper.eq(KitchenOrder::getStoreId, storeId);
        }
        if (chefId != null) {
            wrapper.eq(KitchenOrder::getChefId, chefId);
        }
        wrapper.orderByDesc(KitchenOrder::getPriority).orderByAsc(KitchenOrder::getCreateTime);
        IPage<KitchenOrder> result = kitchenOrderService.page(pageParam, wrapper);
        return Result.success(result);
    }

    @GetMapping("/{kitchenOrderId}")
    @Operation(summary = "根据ID查询后厨订单")
    public Result<KitchenOrder> getById(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId) {
        KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
        if (order == null) {
            return Result.error("后厨订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/{kitchenOrderId}/full")
    @Operation(summary = "根据ID查询后厨订单完整信息（含订单金额）")
    public Result<KitchenOrderFullDTO> getFullById(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId) {
        KitchenOrderFullDTO order = kitchenOrderMapper.selectFullById(kitchenOrderId);
        if (order == null) {
            return Result.error("后厨订单不存在");
        }
        expandComboDishItems(List.of(order));
        return Result.success(order);
    }

    @GetMapping("/active/full")
    @Operation(summary = "获取待处理订单完整信息列表")
    public Result<List<KitchenOrderFullDTO>> getActiveOrdersFull() {
        List<KitchenOrderFullDTO> orders = kitchenOrderMapper.selectActiveOrdersFull();
        expandComboDishItems(orders);
        return Result.success(orders);
    }

    @GetMapping("/recent/full")
    @Operation(summary = "获取最近订单完整信息列表")
    public Result<List<KitchenOrderFullDTO>> getRecentOrdersFull(@Parameter(description = "数量限制") @RequestParam(defaultValue = "50") Integer limit) {
        List<KitchenOrderFullDTO> orders = kitchenOrderMapper.selectRecentOrdersFull(limit);
        expandComboDishItems(orders);
        return Result.success(orders);
    }

    /**
     * P1-COMBO-ORDER-001: 拉单时将 product_type=2 套餐明细展开为 components（不拆 N 张卡片）。
     * dishItems JSON 中 combo 行已带 productType/comboId；此处按 combo_id 查 combo_ingredients 补 components。
     */
    private void expandComboDishItems(List<KitchenOrderFullDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        for (KitchenOrderFullDTO order : orders) {
            if (order == null || order.getDishItems() == null || order.getDishItems().isBlank()) {
                continue;
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(order.getDishItems());
                if (!arr.isArray()) {
                    continue;
                }
                boolean changed = false;
                com.fasterxml.jackson.databind.node.ArrayNode expanded = mapper.createArrayNode();
                for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                    int productType = node.has("productType") ? node.get("productType").asInt(1) : 1;
                    String type = node.has("type") ? node.get("type").asText("") : "";
                    if (productType != 2 && !"combo".equals(type)) {
                        expanded.add(node);
                        continue;
                    }
                    Long comboId = null;
                    if (node.has("comboId") && !node.get("comboId").isNull()) {
                        try {
                            comboId = node.get("comboId").asLong();
                        } catch (Exception ignore) {
                            comboId = null;
                        }
                    }
                    com.fasterxml.jackson.databind.node.ObjectNode comboNode = node.deepCopy();
                    if (comboId != null) {
                        List<Map<String, Object>> components = jdbcTemplate.queryForList(
                                "SELECT ci.food_id, ci.quantity, f.food_name " +
                                "FROM combo_ingredients ci " +
                                "LEFT JOIN foods f ON f.food_id = ci.food_id " +
                                "WHERE ci.combo_id = ? AND ci.deleted = 0 " +
                                "ORDER BY ci.sort_order, ci.ingredient_id",
                                comboId);
                        com.fasterxml.jackson.databind.node.ArrayNode compArr = mapper.createArrayNode();
                        for (Map<String, Object> c : components) {
                            com.fasterxml.jackson.databind.node.ObjectNode comp = mapper.createObjectNode();
                            Object foodId = c.get("food_id");
                            comp.put("foodId", foodId == null ? null : String.valueOf(foodId));
                            Object qty = c.get("quantity");
                            comp.put("quantity", qty == null ? 0 : Integer.parseInt(String.valueOf(qty)));
                            Object name = c.get("food_name");
                            comp.put("name", name == null ? ("菜品" + foodId) : String.valueOf(name));
                            compArr.add(comp);
                        }
                        comboNode.set("components", compArr);
                        comboNode.put("productType", 2);
                        comboNode.put("comboId", comboId);
                    }
                    expanded.add(comboNode);
                    changed = true;
                }
                if (changed) {
                    order.setDishItems(mapper.writeValueAsString(expanded));
                }
            } catch (Exception e) {
                log.warn("KDS 套餐明细展开失败 orderId={}: {}", order.getOrderId(), e.getMessage());
            }
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID查询")
    public Result<KitchenOrder> getByOrderId(@Parameter(description = "订单ID") @PathVariable String orderId) {
        KitchenOrder order = kitchenOrderService.getByOrderId(orderId);
        if (order == null) {
            return Result.error("后厨订单不存在");
        }
        return Result.success(order);
    }

    @PostMapping("/{kitchenOrderId}/receive")
    @Operation(summary = "接单")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> receiveOrder(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId, @Parameter(description = "厨师ID") @RequestParam Long chefId, @Parameter(description = "厨师姓名") @RequestParam String chefName) {
        try {
            KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            String previousStatus = order.getStatus();
            boolean result = kitchenOrderService.receiveOrder(kitchenOrderId, chefId, chefName);
            if (result) {
                KitchenOrder updatedOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
                orderWebSocketController.pushOrderStatusChange(updatedOrder, previousStatus);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    @PostMapping("/{kitchenOrderId}/start-make")
    @Operation(summary = "开始制作")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> startMake(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId) {
        try {
            KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            String previousStatus = order.getStatus();
            boolean result = kitchenOrderService.startMake(kitchenOrderId);
            if (result) {
                KitchenOrder updatedOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
                orderWebSocketController.pushOrderStatusChange(updatedOrder, previousStatus);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("开始制作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{kitchenOrderId}/complete-make")
    @Operation(summary = "完成制作")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> completeMake(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId) {
        try {
            KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            String previousStatus = order.getStatus();
            boolean result = kitchenOrderService.completeMake(kitchenOrderId);
            if (result) {
                KitchenOrder updatedOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
                log.info("制作完成，食品追溯码已在事务内生成: {}", kitchenOrderId);
                orderWebSocketController.pushOrderStatusChange(updatedOrder, previousStatus);
                orderWebSocketController.pushReadyForPickup(updatedOrder);
                try { createCallRecord(updatedOrder); } catch (Exception crEx) { log.warn("创建叫号记录异常（不回滚）: {}", crEx.getMessage()); }
                try {
                    OrderCompletedEvent event = new OrderCompletedEvent();
                    event.setEventId("EVT" + System.currentTimeMillis());
                    event.setOrderId(updatedOrder.getOrderId());
                    event.setOrderNumber(updatedOrder.getOrderNumber());
                    event.setOrderType(updatedOrder.getOrderType());
                    event.setStoreId(updatedOrder.getStoreId());
                    event.setStoreName(updatedOrder.getStoreName());
                    event.setTableNumber(updatedOrder.getTableNumber());
                    event.setCompleteTime(LocalDateTime.now());
                    event.setActualAmount(updatedOrder.getTotalAmount());
                    event.setOrderAmount(updatedOrder.getTotalAmount());
                    eventPublisher.publishEvent(event);
                    log.info("完成制作事件已发布: {}", updatedOrder.getKitchenOrderId());
                } catch (Exception evtEx) { log.warn("完成制件事件发布异常: {}", evtEx.getMessage()); }
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("完成制作失败: " + e.getMessage());
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
            record.setCreateTime(LocalDateTime.now());
            callRecordMapper.insert(record);
            messagingTemplate.convertAndSend("/topic/call-number/new", record);
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

    @PostMapping("/{kitchenOrderId}/serve")
    @Operation(summary = "出餐")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> serve(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId) {
        try {
            KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            String previousStatus = order.getStatus();
            boolean result = kitchenOrderService.serve(kitchenOrderId);
            if (result) {
                KitchenOrder updatedOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
                orderWebSocketController.pushOrderStatusChange(updatedOrder, previousStatus);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("出餐失败: " + e.getMessage());
        }
    }

    @PostMapping("/{kitchenOrderId}/cancel")
    @Operation(summary = "取消订单")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> cancel(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId, @Parameter(description = "取消原因") @RequestParam String reason) {
        try {
            KitchenOrder order = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            String previousStatus = order.getStatus();
            boolean result = kitchenOrderService.cancel(kitchenOrderId, reason);
            if (result) {
                KitchenOrder updatedOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
                orderWebSocketController.pushOrderStatusChange(updatedOrder, previousStatus);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("取消失败: " + e.getMessage());
        }
    }

    @PostMapping("/scan-consume")
    @Operation(summary = "扫码消耗原料")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> scanConsumeMaterial(@RequestBody MaterialScanConsumeDTO dto) {
        try {
            boolean result = kitchenOrderService.scanConsumeMaterial(dto);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("扫码消耗失败: " + e.getMessage());
        }
    }

    @GetMapping("/store-active/{storeId}")
    @Operation(summary = "获取门店待处理订单")
    public Result<List<KitchenOrder>> getStoreActiveOrders(@Parameter(description = "门店ID") @PathVariable Long storeId) {
        List<KitchenOrder> orders = kitchenOrderService.getStoreActiveOrders(storeId);
        return Result.success(orders);
    }

    @GetMapping("/chef-active/{chefId}")
    @Operation(summary = "获取厨师进行中订单")
    public Result<List<KitchenOrder>> getChefActiveOrders(@Parameter(description = "厨师ID") @PathVariable Long chefId) {
        List<KitchenOrder> orders = kitchenOrderService.getChefActiveOrders(chefId);
        return Result.success(orders);
    }

    @PutMapping("/{kitchenOrderId}/priority")
    @Operation(summary = "更新优先级")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    public Result<Boolean> updatePriority(@Parameter(description = "后厨订单ID") @PathVariable String kitchenOrderId, @Parameter(description = "优先级") @RequestParam Integer priority) {
        try {
            boolean result = kitchenOrderService.updatePriority(kitchenOrderId, priority);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("更新优先级失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics/{storeId}")
    @Operation(summary = "统计各状态订单数量")
    public Result<Map<String, Integer>> statistics(@Parameter(description = "门店ID") @PathVariable Long storeId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("pending", kitchenOrderService.countByStatusAndStore("pending", storeId));
        stats.put("received", kitchenOrderService.countByStatusAndStore("received", storeId));
        stats.put("making", kitchenOrderService.countByStatusAndStore("making", storeId));
        stats.put("completed", kitchenOrderService.countByStatusAndStore("completed", storeId));
        stats.put("served", kitchenOrderService.countByStatusAndStore("served", storeId));
        return Result.success(stats);
    }

    @DeleteMapping("/kitchen/clear-test-data")
    @Operation(summary = "清除测试数据（仅开发环境）")
    @PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")
    @org.springframework.context.annotation.Profile("dev")
    public Result<Map<String, Object>> clearTestData() {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<KitchenOrder> kitchenWrapper = new LambdaQueryWrapper<>();
            kitchenOrderService.remove(kitchenWrapper);
            result.put("kitchen_order", "逻辑删除成功");
        } catch (Exception e) {
            result.put("kitchen_order", "逻辑删除失败: " + e.getMessage());
        }
        result.put("orders", "请通过OrderService执行逻辑删除");
        result.put("order_items", "请通过OrderService执行逻辑删除");
        return Result.success(result);
    }

    // ========== 新增API端点 ==========

    @GetMapping("/orders/sorted")
    @Operation(summary = "智能排序查询订单（支持排序和筛选）")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<List<KitchenOrderWithWaitTimeDTO>> getSortedOrders(
            @Parameter(description = "状态过滤: pending/making/completed/served/all") @RequestParam(defaultValue = "all") String status,
            @Parameter(description = "排序字段: waitTime/orderTime/quantity") @RequestParam(defaultValue = "waitTime") String sort,
            @Parameter(description = "排序方向: asc/desc") @RequestParam(defaultValue = "asc") String order,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        try {
            List<KitchenOrderWithWaitTimeDTO> orders = kitchenOrderService.getSortedOrders(status, sort, order, page, size);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/orders/batch-status")
    @Operation(summary = "批量更新订单状态")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<Map<String, Object>> batchUpdateStatus(@RequestBody BatchStatusUpdateDTO dto) {
        try {
            int successCount = kitchenOrderService.batchUpdateStatus(dto);
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", dto.getOrderIds().size());
            result.put("message", String.format("成功更新%d个订单状态", successCount));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("批量更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取厨房统计面板数据")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<KitchenStatsDTO> getKitchenStats(
            @Parameter(description = "日期: today或yyyy-MM-dd格式") @RequestParam(defaultValue = "today") String date) {
        try {
            KitchenStatsDTO stats = kitchenOrderService.getKitchenStats(date);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/orders/overdue")
    @Operation(summary = "获取超时预警订单列表")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<List<KitchenOrder>> getOverdueOrders(
            @Parameter(description = "超时阈值（分钟）") @RequestParam(defaultValue = "15") Integer threshold) {
        try {
            List<KitchenOrder> orders = kitchenOrderService.getOverdueOrders(threshold);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("查询超时订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/orders/grouped-by-food")
    @Operation(summary = "按菜品聚合订单视图")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CHEF', 'ROLE_KITCHEN')")
    public Result<List<FoodGroupedOrderDTO>> getOrdersGroupedByFood(
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status) {
        try {
            List<FoodGroupedOrderDTO> groupedOrders = kitchenOrderService.getOrdersGroupedByFood(status);
            return Result.success(groupedOrders);
        } catch (Exception e) {
            return Result.error("查询聚合订单失败: " + e.getMessage());
        }
    }
}
