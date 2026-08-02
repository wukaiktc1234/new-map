package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.store.operation.CallNumberQueueQueryDTO;
import com.foodtraceability.entity.CallNumberQueueNew;
import java.util.List;
import java.util.Map;

/**
 * 叫号队列服务接口
 */
public interface CallNumberQueueNewService {

    /** 取号 */
    CallNumberQueueNew takeNumber(Long storeId, Integer queueType, Integer peopleCount, String tablePreference);

    /** 叫号（下一个等待中的） */
    CallNumberQueueNew callNext(Integer queueType);

    /** 重叫（对已过号的重新叫号） */
    void reCall(Long queueId);

    /** 标记已用餐 */
    void markDined(Long queueId);

    /** 取消排队 */
    void cancel(Long queueId);

    /** 获取当前排队情况 */
    Map<String, Object> getQueueStatus(Integer queueType);

    /** 获取等待列表 */
    List<CallNumberQueueNew> getWaitingList(Integer queueType);

    /** 按门店查询叫号记录（分页） */
    IPage<CallNumberQueueNew> listByStoreId(Long storeId, CallNumberQueueQueryDTO query);

    /** 按门店统计叫号 */
    Map<String, Object> getStatsByStoreId(Long storeId);
}
