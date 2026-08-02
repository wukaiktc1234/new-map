package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.mapper.CallRecordMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.service.CallNumberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 叫号管理服务实现类
 */
@Service
public class CallNumberServiceImpl implements CallNumberService {

    private static final Logger log = LoggerFactory.getLogger(CallNumberServiceImpl.class);

    private final CallRecordMapper callRecordMapper;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public CallNumberServiceImpl(CallRecordMapper callRecordMapper,
                                  KitchenOrderMapper kitchenOrderMapper,
                                  SimpMessagingTemplate messagingTemplate) {
        this.callRecordMapper = callRecordMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallRecord> getPendingOrders() {
        return callRecordMapper.findActiveOrders();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallRecord> getHistoryByDate(LocalDate date) {
        return callRecordMapper.findByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getStats() {
        int pending = callRecordMapper.countByStatusToday("pending");
        int called = callRecordMapper.countByStatusToday("called");
        int picked = callRecordMapper.countByStatusToday("picked");
        return Map.of("pending", pending, "called", called, "picked", picked);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallRecord callNumber(String orderId, String orderNumber, String tableNumber,
                                 String orderType, Integer itemCount) {
        log.info("叫号请求: orderId={}, orderNumber={}", orderId, orderNumber);
        CallRecord record = callRecordMapper.findByOrderId(orderId);
        if (record == null) {
            record = new CallRecord();
            record.setOrderId(orderId);
            record.setOrderNumber(orderNumber);
            record.setTableNumber(tableNumber);
            record.setOrderType(orderType);
            record.setItemCount(itemCount != null ? itemCount : 0);
            record.setStatus("called");
            record.setCallCount(1);
            record.setFirstCallTime(LocalDateTime.now());
            record.setLastCallTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            callRecordMapper.insert(record);
            log.info("创建新叫号记录: {}", record.getOrderNumber());
        } else {
            callRecordMapper.updateCallStatus(orderId);
            record = callRecordMapper.findByOrderId(orderId);
            log.info("更新叫号记录: {}, 叫号次数: {}", record.getOrderNumber(), record.getCallCount());
        }
        messagingTemplate.convertAndSend("/topic/call-number/called", record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallRecord recallNumber(String orderId) {
        log.info("重叫请求: orderId={}", orderId);
        CallRecord record = callRecordMapper.findByOrderId(orderId);
        if (record == null) {
            return null;
        }
        callRecordMapper.updateCallStatus(orderId);
        record = callRecordMapper.findByOrderId(orderId);
        messagingTemplate.convertAndSend("/topic/call-number/called", record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallRecord markPicked(String orderId) {
        log.info("取餐请求: orderId={}", orderId);
        CallRecord record = callRecordMapper.findByOrderId(orderId);
        if (record == null) {
            return null;
        }
        callRecordMapper.updatePickStatus(orderId);
        record = callRecordMapper.findByOrderId(orderId);
        messagingTemplate.convertAndSend("/topic/call-number/picked", record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromKitchen() {
        log.info("开始从后厨订单同步待取餐订单...");
        QueryWrapper<KitchenOrder> query = new QueryWrapper<>();
        query.eq("status", "completed");
        query.eq("deleted", 0);
        List<KitchenOrder> completedOrders = kitchenOrderMapper.selectList(query);
        int syncCount = 0;
        for (KitchenOrder ko : completedOrders) {
            CallRecord existing = callRecordMapper.findByOrderId(ko.getOrderId());
            if (existing == null) {
                CallRecord record = new CallRecord();
                record.setOrderId(ko.getOrderId());
                record.setOrderNumber(ko.getOrderNumber());
                record.setTableNumber(ko.getTableNumber());
                record.setOrderType(getOrderTypeName(ko.getOrderType()));
                record.setItemCount(ko.getTotalDishes() != null ? ko.getTotalDishes() : 0);
                record.setStatus("pending");
                record.setCallCount(0);
                record.setCreateTime(LocalDateTime.now());
                callRecordMapper.insert(record);
                syncCount++;
            }
        }
        log.info("同步完成，新增 {} 条待取餐记录", syncCount);
        return syncCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallRecord createFromOrder(String orderId, String orderNumber, String tableNumber,
                                      String orderType, Integer itemCount) {
        log.info("从订单创建待取餐记录: orderId={}, orderNumber={}", orderId, orderNumber);
        CallRecord existing = callRecordMapper.findByOrderId(orderId);
        if (existing != null) {
            return existing;
        }
        CallRecord record = new CallRecord();
        record.setOrderId(orderId);
        record.setOrderNumber(orderNumber);
        record.setTableNumber(tableNumber);
        record.setOrderType(orderType);
        record.setItemCount(itemCount != null ? itemCount : 0);
        record.setStatus("pending");
        record.setCallCount(0);
        record.setCreateTime(LocalDateTime.now());
        callRecordMapper.insert(record);
        messagingTemplate.convertAndSend("/topic/call-number/new", record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupOldRecords(int days) {
        int deleted = callRecordMapper.deleteOldRecords(days);
        log.info("清理了 {} 天前的 {} 条叫号记录", days, deleted);
        return deleted;
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
