package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.CallRecord;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.mapper.CallRecordMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.service.CallNumberService;
import com.foodtraceability.service.PosCallNumberService;
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
    /** POS 叫号号服务：叫号屏记录补 callNumber（A001 式），避免屏上直接播订单号 T2026… */
    private final PosCallNumberService posCallNumberService;

    public CallNumberServiceImpl(CallRecordMapper callRecordMapper,
                                  KitchenOrderMapper kitchenOrderMapper,
                                  SimpMessagingTemplate messagingTemplate,
                                  PosCallNumberService posCallNumberService) {
        this.callRecordMapper = callRecordMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.messagingTemplate = messagingTemplate;
        this.posCallNumberService = posCallNumberService;
    }

    /**
     * 取订单的 POS 叫号号（缺失则补取号，订单不在 orders 新表时返回 null）。
     * 异常一律吞掉：叫号屏不能因为号段问题整屏失败。
     */
    private String resolveCallNumber(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }
        try {
            return posCallNumberService.ensureCallNumber(orderId);
        } catch (Exception e) {
            log.warn("叫号号解析失败（叫号屏回落订单号）: orderId={}, error={}", orderId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CallRecord> getPendingOrders() {
        List<CallRecord> records = callRecordMapper.findActiveOrders();
        if (records != null) {
            // 叫号屏兜底：历史/其它入口写入的记录可能没有叫号号，这里补齐并回写（幂等）
            for (CallRecord record : records) {
                if (record == null || (record.getCallNumber() != null && !record.getCallNumber().isEmpty())) {
                    continue;
                }
                String callNumber = resolveCallNumber(record.getOrderId());
                if (callNumber == null || callNumber.isEmpty()) {
                    continue;
                }
                record.setCallNumber(callNumber);
                posCallNumberService.syncToCallRecord(record.getOrderId(), callNumber);
            }
        }
        return records;
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
        String callNumber = resolveCallNumber(orderId);
        if (record == null) {
            record = new CallRecord();
            record.setOrderId(orderId);
            record.setOrderNumber(orderNumber);
            record.setCallNumber(callNumber);
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
            if (callNumber != null) {
                posCallNumberService.syncToCallRecord(orderId, callNumber);
            }
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
                record.setCallNumber(resolveCallNumber(ko.getOrderId()));
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
        record.setCallNumber(resolveCallNumber(orderId));
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
