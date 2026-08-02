package com.foodtraceability.dataservice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dataservice.CallNumberQueueDataService;
import com.foodtraceability.dto.store.operation.vo.CallNumberQueueVO;
import com.foodtraceability.entity.CallNumberQueueNew;
import com.foodtraceability.mapper.CallNumberQueueNewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 叫号队列数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 缓存键格式: callNumberQueue:basic:{queueId}
 */
@Service
@CacheConfig(cacheNames = "callNumberQueue")
public class CallNumberQueueDataServiceImpl implements CallNumberQueueDataService {

    private static final Logger log = LoggerFactory.getLogger(CallNumberQueueDataServiceImpl.class);

    /** 日期时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 等待状态 */
    private static final int STATUS_WAITING = 1;
    /** 已叫号状态 */
    private static final int STATUS_CALLED = 2;

    private final CallNumberQueueNewMapper callNumberQueueNewMapper;

    /**
     * 构造函数注入
     *
     * @param callNumberQueueNewMapper 叫号队列Mapper
     */
    public CallNumberQueueDataServiceImpl(CallNumberQueueNewMapper callNumberQueueNewMapper) {
        this.callNumberQueueNewMapper = callNumberQueueNewMapper;
    }

    @Override
    @Cacheable(key = "'basic:' + #queueId", unless = "#result == null")
    public CallNumberQueueVO getCallNumberQueueBasicInfo(Long queueId) {
        if (queueId == null) {
            return null;
        }

        CallNumberQueueNew queue = callNumberQueueNewMapper.selectById(queueId);
        return convertToVO(queue);
    }

    @Override
    @Cacheable(key = "'stats:' + #storeId", unless = "#result == null || #result.isEmpty()")
    public Map<String, Object> getQueueStatsByStoreId(Long storeId) {
        if (storeId == null) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> counts = callNumberQueueNewMapper.countGroupByStatus(storeId);
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalCount = 0L, waitingCount = 0L, calledCount = 0L, skippedCount = 0L, completedCount = 0L, cancelledCount = 0L;
        for (Map<String, Object> item : counts) {
            int s = ((Number) item.get("status")).intValue();
            long c = ((Number) item.get("cnt")).longValue();
            totalCount += c;
            switch (s) {
                case STATUS_WAITING: waitingCount = c; break;
                case STATUS_CALLED: calledCount = c; break;
                case 3: skippedCount = c; break;
                case 4: completedCount = c; break;
                case 5: cancelledCount = c; break;
                default: break;
            }
        }

        stats.put("totalCount", totalCount);
        stats.put("waitingCount", waitingCount);
        stats.put("calledCount", calledCount);
        stats.put("skippedCount", skippedCount);
        stats.put("completedCount", completedCount);
        stats.put("cancelledCount", cancelledCount);

        LocalDateTime now = LocalDateTime.now();
        OptionalDouble avgWaitMinutes = callNumberQueueNewMapper.selectList(
                        new LambdaQueryWrapper<CallNumberQueueNew>()
                                .eq(CallNumberQueueNew::getStoreId, storeId)
                                .isNotNull(CallNumberQueueNew::getCreateTime)
                                .apply("DATE(create_time) = CURRENT_DATE")
                ).stream()
                .filter(q -> q.getStatus() != null && (q.getStatus() == STATUS_WAITING || q.getStatus() == STATUS_CALLED))
                .mapToLong(q -> Duration.between(q.getCreateTime(), now).toMinutes())
                .average();
        stats.put("avgWaitMinutes", avgWaitMinutes.isPresent() ? (long) avgWaitMinutes.getAsDouble() : 0L);

        return stats;
    }

    @Override
    @CacheEvict(key = "'basic:' + #queueId")
    public void clearCallNumberQueueCache(Long queueId) {
        log.debug("清除叫号队列缓存: queueId={}", queueId);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void clearStoreQueueCache(Long storeId) {
        log.debug("清除门店叫号队列缓存: storeId={}", storeId);
    }

    /**
     * 将实体转换为视图对象
     *
     * @param queue 叫号队列实体
     * @return 视图对象
     */
    private CallNumberQueueVO convertToVO(CallNumberQueueNew queue) {
        if (queue == null) {
            return null;
        }

        CallNumberQueueVO vo = new CallNumberQueueVO();
        vo.setQueueId(queue.getQueueId());
        vo.setStoreId(queue.getStoreId());
        vo.setTicketNumber(queue.getTicketNumber());
        vo.setQueueType(queue.getQueueType());
        vo.setQueueTypeName(getQueueTypeName(queue.getQueueType()));
        vo.setPeopleCount(queue.getPeopleCount());
        vo.setTablePreference(queue.getTablePreference());
        vo.setStatus(queue.getStatus());
        vo.setStatusName(getStatusName(queue.getStatus()));
        vo.setCallTime(formatDateTime(queue.getCallTime()));
        vo.setCalledCount(queue.getCalledCount());
        vo.setCreateTime(formatDateTime(queue.getCreateTime()));
        vo.setUpdateTime(formatDateTime(queue.getUpdateTime()));

        // 计算等待时长（分钟）：等待中或已叫号状态时，从创建时间到当前时间的差值
        if (queue.getStatus() != null
                && (queue.getStatus() == STATUS_WAITING || queue.getStatus() == STATUS_CALLED)
                && queue.getCreateTime() != null) {
            vo.setWaitMinutes(Duration.between(queue.getCreateTime(), LocalDateTime.now()).toMinutes());
        }

        return vo;
    }

    /** 获取状态名称 */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "等待";
            case 2: return "已叫号";
            case 3: return "已过号";
            case 4: return "已用餐";
            case 5: return "已取消";
            default: return "未知";
        }
    }

    /** 获取排队类型名称 */
    private String getQueueTypeName(Integer queueType) {
        if (queueType == null) return "未知";
        switch (queueType) {
            case 1: return "堂食";
            case 2: return "外卖";
            case 3: return "自提";
            default: return "未知";
        }
    }

    /** 格式化LocalDateTime为字符串 */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }
}
