package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.dto.order.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.event.OrderRefundEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.OrderNewService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 系统最核心的业务逻辑层，管理订单完整生命周期
 *
 * 重构说明：
 * - 已移除 RabbitMQ 依赖（RabbitTemplate）
 * - 订单创建事件改为日志记录
 * - 保留所有业务逻辑和数据库操作
 */
@Service
public class OrderNewServiceImpl implements OrderNewService {

    private static final Logger log = LoggerFactory.getLogger(OrderNewServiceImpl.class);
    private static final DateTimeFormatter ORDER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderNewMapper orderNewMapper;
    private final OrderItemNewMapper orderItemNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    private final OrderRefundRecordNewMapper orderRefundRecordNewMapper;
    private final DiningTableNewMapper diningTableNewMapper;
    private final FoodNewMapper foodNewMapper;
    private final DishComboNewMapper dishComboNewMapper;
    private final DishRecipeNewMapper dishRecipeNewMapper;
    private final ComboIngredientMapper comboIngredientMapper;
    private final StoreInventoryService storeInventoryService;
    /** POS订单Mapper（查询 orders_legacy 表），用于管理端查询POS收银端订单 */
    private final OrderMapper posOrderMapper;
    /** POS订单项Mapper（查询 order_items_legacy 表） */
    private final OrderItemMapper posOrderItemMapper;
    /** 事件发布器：用于发布订单完成事件，触发财务收入/凭证等异步联动（DF-008 修复） */
    private final ApplicationEventPublisher applicationEventPublisher;
    /** 成本记录服务：用于持久化订单成本到 cost_record 表（DF-009 修复） */
    private final CostRecordService costRecordService;
    /** 资金流水服务（F6：订单支付落资金流水） */
    private final com.foodtraceability.service.finance.FundFlowService fundFlowService;
    /** 银行账户服务（F6：取默认账户） */
    private final com.foodtraceability.service.finance.BankAccountService bankAccountService;

    public OrderNewServiceImpl(
            OrderNewMapper orderNewMapper,
            OrderItemNewMapper orderItemNewMapper,
            OrderPaymentRecordNewMapper orderPaymentRecordNewMapper,
            OrderRefundRecordNewMapper orderRefundRecordNewMapper,
            DiningTableNewMapper diningTableNewMapper,
            FoodNewMapper foodNewMapper,
            DishComboNewMapper dishComboNewMapper,
            DishRecipeNewMapper dishRecipeNewMapper,
            ComboIngredientMapper comboIngredientMapper,
            StoreInventoryService storeInventoryService,
            OrderMapper posOrderMapper,
            OrderItemMapper posOrderItemMapper,
            ApplicationEventPublisher applicationEventPublisher,
            CostRecordService costRecordService,
            com.foodtraceability.service.finance.FundFlowService fundFlowService,
            com.foodtraceability.service.finance.BankAccountService bankAccountService) {
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
        this.orderRefundRecordNewMapper = orderRefundRecordNewMapper;
        this.diningTableNewMapper = diningTableNewMapper;
        this.foodNewMapper = foodNewMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.dishRecipeNewMapper = dishRecipeNewMapper;
        this.comboIngredientMapper = comboIngredientMapper;
        this.storeInventoryService = storeInventoryService;
        this.posOrderMapper = posOrderMapper;
        this.posOrderItemMapper = posOrderItemMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.costRecordService = costRecordService;
        this.fundFlowService = fundFlowService;
        this.bankAccountService = bankAccountService;
    }

    // ==================== 订单创建 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO createDTO) {
        log.info("开始创建订单, 类型: {}", createDTO.getOrderType());

        // 1. 校验堂食订单的桌台状态
        if (createDTO.getOrderType() != null && createDTO.getOrderType() == 1 && createDTO.getTableId() != null) {
            validateTableAvailable(createDTO.getTableId());
        }

        // 2. 校验菜品/套餐并计算金额
        OrderAmountCalculation calculation = calculateOrderAmount(createDTO.getItems());

        // 3. 生成订单编号
        String orderCode = generateOrderCode();

        // 4. 创建订单主表
        OrderNew order = buildOrderEntity(createDTO, calculation, orderCode);
        orderNewMapper.insert(order);

        // 5. 创建订单明细
        saveOrderItems(order.getOrderId(), createDTO.getItems());

        // 6. 堂食订单锁定桌台
        if (createDTO.getOrderType() != null && createDTO.getOrderType() == 1 && createDTO.getTableId() != null) {
            diningTableNewMapper.lockTable(createDTO.getTableId(), order.getOrderId());
        }

        log.info("订单创建成功, 编号: {}, ID: {}", orderCode, order.getOrderId());

        // 重构说明：已移除 RabbitMQ，订单创建事件改为日志记录
        log.info("订单创建事件：orderId={}, orderCode={}, orderType={}, eventType=ORDER_CREATED",
                order.getOrderId(), orderCode, createDTO.getOrderType());

        return getOrderDetail(order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createPosQuickOrder(PosQuickOrderDTO quickOrderDTO) {
        log.info("POS快速下单, 类型: {}", quickOrderDTO.getOrderType());

        // 将快速下单项转换为标准明细格式
        // 注意：productName/unitPrice 由 createOrder 内部 validateAndPriceFood 统一校验取价，
        // 此处不预查，避免重复 DB 访问 + 防止客户端篡改价格
        List<OrderCreateDTO.OrderItemCreateDTO> items = quickOrderDTO.getItems().stream()
                .map(item -> {
                    OrderCreateDTO.OrderItemCreateDTO dto = new OrderCreateDTO.OrderItemCreateDTO();
                    dto.setProductType(item.getProductType());
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    dto.setSpecification(item.getSpecification());
                    dto.setRemark(item.getRemark());
                    return dto;
                })
                .collect(Collectors.toList());

        // 构建完整创建DTO
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setOrderType(quickOrderDTO.getOrderType());
        createDTO.setStoreId(quickOrderDTO.getStoreId());
        createDTO.setTableId(quickOrderDTO.getTableId());
        createDTO.setDiningPeopleCount(quickOrderDTO.getDiningPeopleCount());
        createDTO.setCustomerPhone(quickOrderDTO.getCustomerPhone());
        createDTO.setItems(items);
        createDTO.setRemark(quickOrderDTO.getRemark());

        return createOrder(createDTO);
    }

    // ==================== 支付处理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(String orderId, OrderPayDTO payDTO) {
        log.info("开始支付订单, orderId: {}", orderId);

        OrderNew order = getAndValidateOrder(orderId);

        // 校验订单状态：必须是待确认或已确认且未完全支付
        if (!isPayableStatus(order.getOrderStatus())) {
            throw new RuntimeException("当前订单状态不允许支付");
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            throw new RuntimeException("订单已支付完成");
        }

        // 计算总支付金额
        long totalPayAmount = payDTO.getPayments().stream()
                .mapToLong(p -> p.getAmount())
                .sum();

        // VULN-06 修复：校验每笔支付金额必须为正数，防止负数支付绕过总金额校验
        // 攻击场景：提交 +1000 和 -900 两笔，总和 100 元绕过校验，但造成数据混乱
        for (OrderPayDTO.PaymentDetail payment : payDTO.getPayments()) {
            if (payment.getAmount() <= 0) {
                throw new RuntimeException("单笔支付金额必须大于0");
            }
        }

        // 验证支付金额不能超过应付金额
        long remainingAmount = order.getFinalAmount() - (order.getPaidAmount() != null ? order.getPaidAmount() : 0L);
        if (totalPayAmount > remainingAmount) {
            throw new RuntimeException("支付金额超过应付金额");
        }

        // 保存每笔支付记录
        for (OrderPayDTO.PaymentDetail payment : payDTO.getPayments()) {
            OrderPaymentRecordNew record = new OrderPaymentRecordNew();
            record.setOrderId(orderId);
            record.setPaymentMethod(payment.getPaymentMethod());
            record.setPaymentAmount(payment.getAmount());
            record.setTransactionNo(payment.getTransactionNo());
            record.setPaymentTime(LocalDateTime.now());
            record.setOperatorId(payDTO.getOperatorId());
            record.setRemark(payDTO.getRemark());
            orderPaymentRecordNewMapper.insert(record);
        }

        // 更新订单支付状态
        long newPaidAmount = (order.getPaidAmount() != null ? order.getPaidAmount() : 0L) + totalPayAmount;
        int newPaymentStatus = (newPaidAmount >= order.getFinalAmount()) ? 2 : 1; // 2已支付 1部分支付

        orderNewMapper.updatePaymentStatus(orderId, newPaymentStatus, newPaidAmount);

        // 如果全额支付，更新订单状态为已确认（制作中）
        if (newPaymentStatus == 2 && (order.getOrderStatus() == null || order.getOrderStatus() == 0)) {
            orderNewMapper.updateStatus(orderId, 1); // 待确认->已确认
        }

        // F6: 订单支付落资金流水（收入/销售收款）
        recordPaymentFundFlow(order, totalPayAmount);

        log.info("订单支付完成, orderId: {}, 支付金额: {}", orderId, totalPayAmount);
        return getOrderDetail(orderId);
    }

    /**
     * F6：订单支付后创建资金流水（收入/销售收款），让资金链可见订单收入。
     * 无可用银行账户时跳过（记录日志）；失败隔离不影响支付主流程。
     */
    private void recordPaymentFundFlow(OrderNew order, long amount) {
        try {
            if (fundFlowService == null || amount <= 0) {
                return;
            }
            Long accountId = getDefaultBankAccountId();
            if (accountId == null) {
                log.info("F6 无可用银行账户，跳过订单支付资金流水：orderId={}", order.getOrderId());
                return;
            }
            com.foodtraceability.dto.finance.FundFlowCreateDTO dto =
                    new com.foodtraceability.dto.finance.FundFlowCreateDTO();
            dto.setAccountId(accountId);
            dto.setFlowDirection(1); // 收入
            dto.setFlowCategory(1);  // 销售收款
            dto.setAmount(amount);
            dto.setCounterpartyName(order.getCustomerName() != null ? order.getCustomerName() : "散客");
            dto.setBusinessDate(java.time.LocalDate.now());
            dto.setRemark("订单支付 - " + (order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId()));
            fundFlowService.create(dto);
            log.info("F6 订单支付资金流水已创建：orderId={}, amount={}分", order.getOrderId(), amount);
        } catch (Exception e) {
            log.error("F6 订单支付资金流水创建失败：orderId={}, 错误={}", order.getOrderId(), e.getMessage());
        }
    }

