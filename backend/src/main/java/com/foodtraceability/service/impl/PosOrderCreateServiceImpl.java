package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCreatedEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.service.PosOrderCreateService;
import com.foodtraceability.service.PosOrderNumberGenerator;
import com.foodtraceability.service.SensitiveDataService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POS订单创建服务实现类
 * 负责订单创建、桌台订单创建等业务逻辑
 */
@Service
public class PosOrderCreateServiceImpl implements PosOrderCreateService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderCreateServiceImpl.class);

    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final FoodMapper foodMapper;
    /** foods 表 Mapper（菜品展示来源），用于同步扣减 foods 表库存，保证两表一致 */
    private final FoodNewMapper foodNewMapper;
    private final DishComboMapper dishComboMapper;
    private final OrderWebSocketController orderWebSocketController;
    private final ApplicationEventPublisher eventPublisher;
    private final SensitiveDataService sensitiveDataService;
    private final PosOrderNumberGenerator numberGenerator;
    /** 门店 Mapper（增强版），用于查询门店名称（POS端订单关联门店）
     *  使用 stores_new 表，与 StoreNewController 一致 */
    private final StoreNewMapper storeNewMapper;

    public PosOrderCreateServiceImpl(
            KitchenOrderMapper kitchenOrderMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            FoodMapper foodMapper,
            FoodNewMapper foodNewMapper,
            DishComboMapper dishComboMapper,
            OrderWebSocketController orderWebSocketController,
            ApplicationEventPublisher eventPublisher,
            SensitiveDataService sensitiveDataService,
            PosOrderNumberGenerator numberGenerator,
            StoreNewMapper storeNewMapper) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.foodMapper = foodMapper;
        this.foodNewMapper = foodNewMapper;
        this.dishComboMapper = dishComboMapper;
        this.orderWebSocketController = orderWebSocketController;
        this.eventPublisher = eventPublisher;
        this.sensitiveDataService = sensitiveDataService;
        this.numberGenerator = numberGenerator;
        this.storeNewMapper = storeNewMapper;
    }

    /**
     * 创建订单
     * 包含幂等性检查、库存校验、订单号生成、金额计算、订单项创建、后厨订单、事件发布等完整流程
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> createOrder(OrderRequestDTO request) {
        log.info("开始创建订单, orderType={}, items={}", request.getOrderType(), request.getItems().size());
        try {
            // 防御性验证
            Result<Void> validationResult = validateOrderItems(request.getItems());
            if (validationResult.getCode() != 0) {
                return Result.error(validationResult.getMessage());
            }

            // 幂等性检查：防止重复提交订单
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
                Long existCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getIdempotencyKey, request.getIdempotencyKey()));
                if (existCount != null && existCount > 0) {
                    return Result.error("请勿重复提交订单");
                }
            }

            // 库存校验（优化：批量查询避免N+1问题）
            List<String> foodIds = request.getItems().stream()
                .map(OrderItemDTO::getId)
                .distinct()
                .collect(Collectors.toList());
            Map<String, Food> foodMap = foodMapper.selectBatchIds(foodIds)
                .stream()
                .collect(Collectors.toMap(Food::getFoodCode, food -> food, (existing, replacement) -> existing));

            for (OrderItemDTO itemDTO : request.getItems()) {
                Food food = foodMap.get(itemDTO.getId());
                if (food != null && food.getStock() != null) {
                    if (food.getStock() < itemDTO.getQuantity()) {
                        return Result.error("菜品【" + food.getFoodName() + "】库存不足，当前库存: " + food.getStock());
                    }
                }
            }

            // 生成订单号和ID
            String orderNumber = numberGenerator.generateOrderNumberByDbSequence(request.getTableNumber());
            String kitchenOrderId = "KO" + numberGenerator.incrementAndGetCounter();
            String orderId = "O" + numberGenerator.incrementAndGetCounter();
            Integer orderType = convertOrderType(request.getOrderType());
            String pickupNumber = numberGenerator.generatePickupNumberByType(orderType);
            String pickupCode = (orderType == 1) ? numberGenerator.generatePickupCode() : null;
            log.info("订单号生成: {}", orderNumber);

            // 计算订单金额
            BigDecimal serverTotalAmount = calculateTotalAmount(request.getItems());
            log.info("订单初始金额计算: orderId={}, serverTotalAmount={}, itemsCount={}", orderId, serverTotalAmount, request.getItems().size());

            // 验证计算出的金额必须大于0
            if (serverTotalAmount == null || serverTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("订单金额计算异常: orderId={}, serverTotalAmount={}, 请检查商品价格配置", orderId, serverTotalAmount);
                return Result.error("订单金额计算异常，请检查商品价格配置");
            }

            String userIdentifier = resolveUserIdentifier(request);
            Integer paymentMethodInt = request.getPaymentMethod() != null ? convertPaymentMethod(request.getPaymentMethod()) : null;

            // 创建订单
            Order order = new Order();
            order.setOrderId(orderId);
            order.setOrderNumber(orderNumber);
            order.setIdempotencyKey(request.getIdempotencyKey());
            order.setOrderType(orderType);
            order.setOrderStatus(0);
            order.setOrderAmount(serverTotalAmount);
            order.setActualAmount(serverTotalAmount);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setPaymentMethod(paymentMethodInt);
            order.setUserId(userIdentifier);
            if (request.getContactName() != null) { order.setContactName(request.getContactName()); }
            if (request.getContactPhone() != null) { order.setContactPhone(sensitiveDataService.encryptPhone(request.getContactPhone())); }
            if (request.getDeliveryAddress() != null) { order.setDeliveryAddress(sensitiveDataService.encryptAddress(request.getDeliveryAddress())); }
            if (request.getRemark() != null) { order.setRemarks(request.getRemark()); }
            order.setOrderSource(4);
            // 设置门店信息：POS端订单关联门店
            // POS端产生的交易行为是管理端获取订单数据的主要来源，订单必须关联门店
            // storeId 为 stores_new.store_id（BIGINT）的字符串形式；未传入时默认测试门店A（store_code=STORE_A）
            String storeId = request.getStoreId();
            if (storeId == null || storeId.isEmpty()) {
                // 默认门店：查询 stores_new 中 store_code='STORE_A' 的记录（真实验收门店）
                StoreNew defaultStore = storeNewMapper.selectByStoreCode("STORE_A");
                storeId = defaultStore != null ? String.valueOf(defaultStore.getStoreId()) : "1";
            }
            order.setStoreId(storeId);
            order.setStoreName(resolveStoreName(storeId));
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            order.setDeleted(0);
            orderMapper.insert(order);

            // 创建订单项并扣减库存
            createOrderItemsAndDeductStock(orderId, request.getItems(), foodMap);

            // 重新计算最终金额
            BigDecimal finalAmount = recalculateOrderAmount(orderId, serverTotalAmount);

            // 最终验证
            if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("订单最终金额异常: orderId={}, finalAmount={}, serverTotalAmount={}", orderId, finalAmount, serverTotalAmount);
                throw new BusinessException(500, "订单金额计算异常，最终金额为0");
            }

            log.info("订单最终金额确定: orderId={}, finalAmount={}", orderId, finalAmount);
            order.setOrderAmount(finalAmount);
            order.setActualAmount(finalAmount);
            orderMapper.updateById(order);

            // 创建后厨订单
            KitchenOrder kitchenOrder = createKitchenOrder(kitchenOrderId, orderId, orderNumber,
                pickupNumber, pickupCode, orderType, request);

            // WebSocket推送
            try { orderWebSocketController.pushNewOrder(kitchenOrder); } catch (Exception wsEx) { log.warn("WebSocket推送异常: {}", wsEx.getMessage()); }

            // 事务提交后发布事件
            publishOrderCreatedEvent(orderId, kitchenOrder, orderNumber, finalAmount);

            log.info("订单创建成功: orderId={}, orderNumber={}, finalAmount={}", orderId, orderNumber, finalAmount);

            // 构建返回结果
            return buildCreateOrderResult(orderId, orderNumber, pickupNumber, pickupCode,
                orderType, request, finalAmount);
        } catch (Exception e) {
            log.error("订单创建失败: {}", e.getMessage(), e);
            return Result.error("订单创建失败: " + e.getMessage());
        }
    }

    /**
     * 创建桌台订单（扫码点餐）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> createTableOrder(TableOrderDTO request) {
        log.info("开始创建桌台订单: tableNumber={}", request.getTableNumber());
        log.info("桌台ID: {}, 桌号: {}", request.getTableId(), request.getTableNumber());
        try {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return Result.error("订单项不能为空");
            }

            // 扣减库存
            for (var item : request.getItems()) {
                int deductResult = foodMapper.deductStock(item.getId(), item.getQuantity());
                if (deductResult == 0) {
                    log.warn("库存扣减失败: 菜品ID={}, 数量={}", item.getId(), item.getQuantity());
                    return Result.error("菜品【" + item.getName() + "】库存不足");
                }
                // 同步扣减 foods 表（菜品展示来源），保证两表库存一致
                int deductNewResult = foodNewMapper.deductStock(item.getId(), item.getQuantity());
                if (deductNewResult == 0) {
                    log.warn("foods表库存扣减失败: 菜品ID={}, 数量={}", item.getId(), item.getQuantity());
                    // food表已扣减成功，foods表扣减失败属于数据不一致，记录日志但不再回滚
                }
            }

            // 计算订单金额
            BigDecimal totalAmount = calculateTableOrderAmount(request.getItems());
            log.info("桌台订单初始金额计算: tableNumber={}, totalAmount={}, itemsCount={}", request.getTableNumber(), totalAmount, request.getItems().size());

            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("桌台订单金额计算异常: tableNumber={}, totalAmount={}", request.getTableNumber(), totalAmount);
                return Result.error("订单金额计算异常，请检查商品价格配置");
            }

            // 生成订单号
            String orderNumber = numberGenerator.generateOrderNumberByDbSequence(
                Integer.parseInt(request.getTableNumber().replaceAll("[^0-9]", "")));
            log.info("生成订单号: {}", orderNumber);

            String orderId = "O" + numberGenerator.incrementAndGetCounter();

            // 创建订单
            Order order = new Order();
            order.setOrderId(orderId);
            order.setOrderNumber(orderNumber);
            order.setOrderAmount(totalAmount);
            order.setActualAmount(totalAmount);
            order.setOrderStatus(0);
            order.setPaymentMethod(0);
            order.setOrderType(0);
            order.setOrderSource(4);
            orderMapper.insert(order);
            log.info("主订单创建成功: {}", orderId);

            // 创建订单项
            for (var item : request.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderId);
                orderItem.setFoodId(item.getId());
                orderItem.setFoodName(item.getName());
                orderItem.setUnitPrice(item.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setSubtotalAmount(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                orderItemMapper.insert(orderItem);
            }
            log.info("订单项创建成功");

            // 创建后厨订单
            Long kitchenOrderId = numberGenerator.incrementAndGetCounter();
            KitchenOrder kitchenOrder = new KitchenOrder();
            kitchenOrder.setId(kitchenOrderId);
            kitchenOrder.setKitchenOrderId("KO" + kitchenOrderId);
            kitchenOrder.setOrderId(orderId);
            kitchenOrder.setOrderNumber(orderNumber);
            kitchenOrder.setOrderType(0);
            kitchenOrder.setTableNumber(request.getTableNumber());
            kitchenOrder.setStatus("pending");
            kitchenOrder.setReceiveTime(LocalDateTime.now());
            kitchenOrder.setTotalDishes(calculateTableOrderDishes(request.getItems()));
            kitchenOrder.setDishItems(buildTableOrderDishItemsJson(request.getItems()));
            kitchenOrder.setCreateTime(LocalDateTime.now());
            kitchenOrder.setDeleted(0);
            kitchenOrder.setPriority(0);
            kitchenOrderMapper.insert(kitchenOrder);
            log.info("后厨订单创建成功: {}", kitchenOrderId);

            // WebSocket推送
            orderWebSocketController.pushNewOrder(kitchenOrder);

            // 构建返回结果
            OrderResultDTO result = new OrderResultDTO();
            result.setOrderId(orderId);
            result.setOrderNumber(orderNumber);
            result.setStatus("pending");
            result.setMessage("桌台订单创建成功");
            result.setTotalAmount(totalAmount);
            result.setOrderSource(4);

            log.info("桌台订单创建成功: orderNumber={}", orderNumber);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建桌台订单失败", e);
            return Result.error("创建桌台订单失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 验证订单项
     */
    private Result<Void> validateOrderItems(List<OrderItemDTO> items) {
        if (items != null) {
            for (OrderItemDTO item : items) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return Result.error("商品数量必须大于0");
                }
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    return Result.error("商品价格必须大于0");
                }
                if (item.getId() == null || item.getId().trim().isEmpty()) {
                    return Result.error("菜品ID不能为空");
                }
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    return Result.error("菜品名称不能为空");
                }
            }
        }
        return Result.success(null);
    }

    /**
     * 创建订单项并扣减库存
     */
    private void createOrderItemsAndDeductStock(String orderId, List<OrderItemDTO> items, Map<String, Food> foodMap) {
        for (OrderItemDTO itemDTO : items) {
            Food dbFood = foodMap.get(itemDTO.getId());
            BigDecimal actualPrice = itemDTO.getPrice();
            if (dbFood != null && dbFood.getPrice() != null && dbFood.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                actualPrice = dbFood.getPrice();
                log.info("使用数据库价格覆盖: foodId={}, dbPrice={}, frontendPrice={}",
                    itemDTO.getId(), dbFood.getPrice(), itemDTO.getPrice());
                if (itemDTO.getPrice() != null && itemDTO.getPrice().compareTo(dbFood.getPrice()) != 0) {
                    log.warn("价格差异检测: foodId={}, 前端价格={}, 数据库价格={}", itemDTO.getId(), itemDTO.getPrice(), dbFood.getPrice());
                }
            } else {
                log.info("使用前端价格: foodId={}, frontendPrice={}, dbPrice={}",
                    itemDTO.getId(), itemDTO.getPrice(),
                    dbFood != null ? dbFood.getPrice() : "null");
            }

            // 防止actualPrice为null导致NPE
            if (actualPrice == null) {
                actualPrice = itemDTO.getPrice() != null ? itemDTO.getPrice() : BigDecimal.ZERO;
                log.warn("菜品价格异常: foodId={}, 使用价格={}", itemDTO.getId(), actualPrice);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setFoodId(itemDTO.getId());
            orderItem.setFoodName(itemDTO.getName());
            orderItem.setUnitPrice(actualPrice);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setSubtotalAmount(actualPrice.multiply(new BigDecimal(itemDTO.getQuantity())));
            orderItem.setCreateTime(LocalDateTime.now());
            orderItem.setUpdateTime(LocalDateTime.now());
            orderItem.setDeleted(0);
            orderItemMapper.insert(orderItem);

            // 扣减库存（food 表 + foods 表，保证两表一致）
            int deductResult = foodMapper.deductStock(itemDTO.getId(), itemDTO.getQuantity());
            if (deductResult == 0) {
                throw new BusinessException(400, "菜品【" + itemDTO.getName() + "】库存不足");
            }
            // 同步扣减 foods 表（菜品展示来源），保证两表库存一致
            int deductNewResult = foodNewMapper.deductStock(itemDTO.getId(), itemDTO.getQuantity());
            if (deductNewResult == 0) {
                log.warn("foods表库存扣减失败: 菜品ID={}, 数量={}", itemDTO.getId(), itemDTO.getQuantity());
                // food表已扣减成功，foods表扣减失败属于数据不一致，记录日志但不再回滚
            }
        }
    }

    /**
     * 重新计算订单金额（基于已插入的订单项）
     */
    private BigDecimal recalculateOrderAmount(String orderId, BigDecimal serverTotalAmount) {
        BigDecimal finalAmount = BigDecimal.ZERO;
        List<OrderItem> insertedItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        log.info("重新计算订单金额: orderId={}, insertedItemCount={}", orderId, insertedItems.size());

        for (OrderItem oi : insertedItems) {
            if (oi.getUnitPrice() != null && oi.getQuantity() != null) {
                BigDecimal itemTotal = oi.getUnitPrice().multiply(new BigDecimal(oi.getQuantity()));
                finalAmount = finalAmount.add(itemTotal);
            } else {
                log.warn("订单项金额异常: foodId={}, unitPrice={}, quantity={}, 跳过该项",
                    oi.getFoodId(), oi.getUnitPrice(), oi.getQuantity());
            }
        }

        // 防御性处理：如果重新计算的金额为0，使用初始计算的金额
        if (finalAmount.compareTo(BigDecimal.ZERO) == 0 && serverTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("订单金额重算为0，回退到初始金额: orderId={}, serverTotalAmount={}", orderId, serverTotalAmount);
            finalAmount = serverTotalAmount;
        }
        return finalAmount;
    }

    /**
     * 创建后厨订单
     */
    private KitchenOrder createKitchenOrder(String kitchenOrderId, String orderId, String orderNumber,
            String pickupNumber, String pickupCode, Integer orderType, OrderRequestDTO request) {
        KitchenOrder kitchenOrder = new KitchenOrder();
        kitchenOrder.setKitchenOrderId(kitchenOrderId);
        kitchenOrder.setOrderId(orderId);
        kitchenOrder.setOrderNumber(orderNumber);
        kitchenOrder.setPickupNumber(pickupNumber);
        kitchenOrder.setPickupCode(pickupCode);
        kitchenOrder.setOrderType(orderType);
        kitchenOrder.setTableNumber(request.getTableNumber() != null ? request.getTableNumber().toString() : null);
        kitchenOrder.setStatus("pending");
        kitchenOrder.setReceiveTime(LocalDateTime.now());
        kitchenOrder.setTotalDishes(calculateTotalDishes(request.getItems()));
        kitchenOrder.setDishItems(buildDishItemsJson(request.getItems()));
        kitchenOrder.setCreateTime(LocalDateTime.now());
        kitchenOrder.setDeleted(0);
        kitchenOrder.setPriority(0);
        kitchenOrderMapper.insert(kitchenOrder);
        log.info("后厨订单创建: {}", kitchenOrderId);
        return kitchenOrder;
    }

    /**
     * 事务提交后发布订单创建事件
     */
    private void publishOrderCreatedEvent(String orderId, KitchenOrder kitchenOrder,
            String orderNumber, BigDecimal finalAmount) {
        final String fOrderId = orderId;
        final String fKitchenOrderId = kitchenOrder.getKitchenOrderId();
        final String fOrderNumber = orderNumber;
        final Integer fOrderType = kitchenOrder.getOrderType();
        final String fTableNumber = kitchenOrder.getTableNumber();
        final Integer fTotalDishes = kitchenOrder.getTotalDishes();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                public void afterCommit() {
                    try {
                        OrderCreatedEvent event = new OrderCreatedEvent();
                        event.setEventId("EVT" + System.currentTimeMillis());
                        event.setKitchenOrderId(fKitchenOrderId);
                        event.setOrderId(fOrderId);
                        event.setOrderNumber(fOrderNumber);
                        event.setOrderType(fOrderType);
                        event.setTableNumber(fTableNumber);
                        event.setTotalAmount(finalAmount);
                        event.setTotalDishes(fTotalDishes);
                        event.setCreateTime(LocalDateTime.now());
                        eventPublisher.publishEvent(event);
                    } catch (Exception e) {
                        log.warn("事务提交后事件发布异常: {}", e.getMessage());
                    }
                }
            });
    }

    /**
     * 构建创建订单返回结果
     */
    private Result<OrderResultDTO> buildCreateOrderResult(String orderId, String orderNumber,
            String pickupNumber, String pickupCode, Integer orderType,
            OrderRequestDTO request, BigDecimal finalAmount) {
        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(orderId);
        result.setOrderNumber(orderNumber);
        result.setPickupNumber(pickupNumber);
        result.setPickupCode(pickupCode);
        result.setStatus("pending");
        result.setMessage("订单创建成功");
        result.setOrderType(request.getOrderType());
        result.setTableNumber(request.getTableNumber() != null ? request.getTableNumber().toString() : null);
        result.setTotalAmount(finalAmount);
        result.setOrderSource(4);
        result.setCreateTime(LocalDateTime.now().toString());

        // 验证返回结果的金额
        if (result.getTotalAmount() == null || result.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("返回DTO金额异常: orderId={}, totalAmount={}", orderId, result.getTotalAmount());
            throw new BusinessException(500, "订单返回金额异常");
        }

        log.info("构建订单返回结果: orderId={}, totalAmount={}", orderId, result.getTotalAmount());
        List<OrderResultDTO.OrderItemInfo> itemInfos = request.getItems().stream().map(item -> {
            OrderResultDTO.OrderItemInfo info = new OrderResultDTO.OrderItemInfo();
            info.setId(item.getId());
            info.setName(item.getName());
            info.setPrice(item.getPrice());
            info.setQuantity(item.getQuantity());
            return info;
        }).collect(Collectors.toList());
        result.setOrderItems(itemInfos);

        return Result.success(result);
    }

    /**
     * 解析用户标识
     */
    private String resolveUserIdentifier(OrderRequestDTO request) {
        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            return truncateUserId(request.getUserId());
        }
        if (request.getOpenid() != null && !request.getOpenid().isEmpty()) {
            return truncateUserId("wx_" + request.getOpenid().hashCode());
        }
        return truncateUserId("tmp_" + System.currentTimeMillis());
    }

    /**
     * 截断用户ID到32位
     */
    private String truncateUserId(String userId) {
        if (userId == null) return null;
        if (userId.length() <= 32) return userId;
        return userId.substring(0, 32);
    }

    /**
     * 根据门店ID查询门店名称
     * POS端订单关联门店时使用，缓存门店名称避免频繁查询
     * @param storeId 门店ID（stores_new.store_id 的字符串形式）
     * @return 门店名称（查询失败返回"未知门店"）
     */
    private String resolveStoreName(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            return "未知门店";
        }
        try {
            Long longStoreId = Long.parseLong(storeId);
            StoreNew store = storeNewMapper.selectById(longStoreId);
            return store != null && store.getStoreName() != null ? store.getStoreName() : "未知门店";
        } catch (NumberFormatException e) {
            log.warn("门店ID格式错误，无法转换为Long: storeId={}", storeId);
            return "未知门店";
        } catch (Exception e) {
            log.warn("查询门店名称失败: storeId={}, error={}", storeId, e.getMessage());
            return "未知门店";
        }
    }

    /**
     * 转换订单类型
     */
    private Integer convertOrderType(String orderType) {
        if (orderType == null) return 0;
        return switch (orderType.toLowerCase()) {
            case "dinein" -> 0;
            case "takeout" -> 1;
            case "pickup" -> 2;
            default -> 0;
        };
    }

    /**
     * 转换支付方式
     */
    private Integer convertPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return 2;
        return switch (paymentMethod) {
            case "微信支付", "wechat", "WECHAT" -> 0;
            case "余额支付", "balance", "BALANCE" -> 4;
            case "支付宝", "alipay", "ALIPAY" -> 1;
            case "现金", "cash", "CASH" -> 2;
            case "银行卡", "card", "CARD" -> 3;
            default -> 2;
        };
    }

    /**
     * 计算订单总金额
     */
    private BigDecimal calculateTotalAmount(List<OrderItemDTO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDTO item : items) {
            if (item.getPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        }
        return total;
    }

    /**
     * 计算桌台订单总金额
     */
    private BigDecimal calculateTableOrderAmount(List<TableOrderDTO.TableOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (TableOrderDTO.TableOrderItem item : items) {
            if (item.getPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        }
        return total;
    }

    /**
     * 计算菜品总数量
     */
    private Integer calculateTotalDishes(List<OrderItemDTO> items) {
        return items.stream().mapToInt(OrderItemDTO::getQuantity).sum();
    }

    /**
     * 计算桌台订单菜品总数量
     */
    private Integer calculateTableOrderDishes(List<TableOrderDTO.TableOrderItem> items) {
        return items.stream().mapToInt(TableOrderDTO.TableOrderItem::getQuantity).sum();
    }

    /**
     * 构建菜品项JSON
     */
    private String buildDishItemsJson(List<OrderItemDTO> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            OrderItemDTO item = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(item.getName()).append("\"")
              .append(",\"quantity\":").append(item.getQuantity())
              .append(",\"price\":").append(item.getPrice())
              .append(",\"type\":\"").append(item.getDishType()).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 构建桌台订单菜品项JSON
     */
    private String buildTableOrderDishItemsJson(List<TableOrderDTO.TableOrderItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            TableOrderDTO.TableOrderItem item = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(item.getName()).append("\",\"quantity\":").append(item.getQuantity()).append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
