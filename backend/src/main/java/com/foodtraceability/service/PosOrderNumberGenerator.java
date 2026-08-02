package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.Order;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * POS订单号生成器
 * 负责生成订单号、取餐号、取餐码等唯一标识
 */
@Component
public class PosOrderNumberGenerator {

    private static final Logger log = LoggerFactory.getLogger(PosOrderNumberGenerator.class);

    /**
     * 订单ID原子计数器，用于生成唯一订单ID
     * 初始值为当前时间戳，避免高并发下的ID碰撞问题
     */
    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis());

    /**
     * 订单号生成锁，保证线程安全
     */
    private static final Object orderNumberLock = new Object();

    private final KitchenOrderMapper kitchenOrderMapper;
    private final OrderMapper orderMapper;

    public PosOrderNumberGenerator(KitchenOrderMapper kitchenOrderMapper, OrderMapper orderMapper) {
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 获取并递增订单计数器
     * @return 当前计数值
     */
    public long incrementAndGetCounter() {
        return orderCounter.incrementAndGet();
    }

    /**
     * 生成订单号（基于数据库序列）
     * 优先使用数据库序列，失败时降级为同步方法
     * @param tableNumber 桌号
     * @return 订单号
     */
    public String generateOrderNumberByDbSequence(Integer tableNumber) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(dateFormatter);
        String prefix = tableNumber != null ? "T" : "W";
        String sequenceKey = prefix + dateStr;
        try {
            orderMapper.initSequence(sequenceKey);
            orderMapper.incrementSequence(sequenceKey);
            int sequence = orderMapper.getLastInsertId();
            return sequenceKey + String.format("%03d", sequence);
        } catch (Exception e) {
            log.warn("数据库序列生成失败，降级使用同步方法: {}", e.getMessage());
            return generateOrderNumberSafe(tableNumber);
        }
    }

    /**
     * 根据订单类型生成取餐号
     * @param orderType 订单类型
     * @return 取餐号
     */
    public String generatePickupNumberByType(Integer orderType) {
        String prefix = switch (orderType) {
            case 0 -> "A";
            case 1 -> "W";
            case 2 -> "P";
            default -> "A";
        };
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(dateFormatter);
        String todayPrefix = "T" + dateStr;
        Long todayOrderCount = kitchenOrderMapper.selectCount(
            new LambdaQueryWrapper<KitchenOrder>().likeRight(KitchenOrder::getOrderNumber, todayPrefix));
        int sequence = todayOrderCount != null ? todayOrderCount.intValue() + 1 : 1;
        String sequenceStr = String.format("%03d", sequence);
        return prefix + sequenceStr;
    }

    /**
     * 生成取餐码（4位随机数字）
     * @return 取餐码
     */
    public String generatePickupCode() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    /**
     * 生成订单号（基础方法）
     * @param tableNumber 桌号
     * @return 订单号
     */
    private String generateOrderNumber(Integer tableNumber) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(dateFormatter);
        String prefix = tableNumber != null ? "T" : "W";
        String todayPrefix = prefix + dateStr;
        int maxSequence = 0;

        List<KitchenOrder> todayKitchenOrders = kitchenOrderMapper.selectList(
            new LambdaQueryWrapper<KitchenOrder>()
                .likeRight(KitchenOrder::getOrderNumber, todayPrefix)
                .orderByDesc(KitchenOrder::getOrderNumber)
                .last("LIMIT 1"));
        if (!todayKitchenOrders.isEmpty()) {
            String lastOrderNumber = todayKitchenOrders.get(0).getOrderNumber();
            if (lastOrderNumber != null && lastOrderNumber.length() > todayPrefix.length()) {
                try {
                    String seqStr = lastOrderNumber.substring(todayPrefix.length());
                    maxSequence = Math.max(maxSequence, Integer.parseInt(seqStr));
                } catch (NumberFormatException e) {
                    // 忽略解析异常
                }
            }
        }

        List<Order> todayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .likeRight(Order::getOrderNumber, todayPrefix)
                .orderByDesc(Order::getOrderNumber)
                .last("LIMIT 1"));
        if (!todayOrders.isEmpty()) {
            String lastOrderNumber = todayOrders.get(0).getOrderNumber();
            if (lastOrderNumber != null && lastOrderNumber.length() > todayPrefix.length()) {
                try {
                    String seqStr = lastOrderNumber.substring(todayPrefix.length());
                    maxSequence = Math.max(maxSequence, Integer.parseInt(seqStr));
                } catch (NumberFormatException e) {
                    // 忽略解析异常
                }
            }
        }

        int sequence = maxSequence + 1;
        String sequenceStr = String.format("%03d", sequence);
        return todayPrefix + sequenceStr;
    }

    /**
     * 生成订单号（线程安全）
     * @param tableNumber 桌号
     * @return 订单号
     */
    private String generateOrderNumberSafe(Integer tableNumber) {
        synchronized (orderNumberLock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return generateOrderNumber(tableNumber);
        }
    }
}
