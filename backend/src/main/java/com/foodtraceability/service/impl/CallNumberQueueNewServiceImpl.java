package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.store.operation.CallNumberQueueQueryDTO;
import com.foodtraceability.entity.CallNumberQueueNew;
import com.foodtraceability.mapper.CallNumberQueueNewMapper;
import com.foodtraceability.service.CallNumberQueueNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 叫号队列服务实现类
 */
@Service
public class CallNumberQueueNewServiceImpl implements CallNumberQueueNewService {

    private static final Logger log = LoggerFactory.getLogger(CallNumberQueueNewServiceImpl.class);
    private static final DateTimeFormatter TICKET_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final CallNumberQueueNewMapper callNumberQueueNewMapper;

    public CallNumberQueueNewServiceImpl(CallNumberQueueNewMapper callNumberQueueNewMapper) {
        this.callNumberQueueNewMapper = callNumberQueueNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallNumberQueueNew takeNumber(Long storeId, Integer queueType, Integer peopleCount, String tablePreference) {
        log.info("取号, storeId: {}, 类型: {}, 人数: {}", storeId, queueType, peopleCount);

        // 获取今日最大号码
        Integer maxNum = callNumberQueueNewMapper.getMaxTicketNumberToday(queueType);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;

        // 生成号码（根据类型加前缀）
        String prefix = getPrefix(queueType);
        String ticketNumber = prefix + String.format("%03d", nextNum);

        CallNumberQueueNew queue = new CallNumberQueueNew();
        queue.setStoreId(storeId);
        queue.setTicketNumber(ticketNumber);
        queue.setQueueType(queueType);
        queue.setPeopleCount(peopleCount != null ? peopleCount : 1);
        queue.setTablePreference(tablePreference);
        queue.setStatus(1); // 等待中
        queue.setCalledCount(0);
        callNumberQueueNewMapper.insert(queue);

        log.info("取号成功, 号码: {}", ticketNumber);
        return queue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallNumberQueueNew callNext(Integer queueType) {
        log.info("叫下一个, 类型: {}", queueType);

        List<CallNumberQueueNew> waitingList = callNumberQueueNewMapper.selectWaitingByType(queueType);
        if (waitingList.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前没有等待中的顾客");
        }

        CallNumberQueueNew next = waitingList.get(0);
        callNumberQueueNewMapper.callNumber(next.getQueueId(), 2); // 2=已叫号

        log.info("叫号成功, 号码: {}", next.getTicketNumber());
        return callNumberQueueNewMapper.selectById(next.getQueueId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reCall(Long queueId) {
        log.info("重叫, queueId: {}", queueId);
        callNumberQueueNewMapper.callNumber(queueId, 2); // 重新标记为已叫号
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDined(Long queueId) {
        log.info("标记已用餐, queueId: {}", queueId);
        callNumberQueueNewMapper.callNumber(queueId, 4); // 4=已用餐
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long queueId) {
        log.info("取消排队, queueId: {}", queueId);
        CallNumberQueueNew queue = callNumberQueueNewMapper.selectById(queueId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "排队记录不存在");
        }
        queue.setStatus(5); // 已取消
        callNumberQueueNewMapper.updateById(queue);
    }

    @Override
    public Map<String, Object> getQueueStatus(Integer queueType) {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> counts = callNumberQueueNewMapper.countByStatusToday(queueType);
        long waiting = 0L, called = 0L, dined = 0L, expired = 0L;
        for (Map<String, Object> item : counts) {
            int s = ((Number) item.get("status")).intValue();
            long c = ((Number) item.get("cnt")).longValue();
            switch (s) {
                case 1: waiting = c; break;
                case 2: called = c; break;
                case 3: expired = c; break;
                case 4: dined = c; break;
                default: break;
            }
        }
        status.put("waiting", waiting);
        status.put("called", called);
        status.put("dined", dined);
        status.put("expired", expired);
        return status;
    }

    @Override
    public List<CallNumberQueueNew> getWaitingList(Integer queueType) {
        return callNumberQueueNewMapper.selectWaitingByType(queueType);
    }

    @Override
    public IPage<CallNumberQueueNew> listByStoreId(Long storeId, CallNumberQueueQueryDTO query) {
        log.info("按门店查询叫号记录, storeId: {}", storeId);
        Page<CallNumberQueueNew> page = new Page<>(query.getPage(), query.getSize());
        return callNumberQueueNewMapper.selectByStoreId(page, storeId, query.getStatus(), query.getQueueType(), query.getKeyword(), query.getStartDate(), query.getEndDate());
    }

    @Override
    public Map<String, Object> getStatsByStoreId(Long storeId) {
        log.info("按门店统计叫号, storeId: {}", storeId);
        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> counts = callNumberQueueNewMapper.countGroupByStatus(storeId);
        long waitingCount = 0L, calledCount = 0L, dinedCount = 0L, expiredCount = 0L, cancelledCount = 0L;
        long total = 0L;
        for (Map<String, Object> item : counts) {
            int s = ((Number) item.get("status")).intValue();
            long c = ((Number) item.get("cnt")).longValue();
            total += c;
            switch (s) {
                case 1: waitingCount = c; break;
                case 2: calledCount = c; break;
                case 3: expiredCount = c; break;
                case 4: dinedCount = c; break;
                case 5: cancelledCount = c; break;
                default: break;
            }
        }

        double avgWaitMinutes = 0.0;
        List<CallNumberQueueNew> calledRecords = callNumberQueueNewMapper.selectList(
                new LambdaQueryWrapper<CallNumberQueueNew>()
                        .eq(CallNumberQueueNew::getStoreId, storeId)
                        .apply("DATE(create_time) = CURRENT_DATE")
                        .isNotNull(CallNumberQueueNew::getCallTime)
                        .isNotNull(CallNumberQueueNew::getCreateTime)
        );
        if (!calledRecords.isEmpty()) {
            long totalWaitMinutes = 0L;
            for (CallNumberQueueNew r : calledRecords) {
                totalWaitMinutes += ChronoUnit.MINUTES.between(r.getCreateTime(), r.getCallTime());
            }
            avgWaitMinutes = (double) totalWaitMinutes / calledRecords.size();
        }

        stats.put("waitingCount", waitingCount);
        stats.put("calledCount", calledCount);
        stats.put("dinedCount", dinedCount);
        stats.put("expiredCount", expiredCount);
        stats.put("cancelledCount", cancelledCount);
        stats.put("avgWaitMinutes", avgWaitMinutes);
        return stats;
    }

    /**
     * 根据排队类型获取号码前缀
     */
    private String getPrefix(Integer queueType) {
        switch (queueType) {
            case 1: return "A"; // 堂食
            case 2: return "B"; // 外卖
            case 3: return "C"; // 自提
            default: return "A";
        }
    }
}
