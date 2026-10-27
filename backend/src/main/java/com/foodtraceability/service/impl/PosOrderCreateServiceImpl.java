package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.NoLocationContextException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCreatedEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.service.PosCallNumberService;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    /** P1-COMBO-ORDER-001: 新表套餐/配料（下单期校验 dishType=combo） */
    private final DishComboNewMapper dishComboNewMapper;
    private final ComboIngredientNewMapper comboIngredientNewMapper;
    private final OrderWebSocketController orderWebSocketController;
    private final ApplicationEventPublisher eventPublisher;
    private final SensitiveDataService sensitiveDataService;
    private final PosOrderNumberGenerator numberGenerator;
    /** POS 叫号号服务：独立号段（A001/B023），按门店每天自增，与订单号解耦 */
    private final PosCallNumberService posCallNumberService;
    /** 门店 Mapper（增强版），用于查询门店名称（POS端订单关联门店）
     *  使用 stores_new 表，与 StoreNewController 一致 */
    private final StoreNewMapper storeNewMapper;
    /** W1-EC-01: canonical 订单表 Mapper（orders 表） */
    private final OrderNewMapper orderNewMapper;

    /** bug-㉒：独立事务回查（外层事务被 PG 标记 aborted 时使用） */
    @org.springframework.beans.factory.annotation.Autowired
    private OrderIdempotencyLookup orderIdempotencyLookup;

    /** bug-㉒：保存点级插入（撞唯一键时外层事务不被中止） */
    @org.springframework.beans.factory.annotation.Autowired
    private OrderInsertAttempt orderInsertAttempt;
    /** W1-EC-01: canonical 订单明细表 Mapper（order_items 表） */
    private final OrderItemNewMapper orderItemNewMapper;

    public PosOrderCreateServiceImpl(
            KitchenOrderMapper kitchenOrderMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            FoodMapper foodMapper,
            FoodNewMapper foodNewMapper,
            DishComboMapper dishComboMapper,
            DishComboNewMapper dishComboNewMapper,
            ComboIngredientNewMapper comboIngredientNewMapper,
            OrderWebSocketController orderWebSocketController,
            ApplicationEventPublisher eventPublisher,
            SensitiveDataService sensitiveDataService,
            PosOrderNumberGenerator numberGenerator,
            StoreNewMapper storeNewMapper,
            OrderNewMapper orderNewMapper,
            OrderItemNewMapper orderItemNewMapper,
            PosCallNumberService posCallNumberService) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.foodMapper = foodMapper;
        this.foodNewMapper = foodNewMapper;
        this.dishComboMapper = dishComboMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.comboIngredientNewMapper = comboIngredientNewMapper;
        this.orderWebSocketController = orderWebSocketController;
        this.eventPublisher = eventPublisher;
        this.sensitiveDataService = sensitiveDataService;
        this.numberGenerator = numberGenerator;
        this.storeNewMapper = storeNewMapper;
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.posCallNumberService = posCallNumberService;
    }

    /**
     * 创建订单（POS收银入口）
     * 委托给 createCanonicalOrder 统一处理，保留取餐号/取餐码等入口特有逻辑。
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> createOrder(OrderRequestDTO request) {
        log.info("开始创建订单, orderType={}, items={}", request.getOrderType(), request.getItems().size());

        // 构建 Canonical 命令
        CanonicalOrderCommand command = new CanonicalOrderCommand();
        command.setTableNumber(request.getTableNumber());
        command.setItems(request.getItems().stream()
            .map(this::toCanonicalItem).collect(Collectors.toList()));
        command.setOrderType(convertOrderType(request.getOrderType()));
        command.setOrderSource(4); // POS终端
        command.setStoreId(request.getStoreId());
        command.setUserId(resolveUserIdentifier(request));
        command.setIdempotencyKey(request.getIdempotencyKey());
        command.setRemark(request.getRemark());
        command.setContactName(request.getContactName());
        command.setContactPhone(request.getContactPhone());
        command.setDeliveryAddress(request.getDeliveryAddress());
        command.setPaymentMethod(request.getPaymentMethod());

        // 委托统一合同方法
        Result<OrderResultDTO> result = createCanonicalOrder(command);

        // 入口特有：补充叫号号/取餐码
        // 注意：叫号号已在 createCanonicalOrder 内取号并落库，这里只做兜底读回，绝不覆盖已分配的号
        if (result.getCode() == 0 && result.getData() != null) {
            OrderResultDTO data = result.getData();
            Integer orderType = command.getOrderType();
            String callNumber = data.getCallNumber();
            if (callNumber == null || callNumber.isEmpty()) {
                callNumber = posCallNumberService.ensureCallNumber(data.getOrderId());
                data.setCallNumber(callNumber);
            }
            if (callNumber != null && !callNumber.isEmpty()) {
                data.setPickupNumber(callNumber);
            }
            if (orderType != null && orderType == 1 && data.getPickupCode() == null) {
                data.setPickupCode(numberGenerator.generatePickupCode());
            }
            data.setOrderType(request.getOrderType());
            data.setTableNumber(request.getTableNumber() != null ? request.getTableNumber().toString() : null);
        }

        return result;
    }

    /**
     * 创建桌台订单（POS扫码入口）
     * 委托给 createCanonicalOrder 统一处理。
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> createTableOrder(TableOrderDTO request) {
        log.info("开始创建桌台订单: tableNumber={}", request.getTableNumber());
        log.info("桌台ID: {}, 桌号: {}", request.getTableId(), request.getTableNumber());

        // 构建 Canonical 命令
        CanonicalOrderCommand command = new CanonicalOrderCommand();
        if (request.getTableNumber() != null) {
            try {
                command.setTableNumber(Integer.parseInt(request.getTableNumber().replaceAll("[^0-9]", "")));
            } catch (NumberFormatException e) {
                command.setTableNumber(0);
            }
        }
        command.setTableId(request.getTableId());
        command.setItems(request.getItems().stream()
            .map(this::toCanonicalTableItem).collect(Collectors.toList()));
        command.setOrderType(0); // 堂食
        command.setOrderSource(4); // POS终端

        // 委托统一合同方法
        Result<OrderResultDTO> result = createCanonicalOrder(command);

        // 入口特有：补充桌台信息
        if (result.getCode() == 0 && result.getData() != null) {
            OrderResultDTO data = result.getData();
            data.setMessage("桌台订单创建成功");
        }

        return result;
    }

    // ==================== Canonical Order Creation（统一合同） ====================

    /**
     * Canonical Order Creation - 所有订单创建入口的统一方法。
     *
     * 职责：
     * - 验证订单项
     * - 幂等性检查（W1-EC-01: orders 表暂不支持，跳过）
     * - 库存预检
     * - 订单号生成（ORD{yyyyMMdd}{sequence}）
     * - 金额计算（元→分）+ 校验
     * - 订单实体创建 → orders（W1-EC-01 已切换）
     * - 订单项创建 → order_items（W1-EC-01 已切换）+ 库存扣减
     * - 金额重算（基于 DB 价格）
     * - 后厨订单创建
     * - WebSocket 推送
     * - OrderCreatedEvent 发布（事务提交后）
     *
     * @param command 统一订单创建命令（金额单位：元，canonical 内部转分）
     * @return 订单创建结果
     */
    /** 按幂等键查既有订单（partial unique index 只对非 NULL 生效，查不到返回 null） */
    private OrderNew findOrderByIdempotencyKey(String idempotencyKey) {
        List<OrderNew> found = orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getIdempotencyKey, idempotencyKey)
                .eq(OrderNew::getDeleted, 0)
                .orderByAsc(OrderNew::getOrderId)
                .last("LIMIT 1"));
        return found == null || found.isEmpty() ? null : found.get(0);
    }

    /** 重复提交的返回体：与首次创建同形，reused=true 便于客户端识别 */
    private OrderResultDTO buildReplayResult(OrderNew existing) {
        OrderResultDTO dto = new OrderResultDTO();
        dto.setOrderId(String.valueOf(existing.getOrderId()));
        dto.setOrderNumber(existing.getOrderNumber());
        dto.setStatus(existing.getOrderStatus() == null ? null : String.valueOf(existing.getOrderStatus()));
        dto.setMessage("重复提交：返回首次创建的订单（幂等命中）");
        dto.setOrderSource(existing.getOrderSource());
        dto.setReused(Boolean.TRUE);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> createCanonicalOrder(CanonicalOrderCommand command) {
        log.info("开始创建统一订单, orderType={}, items={}", command.getOrderType(),
            command.getItems() != null ? command.getItems().size() : 0);
        try {
            // 1. 验证订单项
            if (command.getItems() == null || command.getItems().isEmpty()) {
                return Result.error("订单项不能为空");
            }
            Result<Void> validationResult = validateCanonicalItems(command.getItems());
            if (validationResult.getCode() != 0) {
                return Result.error(validationResult.getMessage());
            }

            // 2. 幂等性检查（bug-③）：同一 idempotencyKey 第二次提交 → 返回第一次创建的同一订单
            //    语义（Owner 裁定）：HTTP 200 + code=0 + 同一 orderId/orderNumber，非 409（重复提交是正常重试）
            final String idempotencyKey = command.getIdempotencyKey() == null ? null : command.getIdempotencyKey().trim();
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                OrderNew replayed = findOrderByIdempotencyKey(idempotencyKey);
                if (replayed != null) {
                    log.info("[METRIC] order_idempotent_replay idempotencyKey={} orderId={} orderNumber={}",
                            idempotencyKey, replayed.getOrderId(), replayed.getOrderNumber());
                    return Result.success(buildReplayResult(replayed));
                }
            }

            // 3. 库存预检（批量查询避免 N+1）
            List<String> foodIds = command.getItems().stream()
                .map(CanonicalOrderItemCommand::getFoodId)
                .distinct()
                .collect(Collectors.toList());
            // W1-EC-01: 使用 foodNewMapper 查询 foods 表（主键 food_id）
            Map<Long, FoodNew> foodNewMap = foodNewMapper.selectBatchIds(foodIds.stream()
                .map(id -> {
                    try { return Long.parseLong(id); } catch (NumberFormatException e) { return 0L; }
                })
                .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(FoodNew::getFoodId, food -> food, (existing, replacement) -> existing));

            // W1-EC-01: 构建 foodCode → 数字 foodId 映射（用于 order_items.food_id，Long 类型）
            Map<String, Long> foodCodeToIdMap = foodNewMapper.selectList(
                new LambdaQueryWrapper<FoodNew>().in(FoodNew::getFoodCode, foodIds))
                .stream()
                .collect(Collectors.toMap(FoodNew::getFoodCode, FoodNew::getFoodId, (a, b) -> a));

            // P1-POS-FOODID-MAP-001 B2: 映射 miss → 下单期 400 拒绝（单品+套餐均生效）
            // P1-COMBO-ORDER-001: dishType=combo 走 combo_id 校验（新表），不要求 food_code 映射
            // 预检点：Map 构建后、orders insert(:312) 前；复用 Result.error(400) 避开 :355-358 catch 兜底
            for (CanonicalOrderItemCommand item : command.getItems()) {
                if (item.getFoodId() == null || item.getFoodId().trim().isEmpty()) {
                    return Result.error(400, "菜品ID不能为空");
                }
                if (isComboItem(item)) {
                    Long comboId;
                    try {
                        comboId = Long.parseLong(item.getFoodId().trim());
                    } catch (NumberFormatException e) {
                        return Result.error(400, "套餐[" + item.getFoodName() + "]comboId 非法，拒绝下单");
                    }
                    DishComboNew combo = dishComboNewMapper.selectById(comboId);
                    if (combo == null) {
                        return Result.error(400, "套餐[" + item.getFoodName() + "]不存在（combo_id=" + comboId + "），拒绝下单");
                    }
                    long ingredientCount = comboIngredientNewMapper.selectCount(
                        new LambdaQueryWrapper<ComboIngredientNew>()
                            .eq(ComboIngredientNew::getComboId, comboId));
                    if (ingredientCount <= 0) {
                        return Result.error(400, "套餐[" + item.getFoodName() + "]无配料（combo_ingredients 为空），拒绝下单");
                    }
                } else if (!foodCodeToIdMap.containsKey(item.getFoodId())) {
                    return Result.error(400, "菜品[" + item.getFoodId() + "]映射失败: food_code 不存在，拒绝下单");
                }
            }

            for (CanonicalOrderItemCommand item : command.getItems()) {
                if (isComboItem(item)) {
                    // 套餐无独立 foods 库存；组件扣减在出餐扣料路径（combo_ingredients）
                    continue;
                }
                Long foodIdLong = null;
                try { foodIdLong = Long.parseLong(item.getFoodId()); } catch (NumberFormatException e) {}
                FoodNew foodNew = foodIdLong != null ? foodNewMap.get(foodIdLong) : null;
                if (foodNew != null && foodNew.getStock() != null) {
                    if (foodNew.getStock() < item.getQuantity()) {
                        return Result.error("菜品【" + foodNew.getFoodName() + "】库存不足，当前库存: " + foodNew.getStock());
                    }
                }
            }

            // 4. 生成订单号
            String orderNumber = numberGenerator.generateOrderNumberByDbSequence(command.getTableNumber());
            log.info("订单号生成: {}", orderNumber);

            // 5. 生成 ID
            String kitchenOrderId = "KO" + numberGenerator.incrementAndGetCounter();
            String orderId = "O" + numberGenerator.incrementAndGetCounter();
            Integer orderType = command.getOrderType() != null ? command.getOrderType() : 0;

            // 6. 计算订单金额（元）→ 转分
            BigDecimal totalAmountYuan = calculateCanonicalTotalAmount(command.getItems());
            Long totalAmountFen = yuanToFen(totalAmountYuan);
            log.info("订单初始金额计算: orderId={}, totalAmountYuan={}, totalAmountFen={}, itemsCount={}",
                orderId, totalAmountYuan, totalAmountFen, command.getItems().size());

            if (totalAmountFen == null || totalAmountFen <= 0) {
                log.error("订单金额计算异常: orderId={}, totalAmountYuan={}, totalAmountFen={}", orderId, totalAmountYuan, totalAmountFen);
                return Result.error("订单金额计算异常，请检查商品价格配置");
            }

            // 7. 解析用户标识
            String userId = command.getUserId();
            if (userId == null || userId.isEmpty()) {
                userId = "tmp_" + System.currentTimeMillis();
            }
            userId = truncateUserId(userId);

            // 8. 转换支付方式
            Integer paymentMethodInt = command.getPaymentMethod() != null
                ? convertPaymentMethod(command.getPaymentMethod()) : null;

            // 9. 解析门店信息（P1-USER-LOCATION-001 #11：删除 STORE_A 查询与 "1" 数值兜底）
            String storeId = command.getStoreId();
            if (storeId == null || storeId.isEmpty()) {
                // 位置上下文解析链 ②：从登录上下文推导（POS token 必带 locationId，经 location_id_map 反查 store_id）
                storeId = SecurityUtils.getCurrentUserStoreId();
                if (storeId == null) {
                    throw new NoLocationContextException("缺少门店信息：请求未携带 storeId，且登录上下文无归属位置");
                }
            }
            String storeName = resolveStoreName(storeId);
            Long storeIdLong = null;
            try {
                storeIdLong = Long.parseLong(storeId);
            } catch (NumberFormatException e) {
                log.warn("门店ID格式错误，无法转换为Long: storeId={}", storeId);
            }

            // 10. W1-EC-01: 创建订单实体 → orders 表（canonical）
            OrderNew orderNew = new OrderNew();
            orderNew.setOrderId(orderId);
            orderNew.setOrderCode(orderNumber);
            orderNew.setOrderNumber(orderNumber);
            orderNew.setOrderType(orderType);
            orderNew.setOrderSource(command.getOrderSource() != null ? command.getOrderSource() : 4);
            orderNew.setStoreId(storeIdLong);
            orderNew.setCustomerName(command.getContactName());
            if (command.getContactPhone() != null) {
                // customer_phone 存脱敏值（varchar(20) 放得下，修复 POS 下单带电话 500）；
                // customer_phone_encrypted 存可还原密文（供授权回拨/隐私分级）
                orderNew.setCustomerPhone(sensitiveDataService.maskPhone(command.getContactPhone()));
                orderNew.setCustomerPhoneEncrypted(sensitiveDataService.encryptPhone(command.getContactPhone()));
            }
            if (command.getDeliveryAddress() != null) {
                orderNew.setDeliveryAddress(sensitiveDataService.encryptAddress(command.getDeliveryAddress()));
            }
            if (command.getRemark() != null) { orderNew.setRemark(command.getRemark()); }
            if (command.getTableNumber() != null) {
                orderNew.setTableName("桌" + command.getTableNumber());
            }
            orderNew.setTableId(command.getTableId());
            orderNew.setOrderStatus(0);   // 初始态：待确认
            orderNew.setPaymentStatus(0);  // 初始态：未支付
            orderNew.setTotalAmount(totalAmountFen);
            orderNew.setFinalAmount(totalAmountFen);
            orderNew.setDiscountAmount(0L);
            orderNew.setPaidAmount(0L);
            orderNew.setRefundAmount(0L);
            orderNew.setCreateTime(LocalDateTime.now());
            orderNew.setUpdateTime(LocalDateTime.now());
            orderNew.setDeleted(0);
            // bug-③：幂等键随订单在同一事务落库（有值才写），唯一部分索引 uk_orders_idempotency_key 兜底并发
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                orderNew.setIdempotencyKey(idempotencyKey);
            }
            try {
                // bug-㉒：走保存点（NESTED）插入 —— 撞 uk_orders_idempotency_key 时只回滚保存点，
                // 外层事务**不会被 PG 标记 aborted**，后续叫号号/明细/库存仍可正常写。
                if (orderInsertAttempt != null) {
                    orderInsertAttempt.insertWithinSavepoint(orderNew);
                } else {
                    orderNewMapper.insert(orderNew);
                }
            } catch (org.springframework.dao.DuplicateKeyException duplicateKeyException) {
                // 并发双创建：另一请求已用同一 key 落库 → 回查并返回它的订单（不是 500）
                if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                    // bug-㉒：必须用独立事务回查 —— 本事务已被 PG 标记 aborted，同事务再查必 500。
                    // 且必须**重试**：撞唯一键的那一刻，胜者事务可能还没提交，独立事务查不到（实测：单次回查为 null → 500）。
                    // 采用短轮询（最多 10 次 × 100ms = 1s），仍查不到才如实抛错。
                    OrderNew concurrent = null;
                    for (int attempt = 0; attempt < 10; attempt++) {
                        if (orderIdempotencyLookup != null) {
                            concurrent = orderIdempotencyLookup.findByKeyInNewTx(idempotencyKey);
                        }
                        if (concurrent != null) {
                            break;
                        }
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    if (concurrent != null) {
                        log.info("[METRIC] order_idempotent_replay_concurrent idempotencyKey={} orderId={} orderNumber={}",
                                idempotencyKey, concurrent.getOrderId(), concurrent.getOrderNumber());
                        return Result.success(buildReplayResult(concurrent));
                    }
                }
                throw duplicateKeyException;
            }

            // 10.5 生成 POS 叫号号（独立号段：门店 + 自然日 + 类型前缀，每天从 1 自增，跨天重置）
            //      入口刻意放在状态机之外：本服务是 POS 下单收尾方法，只追加叫号号字段，不改订单状态。
            //      幂等：同一 orderId 重复调用只会读回已落库的号，不会二次取号。
            String callNumber = posCallNumberService.ensureCallNumber(orderId, storeIdLong, orderType);
            orderNew.setCallNumber(callNumber);
            log.info("POS 叫号号确定: orderId={}, callNumber={}", orderId, callNumber);

            // 11. W1-EC-01: 创建订单项 → order_items 表（canonical）+ 扣减库存
            createCanonicalOrderItemsAndDeductStockNew(orderId, command.getItems(), foodNewMap, foodCodeToIdMap);

            // 12. W1-EC-01: 重新计算金额（基于已插入的 order_items 表订单项，单位：分）
            Long finalAmountFen = recalculateOrderAmountInFen(orderId, totalAmountFen);

            if (finalAmountFen == null || finalAmountFen <= 0) {
                log.error("订单最终金额异常: orderId={}, finalAmountFen={}", orderId, finalAmountFen);
                throw new BusinessException(500, "订单金额计算异常，最终金额为0");
            }

            log.info("订单最终金额确定: orderId={}, finalAmountFen={}", orderId, finalAmountFen);
            orderNew.setTotalAmount(finalAmountFen);
            orderNew.setFinalAmount(finalAmountFen);
            orderNewMapper.updateById(orderNew);

            // 13. 创建后厨订单
            //     取餐号 = 叫号号（callNumber）。旧实现用「今日后厨单计数 + 1」，跨门店不隔离且并发会重号，
            //     现已统一走 posCallNumberService 的原子号段；pickupCode 仅外卖使用，保持原逻辑。
            String pickupCode = (orderType == 1) ? numberGenerator.generatePickupCode() : null;
            KitchenOrder kitchenOrder = createKitchenOrder(kitchenOrderId, orderId, orderNumber,
                callNumber, pickupCode, orderType, command);

            // 14. WebSocket 推送
            try {
                orderWebSocketController.pushNewOrder(kitchenOrder);
            } catch (Exception wsEx) {
                log.warn("WebSocket推送异常: {}", wsEx.getMessage());
            }

            // W1-EC-01: 分→元（用于事件发布和 DTO 返回）
            BigDecimal finalAmountYuan = fenToYuan(finalAmountFen);

            // 15. 事务提交后发布 OrderCreatedEvent
            publishOrderCreatedEvent(orderId, kitchenOrder, orderNumber, finalAmountYuan);

            log.info("统一订单创建成功: orderId={}, orderNumber={}, finalAmountFen={}, finalAmountYuan={}",
                orderId, orderNumber, finalAmountFen, finalAmountYuan);

            // 16. 构建返回结果
            return buildCanonicalOrderResult(orderId, orderNumber, callNumber, pickupCode,
                orderType, command, finalAmountYuan);
        } catch (Exception e) {
            log.error("统一订单创建失败: {}", e.getMessage(), e);
            return Result.error("订单创建失败: " + e.getMessage());
        }
    }

    // ==================== Canonical 私有辅助方法 ====================

    /**
     * W1-EC-01: 元（BigDecimal）→ 分（Long）转换
     * 公式：分 = 元 × 100，四舍五入取整
     * 示例：128.50 元 → 12850 分
     */
    private Long yuanToFen(BigDecimal yuan) {
        if (yuan == null) return 0L;
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * W1-EC-01: 分（Long）→ 元（BigDecimal）转换（用于 DTO 返回）
     */
    private BigDecimal fenToYuan(Long fen) {
        if (fen == null || fen == 0) return BigDecimal.ZERO;
        return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 验证 Canonical 订单项
     */
    private Result<Void> validateCanonicalItems(List<CanonicalOrderItemCommand> items) {
        if (items != null) {
            for (CanonicalOrderItemCommand item : items) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return Result.error("商品数量必须大于0");
                }
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    return Result.error("商品价格必须大于0");
                }
                if (item.getFoodId() == null || item.getFoodId().trim().isEmpty()) {
                    return Result.error("菜品ID不能为空");
                }
                if (item.getFoodName() == null || item.getFoodName().trim().isEmpty()) {
                    return Result.error("菜品名称不能为空");
                }
            }
        }
        return Result.success(null);
    }

    /**
     * 计算 Canonical 订单总金额（元）
     */
    private BigDecimal calculateCanonicalTotalAmount(List<CanonicalOrderItemCommand> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CanonicalOrderItemCommand item : items) {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        }
        return total;
    }

    /**
     * @param foodNewMap foods 表 foodId → FoodNew 映射
     * @param foodCodeToIdMap foods 表 foodCode → 数字 foodId 映射
     */
    private void createCanonicalOrderItemsAndDeductStockNew(String orderId,
            List<CanonicalOrderItemCommand> items, Map<Long, FoodNew> foodNewMap,
            Map<String, Long> foodCodeToIdMap) {
        for (CanonicalOrderItemCommand item : items) {
            Long foodIdLong = null;
            try { foodIdLong = Long.parseLong(item.getFoodId()); } catch (NumberFormatException e) {}
            FoodNew dbFood = foodIdLong != null ? foodNewMap.get(foodIdLong) : null;
            BigDecimal actualPriceYuan = item.getUnitPrice();

            // P1-COMBO-ORDER-001: 套餐 product_type=2，combo_id=comboId，food_id=null；不走 foods 价格覆盖/库存扣减
            if (isComboItem(item)) {
                Long comboId;
                try {
                    comboId = Long.parseLong(item.getFoodId().trim());
                } catch (NumberFormatException e) {
                    throw new BusinessException(400, "套餐[" + item.getFoodName() + "]comboId 非法");
                }
                DishComboNew combo = dishComboNewMapper.selectById(comboId);
                if (combo == null) {
                    // R1 门禁：禁止 warn-then-insert
                    throw new BusinessException(400, "套餐[" + item.getFoodName() + "]不存在（combo_id=" + comboId + "）");
                }
                long ingredientCount = comboIngredientNewMapper.selectCount(
                    new LambdaQueryWrapper<ComboIngredientNew>()
                        .eq(ComboIngredientNew::getComboId, comboId));
                if (ingredientCount <= 0) {
                    throw new BusinessException(400, "套餐[" + item.getFoodName() + "]无配料，拒绝下单");
                }

                Long unitPriceFenCombo = yuanToFen(actualPriceYuan);
                Long subtotalFenCombo = unitPriceFenCombo * item.getQuantity();

                OrderItemNew comboOrderItem = new OrderItemNew();
                comboOrderItem.setOrderId(orderId);
                comboOrderItem.setProductType(2);
                comboOrderItem.setComboId(comboId);
                comboOrderItem.setFoodId(null);
                comboOrderItem.setProductName(item.getFoodName());
                comboOrderItem.setUnitPrice(unitPriceFenCombo);
                comboOrderItem.setQuantity(item.getQuantity());
                comboOrderItem.setAmount(subtotalFenCombo);
                comboOrderItem.setDiscountAmount(0L);
                comboOrderItem.setKitchenStatus(0);
                comboOrderItem.setCreateTime(LocalDateTime.now());
                comboOrderItem.setUpdateTime(LocalDateTime.now());
                comboOrderItem.setDeleted(0);
                orderItemNewMapper.insert(comboOrderItem);
                continue;
            }

            if (dbFood != null && dbFood.getSalePrice() != null && dbFood.getSalePrice() > 0) {
                actualPriceYuan = new BigDecimal(dbFood.getSalePrice()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                log.info("使用数据库价格覆盖: foodId={}, dbPrice={}, frontendPrice={}",
                    item.getFoodId(), dbFood.getSalePrice(), item.getUnitPrice());
                if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(actualPriceYuan) != 0) {
                    log.warn("价格差异检测: foodId={}, 前端价格={}, 数据价格={}",
                        item.getFoodId(), item.getUnitPrice(), actualPriceYuan);
                }
            } else {
                log.info("使用前端价格: foodId={}, frontendPrice={}, dbPrice={}",
                    item.getFoodId(), item.getUnitPrice(),
                    dbFood != null ? dbFood.getSalePrice() : "null");
            }

            // W1-EC-01: 金额转换 元→分
            Long unitPriceFen = yuanToFen(actualPriceYuan);
            Long subtotalFen = unitPriceFen * item.getQuantity();

            // W1-EC-01: 查找数字 foodId（order_items.food_id 为 Long 类型）
            // P1-POS-FOODID-MAP-001 B2: 预检已保证命中；此处改为 fail-fast，禁止 warn-then-insert（R1 门禁）
            Long numericFoodId = foodCodeToIdMap.get(item.getFoodId());
            if (numericFoodId == null) {
                throw new BusinessException(400, "菜品[" + item.getFoodId() + "]food_code 映射失败");
            }

            OrderItemNew orderItemNew = new OrderItemNew();
            orderItemNew.setOrderId(orderId);
            orderItemNew.setProductType(1); // 单品
            orderItemNew.setFoodId(numericFoodId);
            orderItemNew.setProductName(item.getFoodName());
            orderItemNew.setUnitPrice(unitPriceFen);
            orderItemNew.setQuantity(item.getQuantity());
            orderItemNew.setAmount(subtotalFen);
            orderItemNew.setDiscountAmount(0L);
            orderItemNew.setKitchenStatus(0); // 待制作
            orderItemNew.setCreateTime(LocalDateTime.now());
            orderItemNew.setUpdateTime(LocalDateTime.now());
            orderItemNew.setDeleted(0);
            orderItemNewMapper.insert(orderItemNew);

            // 扣减库存（food 表 + foods 表，不改变现有库存逻辑）
            // 使用 foodCode（非 foodId）调用 deductStock
            String foodCodeForDeduct = dbFood != null ? dbFood.getFoodCode() : item.getFoodId();
            int deductResult = foodMapper.deductStock(foodCodeForDeduct, item.getQuantity());
            if (deductResult == 0) {
                // P1-NEW-FOOD-LEGACY-SYNC-001: 自愈——legacy food 行缺失（新建菜品未同步）时按 foods 补行后重试一次
                ensureLegacyFoodRow(foodCodeForDeduct);
                deductResult = foodMapper.deductStock(foodCodeForDeduct, item.getQuantity());
            }
            if (deductResult == 0) {
                throw new BusinessException(400, "菜品【" + item.getFoodName() + "】库存不足");
            }
            int deductNewResult = foodNewMapper.deductStock(foodCodeForDeduct, item.getQuantity());
            if (deductNewResult == 0) {
                log.warn("foods表库存扣减失败: 菜品Code={}, 数量={}", foodCodeForDeduct, item.getQuantity());
            }
        }
    }

    /**
     * 创建后厨订单（Canonical 版本，接受 CanonicalOrderCommand）
     */
    private KitchenOrder createKitchenOrder(String kitchenOrderId, String orderId, String orderNumber,
            String pickupNumber, String pickupCode, Integer orderType, CanonicalOrderCommand command) {
        KitchenOrder kitchenOrder = new KitchenOrder();
        kitchenOrder.setKitchenOrderId(kitchenOrderId);
        kitchenOrder.setOrderId(orderId);
        kitchenOrder.setOrderNumber(orderNumber);
        kitchenOrder.setPickupNumber(pickupNumber);
        kitchenOrder.setPickupCode(pickupCode);
        kitchenOrder.setOrderType(orderType);
        kitchenOrder.setTableNumber(command.getTableNumber() != null ? command.getTableNumber().toString() : null);
        // 2026-10-23 修复：后厨单补齐门店归属（原实现 store_id/store_name 为空，追溯码与门店维度统计均缺数据）
        kitchenOrder.setStoreId(parseStoreId(command.getStoreId()));
        kitchenOrder.setStoreName(resolveStoreName(command.getStoreId()));
        kitchenOrder.setStatus("pending");
        kitchenOrder.setReceiveTime(LocalDateTime.now());
        kitchenOrder.setTotalDishes(calculateCanonicalTotalDishes(command.getItems()));
        kitchenOrder.setDishItems(buildCanonicalDishItemsJson(command.getItems()));
        kitchenOrder.setCreateTime(LocalDateTime.now());
        kitchenOrder.setDeleted(0);
        kitchenOrder.setPriority(0);
        kitchenOrderMapper.insert(kitchenOrder);
        log.info("后厨订单创建: {}", kitchenOrderId);
        return kitchenOrder;
    }

    /**
     * 构建 Canonical 订单创建返回结果
     */
    private Result<OrderResultDTO> buildCanonicalOrderResult(String orderId, String orderNumber,
            String callNumber, String pickupCode, Integer orderType,
            CanonicalOrderCommand command, BigDecimal finalAmount) {
        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(orderId);
        result.setOrderNumber(orderNumber);
        // callNumber 是顾客侧口径（A001）；pickupNumber 为兼容旧字段，值保持一致
        result.setCallNumber(callNumber);
        result.setPickupNumber(callNumber);
        result.setPickupCode(pickupCode);
        result.setStatus("pending");
        result.setMessage("订单创建成功");
        result.setOrderType(String.valueOf(orderType));
        result.setTableNumber(command.getTableNumber() != null ? command.getTableNumber().toString() : null);
        result.setTotalAmount(finalAmount);
        result.setOrderSource(command.getOrderSource() != null ? command.getOrderSource() : 4);
        result.setCreateTime(LocalDateTime.now().toString());

        if (result.getTotalAmount() == null || result.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("返回DTO金额异常: orderId={}, totalAmount={}", orderId, result.getTotalAmount());
            throw new BusinessException(500, "订单返回金额异常");
        }

        List<OrderResultDTO.OrderItemInfo> itemInfos = command.getItems().stream().map(item -> {
            OrderResultDTO.OrderItemInfo info = new OrderResultDTO.OrderItemInfo();
            info.setId(item.getFoodId());
            info.setName(item.getFoodName());
            info.setPrice(item.getUnitPrice());
            info.setQuantity(item.getQuantity());
            return info;
        }).collect(Collectors.toList());
        result.setOrderItems(itemInfos);

        return Result.success(result);
    }

    /**
     * P1-COMBO-ORDER-001: 是否套餐行（dishType=combo）
     */
    private boolean isComboItem(CanonicalOrderItemCommand item) {
        return "combo".equals(item.getDishType());
    }

    /**
     * 计算 Canonical 订单菜品总数量
     */
    private Integer calculateCanonicalTotalDishes(List<CanonicalOrderItemCommand> items) {
        return items.stream().mapToInt(CanonicalOrderItemCommand::getQuantity).sum();
    }

    /**
     * 构建 Canonical 菜品项JSON
     * P1-COMBO-ORDER-001: combo 行写入 productType/comboId，供 KDS 拉单展开
     */
    private String buildCanonicalDishItemsJson(List<CanonicalOrderItemCommand> items) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CanonicalOrderItemCommand item : items) {
            rows.add(canonicalDishItem(item.getFoodId(), item.getFoodName(), item.getQuantity(),
                    item.getUnitPrice(), item.getDishType(), isComboItem(item)));
        }
        return writeDishItems(rows);
    }

    /**
     * 将 OrderItemDTO 转换为 CanonicalOrderItemCommand
     */
    private CanonicalOrderItemCommand toCanonicalItem(OrderItemDTO item) {
        CanonicalOrderItemCommand cmd = new CanonicalOrderItemCommand();
        cmd.setFoodId(item.getId());
        cmd.setFoodName(item.getName());
        cmd.setUnitPrice(item.getPrice());
        cmd.setQuantity(item.getQuantity());
        cmd.setDishType(item.getDishType());
        return cmd;
    }

    /**
     * 将 TableOrderDTO.TableOrderItem 转换为 CanonicalOrderItemCommand
     */
    private CanonicalOrderItemCommand toCanonicalTableItem(TableOrderDTO.TableOrderItem item) {
        CanonicalOrderItemCommand cmd = new CanonicalOrderItemCommand();
        cmd.setFoodId(item.getId());
        cmd.setFoodName(item.getName());
        cmd.setUnitPrice(item.getPrice());
        cmd.setQuantity(item.getQuantity());
        return cmd;
    }
    /**
     * W1-EC-01: 重新计算订单金额（基于 order_items 表，单位：分）
     * 从 orderItemNewMapper 读取已插入的订单明细，累加 unit_price × quantity（均为 Long 分）
     *
     * @param orderId 订单ID
     * @param fallbackAmountFen 初始金额（分），重算为0时回退
     * @return 最终金额（分）
     */
    private Long recalculateOrderAmountInFen(String orderId, Long fallbackAmountFen) {
        Long finalAmount = 0L;
        List<OrderItemNew> insertedItems = orderItemNewMapper.selectList(
            new LambdaQueryWrapper<OrderItemNew>().eq(OrderItemNew::getOrderId, orderId));
        log.info("重新计算订单金额(fen): orderId={}, insertedItemCount={}", orderId, insertedItems.size());

        for (OrderItemNew oi : insertedItems) {
            if (oi.getUnitPrice() != null && oi.getQuantity() != null) {
                Long itemTotal = oi.getUnitPrice() * oi.getQuantity();
                finalAmount = finalAmount + itemTotal;
            } else {
                log.warn("订单项金额异常: foodId={}, unitPrice={}, quantity={}, 跳过该项",
                    oi.getFoodId(), oi.getUnitPrice(), oi.getQuantity());
            }
        }

        // 防御性处理：如果重新计算的金额为0，使用初始计算的金额
        if (finalAmount == 0 && fallbackAmountFen != null && fallbackAmountFen > 0) {
            log.warn("订单金额重算为0，回退到初始金额: orderId={}, fallbackAmountFen={}", orderId, fallbackAmountFen);
            finalAmount = fallbackAmountFen;
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
        // 2026-10-23 修复：后厨单补齐门店归属（同上）
        kitchenOrder.setStoreId(parseStoreId(request.getStoreId()));
        kitchenOrder.setStoreName(resolveStoreName(request.getStoreId()));
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
    /** 后厨单菜品项 JSON 序列化器（统一出口，保证转义正确）。 */
    private static final ObjectMapper DISH_ITEMS_MAPPER = new ObjectMapper();

    /**
     * 2026-10-23（Owner 裁决：后端 DTO 为 Canonical 真相源）：
     * 后厨单 dish_items 统一按 KitchenOrderCreateDTO.DishItemDTO 的键名落库 ——
     *   dishId / dishName / quantity / remark
     * price / type / comboId 仅作为展示元数据附带，读取方一律以 dishId/dishName 为准。
     * 修复前该字段写的是 POS 键名 {name,quantity,price,type}，
     * 导致 generateFoodTraceCodes 反序列化为 DishItemDTO 时 dishId/dishName 为空（追溯码缺菜品信息）。
     */
    private Map<String, Object> canonicalDishItem(String dishId, String dishName, Integer quantity,
                                                  BigDecimal price, String dishType, boolean combo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dishId", dishId);
        row.put("dishName", dishName);
        row.put("quantity", quantity);
        row.put("remark", null);
        row.put("price", price);
        row.put("type", dishType != null ? dishType : "");
        if (combo) {
            row.put("productType", 2);
            row.put("comboId", dishId);
        }
        return row;
    }

    private String writeDishItems(List<Map<String, Object>> rows) {
        try {
            return DISH_ITEMS_MAPPER.writeValueAsString(rows);
        } catch (Exception e) {
            log.error("后厨单菜品项序列化失败", e);
            return "[]";
        }
    }

    /** 门店 ID 字符串转 Long，非法或空返回 null（下游按无门店处理）。 */
    private Long parseStoreId(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(storeId.trim());
        } catch (NumberFormatException e) {
            log.warn("门店ID格式非法，忽略: storeId={}", storeId);
            return null;
        }
    }
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
     * 计算菜品总数量
     */
    private Integer calculateTotalDishes(List<OrderItemDTO> items) {
        return items.stream().mapToInt(OrderItemDTO::getQuantity).sum();
    }
    /**
     * 构建菜品项JSON
     * P1-COMBO-ORDER-001: combo 行写入 productType/comboId，供 KDS 拉单展开
     */
    private String buildDishItemsJson(List<OrderItemDTO> items) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OrderItemDTO item : items) {
            rows.add(canonicalDishItem(item.getId(), item.getName(), item.getQuantity(),
                    item.getPrice(), item.getDishType(), "combo".equals(item.getDishType())));
        }
        return writeDishItems(rows);
    }
    /**
     * P1-NEW-FOOD-LEGACY-SYNC-001: legacy food 行缺失时按 foods 新表数据补行（自愈兜底）。
     * 覆盖 DatabaseFixConfig 启动同步无法覆盖的运行期新建菜品场景。
     */
    private void ensureLegacyFoodRow(String foodCode) {
        try {
            if (foodCode == null || foodMapper.selectByFoodCodeForUpdate(foodCode) != null) {
                return;
            }
            FoodNew fn = foodNewMapper.selectByFoodCode(foodCode);
            if (fn == null) {
                return;
            }
            Food legacy = new Food();
            legacy.setFoodCode(fn.getFoodCode());
            legacy.setFoodName(fn.getFoodName());
            legacy.setFoodPrice(fn.getSalePrice() != null ? BigDecimal.valueOf(fn.getSalePrice(), 2) : BigDecimal.ZERO);
            legacy.setCostPrice(fn.getCostPrice() != null ? BigDecimal.valueOf(fn.getCostPrice(), 2) : BigDecimal.ZERO);
            legacy.setFoodStatus(fn.getStatus() == null ? "active"
                    : fn.getStatus() == 1 ? "active"
                    : fn.getStatus() == 0 ? "inactive"
                    : fn.getStatus() == 2 ? "sold_out" : "active");
            legacy.setStock(fn.getStock() != null ? fn.getStock() : 0);
            foodMapper.insert(legacy);
            log.info("自愈：legacy food 行缺失，已按 foods 补行 foodCode={}", foodCode);
        } catch (Exception e) {
            log.warn("自愈补行失败: foodCode={}, err={}", foodCode, e.getMessage());
        }
    }

}