    /** 取默认银行账户（第一个启用的），无则返回 null */
    private Long getDefaultBankAccountId() {
        try {
            java.util.List<com.foodtraceability.entity.finance.BankAccount> accounts = bankAccountService.list();
            for (com.foodtraceability.entity.finance.BankAccount account : accounts) {
                if (account.getStatus() == null || account.getStatus() == 1) {
                    return account.getAccountId();
                }
            }
        } catch (Exception e) {
            log.warn("F6 查询默认银行账户失败：{}", e.getMessage());
        }
        return null;
    }

    // ==================== 取消订单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(String orderId, String cancelReason) {
        log.info("取消订单, orderId: {}, 原因: {}", orderId, cancelReason);

        OrderNew order = getAndValidateOrder(orderId);

        // 校验是否可以取消：只有待确认和已确认状态的订单可以取消
        Integer status = order.getOrderStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("当前订单状态不允许取消");
        }

        // 如果已支付，需要退款逻辑
        if (order.getPaymentStatus() != null && order.getPaymentStatus() >= 1) {
            // TODO: 调用退款服务处理自动退款
            log.warn("订单已支付，需要处理退款逻辑, orderId: {}", orderId);
        }

        // 更新订单状态为已取消
        orderNewMapper.updateStatus(orderId, 3); // 3=已取消

        // 解锁桌台
        diningTableNewMapper.unlockTableByOrderId(orderId);

        log.info("订单取消成功, orderId: {}", orderId);
        return getOrderDetail(orderId);
    }

    // ==================== 退款处理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.OrderRefundRecordVO applyRefund(String orderId, OrderRefundDTO refundDTO) {
        log.info("申请退款, orderId: {}, 类型: {}", orderId, refundDTO.getRefundType());

        OrderNew order = getAndValidateOrder(orderId);

        // 校验退款类型和金额
        Long refundAmount = refundDTO.getRefundType() == 1 ? order.getFinalAmount() : refundDTO.getRefundAmount();

        // 创建退款记录
        OrderRefundRecordNew refundRecord = new OrderRefundRecordNew();
        refundRecord.setOrderId(orderId);
        refundRecord.setRefundType(refundDTO.getRefundType());
        refundRecord.setRefundAmount(refundAmount);
        refundRecord.setRefundReason(refundDTO.getRefundReason());
        refundRecord.setRefundMethod(refundDTO.getRefundMethod());
        refundRecord.setRefundStatus(0); // 0=待审核
        // DF-024 修复：保存退款明细ID列表（CSV格式），用于部分退款精确回补库存
        refundRecord.setPaymentChannelStatus(0); // 0=未提交
        refundRecord.setRefundItemIds(convertRefundItemIdsToCsv(refundDTO.getItemIds()));
        orderRefundRecordNewMapper.insert(refundRecord);

        // 如果是全额退款，直接更新订单状态
        if (refundDTO.getRefundType() == 1) {
            int newOrderStatus = 5; // 全额退款
            orderNewMapper.addRefundAmount(orderId, refundAmount, newOrderStatus);
        } else {
            // 部分退款，更新为部分退款状态
            orderNewMapper.addRefundAmount(orderId, refundAmount, 4); // 4=部分退款
        }

        log.info("退款申请提交成功, refundId: {}", refundRecord.getRefundId());
        return convertToRefundVO(refundRecord);
    }

    /**
     * 将退款明细ID列表转换为CSV字符串
     * @param itemIds 退款明细ID列表
     * @return CSV字符串（如 "id1,id2,id3"），空列表返回 null
     */
    private String convertRefundItemIdsToCsv(java.util.List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }
        return String.join(",", itemIds);
    }

    /**
     * 将CSV字符串解析为退款明细ID列表
     * @param csv 退款明细ID CSV字符串
     * @return 退款明细ID列表，空CSV返回空列表
     */
    private java.util.List<String> parseRefundItemIdsFromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId, boolean approved, Long approveUserId) {
        log.info("审批退款, refundId: {}, 同意: {}", refundId, approved);

        OrderRefundRecordNew refundRecord = orderRefundRecordNewMapper.selectById(refundId);
        if (refundRecord == null) {
            throw new RuntimeException("退款记录不存在");
        }

        if (approved) {
            // DF-022 修复：实现"退款记录持久化"方案
            // 由于暂未接入真实支付渠道 SDK，本次采用"退款记录持久化"方案：
            //   1. 标记退款记录为"已退款"（refund_status=3）
            //   2. 标记支付渠道状态为"待渠道处理"（payment_channel_status=1）
            //   3. 记录渠道提交时间
            //   4. 后续接入真实支付 SDK 时，再实现实际退款（更新 payment_channel_status 为成功/失败）
            refundRecord.setApproveUserId(approveUserId);
            refundRecord.setRefundStatus(3); // 3=已退款
            refundRecord.setPaymentChannelStatus(1); // 1=待渠道处理
            refundRecord.setPaymentChannelSubmitTime(LocalDateTime.now());
            refundRecord.setCompleteTime(LocalDateTime.now());

            // 退款通过时回补库存（仅当订单已完成且有关联门店时）
            OrderNew order = orderNewMapper.selectById(refundRecord.getOrderId());
            if (order != null && order.getStoreId() != null
                    && order.getOrderStatus() != null && order.getOrderStatus() == 2) {
                // DF-023 修复：库存回补失败必须抛出异常回滚主事务
                // 原实现 try/catch 静默吞异常，导致退款已审批但库存未回补的数据不一致
                // 现改为：库存回补失败抛出异常 → 主事务回滚 → 退款记录不更新为"已退款"
                restoreInventoryForRefund(order, refundRecord);
            }

            // 发布退款事件（DF-022 修复）：触发下游财务联动
            // 监听器 OrderRefundEventListener 会在主事务提交后（AFTER_COMMIT）异步：
            //   1. 生成退款支出财务流水（finance_record）
            //   2. （未来）生成红字凭证关联原销售凭证
            publishOrderRefundEvent(order, refundRecord);
        } else {
            refundRecord.setRefundStatus(2); // 2=已拒绝
            refundRecord.setApproveUserId(approveUserId);
        }

        orderRefundRecordNewMapper.updateById(refundRecord);
    }

    /**
     * 发布订单退款事件
     * 触发下游财务联动：退款支出流水、红字凭证等
     *
     * @param order 关联订单（可能为 null，如订单已被删除）
     * @param refundRecord 退款记录
     */
    private void publishOrderRefundEvent(OrderNew order, OrderRefundRecordNew refundRecord) {
        try {
            OrderRefundEvent event = new OrderRefundEvent();
            event.setRefundId(refundRecord.getRefundId());
            event.setOrderId(refundRecord.getOrderId());
            event.setOrderNumber(order != null ? order.getOrderCode() : null);
            event.setStoreId(order != null ? order.getStoreId() : null);
            event.setRefundType(refundRecord.getRefundType());
            event.setRefundAmount(refundRecord.getRefundAmount());
            event.setRefundReason(refundRecord.getRefundReason());
            event.setRefundMethod(refundRecord.getRefundMethod());
            event.setApproveUserId(refundRecord.getApproveUserId());
            event.setRefundItemIds(parseRefundItemIdsFromCsv(refundRecord.getRefundItemIds()));
            event.setPaymentChannelStatus(refundRecord.getPaymentChannelStatus());
            event.setCompleteTime(refundRecord.getCompleteTime());

            applicationEventPublisher.publishEvent(event);
            log.info("已发布订单退款事件: refundId={}, orderId={}",
                    refundRecord.getRefundId(), refundRecord.getOrderId());
        } catch (Exception e) {
            // 事件发布失败仅记录日志，不影响主事务
            // （与项目现有事件发布模式保持一致，DF-038 待后续引入事件持久化机制后统一解决）
            log.error("发布订单退款事件失败: refundId={}, 错误={}",
                    refundRecord.getRefundId(), e.getMessage(), e);
        }
    }

    // ==================== 订单状态变更 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderId) {
        log.info("确认订单, orderId: {}", orderId);
        OrderNew order = getAndValidateOrder(orderId);
        if (order.getOrderStatus() != null && order.getOrderStatus() != 0) {
            throw new RuntimeException("只有待确认状态的订单才能确认");
        }
        orderNewMapper.updateStatus(orderId, 1); // 1=已确认/制作中
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(String orderId) {
        log.info("完成订单, orderId: {}", orderId);
        OrderNew order = getAndValidateOrder(orderId);
        if (order.getOrderStatus() == null || (order.getOrderStatus() != 1 && order.getOrderStatus() != 2)) {
            throw new RuntimeException("当前订单状态无法标记完成");
        }

        // 扣减门店库存并结转成本（仅当订单有关联门店时）
        // DF-009 修复：原 totalCost 仅 log 后丢弃，现返回并持久化到 cost_record 表
        long totalCost = 0L;
        if (order.getStoreId() != null) {
            totalCost = deductInventoryAndCalculateCost(order);
            // 同步持久化订单成本到 cost_record 表（同事务强一致，确保成本数据不丢失）
            persistOrderCost(order, totalCost);
        }

        orderNewMapper.updateStatus(orderId, 2); // 2=已完成
        // 解锁桌台
        diningTableNewMapper.unlockTableByOrderId(orderId);

        // DF-008 修复：原 completeOrder 未发布 OrderCompletedEvent，导致订单完成→财务链路完全断裂
        // 监听器 OrderCompletedEventListener 使用 @TransactionalEventListener(AFTER_COMMIT) + @Async
        // 异常隔离，发布失败不影响主事务
        publishOrderCompletedEvent(order, totalCost);
    }

    /**
     * 持久化订单成本到 cost_record 表（DF-009 修复）
     * 同步同事务写入，确保成本数据不丢失
     * @param order 订单实体
     * @param totalCost 订单总成本（单位：分）
     */
    private void persistOrderCost(OrderNew order, long totalCost) {
        if (totalCost <= 0) {
            log.info("订单成本为0，跳过持久化, orderId: {}", order.getOrderId());
            return;
        }
        try {
            CostRecordCreateDTO dto = new CostRecordCreateDTO();
            dto.setCostType(1); // 1-食材成本
            dto.setCostCenterId(order.getStoreId());
            dto.setAmount(totalCost); // 单位：分
            dto.setPeriod(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            dto.setRemark("订单材料成本 - " + order.getOrderCode());
            dto.setCalculationMethod(1); // 1-实际发生
            costRecordService.create(dto);
            log.info("订单成本已持久化, orderId: {}, totalCost: {}分", order.getOrderId(), totalCost);
        } catch (Exception e) {
            log.error("持久化订单成本失败, orderId: {}, error: {}", order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("持久化订单成本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发布订单完成事件（DF-008 修复）
     * 触发 OrderCompletedEventListener 联动生成财务收入记录等
     * 异常隔离，发布失败不影响主事务
     * @param order 订单实体
     * @param totalCost 订单总成本（单位：分）
     */
    private void publishOrderCompletedEvent(OrderNew order, long totalCost) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent();
            event.setEventId("EVT" + System.currentTimeMillis());
            event.setOrderId(order.getOrderId());
            event.setOrderNumber(order.getOrderCode());
            event.setOrderType(order.getOrderType());
            event.setStoreId(order.getStoreId());
            event.setCompleteTime(LocalDateTime.now());
            event.setOrderTime(order.getCreateTime());

            // 金额：订单表存分（Long），事件语义为元（BigDecimal）
            Long finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : 0L;
            BigDecimal amountYuan = BigDecimal.valueOf(finalAmount)
                    .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
            event.setOrderAmount(amountYuan);
            event.setActualAmount(amountYuan);

            // 成本：totalCost 是分（long），事件语义为元（BigDecimal）
            // 注意：不设置 materialCost 字段，避免监听器重复记录成本（成本已在 persistOrderCost 中同步持久化）
            if (totalCost > 0) {
                BigDecimal costYuan = BigDecimal.valueOf(totalCost)
                        .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
                event.setTotalCost(costYuan);
            }

            applicationEventPublisher.publishEvent(event);
            log.info("已发布订单完成事件: orderId={}, totalCost={}分", order.getOrderId(), totalCost);
        } catch (Exception e) {
            log.error("发布订单完成事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 扣减门店库存并计算订单成本
     * 按菜品BOM配方展开，扣减每种原料的库存数量，同时结转对应的成本
     * @return 订单总成本（单位：分）
     */
    private long deductInventoryAndCalculateCost(OrderNew order) {
        Long storeId = order.getStoreId();
        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());

        // 收集所有需要扣减的原料及其数量（按原料ID汇总）
        Map<Long, BigDecimal> materialDeductionMap = new HashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();

        for (OrderItemNew item : orderItems) {
            // 跳过已退款的菜品
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                continue;
            }

            Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

            if (item.getProductType() != null && item.getProductType() == 1) {
                // 单品：查找配方
                if (item.getFoodId() != null) {
                    addFoodMaterialDeductions(
                            item.getFoodId(),
                            quantity,
                            materialDeductionMap,
                            materialNameMap);
                }
            } else if (item.getProductType() != null && item.getProductType() == 2) {
                // 套餐：展开套餐内的所有菜品，再按菜品配方扣减
                if (item.getFoodId() != null) {
                    List<ComboIngredient> comboIngredients = comboIngredientMapper.selectByComboId(item.getFoodId());
                    for (ComboIngredient ingredient : comboIngredients) {
                        if (ingredient.getFoodId() == null) continue;
                        BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                                ? ingredient.getQuantity() : BigDecimal.ONE;
                        // 套餐中菜品的实际数量 = 套餐中该菜品数量 × 套餐购买数量
                        int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                        if (actualFoodQty <= 0) continue;

                        try {
                            Long foodIdLong = Long.parseLong(ingredient.getFoodId());
                            addFoodMaterialDeductions(
                                    foodIdLong,
                                    actualFoodQty,
                                    materialDeductionMap,
                                    materialNameMap);
                        } catch (NumberFormatException e) {
                            log.warn("套餐菜品ID格式异常, comboId: {}, foodId: {}", item.getFoodId(), ingredient.getFoodId());
                        }
                    }
                }
            }
        }

        // 执行库存扣减
        long totalCost = 0L;
        for (Map.Entry<Long, BigDecimal> entry : materialDeductionMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            String materialName = materialNameMap.get(materialId);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            try {
                // 扣减库存，返回出库成本（分）
                Long cost = storeInventoryService.decreaseStock(
                        String.valueOf(storeId),
                        materialId,
                        qty,
                        2,
                        "订单销售出库 - 订单:" + order.getOrderCode());
                if (cost != null) {
                    totalCost += cost;
                }
                log.info("订单扣减库存, orderId: {}, materialId: {}, materialName: {}, qty: {}, cost: {}",
                        order.getOrderId(), materialId, materialName, qty, cost);
            } catch (Exception e) {
                log.error("订单扣减库存失败, orderId: {}, materialId: {}, error: {}",
                        order.getOrderId(), materialId, e.getMessage());
                throw new RuntimeException("库存扣减失败：" + materialName + " - " + e.getMessage(), e);
            }
        }

        log.info("订单成本计算完成, orderId: {}, totalCost: {}分", order.getOrderId(), totalCost);
        return totalCost;
    }

    /**
     * 获取订单明细列表
     */
    private List<OrderItemNew> getOrderItems(String orderId) {
        LambdaQueryWrapper<OrderItemNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItemNew::getOrderId, orderId);
        return orderItemNewMapper.selectList(wrapper);
    }

    /**
     * 获取菜品配方列表
     */
    private List<DishRecipeNew> getDishRecipes(Long foodId) {
        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getFoodId, foodId);
        return dishRecipeNewMapper.selectList(wrapper);
    }

    /**
     * 累加某个菜品的原料扣减需求
     * @param foodId 菜品ID
     * @param quantity 菜品数量
     * @param materialDeductionMap 原料扣减汇总Map（累加）
     * @param materialNameMap 原料名称Map
     */
    private void addFoodMaterialDeductions(
            Long foodId,
            int quantity,
            Map<Long, BigDecimal> materialDeductionMap,
            Map<Long, String> materialNameMap) {
        List<DishRecipeNew> recipes = getDishRecipes(foodId);
        for (DishRecipeNew recipe : recipes) {
            Long materialId = recipe.getMaterialId();
            if (materialId == null) continue;

            BigDecimal requiredQty = recipe.getRequiredQuantity() != null
                    ? recipe.getRequiredQuantity() : BigDecimal.ZERO;
            BigDecimal lossRate = recipe.getLossRate() != null
                    ? recipe.getLossRate() : BigDecimal.ZERO;

            // 实际用量 = 标准用量 × 数量 × (1 + 损耗率)
            BigDecimal actualQty = requiredQty
                    .multiply(new BigDecimal(quantity))
                    .multiply(BigDecimal.ONE.add(lossRate.divide(new BigDecimal(100))));

            materialDeductionMap.merge(materialId, actualQty, BigDecimal::add);
            materialNameMap.put(materialId, recipe.getMaterialName());
        }
    }

    /**
     * 退款时回补库存
     * 全额退款（refundType=1）：回补所有未退款菜品的库存
     * 部分退款（refundType=2）：仅回补退款明细中指定菜品的库存（DF-024 修复）
     *
     * <p>DF-023 修复：库存回补失败抛出异常，由调用方决定是否回滚主事务。
     * 原实现 try/catch 静默吞异常，导致退款已审批但库存未回补的数据不一致。
     * 现改为：库存回补失败 → 抛出 RuntimeException → 主事务回滚 → 退款记录不更新为"已退款"。</p>
     *
     * @param order 订单实体
     * @param refundRecord 退款记录（包含 refundType 与 refundItemIds）
     */
    private void restoreInventoryForRefund(OrderNew order, OrderRefundRecordNew refundRecord) {
        Long storeId = order.getStoreId();
        Integer refundType = refundRecord.getRefundType();

        if (refundType == null) {
            return;
        }

        // 收集需要回补的原料及其数量（按原料ID汇总）
        Map<Long, BigDecimal> materialRestoreMap = new HashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();

        if (refundType == 1) {
            // 全额退款：回补所有未退款菜品的库存
            collectMaterialsForAllOrderItems(order, materialRestoreMap, materialNameMap);
        } else if (refundType == 2) {
            // DF-024 修复：部分退款 - 仅回补退款明细中指定的菜品库存
            collectMaterialsForRefundItems(order, refundRecord, materialRestoreMap, materialNameMap);
        } else {
            log.warn("未知退款类型，跳过库存回补: refundId={}, refundType={}",
                    refundRecord.getRefundId(), refundType);
            return;
        }

        // 执行库存回补
        // DF-023 修复：回补失败抛出异常，回滚主事务
        for (Map.Entry<Long, BigDecimal> entry : materialRestoreMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            String materialName = materialNameMap.get(materialId);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 获取当前库存记录，用当前单位成本回补（保持单位成本不变）
            StoreInventory currentInventory = storeInventoryService.getByStoreAndMaterial(
                    String.valueOf(storeId), materialId);
            Long currentUnitCost = (currentInventory != null && currentInventory.getUnitCost() != null)
                    ? currentInventory.getUnitCost() : 0L;

            try {
                storeInventoryService.increaseStock(
                        String.valueOf(storeId),
                        materialId,
                        materialName,
                        qty,
                        null,
                        currentUnitCost,
                        1,
                        "订单退款回补库存 - 订单:" + order.getOrderCode());
                log.info("退款回补库存, orderId: {}, materialId: {}, materialName: {}, qty: {}, unitCost: {}分",
                        order.getOrderId(), materialId, materialName, qty, currentUnitCost);
            } catch (Exception e) {
                // DF-023 修复：抛出异常回滚主事务，避免退款已审批但库存未回补
                log.error("退款回补库存失败, orderId: {}, materialId: {}, error: {}",
                        order.getOrderId(), materialId, e.getMessage());
                throw new RuntimeException("退款回补库存失败：" + materialName
                        + "（materialId=" + materialId + "）- " + e.getMessage(), e);
            }
        }

        log.info("退款回补库存完成, orderId: {}, refundType: {}, materialCount: {}",
                order.getOrderId(), refundType, materialRestoreMap.size());
    }

    /**
     * 收集全额退款需要回补的所有原料（遍历订单全部明细）
     * @param order 订单实体
     * @param materialRestoreMap 原料回补汇总Map（累加）
     * @param materialNameMap 原料名称Map
     */
    private void collectMaterialsForAllOrderItems(OrderNew order,
                                                  Map<Long, BigDecimal> materialRestoreMap,
                                                  Map<Long, String> materialNameMap) {
        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());
        for (OrderItemNew item : orderItems) {
            // 跳过厨房状态为已退款（4）的菜品
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                continue;
            }
            collectMaterialsFromOrderItem(item, materialRestoreMap, materialNameMap);
        }
    }

    /**
     * 收集部分退款需要回补的原料（仅遍历退款明细中指定的菜品）
     * @param order 订单实体
     * @param refundRecord 退款记录（包含 refundItemIds CSV）
     * @param materialRestoreMap 原料回补汇总Map（累加）
     * @param materialNameMap 原料名称Map
     */
    private void collectMaterialsForRefundItems(OrderNew order,
                                                OrderRefundRecordNew refundRecord,
                                                Map<Long, BigDecimal> materialRestoreMap,
                                                Map<Long, String> materialNameMap) {
        List<String> refundItemIds = parseRefundItemIdsFromCsv(refundRecord.getRefundItemIds());
        if (refundItemIds.isEmpty()) {
            log.warn("部分退款未提供退款明细ID列表，跳过库存回补: refundId={}, orderId={}",
                    refundRecord.getRefundId(), order.getOrderId());
            return;
        }

        List<OrderItemNew> orderItems = getOrderItems(order.getOrderId());
        // 构建 itemId → OrderItemNew 索引，避免 N+1 查询
        Map<String, OrderItemNew> orderItemMap = new HashMap<>();
        for (OrderItemNew item : orderItems) {
            if (item.getItemId() != null) {
                orderItemMap.put(item.getItemId(), item);
            }
        }

        int matchedCount = 0;
        for (String itemId : refundItemIds) {
            OrderItemNew item = orderItemMap.get(itemId);
            if (item == null) {
                log.warn("退款明细ID在订单中不存在，跳过: orderId={}, itemId={}",
                        order.getOrderId(), itemId);
                continue;
            }
            // 跳过厨房状态为已退款（4）的菜品（防止重复回补）
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                log.info("菜品已退款，跳过库存回补: orderId={}, itemId={}",
                        order.getOrderId(), itemId);
                continue;
            }
            collectMaterialsFromOrderItem(item, materialRestoreMap, materialNameMap);
            matchedCount++;
        }

        log.info("部分退款匹配明细数: refundId={}, requested={}, matched={}",
                refundRecord.getRefundId(), refundItemIds.size(), matchedCount);
    }

    /**
     * 从单个订单明细中收集需要回补的原料
     * 支持单品（productType=1）和套餐（productType=2）两种类型
     * @param item 订单明细
     * @param materialRestoreMap 原料回补汇总Map（累加）
     * @param materialNameMap 原料名称Map
     */
    private void collectMaterialsFromOrderItem(OrderItemNew item,
                                                Map<Long, BigDecimal> materialRestoreMap,
                                                Map<Long, String> materialNameMap) {
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

        if (item.getProductType() != null && item.getProductType() == 1) {
            // 单品
            if (item.getFoodId() != null) {
                addFoodMaterialDeductions(
                        item.getFoodId(),
                        quantity,
                        materialRestoreMap,
                        materialNameMap);
            }
        } else if (item.getProductType() != null && item.getProductType() == 2) {
            // 套餐
            if (item.getFoodId() != null) {
                List<ComboIngredient> comboIngredients = comboIngredientMapper.selectByComboId(item.getFoodId());
                for (ComboIngredient ingredient : comboIngredients) {
                    if (ingredient.getFoodId() == null) continue;
                    BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                            ? ingredient.getQuantity() : BigDecimal.ONE;
                    int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                    if (actualFoodQty <= 0) continue;

                    try {
                        Long foodIdLong = Long.parseLong(ingredient.getFoodId());
                        addFoodMaterialDeductions(
                                foodIdLong,
                                actualFoodQty,
                                materialRestoreMap,
                                materialNameMap);
                    } catch (NumberFormatException e) {
                        log.warn("套餐菜品ID格式异常, comboId: {}, foodId: {}", item.getFoodId(), ingredient.getFoodId());
                    }
                }
            }
        }
    }

    // ==================== 查询方法 ====================

    @Override
    public OrderVO getOrderDetail(String orderId) {
        OrderNew order = getAndValidateOrder(orderId);
        return convertToOrderVO(order);
    }

    @Override
    public Page<OrderVO> queryOrders(OrderQueryDTO queryDTO) {
        Page<OrderNew> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<OrderNew> wrapper = buildQueryWrapper(queryDTO);
        page = orderNewMapper.selectPage(page, wrapper);

        // 转换为VO列表
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertToSimpleOrderVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 分页查询POS终端订单（orders_legacy 表）
     * 与 queryOrders（查询 orders 表）互补，让管理端能查看POS收银端创建的订单。
     * 内部完成订单状态/支付方式/金额单位（元→分）的映射转换。
     */
    @Override
    public Page<OrderVO> queryPosOrders(OrderQueryDTO queryDTO) {
        Page<com.foodtraceability.entity.Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<com.foodtraceability.entity.Order> wrapper = buildPosQueryWrapper(queryDTO);
        page = posOrderMapper.selectPage(page, wrapper);

        // 转换为VO列表（包含状态码/支付方式/金额单位映射）
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertPosOrderToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 根据订单编号获取POS终端订单详情（含菜品明细）
     * 用于管理端订单中心查看POS收银端订单的完整明细（数据追溯）。
     *
     * 实现要点：
     * 1. 通过 orderNumber 查询 orders_legacy 表获取订单主信息
     * 2. 通过 orderId 查询 order_items_legacy 表获取菜品明细列表
     * 3. 调用 convertPosOrderToVO 完成状态/支付方式/金额单位的统一映射
     * 4. 补充 items 列表（OrderItemVO 格式，金额转分）
     */
    @Override
    public OrderVO getPosOrderDetail(String orderNumber) {
        log.info("查询POS订单详情, orderNumber: {}", orderNumber);
        if (orderNumber == null || orderNumber.isEmpty()) {
            throw new RuntimeException("订单编号不能为空");
        }
        // 按订单编号查询 orders_legacy 表
        com.foodtraceability.entity.Order posOrder = posOrderMapper.selectOne(
                new LambdaQueryWrapper<com.foodtraceability.entity.Order>()
                        .eq(com.foodtraceability.entity.Order::getOrderNumber, orderNumber));
        if (posOrder == null) {
            throw new RuntimeException("POS订单不存在: " + orderNumber);
        }
        // 转换为 OrderVO（含状态/支付方式/金额单位映射，但不含明细）
        OrderVO vo = convertPosOrderToVO(posOrder);

        // 查询订单明细（order_items_legacy 表）
        List<OrderItem> posItems = posOrderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, posOrder.getOrderId()));
        // 转换为 OrderItemVO（金额 元→分，与 OrderVO 单位保持一致）
        List<OrderVO.OrderItemVO> itemVos = posItems.stream()
                .map(this::convertPosItemToVO)
                .collect(Collectors.toList());
        vo.setItems(itemVos);

        log.info("查询POS订单详情成功, orderNumber: {}, 明细数: {}", orderNumber, itemVos.size());
        return vo;
    }

    /**
     * 将POS订单项（order_items_legacy）转换为管理端 OrderItemVO
     * 金额单位：元（BigDecimal）→ 分（Long），与 OrderVO 保持一致
     */
    private OrderVO.OrderItemVO convertPosItemToVO(OrderItem item) {
        OrderVO.OrderItemVO vo = new OrderVO.OrderItemVO();
        vo.setItemId(item.getOrderItemId());
        // POS订单项无产品类型字段，默认为单品
        vo.setProductType(1);
        vo.setProductTypeName("单品");
        vo.setProductName(item.getFoodName());
        vo.setSpecification(item.getSpecification());
        // 元转分
        if (item.getUnitPrice() != null) {
            vo.setUnitPrice(item.getUnitPrice().multiply(new BigDecimal("100")).longValue());
        }
        vo.setQuantity(item.getQuantity());
        if (item.getSubtotalAmount() != null) {
            vo.setAmount(item.getSubtotalAmount().multiply(new BigDecimal("100")).longValue());
        }
        return vo;
    }

    /**
     * 构建POS订单（orders_legacy）查询条件
     * 字段名使用 orders_legacy 表的实际列名（大写下划线）
     */
    private QueryWrapper<com.foodtraceability.entity.Order> buildPosQueryWrapper(OrderQueryDTO queryDTO) {
        QueryWrapper<com.foodtraceability.entity.Order> wrapper = new QueryWrapper<>();
        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            wrapper.like("ORDER_NUMBER", queryDTO.getOrderCode());
        }
        if (queryDTO.getOrderType() != null) {
            wrapper.eq("ORDER_TYPE", queryDTO.getOrderType());
        }
        // 订单状态映射：管理端状态码 → POS端状态码
        if (queryDTO.getOrderStatus() != null) {
            List<Integer> posStatusList = mapAdminStatusToPosStatus(queryDTO.getOrderStatus());
            if (posStatusList.size() == 1) {
                wrapper.eq("ORDER_STATUS", posStatusList.get(0));
            } else {
                wrapper.in("ORDER_STATUS", posStatusList);
            }
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge("CREATE_TIME", parseDateTime(queryDTO.getStartTime()));
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le("CREATE_TIME", parseDateTime(queryDTO.getEndTime()));
        }
        // 金额条件：管理端是分，orders_legacy 是元（numeric），需要转换
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("ACTUAL_AMOUNT", new BigDecimal(queryDTO.getMinAmount()).movePointLeft(2));
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("ACTUAL_AMOUNT", new BigDecimal(queryDTO.getMaxAmount()).movePointLeft(2));
        }
        // 门店ID过滤：orders_legacy.STORE_ID 是 String 类型（与 stores_new.store_id Long 关联）
        if (queryDTO.getStoreId() != null && !queryDTO.getStoreId().isEmpty()) {
            wrapper.eq("STORE_ID", queryDTO.getStoreId());
        }
        wrapper.orderByDesc("CREATE_TIME");
        return wrapper;
    }

    /**
     * 管理端订单状态码 → POS端订单状态码映射
     * 管理端：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价
     * POS端：0待支付 -1支付中 1已支付 2待配送 3配送中 4已完成 5已取消 6退款中 7已退款
     */
    private List<Integer> mapAdminStatusToPosStatus(Integer adminStatus) {
        List<Integer> posStatusList = new ArrayList<>();
        switch (adminStatus) {
            case 0: // 待确认 → POS待支付/支付中
                posStatusList.add(0);
                posStatusList.add(-1);
                break;
            case 1: // 已确认 → POS已支付/待配送/配送中
                posStatusList.add(1);
                posStatusList.add(2);
                posStatusList.add(3);
                break;
            case 2: // 已完成 → POS已完成
                posStatusList.add(4);
                break;
            case 3: // 已取消 → POS已取消
                posStatusList.add(5);
                break;
            case 4: // 部分退款 → POS退款中
                posStatusList.add(6);
                break;
            case 5: // 全额退款 → POS已退款
                posStatusList.add(7);
                break;
            case 6: // 待评价 → POS已完成（管理端独有，映射到POS已完成）
                posStatusList.add(4);
                break;
            default:
                // 不限制状态
        }
        return posStatusList;
    }

    /**
     * 将POS订单实体（orders_legacy）转换为管理端OrderVO
     * 包含：订单状态/支付方式/金额单位（元→分）的映射
     */
    private OrderVO convertPosOrderToVO(com.foodtraceability.entity.Order posOrder) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(posOrder.getOrderId());
        vo.setOrderCode(posOrder.getOrderNumber());
        // 订单类型映射：POS端 0堂食1外卖2自提 → 管理端 1堂食2外卖3自提4打包
        Integer adminOrderType = mapPosOrderTypeToAdmin(posOrder.getOrderType());
        vo.setOrderType(adminOrderType);
        vo.setOrderTypeName(getOrderTypeName(adminOrderType));
        vo.setOrderSource(posOrder.getOrderSource());
        vo.setCustomerName(posOrder.getContactName());
        vo.setCustomerPhone(posOrder.getContactPhone());
        vo.setRemark(posOrder.getRemarks());
        vo.setCancelReason(posOrder.getCancelReason());
        vo.setDeliveryAddress(posOrder.getDeliveryAddress());

        // 订单状态映射：POS端 → 管理端
        Integer posStatus = posOrder.getOrderStatus();
        Integer adminStatus = mapPosStatusToAdminStatus(posStatus);
        vo.setOrderStatus(adminStatus);
        vo.setOrderStatusName(getOrderStatusName(adminStatus));

        // 支付状态：POS已支付(1)/待配送(2)/配送中(3)/已完成(4) → 已支付(2)；已退款(7)/退款中(6) → 已退款(3)；其他 → 未支付(0)
        Integer paymentStatus = mapPosStatusToPaymentStatus(posStatus);
        vo.setPaymentStatus(paymentStatus);
        vo.setPaymentStatusName(getPaymentStatusName(paymentStatus));

        // 支付方式映射：POS端 → 管理端
        // POS: 0微信 1支付宝 2现金 3银行卡 4余额
        // 管理端: 1现金 2微信 3支付宝 4银行卡 5积分(余额) 6混合支付
        Integer posPayMethod = posOrder.getPaymentMethod();
        Integer adminPayMethod = null;
        if (posPayMethod != null) {
            switch (posPayMethod) {
                case 0: adminPayMethod = 2; break; // 微信
                case 1: adminPayMethod = 3; break; // 支付宝
                case 2: adminPayMethod = 1; break; // 现金
                case 3: adminPayMethod = 4; break; // 银行卡
                case 4: adminPayMethod = 5; break; // 余额→积分
                default: adminPayMethod = null;
            }
        }
        vo.setPaymentMethodName(adminPayMethod != null ? getPaymentMethodName(adminPayMethod) : "未支付");

        // 金额转换：orders_legacy 是元（BigDecimal），OrderVO 是分（Long）
        BigDecimal actualAmount = posOrder.getActualAmount() != null ? posOrder.getActualAmount() : posOrder.getOrderAmount();
        if (actualAmount != null) {
            long fenAmount = actualAmount.multiply(new BigDecimal("100")).longValue();
            vo.setTotalAmount(fenAmount);
            vo.setFinalAmount(fenAmount);
            vo.setPaidAmount(paymentStatus != null && paymentStatus == 2 ? fenAmount : 0L);
        }
        if (posOrder.getDiscountAmount() != null) {
            vo.setDiscountAmount(posOrder.getDiscountAmount().multiply(new BigDecimal("100")).longValue());
        }
        if (posOrder.getRefundAmount() != null && posOrder.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            vo.setRefundAmount(posOrder.getRefundAmount().multiply(new BigDecimal("100")).longValue());
        }

        vo.setCreateTime(posOrder.getCreateTime());
        vo.setUpdateTime(posOrder.getUpdateTime());
        // 门店信息：POS端订单关联的门店
        vo.setStoreId(posOrder.getStoreId());
        vo.setStoreName(posOrder.getStoreName() != null ? posOrder.getStoreName() : "中心旗舰店");
        return vo;
    }

    /**
     * POS端订单状态码 → 管理端订单状态码映射
     */
    private Integer mapPosStatusToAdminStatus(Integer posStatus) {
        if (posStatus == null) return 0;
        switch (posStatus) {
            case 0: case -1: return 0; // 待支付/支付中 → 待确认
            case 1: case 2: case 3: return 1; // 已支付/待配送/配送中 → 已确认
            case 4: return 2; // 已完成 → 已完成
            case 5: return 3; // 已取消 → 已取消
            case 6: return 4; // 退款中 → 部分退款
            case 7: return 5; // 已退款 → 全额退款
            default: return 0;
        }
    }

    /**
     * POS端订单状态码 → 管理端支付状态码映射
     */
    private Integer mapPosStatusToPaymentStatus(Integer posStatus) {
        if (posStatus == null) return 0;
        switch (posStatus) {
            case 1: case 2: case 3: case 4: return 2; // 已支付/待配送/配送中/已完成 → 已支付
            case 6: case 7: return 3; // 退款中/已退款 → 已退款
            default: return 0; // 待支付/支付中/已取消 → 未支付
        }
    }

    @Override
    public PageResult<OrderRefundListVO> queryRefunds(OrderRefundQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;

        LambdaQueryWrapper<OrderRefundRecordNew> wrapper = new LambdaQueryWrapper<>();
        // 退款状态过滤（实体取值：0待审核 1已同意 2已拒绝 3已退款）
        if (queryDTO.getRefundStatus() != null) {
            wrapper.eq(OrderRefundRecordNew::getRefundStatus, queryDTO.getRefundStatus());
        }
        // 申请时间范围过滤
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge(OrderRefundRecordNew::getCreateTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le(OrderRefundRecordNew::getCreateTime, queryDTO.getEndTime());
        }
        // 订单编号模糊过滤：退款记录仅存 orderId，需先查订单获取 orderId 集合
        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            QueryWrapper<OrderNew> orderWrapper = new QueryWrapper<>();
            orderWrapper.like("order_code", queryDTO.getOrderCode());
            List<OrderNew> matchedOrders = orderNewMapper.selectList(orderWrapper);
            if (matchedOrders.isEmpty()) {
                return new PageResult<>(0L, new ArrayList<>(), (long) page, (long) size);
            }
            List<String> orderIds = matchedOrders.stream()
                    .map(OrderNew::getOrderId)
                    .collect(Collectors.toList());
            wrapper.in(OrderRefundRecordNew::getOrderId, orderIds);
        }
        wrapper.orderByDesc(OrderRefundRecordNew::getCreateTime);

        Page<OrderRefundRecordNew> refundPage = new Page<>(page, size);
        refundPage = orderRefundRecordNewMapper.selectPage(refundPage, wrapper);

        // 批量查询关联订单编号，避免 N+1 查询
        List<String> orderIdList = refundPage.getRecords().stream()
                .map(OrderRefundRecordNew::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> orderCodeMap = new HashMap<>();
        if (!orderIdList.isEmpty()) {
            List<OrderNew> orders = orderNewMapper.selectList(
                    new QueryWrapper<OrderNew>().in("order_id", orderIdList));
            for (OrderNew o : orders) {
                orderCodeMap.put(o.getOrderId(), o.getOrderCode());
            }
        }

        List<OrderRefundListVO> voList = refundPage.getRecords().stream()
                .map(record -> convertToRefundListVO(record, orderCodeMap))
                .collect(Collectors.toList());

        return new PageResult<>(refundPage.getTotal(), voList, refundPage.getCurrent(), refundPage.getSize());
    }

    /**
     * 转换退款记录为列表VO
     * @param record 退款记录实体
     * @param orderCodeMap 订单ID到订单编号的映射
     * @return 退款列表VO
     */
    private OrderRefundListVO convertToRefundListVO(OrderRefundRecordNew record, Map<String, String> orderCodeMap) {
        OrderRefundListVO vo = new OrderRefundListVO();
        vo.setRefundId(record.getRefundId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderCode(orderCodeMap.get(record.getOrderId()));
        vo.setRefundAmount(record.getRefundAmount());
        vo.setRefundReason(record.getRefundReason());
        vo.setRefundStatus(record.getRefundStatus());
        vo.setRefundStatusName(getRefundStatusName(record.getRefundStatus()));
        vo.setCreateTime(record.getCreateTime());
        vo.setCompleteTime(record.getCompleteTime());
        return vo;
    }

    @Override
    public TodayStatisticsVO getTodayStatistics() {
        TodayStatisticsVO stats = new TodayStatisticsVO();
        // 订单统计聚合：orders（管理端订单） + orders_legacy（POS端订单，主要数据来源）
        // POS端产生的交易行为是管理端获取订单数据的主要来源，必须包含在内
        long todayOrdersAdmin = orderNewMapper.countTodayOrders();
        long todayOrdersPos = posOrderMapper.countTodayPosOrders();
        stats.setTotalOrders(todayOrdersAdmin + todayOrdersPos);

        // 销售额聚合（单位：分）
        long todaySalesAdmin = orderNewMapper.sumTodaySales();
        long todaySalesPos = posOrderMapper.sumTodayPosSales();
        stats.setTotalSalesAmount(todaySalesAdmin + todaySalesPos);

        // 已完成订单（orders.order_status=2 + orders_legacy.order_status=4）
        long completedOrdersAdmin = countOrdersByStatus(2);
        long completedOrdersPos = posOrderMapper.countTodayPosCompletedOrders();
        stats.setCompletedOrders(completedOrdersAdmin + completedOrdersPos);

        // 已取消订单（orders.order_status=3 + orders_legacy.order_status=5）
        long cancelledOrdersAdmin = countOrdersByStatus(3);
        long cancelledOrdersPos = posOrderMapper.countTodayPosCancelledOrders();
        stats.setCancelledOrders(cancelledOrdersAdmin + cancelledOrdersPos);
        return stats;
    }

    @Override
    public Map<String, Object> getShiftSummary(Long cashierUserId) {
        Map<String, Object> summary = new HashMap<>();
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();
        wrapper.eq("cashier_user_id", cashierUserId);
        wrapper.ge("create_time", LocalDateTime.now().with(java.time.LocalTime.MIN));
        wrapper.eq("payment_status", 2); // 已支付
        Long totalCount = orderNewMapper.selectCount(wrapper);
        summary.put("totalOrders", totalCount);
        // TODO: 补充更多交接班统计信息
        return summary;
    }

    @Override
    public void updateItemKitchenStatus(String itemId, Integer kitchenStatus) {
        orderItemNewMapper.updateKitchenStatus(itemId, kitchenStatus);
    }

    @Override
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        // 订单统计聚合：orders（管理端订单） + orders_legacy（POS端订单，主要数据来源）
        // POS端产生的交易行为是管理端获取订单数据的主要来源，必须包含在内
        long totalOrdersAdmin = orderNewMapper.countAllOrders();
        long totalOrdersPos = posOrderMapper.countAllPosOrders();
        stats.put("totalOrders", totalOrdersAdmin + totalOrdersPos);

        long completedOrdersAdmin = orderNewMapper.countAllCompletedOrders();
        long completedOrdersPos = posOrderMapper.countAllPosCompletedOrders();
        stats.put("completedOrders", completedOrdersAdmin + completedOrdersPos);

        long todayOrdersAdmin = orderNewMapper.countTodayOrders();
        long todayOrdersPos = posOrderMapper.countTodayPosOrders();
        stats.put("todayOrders", todayOrdersAdmin + todayOrdersPos);

        long totalSalesAdmin = orderNewMapper.sumAllSales();
        long totalSalesPos = posOrderMapper.sumAllPosSales();
        stats.put("totalSales", totalSalesAdmin + totalSalesPos);

        long todaySalesAdmin = orderNewMapper.sumTodaySales();
        long todaySalesPos = posOrderMapper.sumTodayPosSales();
        stats.put("todaySales", todaySalesAdmin + todaySalesPos);
        return stats;
    }

    @Override
    public OrderRefundStatsVO getRefundStats() {
        OrderRefundStatsVO stats = new OrderRefundStatsVO();
        stats.setPendingCount(orderRefundRecordNewMapper.countPendingRefunds());
        stats.setRefundedAmount(orderRefundRecordNewMapper.sumRefundedAmount());

        // 退款率 = 退款单数 / 总订单数 * 100
        long totalOrders = orderNewMapper.countAllOrders();
        long totalRefunds = orderRefundRecordNewMapper.countAllRefunds();
        double refundRate = totalOrders > 0
                ? (double) totalRefunds / totalOrders * 100.0
                : 0.0;
        stats.setRefundRate(Math.round(refundRate * 100.0) / 100.0);

        stats.setAvgProcessHours(orderRefundRecordNewMapper.avgRefundProcessHours());
        return stats;
    }

    @Override
    public List<com.foodtraceability.dto.order.DailyStatsVO> getDailyStats(String startDate, String endDate, String storeName) {
        // 默认查询近7天（含今天）
        LocalDate end = (endDate != null && !endDate.trim().isEmpty())
                ? LocalDate.parse(endDate)
                : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.trim().isEmpty())
                ? LocalDate.parse(startDate)
                : end.minusDays(6);

        // 聚合 orders 表的每日趋势
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        List<Map<String, Object>> adminTrend = orderNewMapper.getDailyTrend(startDateTime, endDateTime);

        // 聚合 orders_legacy 表的每日趋势
        List<Map<String, Object>> posTrend = posOrderMapper.getDailyTrendForPos(start, end);

        // 合并两表数据到同一 Map（按日期分组累加）
        Map<String, long[]> mergedData = new TreeMap<>();
        if (adminTrend != null) {
            for (Map<String, Object> row : adminTrend) {
                String date = String.valueOf(row.get("date"));
                long orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).longValue() : 0L;
                long revenue = row.get("revenue") != null ? ((Number) row.get("revenue")).longValue() : 0L;
                long[] existing = mergedData.getOrDefault(date, new long[]{0L, 0L});
                existing[0] += orderCount;
                existing[1] += revenue;
                mergedData.put(date, existing);
            }
        }
        if (posTrend != null) {
            for (Map<String, Object> row : posTrend) {
                String date = String.valueOf(row.get("date"));
                long orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).longValue() : 0L;
                long revenue = row.get("revenue") != null ? ((Number) row.get("revenue")).longValue() : 0L;
                long[] existing = mergedData.getOrDefault(date, new long[]{0L, 0L});
                existing[0] += orderCount;
                existing[1] += revenue;
                mergedData.put(date, existing);
            }
        }

        // 构建结果列表
        List<com.foodtraceability.dto.order.DailyStatsVO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : mergedData.entrySet()) {
            com.foodtraceability.dto.order.DailyStatsVO vo = new com.foodtraceability.dto.order.DailyStatsVO();
            vo.setDate(entry.getKey());
            vo.setStoreName(storeName != null && !storeName.isEmpty() ? storeName : "全部门店");
            vo.setOrderCount(entry.getValue()[0]);
            vo.setRevenue(entry.getValue()[1]);
            // 成本暂未实现，设为0；利润 = 营业额 - 成本
            vo.setCost(0L);
            vo.setProfit(entry.getValue()[1]);
            // 利润率 = 利润 / 营业额 * 100
            vo.setProfitRate(entry.getValue()[1] > 0 ? 100.0 : 0.0);
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Object> getOrderTrends(String startDate, String endDate, String granularity) {
        // 默认查询近7天（含今天）
        LocalDate end = (endDate != null && !endDate.trim().isEmpty())
                ? LocalDate.parse(endDate)
                : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.trim().isEmpty())
                ? LocalDate.parse(startDate)
                : end.minusDays(6);

        // 上一周期用于同比环比计算
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevStart = start.minusDays(periodDays);
        LocalDate prevEnd = start.minusDays(1);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        LocalDateTime prevStartDateTime = prevStart.atStartOfDay();
        LocalDateTime prevEndDateTime = prevEnd.plusDays(1).atStartOfDay();

        // 1. 总览统计：使用全量数据，不按时间范围过滤
        long totalOrders = orderNewMapper.countAllOrders();
        long totalSales = orderNewMapper.sumAllSales();
        long totalCustomers = orderNewMapper.countDistinctCustomers();
        double avgOrderValue = totalOrders > 0
                ? (double) totalSales / totalOrders
                : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders", totalOrders);
        summary.put("totalSales", totalSales);
        summary.put("averageOrderValue", Math.round(avgOrderValue * 100.0) / 100.0);
        summary.put("totalCustomers", totalCustomers);

        // 2. 时间序列：按天聚合当前周期数据
        List<Map<String, Object>> rawTrend = orderNewMapper.getDailyTrend(startDateTime, endDateTime);
        List<Map<String, Object>> timeSeries = new ArrayList<>();
        // 补齐无数据日期，保证时间序列连续
        Map<String, Map<String, Object>> trendMap = new LinkedHashMap<>();
        if (rawTrend != null) {
            for (Map<String, Object> row : rawTrend) {
                String dateKey = String.valueOf(row.get("date"));
                trendMap.put(dateKey, row);
            }
        }
        for (int i = 0; i < periodDays; i++) {
            LocalDate day = start.plusDays(i);
            String dateKey = day.toString();
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dateKey);
            Map<String, Object> raw = trendMap.get(dateKey);
            long orderCount = raw == null ? 0L
                    : ((Number) raw.getOrDefault("order_count", 0)).longValue();
            long revenue = raw == null ? 0L
                    : ((Number) raw.getOrDefault("revenue", 0)).longValue();
            dayData.put("orderCount", orderCount);
            dayData.put("sales", revenue);
            timeSeries.add(dayData);
        }

        // 3. 订单类型分布：堂食/外卖/自提/打包占比
        List<Map<String, Object>> typeRows = orderNewMapper.countByOrderType();
        Map<String, Object> orderTypeDistribution = new LinkedHashMap<>();
        long typeTotal = 0L;
        if (typeRows != null) {
            for (Map<String, Object> row : typeRows) {
                Integer orderType = row.get("order_type") == null
                        ? null
                        : ((Number) row.get("order_type")).intValue();
                long cnt = ((Number) row.getOrDefault("cnt", 0)).longValue();
                typeTotal += cnt;
                orderTypeDistribution.put(getOrderTypeName(orderType), cnt);
            }
        }
        // 转换为百分比占比
        Map<String, Object> orderTypeRatio = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : orderTypeDistribution.entrySet()) {
            long cnt = ((Number) e.getValue()).longValue();
            double ratio = typeTotal > 0 ? (double) cnt / typeTotal * 100.0 : 0.0;
            orderTypeRatio.put(e.getKey(), Math.round(ratio * 100.0) / 100.0);
        }

        // 4. 同比环比：当前周期 vs 上一周期
        List<Map<String, Object>> prevTrend = orderNewMapper.getDailyTrend(prevStartDateTime, prevEndDateTime);
        long prevOrderCount = 0L;
        long prevSales = 0L;
        if (prevTrend != null) {
            for (Map<String, Object> row : prevTrend) {
                prevOrderCount += ((Number) row.getOrDefault("order_count", 0)).longValue();
                prevSales += ((Number) row.getOrDefault("revenue", 0)).longValue();
            }
        }
        long curOrderCount = timeSeries.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue())
                .sum();
        long curSales = timeSeries.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("sales", 0)).longValue())
                .sum();
        double curAvg = curOrderCount > 0 ? (double) curSales / curOrderCount : 0.0;
        double prevAvg = prevOrderCount > 0 ? (double) prevSales / prevOrderCount : 0.0;

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("orderCountChange", calcChangeRate(curOrderCount, prevOrderCount));
        comparison.put("salesChange", calcChangeRate(curSales, prevSales));
        comparison.put("averageOrderValueChange", calcChangeRate(curAvg, prevAvg));

        // 5. 趋势分析：基于时间序列判断上升/下降/平稳，找出峰值日
        Map<String, Object> trendAnalysis = new LinkedHashMap<>();
        if (timeSeries.isEmpty() || curOrderCount == 0) {
            trendAnalysis.put("trend", "stable");
            trendAnalysis.put("growthRate", 0.0);
            trendAnalysis.put("peakDay", null);
        } else {
            // 前半段 vs 后半段判断趋势方向
            int half = timeSeries.size() / 2;
            long firstHalf = timeSeries.subList(0, Math.max(1, half)).stream()
                    .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()).sum();
            long secondHalf = timeSeries.subList(half, timeSeries.size()).stream()
                    .mapToLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()).sum();
            String trend;
            if (secondHalf > firstHalf * 1.1) {
                trend = "up";
            } else if (secondHalf < firstHalf * 0.9) {
                trend = "down";
            } else {
                trend = "stable";
            }
            trendAnalysis.put("trend", trend);
            trendAnalysis.put("growthRate", calcChangeRate(secondHalf, firstHalf));
            // 峰值日：订单数最多的日期
            String peakDay = timeSeries.stream()
                    .max(Comparator.comparingLong(m -> ((Number) m.getOrDefault("orderCount", 0)).longValue()))
                    .map(m -> String.valueOf(m.get("date")))
                    .orElse(null);
            trendAnalysis.put("peakDay", peakDay);
        }
        // peakHour 暂无小时级数据，保持 null
        trendAnalysis.put("peakHour", null);

        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("timeSeries", timeSeries);
        trends.put("summary", summary);
        trends.put("comparison", comparison);
        trends.put("trendAnalysis", trendAnalysis);
        trends.put("orderTypeDistribution", orderTypeRatio);
        return trends;
    }

    /**
     * 计算同比/环比变化率
     * @param current 当前值
     * @param previous 上一周期值
     * @return 变化率百分比（保留两位小数），上一周期为 0 时返回 0
     */
    private double calcChangeRate(double current, double previous) {
        if (previous == 0) {
            return 0.0;
        }
        return Math.round((current - previous) / previous * 100.0 * 100.0) / 100.0;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取并校验订单是否存在
     */
    private OrderNew getAndValidateOrder(String orderId) {
        OrderNew order = orderNewMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在，ID: " + orderId);
        }
        return order;
    }

    /**
     * 校验桌台是否可用
     */
    private void validateTableAvailable(Long tableId) {
        DiningTableNew table = diningTableNewMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("桌台不存在");
        }
        if (table.getStatus() == null || table.getStatus() != 1) {
            throw new RuntimeException("桌台当前不可用，状态: " + table.getStatus());
        }
    }

    /**
     * 判断订单是否处于可支付状态
     */
    private boolean isPayableStatus(Integer orderStatus) {
        return orderStatus != null && (orderStatus == 0 || orderStatus == 1);
    }

    /**
     * 统计指定状态的订单数量
     */
    private long countOrdersByStatus(Integer status) {
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();
        wrapper.eq("order_status", status);
        return orderNewMapper.selectCount(wrapper);
    }

    /**
     * 计算订单金额（含商品校验）
     * 安全要点：
     * 1. 不信任客户端传入的 unitPrice，一律以数据库 sale_price/combo_price 为准（防篡改）
     * 2. 校验商品存在性、在售状态、库存（限量菜品）
     * 3. 用 DB 名称覆盖客户端 productName（确保快照准确）
     */
    private OrderAmountCalculation calculateOrderAmount(List<OrderCreateDTO.OrderItemCreateDTO> items) {
        OrderAmountCalculation calc = new OrderAmountCalculation();
        long totalAmount = 0L;
        long discountAmount = 0L;

        for (OrderCreateDTO.OrderItemCreateDTO item : items) {
            // 校验商品并获取真实价格
            ValidatedFood food = validateAndPriceFood(item.getProductId(), item.getProductType(), item.getQuantity());
            // 用 DB 真实数据覆盖客户端传入值（防篡改 + 快照准确）
            item.setUnitPrice(food.salePrice);
            item.setProductName(food.name);

            long itemAmount = food.salePrice * item.getQuantity();
            totalAmount += itemAmount;
            discountAmount += (item.getDiscountAmount() != null ? item.getDiscountAmount() : 0L);
        }

        calc.totalAmount = totalAmount;
        calc.discountAmount = discountAmount;
        calc.finalAmount = totalAmount - discountAmount;
        return calc;
    }

    /**
     * 校验商品存在性/状态/库存，返回 DB 真实名称和价格
     * @param productId 菜品或套餐ID
     * @param productType 1菜品 2套餐
     * @param quantity 购买数量
     */
    private ValidatedFood validateAndPriceFood(Long productId, Integer productType, Integer quantity) {
        if (productType == null) {
            throw new RuntimeException("商品类型不能为空");
        }
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (productType == 1) {
            FoodNew food = foodNewMapper.selectById(productId);
            if (food == null) {
                throw new RuntimeException("菜品不存在: ID=" + productId);
            }
            if (food.getStatus() == null || food.getStatus() != 1) {
                throw new RuntimeException("菜品当前不可购买（已停售/售罄）: " + food.getFoodName());
            }
            // 限量菜品库存校验（stock > 0 表示启用库存管理）
            if (food.getStock() != null && food.getStock() > 0 && quantity > food.getStock()) {
                throw new RuntimeException("菜品库存不足: " + food.getFoodName() + " 剩余 " + food.getStock() + " 份");
            }
            return new ValidatedFood(food.getFoodName(), food.getSalePrice());
        } else if (productType == 2) {
            DishComboNew combo = dishComboNewMapper.selectById(productId);
            if (combo == null) {
                throw new RuntimeException("套餐不存在: ID=" + productId);
            }
            if (combo.getStatus() == null || combo.getStatus() != 1) {
                throw new RuntimeException("套餐当前不可购买（已停售/售罄）: " + combo.getComboName());
            }
            return new ValidatedFood(combo.getComboName(), combo.getComboPrice());
        }
        throw new RuntimeException("未知商品类型: " + productType);
    }

    /**
     * 已校验商品（名称 + 价格）
     */
    private static class ValidatedFood {
        final String name;
        final Long salePrice;
        ValidatedFood(String name, Long salePrice) {
            this.name = name;
            this.salePrice = salePrice;
        }
    }

    /**
     * 生成订单编号
     * 格式: ORD + 年月日时分秒 + 4位序号
     */
    private String generateOrderCode() {
        String prefix = "ORD" + LocalDateTime.now().format(ORDER_CODE_FORMATTER);
        int sequence = orderNewMapper.getMaxTodaySequence(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    /**
     * 构建订单实体
     */
    private OrderNew buildOrderEntity(OrderCreateDTO createDTO, OrderAmountCalculation calc, String orderCode) {
        OrderNew order = new OrderNew();
        order.setOrderCode(orderCode);
        order.setOrderNumber(orderCode);
        order.setOrderType(createDTO.getOrderType());
        order.setOrderSource(createDTO.getOrderSource() != null ? createDTO.getOrderSource() : 1);
        order.setStoreId(createDTO.getStoreId());
        order.setCustomerId(createDTO.getCustomerId());
        order.setCustomerName(createDTO.getCustomerName());
        order.setCustomerPhone(createDTO.getCustomerPhone());
        order.setTableId(createDTO.getTableId());
        order.setDiningPeopleCount(createDTO.getDiningPeopleCount());
        order.setOrderStatus(0); // 待确认
        order.setPaymentStatus(0); // 未支付
        order.setTotalAmount(calc.totalAmount);
        order.setDiscountAmount(calc.discountAmount);
        order.setDeliveryFee(0L);
        order.setPackagingFee(0L);
        order.setFinalAmount(calc.finalAmount);
        order.setPaidAmount(0L);
        order.setRefundAmount(0L);
        order.setDeliveryAddress(createDTO.getDeliveryAddress());
        order.setRemark(createDTO.getRemark());
        return order;
    }

    /**
     * 保存订单明细
     */
    private void saveOrderItems(String orderId, List<OrderCreateDTO.OrderItemCreateDTO> items) {
        for (OrderCreateDTO.OrderItemCreateDTO item : items) {
            OrderItemNew orderItem = new OrderItemNew();
            orderItem.setOrderId(orderId);
            orderItem.setProductType(item.getProductType());
            if (item.getProductType() == 1) {
                orderItem.setFoodId(item.getProductId());
            } else {
                orderItem.setComboId(item.getProductId());
            }
            orderItem.setProductName(item.getProductName());
            orderItem.setSpecification(item.getSpecification());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setAmount(item.getUnitPrice() * item.getQuantity());
            orderItem.setDiscountAmount(item.getDiscountAmount() != null ? item.getDiscountAmount() : 0L);
            orderItem.setRemark(item.getRemark());
            orderItem.setKitchenStatus(0); // 待制作
            orderItemNewMapper.insert(orderItem);
        }
    }

    /**
     * 构建查询条件包装器
     */
    private QueryWrapper<OrderNew> buildQueryWrapper(OrderQueryDTO queryDTO) {
        QueryWrapper<OrderNew> wrapper = new QueryWrapper<>();

        if (queryDTO.getOrderCode() != null && !queryDTO.getOrderCode().isEmpty()) {
            wrapper.like("order_code", queryDTO.getOrderCode());
        }
        if (queryDTO.getOrderType() != null) {
            wrapper.eq("order_type", queryDTO.getOrderType());
        }
        if (queryDTO.getOrderStatus() != null) {
            wrapper.eq("order_status", queryDTO.getOrderStatus());
        }
        if (queryDTO.getPaymentStatus() != null) {
            wrapper.eq("payment_status", queryDTO.getPaymentStatus());
        }
        if (queryDTO.getCustomerId() != null) {
            wrapper.eq("customer_id", queryDTO.getCustomerId());
        }
        if (queryDTO.getTableId() != null) {
            wrapper.eq("table_id", queryDTO.getTableId());
        }
        if (queryDTO.getStartTime() != null && !queryDTO.getStartTime().isEmpty()) {
            wrapper.ge("create_time", queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null && !queryDTO.getEndTime().isEmpty()) {
            wrapper.le("create_time", queryDTO.getEndTime());
        }
        if (queryDTO.getMinAmount() != null) {
            wrapper.ge("final_amount", queryDTO.getMinAmount());
        }
        if (queryDTO.getMaxAmount() != null) {
            wrapper.le("final_amount", queryDTO.getMaxAmount());
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    /**
     * 转换为完整订单VO（含明细和支付记录）
     */
    private OrderVO convertToOrderVO(OrderNew order) {
        OrderVO vo = convertToSimpleOrderVO(order);

        // 查询并设置订单明细
        List<OrderItemNew> items = orderItemNewMapper.selectByOrderId(order.getOrderId());
        vo.setItems(items.stream().map(this::convertToItemVO).collect(Collectors.toList()));

        // 查询并设置支付记录
        List<OrderPaymentRecordNew> payments = orderPaymentRecordNewMapper.selectByOrderId(order.getOrderId());
        vo.setPayments(payments.stream().map(this::convertToPaymentVO).collect(Collectors.toList()));

        // 查询并设置退款记录
        List<OrderRefundRecordNew> refunds = orderRefundRecordNewMapper.selectByOrderId(order.getOrderId());
        vo.setRefunds(refunds.stream().map(this::convertToRefundVO).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 转换为简单订单VO（不含明细）
     */
    private OrderVO convertToSimpleOrderVO(OrderNew order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderCode(order.getOrderCode());
        vo.setOrderType(order.getOrderType());
        vo.setOrderTypeName(getOrderTypeName(order.getOrderType()));
        vo.setOrderSource(order.getOrderSource());
        vo.setCustomerId(order.getCustomerId());
        vo.setCustomerName(order.getCustomerName());
        vo.setCustomerPhone(order.getCustomerPhone());
        vo.setTableId(order.getTableId());
        vo.setTableName(order.getTableName());
        vo.setDiningPeopleCount(order.getDiningPeopleCount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setPaymentStatusName(getPaymentStatusName(order.getPaymentStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPointsAmount(order.getPointsAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setPackagingFee(order.getPackagingFee());
        vo.setFinalAmount(order.getFinalAmount());
        vo.setPaidAmount(order.getPaidAmount());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setPointsEarned(order.getPointsEarned());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setDeliveryAddress(order.getDeliveryAddress());
        vo.setExpectedTime(order.getExpectedTime());
        vo.setActualDeliveryTime(order.getActualDeliveryTime());
        vo.setCashierUserId(order.getCashierUserId());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    /**
     * 转换订单明细为VO
     */
    private OrderVO.OrderItemVO convertToItemVO(OrderItemNew item) {
        OrderVO.OrderItemVO vo = new OrderVO.OrderItemVO();
        vo.setItemId(item.getItemId());
        vo.setProductType(item.getProductType());
        vo.setProductTypeName(item.getProductType() == 1 ? "单品" : "套餐");
        vo.setFoodId(item.getFoodId());
        vo.setComboId(item.getComboId());
        vo.setProductName(item.getProductName());
        vo.setSpecification(item.getSpecification());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(item.getAmount());
        vo.setDiscountAmount(item.getDiscountAmount());
        vo.setRemark(item.getRemark());
        vo.setKitchenStatus(item.getKitchenStatus());
        vo.setKitchenStatusName(getKitchenStatusName(item.getKitchenStatus()));
        return vo;
    }

    /**
     * 转换支付记录为VO
     */
    private OrderVO.OrderPaymentRecordVO convertToPaymentVO(OrderPaymentRecordNew record) {
        OrderVO.OrderPaymentRecordVO vo = new OrderVO.OrderPaymentRecordVO();
        vo.setPaymentId(record.getPaymentId());
        vo.setPaymentMethod(record.getPaymentMethod());
        vo.setPaymentMethodName(getPaymentMethodName(record.getPaymentMethod()));
        vo.setPaymentAmount(record.getPaymentAmount());
        vo.setTransactionNo(record.getTransactionNo());
        vo.setPaymentTime(record.getPaymentTime());
        return vo;
    }

    /**
     * 转换退款记录为VO
     */
    private OrderVO.OrderRefundRecordVO convertToRefundVO(OrderRefundRecordNew record) {
        OrderVO.OrderRefundRecordVO vo = new OrderVO.OrderRefundRecordVO();
        vo.setRefundId(record.getRefundId());
        vo.setRefundType(record.getRefundType());
        vo.setRefundTypeName(record.getRefundType() == 1 ? "全额退款" : "部分退款");
        vo.setRefundAmount(record.getRefundAmount());
        vo.setRefundReason(record.getRefundReason());
        vo.setRefundMethod(record.getRefundMethod());
        vo.setRefundStatus(record.getRefundStatus());
        vo.setRefundStatusName(getRefundStatusName(record.getRefundStatus()));
        vo.setCreateTime(record.getCreateTime());
        vo.setCompleteTime(record.getCompleteTime());
        return vo;
    }

    // ==================== 状态名称映射 ====================

    private String getOrderTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "堂食";
            case 2: return "外卖";
            case 3: return "自提";
            case 4: return "打包";
            default: return "未知";
        }
    }

    /**
     * POS端订单类型码 → 管理端订单类型码映射
     * POS端 orders_legacy.ORDER_TYPE: 0堂食 1外卖 2自提
     * 管理端 orders.order_type: 1堂食 2外卖 3自提 4打包
     */
    private Integer mapPosOrderTypeToAdmin(Integer posOrderType) {
        if (posOrderType == null) return null;
        switch (posOrderType) {
            case 0: return 1; // 堂食
            case 1: return 2; // 外卖
            case 2: return 3; // 自提
            default: return null;
        }
    }

    /**
     * 将字符串日期解析为 LocalDateTime
     * 支持格式：yyyy-MM-dd HH:mm:ss 和 yyyy-MM-dd
     * PostgreSQL 无法直接比较 TIMESTAMP 与 VARCHAR，需要显式转换为 LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String trimmed = dateStr.trim();
        try {
            if (trimmed.length() <= 10) {
                return LocalDate.parse(trimmed).atStartOfDay();
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.warn("解析日期字符串失败: {}, 错误: {}", dateStr, e.getMessage());
            return null;
        }
    }

    private String getOrderStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待确认";
            case 1: return "已确认";
            case 2: return "已完成";
            case 3: return "已取消";
            case 4: return "部分退款";
            case 5: return "全额退款";
            case 6: return "待评价";
            default: return "未知";
        }
    }

    private String getPaymentStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "未支付";
            case 1: return "部分支付";
            case 2: return "已支付";
            case 3: return "已退款";
            default: return "未知";
        }
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null) return "未知";
        switch (method) {
            case 1: return "现金";
            case 2: return "微信";
            case 3: return "支付宝";
            case 4: return "银行卡";
            case 5: return "积分";
            case 6: return "混合支付";
            default: return "未知";
        }
    }

    private String getKitchenStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待制作";
            case 1: return "制作中";
            case 2: return "已完成";
            case 3: return "已上菜";
            case 4: return "已退款";
            default: return "未知";
        }
    }

    private String getRefundStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已同意";
            case 2: return "已拒绝";
            case 3: return "已退款";
            default: return "未知";
        }
    }

    /**
     * 订单金额计算结果（内部类）
     */
    private static class OrderAmountCalculation {
        long totalAmount;
        long discountAmount;
        long finalAmount;
    }
}
