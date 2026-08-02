package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.OrderRequest;
import com.foodtraceability.dto.OrderResponse;
import com.foodtraceability.dto.finance.FinanceRecordCreateDTO;
import com.foodtraceability.entity.Order;
import com.foodtraceability.entity.OrderItem;
import com.foodtraceability.mapper.OrderMapper;
import com.foodtraceability.mapper.OrderItemMapper;
import com.foodtraceability.service.OrderService;
import com.foodtraceability.service.finance.FinanceRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务实现类
 * 实现订单管理相关的业务逻辑
 */
@Service
public class OrderServiceImpl implements OrderService {
    

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, FinanceRecordService financeRecordService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.financeRecordService = financeRecordService;
    }

    private final OrderMapper orderMapper;
    
    private final OrderItemMapper orderItemMapper;
    
    private final FinanceRecordService financeRecordService;
    
    /**
     * 创建订单
     * @param orderRequest 订单请求DTO
     * @return 订单响应DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // 生成订单ID和订单编号
        String orderId = UUID.randomUUID().toString().replace("-", "");
        String orderNumber = generateOrderNumber();
        
        // 创建订单实体
        Order order = new Order();
        // 手动复制属性以避免类型安全警告
        copyOrderRequestToOrder(orderRequest, order);
        order.setOrderId(orderId);
        order.setOrderNumber(orderNumber);
        order.setOrderStatus(0); // 初始状态：待支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy("system");
        order.setUpdateBy("system");
        order.setDeleted(0);
        
        // 保存订单
        orderMapper.insert(order);
        
        // 保存订单项
        // 这里需要根据orderRequest中的orderItems创建OrderItem实体并保存
        
        // 转换为响应DTO
        return convertToResponse(order);
    }
    
    /**
     * 根据ID获取订单
     * @param orderId 订单ID
     * @return 订单响应DTO
     */
    @Override
    public OrderResponse getOrderById(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        return convertToResponse(order);
    }
    
    /**
     * 分页查询订单列表
     * @param pageParam 分页参数
     * @param orderNumber 订单编号
     * @param orderType 订单类型
     * @param orderStatus 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单分页列表
     */
    @Override
    public Page<OrderResponse> getOrderPage(Page<Order> pageParam, String orderNumber, Integer orderType, Integer orderStatus, String startTime, String endTime) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        
        // 构建查询条件
        if (orderNumber != null && !orderNumber.isEmpty()) {
            queryWrapper.like("order_number", orderNumber);
        }
        if (orderType != null) {
            queryWrapper.eq("order_type", orderType);
        }
        if (orderStatus != null) {
            queryWrapper.eq("order_status", orderStatus);
        }
        if (startTime != null && !startTime.isEmpty()) {
            queryWrapper.ge("create_time", startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            queryWrapper.le("create_time", endTime);
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc("create_time");
        
        // 执行分页查询
        Page<Order> orderPage = orderMapper.selectPage(pageParam, queryWrapper);
        
        // 转换为响应DTO
        Page<OrderResponse> responsePage = new Page<>();
        // 手动复制分页属性以避免类型安全警告
        responsePage.setCurrent(orderPage.getCurrent());
        responsePage.setSize(orderPage.getSize());
        responsePage.setTotal(orderPage.getTotal());
        responsePage.setPages(orderPage.getPages());
        
        List<OrderResponse> orderResponses = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            orderResponses.add(convertToResponse(order));
        }
        responsePage.setRecords(orderResponses);
        
        return responsePage;
    }
    
    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param orderStatus 订单状态
     * @return 更新后的订单响应DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(String orderId, Integer orderStatus) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        Integer oldStatus = order.getOrderStatus();
        // 更新订单状态
        order.setOrderStatus(orderStatus);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy("system");
        
        orderMapper.updateById(order);
        
        // 订单支付成功，生成财务收入记录
        if (orderStatus == 1 && oldStatus != 1) {
            createFinanceRecord(order, "1"); // 1表示收入
        }
        
        // 订单退款成功，生成财务支出记录
        if (orderStatus == 7 && oldStatus != 7) {
            createFinanceRecord(order, "2"); // 2表示支出
        }
        
        return convertToResponse(order);
    }
    
    /**
     * 创建财务收支记录
     * @param order 订单信息
     * @param recordType 记录类型：1-收入，2-支出
     */
    private void createFinanceRecord(Order order, String recordType) {
        // 构造收支记录创建DTO
        FinanceRecordCreateDTO dto = new FinanceRecordCreateDTO();

        if ("1".equals(recordType)) {
            // 收入记录
            dto.setRecordType(1); // 1-收入
            dto.setRecordCategory(101); // 101-销售收入
            BigDecimal amountYuan = order.getActualAmount() != null
                    ? order.getActualAmount() : BigDecimal.ZERO;
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            dto.setRemark("订单收入：" + order.getOrderNumber());
        } else {
            // 支出记录（退款）
            dto.setRecordType(2); // 2-支出
            dto.setRecordCategory(205); // 205-其他支出
            BigDecimal amountYuan = order.getRefundAmount() != null
                    ? order.getRefundAmount() : BigDecimal.ZERO;
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            dto.setRemark("订单退款：" + order.getOrderNumber());
        }

        dto.setBusinessDate(LocalDate.now());

        // 创建收支记录
        financeRecordService.create(dto);
    }
    
    /**
     * 更新订单信息
     * @param orderId 订单ID
     * @param orderRequest 订单请求DTO
     * @return 更新后的订单响应DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(String orderId, OrderRequest orderRequest) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 更新订单信息
        // 手动复制属性以避免类型安全警告
        copyOrderRequestToOrder(orderRequest, order);
        order.setOrderId(orderId);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy("system");
        
        orderMapper.updateById(order);
        
        // 更新订单项
        // 这里需要根据orderRequest中的orderItems更新OrderItem实体
        
        return convertToResponse(order);
    }
    
    /**
     * 根据ID删除订单
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 逻辑删除订单
        order.setDeleted(1);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy("system");
        
        orderMapper.updateById(order);
        
        // 删除订单项
        // 这里需要删除该订单下的所有订单项
    }
    
    /**
     * 批量更新订单状态
     * @param orderIds 订单ID列表
     * @param orderStatus 订单状态
     * @return 更新成功的订单数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateOrderStatus(List<String> orderIds, Integer orderStatus) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("order_id", orderIds);
        
        Order order = new Order();
        order.setOrderStatus(orderStatus);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy("system");
        
        return orderMapper.update(order, queryWrapper);
    }
    
    /**
     * 批量删除订单
     * @param orderIds 订单ID列表
     * @return 删除成功的订单数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteOrders(List<String> orderIds) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("order_id", orderIds);
        
        Order order = new Order();
        order.setDeleted(1);
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy("system");
        
        return orderMapper.update(order, queryWrapper);
    }
    
    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");
        
        List<Order> orders = orderMapper.selectList(queryWrapper);
        List<OrderResponse> orderResponses = new ArrayList<>();
        
        for (Order order : orders) {
            orderResponses.add(convertToResponse(order));
        }
        
        return orderResponses;
    }
    
    /**
     * 查询订单统计数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单统计数据
     */
    @Override
    public List<Object> getOrderStatistics(String startTime, String endTime) {
        // 根据时间范围查询订单统计数据，如订单数量、总金额等
        return new ArrayList<>();
    }
    
    /**
     * 查询订单趋势数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单趋势数据
     */
    @Override
    public List<Object> getOrderTrends(String startTime, String endTime) {
        // 根据时间范围查询订单趋势数据，如按天/周/月统计订单数量和金额
        return new ArrayList<>();
    }
    
    /**
     * 生成订单编号
     * @return 订单编号
     */
    private String generateOrderNumber() {
        // 订单编号格式：ORD + 年月日时分秒 + 6位随机数
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", (int)(Math.random() * 1000000));
        return "ORD" + timestamp + random;
    }
    
    /**
     * 将OrderRequest转换为Order实体
     * @param orderRequest 订单请求DTO
     * @param order 订单实体
     */
    private void copyOrderRequestToOrder(OrderRequest orderRequest, Order order) {
        // 手动复制属性，避免类型安全警告
        // 注意：这里需要处理类型转换，因为OrderRequest和Order实体的字段类型可能不同
        if (orderRequest.getUserId() != null) {
            order.setUserId(orderRequest.getUserId());
        }
        if (orderRequest.getOrderType() != null) {
            order.setOrderType(orderRequest.getOrderType());
        }
        if (orderRequest.getOrderSource() != null) {
            order.setOrderSource(orderRequest.getOrderSource());
        }
        if (orderRequest.getPaymentMethod() != null) {
            order.setPaymentMethod(orderRequest.getPaymentMethod());
        }
        if (orderRequest.getDiscountAmount() != null) {
            order.setDiscountAmount(orderRequest.getDiscountAmount());
        }
        if (orderRequest.getActualAmount() != null) {
            order.setActualAmount(orderRequest.getActualAmount());
        }
        if (orderRequest.getDeliveryAddress() != null) {
            order.setDeliveryAddress(orderRequest.getDeliveryAddress());
        }
        if (orderRequest.getContactName() != null) {
            order.setContactName(orderRequest.getContactName());
        }
        if (orderRequest.getContactPhone() != null) {
            order.setContactPhone(orderRequest.getContactPhone());
        }
        if (orderRequest.getRemarks() != null) {
            order.setRemarks(orderRequest.getRemarks());
        }
        if (orderRequest.getMerchantId() != null) {
            order.setMerchantId(orderRequest.getMerchantId());
        }
        if (orderRequest.getEstimatedDeliveryTime() != null) {
            order.setEstimatedDeliveryTime(orderRequest.getEstimatedDeliveryTime());
        }
    }
    
    /**
     * 将订单实体转换为响应DTO
     * @param order 订单实体
     * @return 订单响应DTO
     */
    @SuppressWarnings("null")
    private OrderResponse convertToResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        BeanUtils.copyProperties(order, orderResponse);
        
        // 查询该订单下的所有订单项并转换为OrderItemResponse
        QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", order.getOrderId());
        List<OrderItem> orderItems = orderItemMapper.selectList(queryWrapper);
        
        List<OrderResponse.OrderItemResponse> orderItemResponses = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            OrderResponse.OrderItemResponse itemResponse = new OrderResponse.OrderItemResponse();
            BeanUtils.copyProperties(orderItem, itemResponse);
            orderItemResponses.add(itemResponse);
        }
        
        orderResponse.setOrderItems(orderItemResponses);
        
        return orderResponse;
    }
}
