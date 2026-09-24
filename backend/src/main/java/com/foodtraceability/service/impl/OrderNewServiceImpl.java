package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.MaterialDeductionContext;
import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.dto.order.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.event.OrderRefundEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.MaterialConsumptionAuditService;
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
    private final ComboIngredientNewMapper comboIngredientNewMapper;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final StoreInventoryService storeInventoryService;
    // W1-EC-04C: posOrderMapper/posOrderItemMapper 已移除
    // queryPosOrders/getPosOrderDetail 已迁移至 orders 表（canonical read），不再读 orders_legacy
    /** 事件发布器：用于发布订单完成事件，触发财务收入/凭证等异步联动（DF-008 修复） */
    private final ApplicationEventPublisher applicationEventPublisher;
    /** 成本记录服务：用于持久化订单成本到 cost_record 表（DF-009 修复） */
    private final CostRecordService costRecordService;
    /** 资金流水服务（F6：订单支付落资金流水） */
    private final com.foodtraceability.service.finance.FundFlowService fundFlowService;
    /** 银行账户服务（F6：取默认账户） */
    private final com.foodtraceability.service.finance.BankAccountService bankAccountService;
    /** 扣料失败审计服务（P0：独立 bean + REQUIRES_NEW，禁止 this 自调用） */
    private final MaterialConsumptionAuditService materialConsumptionAuditService;

    public OrderNewServiceImpl(
            OrderNewMapper orderNewMapper,
            OrderItemNewMapper orderItemNewMapper,
            OrderPaymentRecordNewMapper orderPaymentRecordNewMapper,
            OrderRefundRecordNewMapper orderRefundRecordNewMapper,
            DiningTableNewMapper diningTableNewMapper,
            FoodNewMapper foodNewMapper,
            DishComboNewMapper dishComboNewMapper,
            DishRecipeNewMapper dishRecipeNewMapper,
            ComboIngredientNewMapper comboIngredientNewMapper,
            KitchenOrderMapper kitchenOrderMapper,
            StoreInventoryService storeInventoryService,
            ApplicationEventPublisher applicationEventPublisher,
            CostRecordService costRecordService,
            com.foodtraceability.service.finance.FundFlowService fundFlowService,
            com.foodtraceability.service.finance.BankAccountService bankAccountService,
            MaterialConsumptionAuditService materialConsumptionAuditService) {
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
        this.orderRefundRecordNewMapper = orderRefundRecordNewMapper;
        this.diningTableNewMapper = diningTableNewMapper;
        this.foodNewMapper = foodNewMapper;
        this.dishComboNewMapper = dishComboNewMapper;
        this.dishRecipeNewMapper = dishRecipeNewMapper;
        this.comboIngredientNewMapper = comboIngredientNewMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.storeInventoryService = storeInventoryService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.costRecordService = costRecordService;
        this.fundFlowService = fundFlowService;
        this.bankAccountService = bankAccountService;
        this.materialConsumptionAuditService = materialConsumptionAuditService;
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
                // 套餐：展开套餐内的所有菜品，再按菜品配方扣减（P1-COMBO-ORDER-001 读 combo_ingredients）
                Long comboId = resolveComboId(item);
                if (comboId != null) {
                    List<ComboIngredientNew> comboIngredients = selectComboIngredients(comboId);
                    for (ComboIngredientNew ingredient : comboIngredients) {
                        if (ingredient.getFoodId() == null) continue;
                        BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                                ? new BigDecimal(ingredient.getQuantity()) : BigDecimal.ONE;
                        // 套餐中菜品的实际数量 = 套餐中该菜品数量 × 套餐购买数量
                        int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                        if (actualFoodQty <= 0) continue;

                        addFoodMaterialDeductions(
                                ingredient.getFoodId(),
                                actualFoodQty,
                                materialDeductionMap,
                                materialNameMap);
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
     * KDS 出餐时扣减原料库存（P0 严格版：禁止任何"部分跳过后静默成功"）。
     *
     * 幂等策略：先原子占位（UPDATE ... WHERE material_consumed = 0），再扣料。
     * 占位成功（affectedRows=1）获得处理权；占位失败（affectedRows=0）说明已被其他请求处理或记录不存在，直接拒绝。
     * 原子性：所有应扣原料在同一事务内扣减；任何"应扣原料被 continue/catch 跳过"的数据/配置/解析异常
     * 都会抛出 MaterialDeductionFailureException → 整个扣料事务回滚（material_consumed 恢复 0），
     * 并由独立 REQUIRES_NEW 事务（materialConsumptionAuditService）写入 FAILED 审计行。
     *
     * PENDING_BUSINESS_DECISION（行为保持不变，仅记录）：
     *  - 配方 requiredQuantity == 0 / 换算后 qty<=0：不视为失败（可能为合法零消耗配置），
     *    该原料按原逻辑合并为 0 数量并在扣减循环中跳过，记入上下文 notes 供报告与日志。
     *
     * @param kitchenOrderId 后厨订单ID
     * @param sourceRef 来源引用（写入库存流水）
     * @param trayCode 触发服务时的托盘码（可空，写入失败审计行）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductMaterialsForServe(String kitchenOrderId, String sourceRef, String trayCode) {
        MaterialDeductionContext ctx = new MaterialDeductionContext();
        ctx.setKitchenOrderId(kitchenOrderId);
        ctx.setTrayCode(trayCode);
        ctx.setSourceRef(sourceRef);
        try {
            doDeductMaterialsForServe(ctx, sourceRef);
        } catch (MaterialDeductionFailureException e) {
            // 独立 bean + REQUIRES_NEW：主事务回滚后 FAILED 审计行仍持久化。
            // 禁止改为 this 自调用（会绕过代理，退化为同事务随主事务一起回滚）。
            // 审计自身失败不得吞掉主业务异常：仅记 ERROR，仍抛出原 MaterialDeductionFailureException。
            try {
                materialConsumptionAuditService.recordDeductionFailure(e);
            } catch (Exception auditEx) {
                e.addSuppressed(auditEx);
                log.error("[扣料失败审计] 审计写入自身异常，不影响主扣料异常传播: kitchenOrderId={}, failureType={}, auditError={}",
                        ctx.getKitchenOrderId(), e.getFailureType(), auditEx.getMessage(), auditEx);
            }
            throw e;
        }
    }

    private void doDeductMaterialsForServe(MaterialDeductionContext ctx, String sourceRef) {
        String kitchenOrderId = ctx.getKitchenOrderId();

        // 1. 原子占位：将 material_consumed 从 0 改为 1，获取唯一处理权
        LocalDateTime now = LocalDateTime.now();
        int affectedRows = kitchenOrderMapper.update(null,
                new LambdaUpdateWrapper<KitchenOrder>()
                        .eq(KitchenOrder::getKitchenOrderId, kitchenOrderId)
                        .eq(KitchenOrder::getMaterialConsumed, 0)
                        .set(KitchenOrder::getMaterialConsumed, 1)
                        .set(KitchenOrder::getMaterialConsumeTime, now));
        if (affectedRows == 0) {
            KitchenOrder existing = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<KitchenOrder>()
                            .eq(KitchenOrder::getKitchenOrderId, kitchenOrderId));
            if (existing == null) {
                // 后厨单不存在：数据异常，整体失败 + 审计
                log.error("出餐扣料：后厨单不存在, kitchenOrderId={}", kitchenOrderId);
                throw MaterialDeductionFailureException.of(ctx,
                        MaterialDeductionFailureException.KITCHEN_ORDER_NOT_FOUND,
                        "出餐扣料失败：后厨单不存在（kitchenOrderId=" + kitchenOrderId + "）");
            }
            // material_consumed 已为 1：已被其他请求扣减（幂等保护，既有显式失败，保持不变）
            log.warn("出餐扣料：原料已扣减（幂等保护）, kitchenOrderId={}", kitchenOrderId);
            throw new RuntimeException("出餐扣料失败：原料已被扣减（kitchenOrderId=" + kitchenOrderId
                    + "），可能是重复出餐确认或并发请求");
        }

        // 2. 占位成功，重新读取完整记录（用于后续 orderId、storeId 等字段）
        KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>()
                        .eq(KitchenOrder::getKitchenOrderId, kitchenOrderId));
        if (kitchenOrder == null) {
            // 理论上不会发生（刚占位成功），防御性处理
            throw new RuntimeException("出餐扣料失败：占位后重新读取订单失败（kitchenOrderId=" + kitchenOrderId + "）");
        }

        String orderId = kitchenOrder.getOrderId();
        ctx.setOrderId(orderId);
        ctx.setOrderNumber(kitchenOrder.getOrderNumber());
        ctx.setStoreId(kitchenOrder.getStoreId());
        ctx.setStoreName(kitchenOrder.getStoreName());

        OrderNew order = orderNewMapper.selectById(orderId);
        if (order == null) {
            log.error("出餐扣料：订单不存在, orderId={}, kitchenOrderId={}", orderId, kitchenOrderId);
            throw MaterialDeductionFailureException.of(ctx,
                    MaterialDeductionFailureException.ORDER_NOT_FOUND,
                    "出餐扣料失败：订单不存在（orderId=" + orderId + ", kitchenOrderId=" + kitchenOrderId + "）");
        }

        // 3. BOM 展开（P0 严格版：任何"应扣原料被跳过"的数据/配置/解析异常 → 整体失败）
        List<OrderItemNew> orderItems = getOrderItems(orderId);
        Map<Long, BigDecimal> materialDeductionMap = new LinkedHashMap<>();
        Map<Long, String> materialNameMap = new HashMap<>();

        boolean anyActiveItem = false;
        for (OrderItemNew item : orderItems) {
            if (item.getKitchenStatus() != null && item.getKitchenStatus() == 4) {
                // 已退款明细：合法不参与扣减（防止重复回补），记录说明
                log.info("出餐扣料：菜品已退款，跳过, orderId={}, itemId={}", orderId, item.getItemId());
                ctx.addNote("orderItem itemId=" + item.getItemId() + " 已退款(kitchenStatus=4)，不参与扣减");
                continue;
            }
            anyActiveItem = true;
            Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;

            if (item.getProductType() == null) {
                log.error("出餐扣料：明细 productType 为空, orderId={}, itemId={}", orderId, item.getItemId());
                throw MaterialDeductionFailureException.of(ctx,
                        MaterialDeductionFailureException.PRODUCT_TYPE_NULL,
                        "出餐扣料失败：订单明细 productType 为空（orderId=" + orderId
                                + ", itemId=" + item.getItemId() + "）");
            }

            if (item.getProductType() == 1) {
                if (item.getFoodId() == null) {
                    log.error("出餐扣料：单品明细 foodId 为空, orderId={}, itemId={}", orderId, item.getItemId());
                    throw MaterialDeductionFailureException.of(ctx,
                            MaterialDeductionFailureException.ITEM_FOOD_ID_NULL,
                            "出餐扣料失败：单品订单明细 foodId 为空（orderId=" + orderId
                                    + ", itemId=" + item.getItemId() + ", productName=" + item.getProductName() + "）");
                }
                addFoodMaterialDeductionsStrict(ctx, item.getFoodId(), quantity,
                        materialDeductionMap, materialNameMap);
            } else if (item.getProductType() == 2) {
                // P1-COMBO-ORDER-001: combo_id 优先，legacy 回落 foodId（food_id 当 combo_id 的旧数据）
                Long comboId = resolveComboId(item);
                if (comboId == null) {
                    log.error("出餐扣料：套餐明细 comboId/foodId 均为空, orderId={}, itemId={}", orderId, item.getItemId());
                    throw MaterialDeductionFailureException.of(ctx,
                            MaterialDeductionFailureException.ITEM_FOOD_ID_NULL,
                            "出餐扣料失败：套餐订单明细 comboId 为空（orderId=" + orderId
                                    + ", itemId=" + item.getItemId() + ", productName=" + item.getProductName() + "）");
                }
                DishComboNew combo = dishComboNewMapper.selectById(comboId);
                if (combo == null) {
                    log.error("出餐扣料：套餐不存在, comboId={}, orderId={}", comboId, orderId);
                    throw MaterialDeductionFailureException.of(ctx,
                            MaterialDeductionFailureException.COMBO_NOT_FOUND,
                            "出餐扣料失败：套餐不存在（comboId=" + comboId + ", orderId=" + orderId
                                    + ", itemId=" + item.getItemId() + "）");
                }
                List<ComboIngredientNew> comboIngredients = selectComboIngredients(comboId);
                if (comboIngredients == null || comboIngredients.isEmpty()) {
                    log.error("出餐扣料：套餐无配料(BOM未展开), comboId={}, orderId={}", comboId, orderId);
                    throw MaterialDeductionFailureException.of(ctx,
                            MaterialDeductionFailureException.COMBO_INGREDIENTS_EMPTY,
                            "出餐扣料失败：套餐无配料，BOM 未展开（comboId=" + comboId + ", orderId=" + orderId
                                    + ", itemId=" + item.getItemId() + "）");
                }
                for (ComboIngredientNew ingredient : comboIngredients) {
                    if (ingredient.getFoodId() == null) {
                        log.error("出餐扣料：套餐配料 foodId 为空, comboId={}, ingredientId={}",
                                comboId, ingredient.getIngredientId());
                        throw MaterialDeductionFailureException.of(ctx,
                                MaterialDeductionFailureException.INGREDIENT_FOOD_ID_NULL,
                                "出餐扣料失败：套餐配料 foodId 为空（comboId=" + comboId
                                        + ", ingredientId=" + ingredient.getIngredientId() + ", orderId=" + orderId + "）");
                    }
                    // P1-COMBO-ORDER-001: combo_ingredients.quantity 为 Integer，类型适配 BigDecimal 运算
                    BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                            ? new BigDecimal(ingredient.getQuantity()) : BigDecimal.ONE;
                    if (foodQtyInCombo.compareTo(BigDecimal.ZERO) <= 0) {
                        // 配置的配料数量 <= 0：按原逻辑视为零消耗跳过（PENDING 家族，记录说明）
                        ctx.addZeroQtyNote("comboId=" + comboId + " 配料foodId=" + ingredient.getFoodId()
                                + " 配置数量=" + foodQtyInCombo + "（<=0，按零消耗跳过）");
                        continue;
                    }
                    int actualFoodQty = foodQtyInCombo
                            .multiply(new BigDecimal(quantity)).intValue();
                    if (actualFoodQty <= 0) {
                        // 正数量经 int 换算为 0（如 0.5 份）：数据/配置异常，P0 整体失败
                        log.error("出餐扣料：套餐配料数量换算为0, comboId={}, foodId={}, ingredientQty={}, purchaseQty={}",
                                comboId, ingredient.getFoodId(), foodQtyInCombo, quantity);
                        throw MaterialDeductionFailureException.of(ctx,
                                MaterialDeductionFailureException.COMBO_INGREDIENT_QTY_TRUNCATED,
                                "出餐扣料失败：套餐配料数量正数经换算为0（数据/配置异常）（comboId=" + comboId
                                        + ", 配料foodId=" + ingredient.getFoodId()
                                        + ", 配料数量=" + foodQtyInCombo
                                        + ", 购买份数=" + quantity + ", orderId=" + orderId + "）");
                    }
                    // P1-COMBO-ORDER-001: combo_ingredients.food_id 已为 Long，无需解析
                    addFoodMaterialDeductionsStrict(ctx, ingredient.getFoodId(), actualFoodQty,
                            materialDeductionMap, materialNameMap);
                }
            } else {
                log.error("出餐扣料：明细 productType 非法, orderId={}, itemId={}, productType={}",
                        orderId, item.getItemId(), item.getProductType());
                throw MaterialDeductionFailureException.of(ctx,
                        MaterialDeductionFailureException.PRODUCT_TYPE_INVALID,
                        "出餐扣料失败：订单明细 productType 非法（orderId=" + orderId
                                + ", itemId=" + item.getItemId() + ", productType=" + item.getProductType() + "）");
            }
        }

        // 4. 空结果处理（P0：禁止"无明细/无扣减"的静默成功）
        if (orderItems.isEmpty()) {
            log.error("出餐扣料：订单无任何明细, orderId={}, kitchenOrderId={}", orderId, kitchenOrderId);
            throw MaterialDeductionFailureException.of(ctx,
                    MaterialDeductionFailureException.ORDER_ITEMS_EMPTY,
                    "出餐扣料失败：订单无任何明细（orderId=" + orderId + ", kitchenOrderId=" + kitchenOrderId
                            + "），不能按零消耗处理");
        }
        if (materialDeductionMap.isEmpty()) {
            // 仅有全部已退款明细：合法零消耗（既有行为保持不变）
            log.warn("出餐扣料：订单明细全部已退款，未扣减原料（合法零消耗）, orderId={}, kitchenOrderId={}",
                    orderId, kitchenOrderId);
            return;
        }

        // 5. 门店校验：有应扣原料但订单门店为空 → 无法定位库存，整体失败
        if (!materialDeductionMap.isEmpty() && order.getStoreId() == null) {
            log.error("出餐扣料：订单 storeId 为空, orderId={}, kitchenOrderId={}", orderId, kitchenOrderId);
            throw MaterialDeductionFailureException.of(ctx,
                    MaterialDeductionFailureException.ORDER_STORE_ID_NULL,
                    "出餐扣料失败：订单门店 storeId 为空，无法定位门店库存（orderId=" + orderId
                            + ", kitchenOrderId=" + kitchenOrderId + "）");
        }

        // 6. 逐原料扣减（复用 storeInventoryService.decreaseStock()）
        //    任一原料失败 → MaterialDeductionFailureException → 事务回滚 + REQUIRES_NEW 审计
        for (Map.Entry<Long, BigDecimal> entry : materialDeductionMap.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal qty = entry.getValue();
            String materialName = materialNameMap.get(materialId);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                // requiredQuantity==0 等零数量：PENDING_BUSINESS_DECISION，按原逻辑跳过（不视为失败）
                ctx.addNote("materialId=" + materialId + " 汇总数量=" + qty + "（<=0，零数量跳过）");
                continue;
            }

            ctx.addAttempted(materialId, qty, materialName);
            try {
                storeInventoryService.decreaseStock(
                        String.valueOf(order.getStoreId()),
                        materialId, qty, 2, sourceRef);
            } catch (BusinessException e) {
                // decreaseStock 的显式失败（库存不足/记录不存在/冲突）：映射为失败类型，整体失败 + 审计
                ctx.setFailedMaterial(materialId, materialName);
                String failureType = MaterialDeductionFailureException.mapBusinessExceptionType(e);
                log.error("出餐扣料：库存扣减失败, orderId={}, materialId={}, materialName={}, type={}, error={}",
                        orderId, materialId, materialName, failureType, e.getMessage());
                throw MaterialDeductionFailureException.of(ctx, failureType,
                        "出餐扣料失败：库存扣减失败（materialId=" + materialId
                                + ", materialName=" + materialName
                                + ", 数量=" + qty.toPlainString()
                                + ", 原因=" + e.getMessage()
                                + ", orderId=" + orderId + ", kitchenOrderId=" + kitchenOrderId + "）", e);
            } catch (Exception e) {
                ctx.setFailedMaterial(materialId, materialName);
                log.error("出餐扣料：库存扣减未知异常, orderId={}, materialId={}, materialName={}, error={}",
                        orderId, materialId, materialName, e.getMessage(), e);
                throw MaterialDeductionFailureException.of(ctx,
                        MaterialDeductionFailureException.DECREASE_STOCK_ERROR,
                        "出餐扣料失败：库存扣减未知异常（materialId=" + materialId
                                + ", materialName=" + materialName
                                + ", 数量=" + qty.toPlainString()
                                + ", 原因=" + e.getMessage()
                                + ", orderId=" + orderId + ", kitchenOrderId=" + kitchenOrderId + "）", e);
            }
            log.info("出餐扣料成功, orderId={}, materialId={}, materialName={}, qty={}",
                    orderId, materialId, materialName, qty);
        }

        log.info("出餐扣料完成, kitchenOrderId={}, orderId={}, 原料种类={}, 零数量跳过说明={}",
                kitchenOrderId, orderId, materialDeductionMap.size(), ctx.getZeroQtyNotes());
    }

    /**
     * 累加某个菜品的原料扣减需求 —— P0 严格版（仅用于 KDS 出餐扣料路径）。
     * <p>
     * 与既有 {@link #addFoodMaterialDeductions} 的区别：
     *  - 菜品不存在 → FOOD_NOT_FOUND 整体失败（原实现靠空配方静默跳过）；
     *  - 菜品无任何配方（BOM 为空）→ FOOD_NO_RECIPE 整体失败（原实现静默跳过）；
     *  - 配方 materialId 为空 → RECIPE_MATERIAL_ID_NULL 整体失败（原实现合并为0数量后静默跳过）。
     *  - requiredQuantity 为 null / 0：保持原逻辑（null 按 ZERO；0 合并为 0 数量），PENDING_BUSINESS_DECISION。
     *
     * @param ctx 扣料上下文（记录 BOM 展开与失败定位）
     * @param foodId 菜品ID
     * @param quantity 菜品数量（份数）
     * @param materialDeductionMap 原料汇总Map（累加）
     * @param materialNameMap 原料名称Map
     */
    /**
     * P1-COMBO-ORDER-001: 解析套餐明细的 comboId。
     * 优先 combo_id（新数据 food_id=null），legacy 回落 foodId（旧数据把 comboId 写在 food_id）。
     */
    private Long resolveComboId(OrderItemNew item) {
        if (item.getComboId() != null) {
            return item.getComboId();
        }
        return item.getFoodId();
    }

    /**
     * P1-COMBO-ORDER-001: 读新表 combo_ingredients（替代 combo_ingredient）。
     */
    private List<ComboIngredientNew> selectComboIngredients(Long comboId) {
        return comboIngredientNewMapper.selectList(
                new LambdaQueryWrapper<ComboIngredientNew>()
                        .eq(ComboIngredientNew::getComboId, comboId));
    }

    private void addFoodMaterialDeductionsStrict(MaterialDeductionContext ctx,
                                                 Long foodId, int quantity,
                                                 Map<Long, BigDecimal> materialDeductionMap,
                                                 Map<Long, String> materialNameMap) {
        FoodNew food = foodNewMapper.selectById(foodId);
        if (food == null) {
            log.error("出餐扣料：菜品不存在, foodId={}, orderId={}", foodId, ctx.getOrderId());
            ctx.setFailedMaterial(foodId, null);
            throw MaterialDeductionFailureException.of(ctx,
                    MaterialDeductionFailureException.FOOD_NOT_FOUND,
                    "出餐扣料失败：菜品不存在（foodId=" + foodId + ", orderId=" + ctx.getOrderId() + "）");
        }

        List<DishRecipeNew> recipes = getDishRecipes(foodId);
        if (recipes == null || recipes.isEmpty()) {
            log.error("出餐扣料：菜品无BOM配方, foodId={}, foodName={}, orderId={}",
                    foodId, food.getFoodName(), ctx.getOrderId());
            ctx.setFailedMaterial(foodId, food.getFoodName());
            throw MaterialDeductionFailureException.of(ctx,
                    MaterialDeductionFailureException.FOOD_NO_RECIPE,
                    "出餐扣料失败：菜品无任何BOM配方（BOM未展开）（foodId=" + foodId
                            + ", foodName=" + food.getFoodName() + ", orderId=" + ctx.getOrderId() + "）");
        }

        for (DishRecipeNew recipe : recipes) {
            if (recipe.getMaterialId() == null) {
                log.error("出餐扣料：配方 materialId 为空, foodId={}, recipeId={}, orderId={}",
                        foodId, recipe.getRecipeId(), ctx.getOrderId());
                ctx.setFailedMaterial(null, "foodId=" + foodId + " 的 recipeId=" + recipe.getRecipeId() + "（materialId缺失）");
                throw MaterialDeductionFailureException.of(ctx,
                        MaterialDeductionFailureException.RECIPE_MATERIAL_ID_NULL,
                        "出餐扣料失败：配方 materialId 为空（foodId=" + foodId
                                + ", recipeId=" + recipe.getRecipeId()
                                + ", materialName=" + recipe.getMaterialName()
                                + ", orderId=" + ctx.getOrderId() + "）");
            }
            BigDecimal requiredQty = recipe.getRequiredQuantity() != null
                    ? recipe.getRequiredQuantity() : BigDecimal.ZERO;
            if (requiredQty.compareTo(BigDecimal.ZERO) <= 0) {
                // PENDING_BUSINESS_DECISION：零标准量按原逻辑处理（合并为0），不视为失败
                ctx.addZeroQtyNote("foodId=" + foodId + " recipeId=" + recipe.getRecipeId()
                        + " 原料materialId=" + recipe.getMaterialId()
                        + " 标准量=" + requiredQty + "（零数量，PENDING_BUSINESS_DECISION）");
            }
            BigDecimal lossRate = recipe.getLossRate() != null ? recipe.getLossRate() : BigDecimal.ZERO;
            BigDecimal actualQty = requiredQty
                    .multiply(new BigDecimal(quantity))
                    .multiply(BigDecimal.ONE.add(lossRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
            materialDeductionMap.merge(recipe.getMaterialId(), actualQty, BigDecimal::add);
            ctx.addBomMaterial(recipe.getMaterialId(), actualQty, recipe.getMaterialName());
            if (recipe.getMaterialName() != null) {
                materialNameMap.putIfAbsent(recipe.getMaterialId(), recipe.getMaterialName());
            }
        }
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
            // 套餐（P1-COMBO-ORDER-001 读 combo_ingredients）
            Long comboId = resolveComboId(item);
            if (comboId != null) {
                List<ComboIngredientNew> comboIngredients = selectComboIngredients(comboId);
                for (ComboIngredientNew ingredient : comboIngredients) {
                    if (ingredient.getFoodId() == null) continue;
                    BigDecimal foodQtyInCombo = ingredient.getQuantity() != null
                            ? new BigDecimal(ingredient.getQuantity()) : BigDecimal.ONE;
                    int actualFoodQty = foodQtyInCombo.multiply(new BigDecimal(quantity)).intValue();
                    if (actualFoodQty <= 0) continue;

                    addFoodMaterialDeductions(
                            ingredient.getFoodId(),
                            actualFoodQty,
                            materialRestoreMap,
                            materialNameMap);
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
     * 分页查询POS终端订单（orders 表，canonical read）
     * W1-EC-04C: 从 orders_legacy 迁移到 orders 表。
     * orders 表已包含所有订单（POS端+管理端），order_status/payment_status 为 canonical 双字段。
     * 复用 buildQueryWrapper 构建查询条件，金额 orders 已存分，管理端直接使用。
     */
    @Override
    public Page<OrderVO> queryPosOrders(OrderQueryDTO queryDTO) {
        Page<OrderNew> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<OrderNew> wrapper = buildQueryWrapper(queryDTO);
        page = orderNewMapper.selectPage(page, wrapper);

        // 转换为VO列表（orders 字段已与管理端一致，无需状态/类型映射）
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertToSimpleOrderVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 根据订单编号获取POS终端订单详情（含菜品明细）
     * W1-EC-04C: 从 orders_legacy + order_items_legacy 迁移到 orders + order_items（canonical read）。
     * orders 表已包含所有订单（POS端+管理端），order_status/payment_status/orderType 为 canonical 值。
     *
     * @param orderNumber 订单编号（如 ORD20260713002），非订单ID
     */
    @Override
    public OrderVO getPosOrderDetail(String orderNumber) {
        log.info("查询POS订单详情, orderNumber: {}", orderNumber);
        if (orderNumber == null || orderNumber.isEmpty()) {
            throw new RuntimeException("订单编号不能为空");
        }
        // W1-EC-04C: 改读 orders 表（通过 order_code 查询）
        OrderNew order = orderNewMapper.selectByOrderCode(orderNumber);
        if (order == null) {
            throw new RuntimeException("POS订单不存在: " + orderNumber);
        }
        // 转换为 OrderVO（含明细和支付记录）
        return getOrderDetail(order.getOrderId());
    }

    // W1-EC-04C: convertPosItemToVO 已移除（不再读 order_items_legacy，直接使用 order_items）

    // W1-EC-04C: buildPosQueryWrapper 已移除（不再读 orders_legacy，复用 buildQueryWrapper）

    // W1-EC-04C: mapAdminStatusToPosStatus 已移除（状态直接使用 orders 表 canonical 值）

    // W1-EC-04C: convertPosOrderToVO 已移除（不再读 orders_legacy，复用 convertToSimpleOrderVO）

    // W1-EC-04C: mapPosStatusToAdminStatus 已移除（状态直接使用 orders 表 canonical 值）

    // W1-EC-04C: mapPosStatusToPaymentStatus 已移除（支付状态直接使用 orders 表 canonical 值）

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
        // W1-EC-04B-2: 统计聚合迁移 - 所有数据从 orders 表获取
        // orders 表已包含所有订单（POS端+管理端），无需分别查询
        stats.setTotalOrders(orderNewMapper.countTodayOrders());
        stats.setTotalSalesAmount(orderNewMapper.sumTodaySales());
        stats.setCompletedOrders(countOrdersByStatus(2));
        stats.setCancelledOrders(countOrdersByStatus(3));
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
        // W1-EC-04B-2: 统计聚合迁移 - 所有数据从 orders 表获取
        // orders 表已包含所有订单（POS端+管理端），无需分别查询
        stats.put("totalOrders", orderNewMapper.countAllOrders());
        stats.put("completedOrders", orderNewMapper.countAllCompletedOrders());
        stats.put("todayOrders", orderNewMapper.countTodayOrders());
        stats.put("totalSales", orderNewMapper.sumAllSales());
        stats.put("todaySales", orderNewMapper.sumTodaySales());
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

        // W1-EC-04B-2: 统计聚合迁移 - 所有数据从 orders 表获取
        // orders 表已包含所有订单（POS端+管理端），无需分别查询再合并
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        List<Map<String, Object>> dailyTrend = orderNewMapper.getDailyTrend(startDateTime, endDateTime);

        // 构建结果列表
        List<com.foodtraceability.dto.order.DailyStatsVO> result = new ArrayList<>();
        if (dailyTrend != null) {
            for (Map<String, Object> row : dailyTrend) {
                com.foodtraceability.dto.order.DailyStatsVO vo = new com.foodtraceability.dto.order.DailyStatsVO();
                vo.setDate(String.valueOf(row.get("date")));
                vo.setStoreName(storeName != null && !storeName.isEmpty() ? storeName : "全部门店");
                long orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).longValue() : 0L;
                long revenue = row.get("revenue") != null ? ((Number) row.get("revenue")).longValue() : 0L;
                vo.setOrderCount(orderCount);
                vo.setRevenue(revenue);
                // 成本暂未实现，设为0；利润 = 营业额 - 成本
                vo.setCost(0L);
                vo.setProfit(revenue);
                // 利润率 = 利润 / 营业额 * 100
                vo.setProfitRate(revenue > 0 ? 100.0 : 0.0);
                result.add(vo);
            }
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
     * W1-EC-04C: 新增 storeId 过滤（queryPosOrders 迁移后需要门店过滤能力）
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
        // W1-EC-04C: 门店ID过滤（orders.store_id 为 BIGINT，DTO.store_id 为 String，需转换）
        if (queryDTO.getStoreId() != null && !queryDTO.getStoreId().isEmpty()) {
            try {
                Long storeIdLong = Long.parseLong(queryDTO.getStoreId());
                wrapper.eq("store_id", storeIdLong);
            } catch (NumberFormatException e) {
                log.warn("门店ID格式异常, storeId: {}", queryDTO.getStoreId());
            }
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
        // W1-EC-04C: storeId 从 Long 转为 String（OrderVO.storeId 为 String）
        vo.setStoreId(order.getStoreId() != null ? String.valueOf(order.getStoreId()) : null);
        vo.setStoreName(null); // orders 表无 store_name 列，由前端通过 storeId 关联门店名称
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

    // W1-EC-04C: mapPosOrderTypeToAdmin 已移除（订单类型已与管理端一致）

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
