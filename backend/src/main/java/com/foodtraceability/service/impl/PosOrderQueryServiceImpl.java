package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
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
 * POS订单查询服务实现类
 * 负责订单查询、详情获取、小票重打等业务逻辑
 */
@Service
public class PosOrderQueryServiceImpl implements PosOrderQueryService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderQueryServiceImpl.class);

    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SensitiveDataService sensitiveDataService;
    private final ReceiptPrinterService receiptPrinterService;

    public PosOrderQueryServiceImpl(
            KitchenOrderMapper kitchenOrderMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            SensitiveDataService sensitiveDataService,
            ReceiptPrinterService receiptPrinterService) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.sensitiveDataService = sensitiveDataService;
        this.receiptPrinterService = receiptPrinterService;
    }

    /**
     * 获取订单详情
     */
    public Result<OrderDetailDTO> getOrderDetail(String orderNumber) {
        log.info("获取订单详情: {}", orderNumber);
        try {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber));
            if (order == null) {
                return Result.error("订单不存在");
            }

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
     * 查询订单列表
     */
    public Result<List<OrderQueryDTO>> getOrders(String startDate, String endDate, String keyword) {
        log.info("查询订单列表: startDate={}, endDate={}, keyword={}", startDate, endDate, keyword);
        try {
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            // 性能优化：未指定时间范围时默认查最近 7 天，避免全表扫描
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
            // 性能优化：限制最多返回 200 条，避免大量数据导致前端卡顿
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
     * 根据桌台号查询订单
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
     * 通过openid查询订单列表
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
     * 重新打印小票
     */
    public Result<Boolean> reprintReceipt(String orderId) {
        log.info("处理重打小票请求: orderId={}", orderId);
        try {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
            if (order == null) {
                log.warn("重打小票失败: 订单不存在, orderId={}", orderId);
                return Result.error("订单不存在");
            }

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

    // ==================== 私有方法 ====================

    /**
     * 转换Order实体为CustomerOrderDTO
     */
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
        }).collect(Collectors.toList());
        result.setOrderItems(itemResults);

        return result;
    }

    /**
     * 转换支付方式为字符串
     */
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

    /**
     * 转换订单状态为字符串
     */
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
