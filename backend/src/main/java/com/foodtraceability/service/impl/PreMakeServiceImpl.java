package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.FoodTraceCodeGenerateDTO;
import com.foodtraceability.dto.PreMakeRequestDTO;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.entity.Food;
import com.foodtraceability.entity.FoodTraceCode;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.mapper.CallRecordMapper;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.mapper.FoodTraceCodeMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.service.FoodTraceCodeService;
import com.foodtraceability.service.PreMakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 提前制作管理服务实现类
 */
@Service
public class PreMakeServiceImpl implements PreMakeService {

    private static final Logger log = LoggerFactory.getLogger(PreMakeServiceImpl.class);

    private final FoodTraceCodeService foodTraceCodeService;
    private final FoodTraceCodeMapper foodTraceCodeMapper;
    private final FoodMapper foodMapper;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final CallRecordMapper callRecordMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public PreMakeServiceImpl(FoodTraceCodeService foodTraceCodeService,
                               FoodTraceCodeMapper foodTraceCodeMapper,
                               FoodMapper foodMapper,
                               KitchenOrderMapper kitchenOrderMapper,
                               CallRecordMapper callRecordMapper,
                               SimpMessagingTemplate messagingTemplate) {
        this.foodTraceCodeService = foodTraceCodeService;
        this.foodTraceCodeMapper = foodTraceCodeMapper;
        this.foodMapper = foodMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.callRecordMapper = callRecordMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> preMakeFood(PreMakeRequestDTO request) {
        log.info("提前制作: 菜品ID={}, 数量={}", request.getFoodId(), request.getQuantity());
        Food food = foodMapper.selectById(request.getFoodId());
        if (food == null) {
            return Map.of("error", "菜品不存在");
        }

        List<FoodTraceCode> generatedCodes = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            FoodTraceCodeGenerateDTO dto = new FoodTraceCodeGenerateDTO();
            dto.setDishId(food.getFoodCode());
            dto.setDishName(food.getFoodName());
            dto.setDishPrice(food.getFoodPrice());
            dto.setQuantity(1);
            dto.setChefId(request.getChefId());
            dto.setChefName(request.getChefName());
            dto.setStoreId(request.getStoreId());
            dto.setStoreName(request.getStoreName());
            FoodTraceCode code = foodTraceCodeService.generate(dto);
            code.setMakeStatus("completed");
            code.setStatus("ready_to_serve");
            code.setMakeCompleteTime(LocalDateTime.now());
            code.setProduceTime(LocalDateTime.now());
            foodTraceCodeMapper.updateById(code);
            generatedCodes.add(code);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("foodName", food.getFoodName());
        result.put("quantity", request.getQuantity());
        result.put("traceCodes", generatedCodes.stream().map(FoodTraceCode::getTraceCode).toList());
        result.put("message", String.format("已提前制作 %s %d份，食品追溯码已生成", food.getFoodName(), request.getQuantity()));
        log.info("提前制作完成: {} x {}", food.getFoodName(), request.getQuantity());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanServe(String traceCode, String orderNumber, String operatorId, String operatorName) {
        log.info("扫描食品码出餐: traceCode={}, orderNumber={}", traceCode, orderNumber);
        FoodTraceCode foodTraceCode = foodTraceCodeMapper.selectOne(
                new LambdaQueryWrapper<FoodTraceCode>().eq(FoodTraceCode::getTraceCode, traceCode));
        if (foodTraceCode == null) {
            return Map.of("error", "食品追溯码不存在: " + traceCode);
        }
        if ("served".equals(foodTraceCode.getStatus())) {
            return Map.of("error", "该餐品已出餐，请勿重复操作");
        }
        if (!"ready_to_serve".equals(foodTraceCode.getStatus()) && !"completed".equals(foodTraceCode.getStatus())) {
            return Map.of("error", "该餐品状态不允许出餐");
        }

        KitchenOrder matchedOrder = null;
        if (orderNumber != null && !orderNumber.isEmpty()) {
            matchedOrder = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<KitchenOrder>()
                            .eq(KitchenOrder::getOrderNumber, orderNumber)
                            .in(KitchenOrder::getStatus, Arrays.asList("completed", "making")));
        }
        if (matchedOrder == null) {
            matchedOrder = findMatchingPendingOrder(foodTraceCode.getDishId());
        }
        if (matchedOrder == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("foodTraceCode", foodTraceCode);
            result.put("message", "食品码有效，但未找到匹配的待出餐订单");
            result.put("action", "no_order");
            return result;
        }

        foodTraceCode.setOrderId(matchedOrder.getOrderId());
        foodTraceCode.setOrderNumber(matchedOrder.getOrderNumber());
        foodTraceCode.setKitchenOrderId(matchedOrder.getId());
        foodTraceCode.setStatus("served");
        foodTraceCode.setServeTime(LocalDateTime.now());
        foodTraceCodeMapper.updateById(foodTraceCode);

        matchedOrder.setStatus("served");
        matchedOrder.setServeTime(LocalDateTime.now());
        kitchenOrderMapper.updateById(matchedOrder);

        createCallRecord(matchedOrder);
        messagingTemplate.convertAndSend("/topic/order/status-change", matchedOrder);

        Map<String, Object> result = new HashMap<>();
        result.put("foodTraceCode", foodTraceCode);
        result.put("kitchenOrder", matchedOrder);
        result.put("message", "出餐成功: " + matchedOrder.getOrderNumber());
        result.put("action", "served");
        result.put("serveTime", foodTraceCode.getServeTime());
        log.info("食品码 {} 出餐成功，匹配订单: {}, 出餐时间: {}", traceCode, matchedOrder.getOrderNumber(), foodTraceCode.getServeTime());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReadyFoods() {
        List<FoodTraceCode> readyCodes = foodTraceCodeMapper.selectList(
                new LambdaQueryWrapper<FoodTraceCode>()
                        .eq(FoodTraceCode::getStatus, "ready_to_serve")
                        .isNull(FoodTraceCode::getOrderId)
                        .orderByDesc(FoodTraceCode::getMakeCompleteTime));

        Map<String, Map<String, Object>> foodSummary = new LinkedHashMap<>();
        for (FoodTraceCode code : readyCodes) {
            String key = code.getDishId();
            if (!foodSummary.containsKey(key)) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("dishId", code.getDishId());
                summary.put("dishName", code.getDishName());
                summary.put("quantity", 0);
                summary.put("traceCodes", new ArrayList<String>());
                foodSummary.put(key, summary);
            }
            Map<String, Object> summary = foodSummary.get(key);
            summary.put("quantity", (Integer) summary.get("quantity") + 1);
            ((List<String>) summary.get("traceCodes")).add(code.getTraceCode());
        }
        return new ArrayList<>(foodSummary.values());
    }

    /**
     * 查找匹配的待出餐订单
     * @param dishId 菜品ID
     * @return 匹配的后厨订单
     */
    private KitchenOrder findMatchingPendingOrder(String dishId) {
        List<KitchenOrder> pendingOrders = kitchenOrderMapper.selectList(
                new LambdaQueryWrapper<KitchenOrder>()
                        .in(KitchenOrder::getStatus, Arrays.asList("completed", "making"))
                        .orderByAsc(KitchenOrder::getCreateTime));
        for (KitchenOrder order : pendingOrders) {
            if (order.getDishItems() != null && order.getDishItems().contains(dishId)) {
                return order;
            }
        }
        if (!pendingOrders.isEmpty()) {
            return pendingOrders.get(0);
        }
        return null;
    }

    /**
     * 创建叫号记录
     * @param order 后厨订单
     */
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

    /**
     * 获取订单类型名称
     * @param orderType 订单类型编码
     * @return 订单类型名称
     */
    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return "堂食";
        return switch (orderType) {
            case 0 -> "堂食";
            case 1 -> "外卖";
            case 2 -> "自提";
            default -> "堂食";
        };
    }
}
