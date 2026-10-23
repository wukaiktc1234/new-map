package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.PosCallNumberService;
import com.foodtraceability.service.PosOrderQueryService;
import com.foodtraceability.service.SensitiveDataService;
import com.foodtraceability.service.ReceiptPrinterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * POS 订单查询服务实现类。
 *
 * <p><b>表口径（2026-10-23 改造）：</b>POS 快速下单写的是新表 {@code orders}（{@link OrderNew}），
 * 而查询路径此前读的是遗留表 {@code orders_legacy}（{@link Order}），导致「下的单查不到」。
 * 现在所有查询方法都改为<b>新表优先</b>：
 * <ol>
 *   <li>先查 {@code orders} / {@code order_items} / {@code order_payment_records}；</li>
 *   <li>新表查不到时<b>回落</b> {@code orders_legacy} / {@code order_items_legacy}，并打 WARN 日志，
 *       保证历史单仍可查、可重打，不会因为切换口径变成「数据消失」。</li>
 * </ol>
 *
 * <p><b>已知不兼容：</b>{@code getOrdersByOpenid} 依赖 {@code orders_legacy.user_id}
 * （openid 派生而来），新表没有 openid 列，因此该方法保持只读旧表；新表订单目前无法按 openid 反查。</p>
 */
@Service
public class PosOrderQueryServiceImpl implements PosOrderQueryService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderQueryServiceImpl.class);

    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderNewMapper orderNewMapper;
    private final OrderItemNewMapper orderItemNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;
    private final SensitiveDataService sensitiveDataService;
    private final ReceiptPrinterService receiptPrinterService;
    /** POS 叫号号服务：详情/小票展示 callNumber（缺失则补取号，保证老单也有号可叫） */
    private final PosCallNumberService posCallNumberService;

    public PosOrderQueryServiceImpl(
            KitchenOrderMapper kitchenOrderMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            OrderNewMapper orderNewMapper,
            OrderItemNewMapper orderItemNewMapper,
            OrderPaymentRecordNewMapper orderPaymentRecordNewMapper,
            SensitiveDataService sensitiveDataService,
            ReceiptPrinterService receiptPrinterService,
            PosCallNumberService posCallNumberService) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderNewMapper = orderNewMapper;
        this.orderItemNewMapper = orderItemNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
        this.sensitiveDataService = sensitiveDataService;
        this.receiptPrinterService = receiptPrinterService;
        this.posCallNumberService = posCallNumberService;
    }

    /**
     * 获取订单详情（新表优先，旧表兜底）
     */
    public Result<OrderDetailDTO> getOrderDetail(String orderNumber) {
        log.info("获取订单详情: {}", orderNumber);
        try {
            OrderNew newOrder = findNewOrder(orderNumber);
            if (newOrder != null) {
                return Result.success(buildNewOrderDetail(newOrder));
            }

            // ===== 兜底：遗留表（2026-10 之前的订单只在 orders_legacy 里） =====
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber));
            if (order == null) {
                return Result.error("订单不存在");
            }
            log.warn("订单 {} 仅存在于 orders_legacy（历史单），走兼容查询", orderNumber);

            List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getOrderId()));

            OrderDetailDTO dto = new OrderDetailDTO();
            dto.setOrderId(order.getOrderId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
            dto.setTotalAmount(order.getActualAmount());
            dto.setPaymentMethod(convertPaymentMethodToString(order.getPaymentMethod()));
            dto.setTransactionId(order.getTransactionId());
            dto.setStatus(convertOrderStatusToString(order.getOrderStatus()));
            dto.setOrderType(order.getOrderType());
            dto.setOrderSource(order.getOrderSource());
            dto.setContactName(order.getContactName());
            dto.setContactPhone(sensitiveDataService.decryptPhone(order.getContactPhone()));
            dto.setDeliveryAddress(sensitiveDataService.decryptAddress(order.getDeliveryAddress()));
            dto.setRefundAmount(order.getRefundAmount());
            dto.setRefundReason(order.getRefundReason());
            dto.setRefundTime(order.getRefundTime() != null ? order.getRefundTime().toString() : null);
            dto.setRemarks(order.getRemarks());
            dto.setCreateBy(order.getCreateBy());
            dto.setUpdateBy(order.getUpdateBy());

            KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderNumber, orderNumber).last("LIMIT 1"));
            if (kitchenOrder != null) {
                dto.setKitchenStatus(kitchenOrder.getStatus());
                dto.setPickupNumber(kitchenOrder.getPickupNumber());
                dto.setPickupCode(kitchenOrder.getPickupCode());
            }

            if (order.getRemarks() != null && order.getRemarks().startsWith("交易单号:")) {
                dto.setTransactionId(order.getRemarks().replace("交易单号:", ""));
            }

            List<OrderDetailDTO.OrderItemDetail> items = orderItems.stream().map(item -> {
                OrderDetailDTO.OrderItemDetail itemDto = new OrderDetailDTO.OrderItemDetail();
                itemDto.setName(item.getFoodName());
                itemDto.setPrice(item.getUnitPrice());
                itemDto.setQuantity(item.getQuantity());
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(items);

            return Result.success(dto);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 查询订单列表（新表优先，新表为空时回落旧表）
     */
    public Result<List<OrderQueryDTO>> getOrders(String startDate, String endDate, String keyword) {
        log.info("查询订单列表: startDate={}, endDate={}, keyword={}", startDate, endDate, keyword);
        try {
            LambdaQueryWrapper<OrderNew> newWrapper = new LambdaQueryWrapper<>();
            applyDateRange(newWrapper, startDate, endDate);
            if (keyword != null && !keyword.isEmpty()) {
                newWrapper.and(w -> w.like(OrderNew::getOrderNumber, keyword)
                        .or().like(OrderNew::getOrderCode, keyword)
                        .or().like(OrderNew::getCustomerName, keyword));
            }
            newWrapper.orderByDesc(OrderNew::getCreateTime).last("LIMIT 200");

            List<OrderNew> newOrders = orderNewMapper.selectList(newWrapper);
            if (!newOrders.isEmpty()) {
                return Result.success(newOrders.stream().map(this::convertNewToQueryDTO).collect(Collectors.toList()));
            }

            // ===== 兜底：新表没有数据时，回退遗留表 =====
            log.warn("orders 新表在给定条件下无数据，回落 orders_legacy 查询（历史单）");
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
                wrapper.ge(Order::getCreateTime, java.time.LocalDateTime.now().minusDays(7));
            } else {
                if (startDate != null && !startDate.isEmpty()) {
                    wrapper.ge(Order::getCreateTime, startDate + " 00:00:00");
                }
                if (endDate != null && !endDate.isEmpty()) {
                    wrapper.le(Order::getCreateTime, endDate + " 23:59:59");
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(Order::getOrderNumber, keyword)
                    .or().like(Order::getContactName, keyword));
            }
            wrapper.orderByDesc(Order::getCreateTime);
            wrapper.last("LIMIT 200");

            List<Order> orders = orderMapper.selectList(wrapper);
            List<OrderQueryDTO> dtoList = orders.stream().map(order -> {
                OrderQueryDTO dto = new OrderQueryDTO();
                dto.setOrderId(order.getOrderId());
                dto.setOrderNumber(order.getOrderNumber());
                dto.setTotalAmount(order.getActualAmount());
                dto.setStatus(convertOrderStatusToString(order.getOrderStatus()));
                dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
                return dto;
            }).collect(Collectors.toList());

            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return Result.error("查询订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据桌台号查询订单（新表优先，旧表兜底）
     */
    public Result<List<CustomerOrderDTO>> getOrdersByTable(String tableNumber) {
        log.info("根据桌台号查询订单: {}", tableNumber);
        try {
            List<KitchenOrder> kitchenOrders = kitchenOrderMapper.selectList(
                new LambdaQueryWrapper<KitchenOrder>()
                    .eq(KitchenOrder::getTableNumber, tableNumber)
                    .orderByDesc(KitchenOrder::getCreateTime));

            if (kitchenOrders.isEmpty()) {
                return Result.success(new ArrayList<>());
            }

            List<String> orderNumbers = kitchenOrders.stream()
                .map(KitchenOrder::getOrderNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            LambdaQueryWrapper<OrderNew> newWrapper = new LambdaQueryWrapper<>();
            newWrapper.in(OrderNew::getOrderNumber, orderNumbers)
                    .in(OrderNew::getOrderStatus, 0, 1)
                    .orderByDesc(OrderNew::getCreateTime);

            List<OrderNew> newOrders = orderNewMapper.selectList(newWrapper);
            if (!newOrders.isEmpty()) {
                return Result.success(newOrders.stream().map(this::convertNewToCustomerOrderDTO).collect(Collectors.toList()));
            }

            log.warn("桌台 {} 在 orders 新表无在途单，回落 orders_legacy", tableNumber);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Order::getOrderNumber, orderNumbers)
                   .in(Order::getOrderStatus, 0, 1)
                   .orderByDesc(Order::getCreateTime);

            List<Order> orders = orderMapper.selectList(wrapper);
            List<CustomerOrderDTO> dtoList = orders.stream()
                .map(this::convertToCustomerOrderDTO)
                .collect(Collectors.toList());

            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("根据桌台号查询订单失败", e);
            return Result.error("根据桌台号查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 通过 openid 查询订单列表。
     *
     * <p><b>不兼容说明：</b>openid → user_id 的派生关系只存在于 {@code orders_legacy}；
     * 新表 {@code orders} 无 openid 列，故此处保持只读旧表。新表单需按会员 ID/手机号查。</p>
     */
    public Result<List<OrderDetailDTO>> getOrdersByOpenid(String openid) {
        log.info("通过openid查询订单: {}", openid);
        try {
            String userId = "wx_" + openid.hashCode();
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getUserId, userId)
                   .orderByDesc(Order::getCreateTime);

            List<Order> orders = orderMapper.selectList(wrapper);
            List<OrderDetailDTO> dtoList = orders.stream().map(order -> {
                try {
                    return getOrderDetail(order.getOrderNumber()).getData();
                } catch (Exception e) {
                    log.error("转换订单详情失败: {}", order.getOrderNumber(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("通过openid查询订单失败", e);
            return Result.error("通过openid查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 重新打印小票（新表优先，旧表兜底）
     */
    public Result<Boolean> reprintReceipt(String orderId) {
        log.info("处理重打小票请求: orderId={}", orderId);
        try {
            OrderNew newOrder = orderNewMapper.selectOne(
                new LambdaQueryWrapper<OrderNew>().eq(OrderNew::getOrderId, orderId).last("LIMIT 1"));
            if (newOrder != null) {
                // 新表口径：payment_status=2 才是已支付（order_status 表示业务流转进度）
                if (newOrder.getPaymentStatus() == null || newOrder.getPaymentStatus() != 2) {
                    log.warn("重打小票失败: 新表订单未支付 orderId={}, paymentStatus={}", orderId, newOrder.getPaymentStatus());
                    return Result.error("仅支持重打已支付的订单小票");
                }
                KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<KitchenOrder>()
                        .eq(KitchenOrder::getOrderNumber, newOrder.getOrderNumber()).last("LIMIT 1"));
                OrderResultDTO result = buildOrderResultDTO(newOrder, kitchenOrder);
                boolean printSuccess = receiptPrinterService.reprintReceipt(result);
                log.info("小票重打{}（orders 新表）: orderNumber={}", printSuccess ? "成功" : "失败", newOrder.getOrderNumber());
                return Result.success(printSuccess);
            }

            // ===== 兜底：遗留表 =====
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
            if (order == null) {
                log.warn("重打小票失败: 订单不存在, orderId={}", orderId);
                return Result.error("订单不存在");
            }
            log.warn("重打小票走 orders_legacy 兼容路径: orderId={}", orderId);

            if (order.getOrderStatus() == null || order.getOrderStatus() != 1) {
                log.warn("重打小票失败: 订单未支付, orderId={}, status={}", orderId, order.getOrderStatus());
                return Result.error("仅支持重打已支付的订单小票");
            }

            KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, orderId));

            OrderResultDTO result = buildOrderResultDTO(order, kitchenOrder);

            boolean printSuccess = receiptPrinterService.reprintReceipt(result);

            if (printSuccess) {
                log.info("小票重打成功: orderNumber={}", order.getOrderNumber());
                return Result.success(true);
            } else {
                log.warn("小票重打失败: orderNumber={}", order.getOrderNumber());
                return Result.success(false);
            }
        } catch (Exception e) {
            log.error("重打小票异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.error("重打小票失败: " + e.getMessage());
        }
    }

    // ==================== 新表（orders）辅助方法 ====================

    /** 按 orderNumber / orderCode / orderId 任一命中新表订单 */
    private OrderNew findNewOrder(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return orderNewMapper.selectOne(new LambdaQueryWrapper<OrderNew>()
                .and(w -> w.eq(OrderNew::getOrderNumber, key)
                        .or().eq(OrderNew::getOrderCode, key)
                        .or().eq(OrderNew::getOrderId, key))
                .last("LIMIT 1"));
    }

    private void applyDateRange(LambdaQueryWrapper<OrderNew> wrapper, String startDate, String endDate) {
        // 与旧口径一致：未指定时间范围时默认最近 7 天，避免全表扫描
        if ((startDate == null || startDate.isEmpty()) && (endDate == null || endDate.isEmpty())) {
            wrapper.ge(OrderNew::getCreateTime, java.time.LocalDateTime.now().minusDays(7));
            return;
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(OrderNew::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(OrderNew::getCreateTime, endDate + " 23:59:59");
        }
    }

    /** 分 → 元（新表金额单位是「分」，DTO 对外是「元」） */
    private static BigDecimal centsToYuan(Long cents) {
        return cents == null ? null : BigDecimal.valueOf(cents).movePointLeft(2);
    }

    /**
     * 新表订单状态码 → 前端语义字符串。
     * 新表口径：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款（与 orders_legacy 的 0-7 不同）
     */
    private String convertNewOrderStatusToString(Integer orderStatus) {
        if (orderStatus == null) {
            return "pending";
        }
        return switch (orderStatus) {
            case 0 -> "pending";
            case 1 -> "confirmed";
            case 2 -> "completed";
            case 3 -> "cancelled";
            case 4 -> "partial_refunded";
            case 5 -> "refunded";
            default -> "pending";
        };
    }

    private OrderDetailDTO buildNewOrderDetail(OrderNew order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode());
        dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
        Long amount = order.getFinalAmount() != null ? order.getFinalAmount()
                : (order.getPaidAmount() != null ? order.getPaidAmount() : order.getTotalAmount());
        dto.setTotalAmount(centsToYuan(amount));
        dto.setRefundAmount(centsToYuan(order.getRefundAmount()));
        dto.setStatus(convertNewOrderStatusToString(order.getOrderStatus()));
        dto.setOrderType(order.getOrderType());
        dto.setOrderSource(order.getOrderSource());
        dto.setTableNumber(order.getTableName());
        dto.setContactName(order.getCustomerName());
        dto.setContactPhone(resolvePhone(order));
        dto.setRemarks(order.getRemark());

        // 支付信息来自 order_payment_records（新表订单主表不存支付方式/流水号）
        List<OrderPaymentRecordNew> payments = orderPaymentRecordNewMapper.selectByOrderId(order.getOrderId());
        if (payments != null && !payments.isEmpty()) {
            OrderPaymentRecordNew last = payments.get(payments.size() - 1);
            dto.setPaymentMethod(convertPaymentMethodToString(last.getPaymentMethod()));
            dto.setTransactionId(last.getTransactionNo());
        } else if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            dto.setPaymentMethod("已支付");
        }

        KitchenOrder kitchenOrder = findKitchenOrder(order);
        if (kitchenOrder != null) {
            dto.setKitchenStatus(kitchenOrder.getStatus());
            dto.setPickupNumber(kitchenOrder.getPickupNumber());
            dto.setPickupCode(kitchenOrder.getPickupCode());
        }

        // POS 叫号号：详情页/小票以 callNumber 展示（订单号仅作对账口径）。
        // 老单（改造前创建、call_number 为空）在此补取号，保证叫号屏/小票也能显示 A001 式号码。
        String callNumber = order.getCallNumber();
        if (callNumber == null || callNumber.isEmpty()) {
            try {
                callNumber = posCallNumberService.ensureCallNumber(order.getOrderId());
            } catch (Exception e) {
                log.warn("订单详情补取叫号号失败（不影响查询）: orderId={}, error={}", order.getOrderId(), e.getMessage());
            }
        }
        dto.setCallNumber(callNumber);
        if (callNumber != null && !callNumber.isEmpty()) {
            dto.setPickupNumber(callNumber);
        }

        List<OrderItemNew> items = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>().eq(OrderItemNew::getOrderId, order.getOrderId()));
        dto.setItems(items.stream().map(item -> {
            OrderDetailDTO.OrderItemDetail itemDto = new OrderDetailDTO.OrderItemDetail();
            itemDto.setId(item.getItemId());
            itemDto.setName(item.getProductName());
            itemDto.setPrice(centsToYuan(item.getUnitPrice()));
            itemDto.setQuantity(item.getQuantity());
            return itemDto;
        }).collect(Collectors.toList()));

        return dto;
    }

    private String resolvePhone(OrderNew order) {
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
            return sensitiveDataService.decryptPhone(order.getCustomerPhone());
        }
        if (order.getCustomerPhoneEncrypted() != null && !order.getCustomerPhoneEncrypted().isEmpty()) {
            return sensitiveDataService.decryptPhone(order.getCustomerPhoneEncrypted());
        }
        return null;
    }

    private KitchenOrder findKitchenOrder(OrderNew order) {
        String number = order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode();
        if (number != null) {
            KitchenOrder byNumber = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderNumber, number).last("LIMIT 1"));
            if (byNumber != null) {
                return byNumber;
            }
        }
        return kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, order.getOrderId()).last("LIMIT 1"));
    }

    private OrderQueryDTO convertNewToQueryDTO(OrderNew order) {
        OrderQueryDTO dto = new OrderQueryDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode());
        dto.setCallNumber(order.getCallNumber());
        dto.setPickupNumber(order.getCallNumber());
        Long amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getPaidAmount();
        dto.setTotalAmount(centsToYuan(amount));
        dto.setStatus(convertNewOrderStatusToString(order.getOrderStatus()));
        dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
        return dto;
    }

    private CustomerOrderDTO convertNewToCustomerOrderDTO(OrderNew order) {
        CustomerOrderDTO dto = new CustomerOrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode());
        dto.setStatus(convertNewOrderStatusToString(order.getOrderStatus()));
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreateTime(order.getCreateTime());
        Long amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        dto.setTotalAmount(centsToYuan(amount));

        KitchenOrder kitchenOrder = findKitchenOrder(order);
        if (kitchenOrder != null) {
            dto.setTableNumber(kitchenOrder.getTableNumber());
            dto.setTotalDishes(kitchenOrder.getTotalDishes());
        }

        List<OrderItemNew> items = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>().eq(OrderItemNew::getOrderId, order.getOrderId()));
        if (items != null && !items.isEmpty()) {
            dto.setItems(items.stream().map(item -> {
                CustomerOrderDTO.OrderItemDTO itemDto = new CustomerOrderDTO.OrderItemDTO();
                itemDto.setName(item.getProductName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setPrice(centsToYuan(item.getUnitPrice()));
                return itemDto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    private OrderResultDTO buildOrderResultDTO(OrderNew order, KitchenOrder kitchenOrder) {
        OrderResultDTO result = new OrderResultDTO();
        result.setOrderId(order.getOrderId());
        result.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderCode());
        result.setStatus(order.getOrderStatus() != null ? String.valueOf(order.getOrderStatus()) : "0");
        Long amount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getPaidAmount();
        result.setTotalAmount(centsToYuan(amount));
        result.setOrderSource(order.getOrderSource());
        result.setOrderType(order.getOrderType() != null ? String.valueOf(order.getOrderType()) : "0");
        result.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "");
        // 小票显示叫号号（callNumber）：老单无号时补取，避免小票打印出订单号 T2026…
        String receiptCallNumber = order.getCallNumber();
        if (receiptCallNumber == null || receiptCallNumber.isEmpty()) {
            try {
                receiptCallNumber = posCallNumberService.ensureCallNumber(order.getOrderId());
            } catch (Exception e) {
                log.warn("小票补取叫号号失败（不影响重打）: orderId={}, error={}", order.getOrderId(), e.getMessage());
            }
        }
        result.setCallNumber(receiptCallNumber);
        result.setPickupNumber(receiptCallNumber != null && !receiptCallNumber.isEmpty()
                ? receiptCallNumber
                : (kitchenOrder != null ? kitchenOrder.getPickupNumber() : ""));
        result.setPickupCode(kitchenOrder != null ? kitchenOrder.getPickupCode() : "");
        result.setTableNumber(kitchenOrder != null ? kitchenOrder.getTableNumber() : order.getTableName());

        List<OrderItemNew> items = orderItemNewMapper.selectList(
                new LambdaQueryWrapper<OrderItemNew>().eq(OrderItemNew::getOrderId, order.getOrderId()));
        result.setOrderItems(items.stream().map(item -> {
            OrderResultDTO.OrderItemInfo itemResult = new OrderResultDTO.OrderItemInfo();
            itemResult.setName(item.getProductName());
            itemResult.setPrice(centsToYuan(item.getUnitPrice()));
            itemResult.setQuantity(item.getQuantity());
            return itemResult;
        }).collect(Collectors.toList()));

        return result;
    }

    // ==================== 遗留表（orders_legacy）辅助方法 ====================

    private CustomerOrderDTO convertToCustomerOrderDTO(Order order) {
        CustomerOrderDTO dto = new CustomerOrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(convertOrderStatusToString(order.getOrderStatus()));
        dto.setOrderStatus(order.getOrderStatus());
        dto.setCreateTime(order.getCreateTime());
        dto.setTotalAmount(order.getActualAmount() != null ? order.getActualAmount() : order.getOrderAmount());

        KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(
            new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getOrderId, order.getOrderId()));
        if (kitchenOrder != null) {
            dto.setTableNumber(kitchenOrder.getTableNumber());
            dto.setTotalDishes(kitchenOrder.getTotalDishes());
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getOrderId()));
        if (orderItems != null && !orderItems.isEmpty()) {
            List<CustomerOrderDTO.OrderItemDTO> items = orderItems.stream().map(oi -> {
                CustomerOrderDTO.OrderItemDTO itemDto = new CustomerOrderDTO.OrderItemDTO();
                itemDto.setName(oi.getFoodName());
                itemDto.setQuantity(oi.getQuantity());
                itemDto.setPrice(oi.getUnitPrice());
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(items);
        }

        return dto;
    }

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
        }).collect(Collectors.toList());
        result.setOrderItems(itemResults);

        return result;
    }

    private String convertPaymentMethodToString(Integer paymentMethod) {
        if (paymentMethod == null) return "现金";
        return switch (paymentMethod) {
            case 0 -> "微信支付";
            case 1 -> "支付宝";
            case 2 -> "现金";
            case 3 -> "银行卡";
            case 4 -> "余额支付";
            default -> "现金";
        };
    }

    private String convertOrderStatusToString(Integer orderStatus) {
        if (orderStatus == null) return "pending";
        return switch (orderStatus) {
            case 0 -> "pending";
            case 1 -> "paid";
            case 2 -> "making";
            case 3 -> "delivering";
            case 4 -> "completed";
            case 5 -> "cancelled";
            case 6 -> "refunding";
            case 7 -> "refunded";
            default -> "pending";
        };
    }
}
