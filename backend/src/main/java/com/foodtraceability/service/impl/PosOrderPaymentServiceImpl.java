package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OrderResultDTO;
import com.foodtraceability.dto.PayRequestDTO;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.entity.*;
import com.foodtraceability.entity.finance.FinanceRecord;
import com.foodtraceability.mapper.*;
import com.foodtraceability.mapper.finance.FinanceRecordMapper;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.service.MemberService;
import com.foodtraceability.service.PosOrderPaymentService;
import com.foodtraceability.service.SensitiveDataService;
import com.foodtraceability.service.ReceiptPrinterService;
import com.foodtraceability.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * POS订单支付服务实现类
 * 负责订单支付、退款等业务逻辑
 */
@Service
public class PosOrderPaymentServiceImpl implements PosOrderPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderPaymentServiceImpl.class);

    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final FoodMapper foodMapper;
    /** foods 表 Mapper（菜品展示来源），退款时同步回补库存，保证与 food 表一致 */
    private final FoodNewMapper foodNewMapper;
    private final OrderWebSocketController orderWebSocketController;
    private final OperationLogMapper operationLogMapper;
    private final MemberService memberService;
    private final MemberMapper memberMapper;
    private final SensitiveDataService sensitiveDataService;
    private final ReceiptPrinterService receiptPrinterService;
    /** 收支流水Mapper，用于支付成功后自动创建收入记录，确保利润表数据同步 */
    private final FinanceRecordMapper financeRecordMapper;

    public PosOrderPaymentServiceImpl(
            KitchenOrderMapper kitchenOrderMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            FoodMapper foodMapper,
            FoodNewMapper foodNewMapper,
            OrderWebSocketController orderWebSocketController,
            OperationLogMapper operationLogMapper,
            MemberService memberService,
            MemberMapper memberMapper,
            SensitiveDataService sensitiveDataService,
            ReceiptPrinterService receiptPrinterService,
            FinanceRecordMapper financeRecordMapper) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.foodMapper = foodMapper;
        this.foodNewMapper = foodNewMapper;
        this.orderWebSocketController = orderWebSocketController;
        this.operationLogMapper = operationLogMapper;
        this.memberService = memberService;
        this.memberMapper = memberMapper;
        this.sensitiveDataService = sensitiveDataService;
        this.receiptPrinterService = receiptPrinterService;
        this.financeRecordMapper = financeRecordMapper;
    }

    /**
     * 订单支付
     * 包含CAS锁机制、状态管理、余额支付、交易ID生成等完整流程
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR')")
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> payOrder(PayRequestDTO request) {
        log.info("开始处理订单支付: orderId={}", request.getOrderId());
        try {
            // 查询订单
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, request.getOrderId()));
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (order.getOrderSource() == null || order.getOrderSource() != 4) {
                return Result.error("仅支持POS终端订单支付");
            }

            // 获取操作员信息
            String operatorId = SecurityUtils.getCurrentUserId() != null ? String.valueOf(SecurityUtils.getCurrentUserId()) : "unknown";
            String operatorName = SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "unknown";
            log.info("操作员信息: operatorId={}, operatorName={}, orderId={}", operatorId, operatorName, request.getOrderId());
            log.info("查询到订单: orderNumber={}, status={}", order.getOrderNumber(), order.getOrderStatus());

            // 检查订单状态
            Integer currentStatus = order.getOrderStatus();
            if (currentStatus != null && currentStatus == 1) {
                return Result.error("订单已支付，请勿重复操作");
            }

            // 处理CAS锁超时恢复
            Result<Void> statusCheckResult = checkAndRecoverOrderStatus(order, currentStatus, request.getOrderId());
            if (statusCheckResult.getCode() != 0) {
                return Result.error(statusCheckResult.getMessage());
            }

            // 校验支付金额（防篡改：前端金额仅作展示参考，最终以订单实际金额为准）
            BigDecimal orderAmount = order.getOrderAmount() != null ? order.getOrderAmount() : order.getActualAmount();
            if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("订单金额异常，请联系管理员");
            }
            if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && request.getAmount().compareTo(orderAmount) != 0) {
                log.warn("支付金额与订单金额不一致: request={}, order={}, orderId={}",
                        request.getAmount(), orderAmount, request.getOrderId());
                return Result.error("支付金额与订单金额不一致，请刷新后重试");
            }

            // 转换支付方式
            Integer paymentMethodInt = convertPaymentMethod(request.getPaymentMethod());

            // 微信/支付宝扫码支付：返回二维码URL，订单状态保持未支付，等待 confirmQrPayment
            if (isQrCodePayment(paymentMethodInt)) {
                return prepareQrCodePayment(order, paymentMethodInt, orderAmount);
            }

            // CAS锁：将订单状态设置为-1（支付中）
            Order lockCheck = new Order();
            lockCheck.setOrderId(request.getOrderId());
            lockCheck.setOrderStatus(-1);
            lockCheck.setPaymentTime(LocalDateTime.now());
            int updated = orderMapper.update(lockCheck, new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderId, request.getOrderId())
                .eq(Order::getOrderStatus, 0));
            if (updated == 0) {
                return Result.error("订单正在处理中，请勿重复提交");
            }

            // 支付方式已在 CAS 锁前转换，此处统一使用订单实际金额（防篡改）
            BigDecimal payAmount = orderAmount;

            // 余额支付处理
            Result<Void> balanceResult = processBalancePayment(paymentMethodInt, request, payAmount);
            if (balanceResult.getCode() != 0) {
                return Result.error(balanceResult.getMessage());
            }

            // 生成交易ID
            String transactionId = generateTransactionId(paymentMethodInt);

            // 更新订单为已支付状态
            Order paidUpdate = new Order();
            paidUpdate.setOrderId(request.getOrderId());
            paidUpdate.setPaymentMethod(paymentMethodInt);
            paidUpdate.setTransactionId(transactionId);
            paidUpdate.setOrderStatus(1);
            paidUpdate.setActualAmount(payAmount);
            paidUpdate.setPaymentTime(LocalDateTime.now());

            // 关键：必须带status=-1条件，确保只有持锁线程能完成支付
            int payResult = orderMapper.update(paidUpdate,
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderId, request.getOrderId())
                    .eq(Order::getOrderStatus, -1));
            if (payResult == 0) {
                // CAS锁已丢失，回滚状态
                Order recovery = new Order();
                recovery.setOrderId(request.getOrderId());
                recovery.setOrderStatus(0);
                recovery.setPaymentTime(null);
                orderMapper.updateById(recovery);
                return Result.error("支付状态已变更，请刷新后重试");
            }

            // 查询后厨订单
            KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, order.getOrderId()));
            if (kitchenOrder != null) {
                log.info("支付成功-后厨订单状态不变: kitchenOrderId={}, currentStatus={}",
                    kitchenOrder.getKitchenOrderId(), kitchenOrder.getStatus());
            }

            // 自动创建收支流水记录，确保利润表收入数据同步
            try {
                FinanceRecord financeRecord = new FinanceRecord();
                financeRecord.setRecordNo("POS-" + order.getOrderNumber());
                financeRecord.setRecordType(1); // 收入
                financeRecord.setRecordCategory(101); // 销售收入
                // orderAmount 单位为分，直接使用
                financeRecord.setAmount(order.getOrderAmount() != null ? order.getOrderAmount().longValue() : 0L);
                // 支付方式已在前面转换过，直接使用
                if (paymentMethodInt != null) {
                    financeRecord.setPaymentMethod(switch (paymentMethodInt) {
                        case 0 -> 3; // 微信支付
                        case 1 -> 4; // 支付宝
                        case 2 -> 1; // 现金
                        case 3 -> 2; // 银行卡
                        case 4 -> 6; // 余额
                        default -> 6;
                    });
                }
                financeRecord.setCounterpartyName(order.getStoreName());
                financeRecord.setCounterpartyType(2); // 客户
                financeRecord.setBusinessDate(LocalDate.now());
                financeRecord.setRecordDate(LocalDate.now());
                financeRecord.setApprovalStatus(1); // 已审批（POS支付自动确认）
                financeRecord.setRemark("POS订单支付 - " + order.getOrderNumber());
                financeRecordMapper.insert(financeRecord);
                log.info("已自动创建收支流水记录: recordNo=POS-{}, amount={}分", order.getOrderNumber(), order.getOrderAmount());
            } catch (Exception financeEx) {
                // 创建收支记录失败不影响支付结果，仅记录日志
                log.warn("创建收支流水记录失败（不影响支付）: orderNumber={}, error={}",
                    order.getOrderNumber(), financeEx.getMessage());
            }

            // 构建返回结果
            OrderResultDTO result = buildOrderResultDTO(order, kitchenOrder);
            log.info("订单支付成功: {}", order.getOrderNumber());

            // 自动打印小票（打印失败不影响支付结果）
            try {
                receiptPrinterService.printReceipt(result);
                log.info("小票打印请求已发送: orderNumber={}", order.getOrderNumber());
            } catch (Exception printEx) {
                log.warn("小票打印失败（不影响支付）: orderNumber={}, error={}",
                    order.getOrderNumber(), printEx.getMessage());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("订单支付失败: orderId={}", request.getOrderId(), e);
            return Result.error("支付失败，请重试或联系客服");
        }
    }

    /**
     * 订单退款
     * 包含库存回补、后厨通知、日志记录等完整流程
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> refundOrder(String orderNumber, String refundReason, HttpServletRequest request) {
        log.info("处理退款请求: orderNumber={}, reason={}", orderNumber, refundReason);

        // 查询订单
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber));
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getOrderStatus() == 7) {
            return Result.error("订单已退款");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != 1) {
            return Result.error("只能退款已支付的订单");
        }

        // 执行退款
        BigDecimal refundAmount = order.getActualAmount();
        // G4 修复：退款金额必须为正数，防止数据异常导致负数/零金额退款
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("退款金额异常，拒绝退款: orderNumber={}, actualAmount={}", orderNumber, refundAmount);
            return Result.error("订单退款金额异常，请联系管理员核查");
        }
        String operatorName = SecurityUtils.getCurrentUsername();
        if (operatorName == null) {
            operatorName = "system";
        }

        int orderUpdateResult = orderMapper.updateOrderRefund(order.getOrderId(), refundAmount, refundReason, operatorName);
        log.info("管理端订单退款更新结果: {}, 退款金额: {}, 操作人: {}", orderUpdateResult, refundAmount, operatorName);

        // 回补库存
        recoverStockForRefund(order.getOrderId());

        // 更新后厨订单状态
        updateKitchenOrderRefundStatus(order.getOrderId());

        // 创建退款收支记录（负收入冲销）
        try {
            FinanceRecord refundRecord = new FinanceRecord();
            refundRecord.setRecordNo("REFUND-" + order.getOrderNumber());
            refundRecord.setRecordType(1); // 收入（负值表示退款）
            refundRecord.setRecordCategory(101); // 销售收入
            refundRecord.setAmount(refundAmount != null ? -refundAmount.longValue() : 0L); // 负数冲销
            Integer paymentMethodInt = order.getPaymentMethod();
            if (paymentMethodInt != null) {
                refundRecord.setPaymentMethod(switch (paymentMethodInt) {
                    case 0 -> 3; case 1 -> 4; case 2 -> 1; case 3 -> 2; case 4 -> 6; default -> 6;
                });
            }
            refundRecord.setCounterpartyName(order.getStoreName());
            refundRecord.setCounterpartyType(2);
            refundRecord.setBusinessDate(LocalDate.now());
            refundRecord.setRecordDate(LocalDate.now());
            refundRecord.setApprovalStatus(1);
            refundRecord.setRemark("POS订单退款 - " + order.getOrderNumber() + " - " + refundReason);
            financeRecordMapper.insert(refundRecord);
            log.info("已创建退款收支记录: recordNo=REFUND-{}, amount={}分", order.getOrderNumber(), refundAmount);
        } catch (Exception financeEx) {
            log.warn("创建退款收支记录失败（不影响退款）: orderNumber={}, error={}",
                order.getOrderNumber(), financeEx.getMessage());
        }

        // 记录操作日志
        recordRefundLog(order, orderNumber, refundReason, refundAmount, operatorName, request);

        log.info("退款处理成功: orderNumber={}", orderNumber);
        return Result.success(true);
    }

    // ==================== 私有方法 ====================

    /**
     * 检查并恢复订单状态（CAS锁超时恢复）
     */
    private Result<Void> checkAndRecoverOrderStatus(Order order, Integer currentStatus, String orderId) {
        if (currentStatus != null && currentStatus == -1) {
            long createTime = order.getPaymentTime() != null
                ? order.getPaymentTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L;
            if (System.currentTimeMillis() - createTime > 60000) {
                Order recovery = new Order();
                recovery.setOrderId(orderId);
                recovery.setOrderStatus(0);
                recovery.setPaymentTime(null);
                int recoveryResult = orderMapper.update(recovery,
                    new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
                        .eq(Order::getOrderStatus, -1));
                if (recoveryResult == 0) {
                    return Result.error("订单状态已变更，请刷新后重试");
                }
                log.info("CAS锁超时恢复: orderId={}, 已重置为待支付", orderId);
            } else {
                return Result.error("订单正在支付中，请稍后重试");
            }
        } else if (currentStatus != null && currentStatus != 0) {
            return Result.error("订单状态异常，无法支付");
        }
        return Result.success(null);
    }

    /**
     * 处理余额支付
     */
    private Result<Void> processBalancePayment(Integer paymentMethodInt, PayRequestDTO request, BigDecimal payAmount) {
        if (paymentMethodInt == 4) {
            if (request.getOpenid() == null || request.getOpenid().isEmpty()) {
                return Result.error("余额支付需要用户登录");
            }
            String loginPhone = SecurityUtils.getCurrentUsername();
            Member member = memberService.getByPhone(loginPhone);
            if (member == null) {
                member = memberService.lambdaQuery().eq(Member::getMemberNo, "openid_" + loginPhone).one();
            }
            if (member == null) {
                return Result.error("会员信息不存在");
            }
            // 金额元转分（数据库以分为单位存储）
            BigDecimal payAmountFen = payAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
            int deductResult = memberMapper.deductBalance(member.getMemberNo(), payAmountFen);
            if (deductResult == 0) {
                return Result.error("余额不足或扣减失败");
            }
            log.info("余额原子扣减成功: 会员={}, 扣减金额={}", member.getMemberNo(), payAmount);
        }
        return Result.success(null);
    }

    /**
     * 生成交易ID
     * <p>
     * 扫码支付（微信=0、支付宝=1）统一使用 "QR-" 前缀 + 时间戳 + 随机数，
     * 便于 confirmQrPayment 进行格式校验，防止越权确认他人订单（IDOR）。
     * </p>
     * <p>
     * 其他支付方式（现金/银行卡/余额）走同步流程，直接持久化 transactionId，
     * 不进入 confirmQrPayment 路径，前缀仅作标识用途。
     * </p>
     *
     * @param paymentMethodInt 支付方式（0=微信，1=支付宝，2=现金，3=银行卡，4=余额）
     * @return 交易号
     */
    private String generateTransactionId(Integer paymentMethodInt) {
        long ts = System.currentTimeMillis();
        int rand = new Random().nextInt(1000);
        return switch (paymentMethodInt) {
            case 0, 1 -> "QR-" + ts + String.format("%03d", rand);
            case 4 -> "BAL" + ts;
            default -> "CSH" + ts;
        };
    }

    /**
     * 回补退款订单的库存
     */
    private void recoverStockForRefund(String orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("退款订单无订单项: orderId={}", orderId);
            return;
        }

        int stockRecoverySuccess = 0;
        int stockRecoveryFailed = 0;
        for (OrderItem item : orderItems) {
            try {
                if (item.getFoodId() != null && !item.getFoodId().trim().isEmpty()
                    && item.getQuantity() != null && item.getQuantity() > 0) {
                    int addResult = foodMapper.addStock(item.getFoodId(), item.getQuantity());
                    if (addResult > 0) {
                        stockRecoverySuccess++;
                        log.info("库存回补成功: foodId={}, quantity={}", item.getFoodId(), item.getQuantity());
                    } else {
                        stockRecoveryFailed++;
                        log.warn("库存回补上限（已达最大库存）: foodId={}, quantity={}, 菜品名={}",
                            item.getFoodId(), item.getQuantity(), item.getFoodName());
                    }
                    // 同步回补 foods 表库存，保证两表一致
                    try {
                        foodNewMapper.addStock(item.getFoodId(), item.getQuantity());
                    } catch (Exception foodNewEx) {
                        log.warn("foods表库存回补失败（不影响主流程）: foodId={}, error={}",
                            item.getFoodId(), foodNewEx.getMessage());
                    }
                } else {
                    log.warn("跳过无效订单项: foodId={}, quantity={}", item.getFoodId(), item.getQuantity());
                }
            } catch (Exception stockEx) {
                stockRecoveryFailed++;
                log.error("库存回补异常: foodId={}, quantity={}, error={}",
                    item.getFoodId(), item.getQuantity(), stockEx.getMessage());
            }
        }
        log.info("库存回补完成: 成功={}, 失败={}, 总订单项={}",
            stockRecoverySuccess, stockRecoveryFailed, orderItems.size());
    }

    /**
     * 更新后厨订单退款状态
     */
    private void updateKitchenOrderRefundStatus(String orderId) {
        KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
            new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, orderId));
        if (kitchenOrder != null) {
            int kitchenUpdateResult = kitchenOrderMapper.updateStatus(kitchenOrder.getKitchenOrderId(), "refunded");
            log.info("后厨订单状态更新结果: {}", kitchenUpdateResult);
            // 直接使用已有kitchenOrder对象，设置新状态后推送WebSocket通知
            // 避免调用 selectById 传入 String 类型参数导致 bigint 类型不匹配
            kitchenOrder.setStatus("refunded");
            orderWebSocketController.pushOrderRefund(kitchenOrder);
            log.info("已推送退款通知到后厨端");
        }
    }

    /**
     * 记录退款操作日志
     */
    private void recordRefundLog(Order order, String orderNumber, String refundReason,
            BigDecimal refundAmount, String operatorName, HttpServletRequest request) {
        String operatorId = SecurityUtils.getCurrentUserId() != null ? String.valueOf(SecurityUtils.getCurrentUserId()) : "system";
        OperationLogEntity operationLog = new OperationLogEntity();
        operationLog.setOperationType("ORDER_REFUND");
        operationLog.setOperationModule("订单管理");
        operationLog.setOperationDesc("订单退款");
        operationLog.setOperatorId(operatorId);
        operationLog.setOperatorName(operatorName);
        operationLog.setRequestUrl("/v1/pos/order/" + orderNumber + "/refund");
        operationLog.setRequestMethod("POST");
        operationLog.setRequestParams("{\"orderNumber\":\"" + orderNumber + "\",\"refundReason\":\"" + refundReason + "\"}");
        operationLog.setResponseResult("{\"success\":true,\"refundAmount\":" + refundAmount + "}");
        operationLog.setOperatorIp(request.getRemoteAddr());
        operationLog.setStatus("SUCCESS");
        operationLog.setOperationTime(LocalDateTime.now());
        operationLog.setBusinessId(order.getOrderId());
        operationLogMapper.insert(operationLog);
        log.info("退款操作日志已记录");
    }

    /**
     * 构建订单结果DTO
     */
    private OrderResultDTO buildOrderResultDTO(Order order, KitchenOrder kitchenOrder) {
        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(order.getOrderId());
        result.setOrderNumber(order.getOrderNumber());
        result.setStatus(order.getOrderStatus() != null ? String.valueOf(order.getOrderStatus()) : "0");
        result.setTotalAmount(order.getActualAmount() != null ? order.getActualAmount() : order.getOrderAmount());
        result.setOrderSource(order.getOrderSource());
        result.setOrderType(order.getOrderType() != null ? String.valueOf(order.getOrderType()) : "0");
        result.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
        result.setPickupNumber(kitchenOrder != null ? kitchenOrder.getPickupNumber() : "");
        result.setPickupCode(kitchenOrder != null ? kitchenOrder.getPickupCode() : "");
        result.setTableNumber(kitchenOrder != null ? kitchenOrder.getTableNumber() : null);

        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getOrderId()));
        List<OrderResultDTO.OrderItemInfo> itemResults = orderItems.stream().map(item -> {
            OrderResultDTO.OrderItemInfo itemResult = new OrderResultDTO.OrderItemInfo();
            itemResult.setName(item.getFoodName());
            itemResult.setPrice(item.getUnitPrice());
            itemResult.setQuantity(item.getQuantity());
            return itemResult;
        }).collect(java.util.stream.Collectors.toList());
        result.setOrderItems(itemResults);

        return result;
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
     * 判断是否为扫码支付方式（微信/支付宝）
     * 扫码支付走异步流程：先生成二维码，等待用户扫码后再确认完成支付
     */
    private boolean isQrCodePayment(Integer paymentMethodInt) {
        return paymentMethodInt != null && (paymentMethodInt == 0 || paymentMethodInt == 1);
    }

    /**
     * 准备扫码支付（微信/支付宝）
     * <p>
     * 生成二维码URL和交易号，订单状态保持未支付（0）。
     * 前端展示二维码，用户扫码完成支付后调用 {@link #confirmQrPayment} 确认。
     * </p>
     * <p>
     * 二维码URL格式：
     * <ul>
     *   <li>微信：weixin://wxpay/bizpayurl?pr=xxx</li>
     *   <li>支付宝：alipayqr://platformapi/startapp?saId=10000007&qrcode=xxx</li>
     * </ul>
     * 生产环境对接官方支付SDK时，应替换为真实预下单返回的 code_url / qr_code。
     * </p>
     *
     * @param order 订单
     * @param paymentMethodInt 支付方式（0=微信，1=支付宝）
     * @param orderAmount 订单金额
     * @return 含二维码URL的订单结果
     */
    private Result<OrderResultDTO> prepareQrCodePayment(Order order, Integer paymentMethodInt, BigDecimal orderAmount) {
        String transactionId = generateTransactionId(paymentMethodInt);
        // 二维码有效期 15 分钟
        long expiresAt = System.currentTimeMillis() + 15 * 60 * 1000L;

        // 生成 mock 二维码URL（生产环境替换为微信/支付宝官方SDK返回的 code_url）
        String qrCodeUrl;
        if (paymentMethodInt == 0) {
            // 微信 Native 支付格式
            qrCodeUrl = "weixin://wxpay/bizpayurl?pr=mock_" + transactionId.substring(Math.max(0, transactionId.length() - 16));
        } else {
            // 支付宝扫码支付格式
            qrCodeUrl = "alipayqr://platformapi/startapp?saId=10000007&qrcode=mock_" + transactionId.substring(Math.max(0, transactionId.length() - 16));
        }

        // 关键：将 transactionId 持久化到订单表，confirmQrPayment 才能反查到订单。
        // 同时使用 CAS 条件（order_status=0 未支付）确保只在未支付状态下设置 transactionId，
        // 防止并发场景下覆盖已支付的 transactionId。
        Order qrBindUpdate = new Order();
        qrBindUpdate.setOrderId(order.getOrderId());
        qrBindUpdate.setTransactionId(transactionId);
        // 备份 paymentMethod，confirmQrPayment 完成时再写入正式支付方式
        qrBindUpdate.setPaymentMethod(paymentMethodInt);
        int bindResult = orderMapper.update(qrBindUpdate,
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, order.getOrderId())
                        .eq(Order::getOrderStatus, 0));
        if (bindResult == 0) {
            log.warn("扫码支付 transactionId 持久化失败（订单状态已变更）: orderId={}, currentStatus={}",
                    order.getOrderId(), order.getOrderStatus());
            return Result.error("订单状态已变更，请刷新后重试");
        }

        log.info("生成扫码支付二维码: orderId={}, paymentMethod={}, transactionId={}, expiresAt={}",
                order.getOrderId(), paymentMethodInt, transactionId, expiresAt);

        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(order.getOrderId());
        result.setOrderNumber(order.getOrderNumber());
        result.setOrderType(order.getOrderType() != null ? String.valueOf(order.getOrderType()) : null);
        result.setTableNumber(null);
        result.setTotalAmount(orderAmount);
        result.setQrCodeUrl(qrCodeUrl);
        result.setQrTransactionId(transactionId);
        result.setQrExpiresAt(expiresAt);
        result.setStatus("PENDING_PAYMENT");
        result.setMessage("请使用" + (paymentMethodInt == 0 ? "微信" : "支付宝") + "扫码完成支付");

        return Result.success(result, "二维码已生成，请扫码支付");
    }

    /**
     * 确认扫码支付完成（微信/支付宝）
     * <p>
     * 此方法为开发模拟模式：将订单状态从未支付（0）直接更新为已支付（1），
     * 并触发后续流程（后厨通知、流水记录、小票打印等）。
     * </p>
     * <p>
     * 生产环境对接微信/支付宝官方支付时，应改造为：
     * <ol>
     *   <li>接收支付平台的服务端异步回调通知</li>
     *   <li>验证回调签名（防止伪造）</li>
     *   <li>查询订单支付状态（防重放）</li>
     *   <li>更新订单状态并触发后续流程</li>
     * </ol>
     * </p>
     *
     * @param qrTransactionId 扫码支付交易号
     * @return 订单结果
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR')")
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderResultDTO> confirmQrPayment(String qrTransactionId) {
        log.info("确认扫码支付完成: transactionId={}", qrTransactionId);
        if (qrTransactionId == null || qrTransactionId.isBlank()) {
            return Result.error("交易号不能为空");
        }

        // 安全校验：transactionId 必须符合生成格式（QR-前缀 + 时间戳 + 随机数），
        // 拒绝直接传入 orderId 的请求，防止越权确认他人订单
        if (!qrTransactionId.startsWith("QR-")) {
            log.warn("非法交易号格式: transactionId={}", qrTransactionId);
            return Result.error("交易号格式非法");
        }

        // 通过 transactionId 反查订单（生产环境应以 transactionId 为唯一索引）
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getTransactionId, qrTransactionId));
        if (order == null) {
            log.warn("交易号对应的订单不存在: transactionId={}", qrTransactionId);
            return Result.error("订单不存在或交易号已失效");
        }
        if (order.getOrderSource() == null || order.getOrderSource() != 4) {
            return Result.error("仅支持POS终端订单支付");
        }

        Integer currentStatus = order.getOrderStatus();
        if (currentStatus != null && currentStatus == 1) {
            return Result.error("订单已支付，请勿重复操作");
        }

        // CAS 锁：将订单状态设置为 -1（支付中）
        Order lockCheck = new Order();
        lockCheck.setOrderId(order.getOrderId());
        lockCheck.setOrderStatus(-1);
        lockCheck.setPaymentTime(LocalDateTime.now());
        int updated = orderMapper.update(lockCheck, new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderId, order.getOrderId())
                .eq(Order::getOrderStatus, 0));
        if (updated == 0) {
            return Result.error("订单状态已变更，请刷新后重试");
        }

        BigDecimal payAmount = order.getOrderAmount() != null ? order.getOrderAmount() : order.getActualAmount();

        // 推断支付方式：若订单已有 paymentMethod 则使用，否则默认微信（mock 限制）
        Integer paymentMethodInt = order.getPaymentMethod() != null ? order.getPaymentMethod() : 0;
        // transactionId 保留 prepareQrCodePayment 时持久化的值（即入参 qrTransactionId 对应的订单值）。
        // 生产模式对接微信/支付宝时，应在此处替换为支付平台回调返回的真实交易号
        // （微信 transaction_id / 支付宝 trade_no），并保存至订单表以便后续对账。
        String finalTransactionId = order.getTransactionId() != null ? order.getTransactionId() : qrTransactionId;

        // 更新订单为已支付状态
        Order paidUpdate = new Order();
        paidUpdate.setOrderId(order.getOrderId());
        paidUpdate.setPaymentMethod(paymentMethodInt);
        paidUpdate.setTransactionId(finalTransactionId);
        paidUpdate.setOrderStatus(1);
        paidUpdate.setActualAmount(payAmount);
        paidUpdate.setPaymentTime(LocalDateTime.now());

        int payResult = orderMapper.update(paidUpdate,
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, order.getOrderId())
                        .eq(Order::getOrderStatus, -1));
        if (payResult == 0) {
            Order recovery = new Order();
            recovery.setOrderId(order.getOrderId());
            recovery.setOrderStatus(0);
            recovery.setPaymentTime(null);
            orderMapper.updateById(recovery);
            return Result.error("支付状态已变更，请刷新后重试");
        }

        // 复用现有完成支付的后续流程：流水记录、后厨通知、日志记录
        completePaymentPostProcess(order, payAmount, paymentMethodInt);

        OrderResultDTO result = buildOrderResult(order, paymentMethodInt);
        result.setStatus("PAID");
        result.setMessage("支付成功");
        return Result.success(result, "支付成功");
    }

    /**
     * 支付成功后的通用流程（提取自 payOrder，避免代码重复）
     * 包含：收支流水记录、操作日志、后厨订单关联
     */
    private void completePaymentPostProcess(Order order, BigDecimal payAmount, Integer paymentMethodInt) {
        try {
            FinanceRecord financeRecord = new FinanceRecord();
            financeRecord.setRecordNo("POS-" + order.getOrderNumber());
            financeRecord.setRecordType(1);
            financeRecord.setRecordCategory(101);
            financeRecord.setAmount(order.getOrderAmount() != null ? order.getOrderAmount().longValue() : 0L);
            if (paymentMethodInt != null) {
                financeRecord.setPaymentMethod(switch (paymentMethodInt) {
                    case 0 -> 3; // 微信支付
                    case 1 -> 4; // 支付宝
                    case 2 -> 1; // 现金
                    case 3 -> 2; // 银行卡
                    case 4 -> 6; // 余额
                    default -> 1;
                });
            }
            financeRecord.setBusinessDate(LocalDate.now());
            financeRecord.setRemark("POS订单支付收入-订单号:" + order.getOrderNumber());
            financeRecordMapper.insert(financeRecord);
        } catch (Exception e) {
            log.error("自动创建收支流水失败: orderId={}", order.getOrderId(), e);
        }

        // 查询后厨订单（关联存在则保持状态不变，仅日志记录）
        try {
            KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, order.getOrderId()));
            if (kitchenOrder != null) {
                log.info("扫码支付成功-后厨订单状态不变: kitchenOrderId={}, status={}",
                        kitchenOrder.getKitchenOrderId(), kitchenOrder.getStatus());
            }
        } catch (Exception e) {
            log.warn("查询后厨订单失败: orderId={}", order.getOrderId(), e);
        }

        // 操作日志
        try {
            OperationLogEntity opLog = new OperationLogEntity();
            opLog.setOperatorId(SecurityUtils.getCurrentUserId() != null ? String.valueOf(SecurityUtils.getCurrentUserId()) : "unknown");
            opLog.setOperatorName(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "unknown");
            opLog.setOperationType("POS_PAYMENT");
            opLog.setOperationModule("ORDER");
            opLog.setBusinessId(order.getOrderId());
            opLog.setOperationDesc("POS扫码支付完成: 订单号=" + order.getOrderNumber() + ", 金额=" + payAmount);
            opLog.setOperationTime(LocalDateTime.now());
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.warn("操作日志记录失败: orderId={}", order.getOrderId(), e);
        }
    }

    /**
     * 构造订单结果DTO（基础字段，不含二维码信息）
     */
    private OrderResultDTO buildOrderResult(Order order, Integer paymentMethodInt) {
        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(order.getOrderId());
        result.setOrderNumber(order.getOrderNumber());
        result.setOrderType(order.getOrderType() != null ? String.valueOf(order.getOrderType()) : null);
        result.setTableNumber(null);
        result.setTotalAmount(order.getOrderAmount() != null ? order.getOrderAmount() : order.getActualAmount());
        result.setOrderSource(order.getOrderSource());
        return result;
    }
}
