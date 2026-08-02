package com.foodtraceability.dataservice;

import com.foodtraceability.dto.store.operation.vo.CallNumberQueueVO;

import java.util.Map;

/**
 * 叫号队列数据服务接口
 * 提供叫号队列的二级缓存(L1本地+L2 Redis)和批量查询功能
 * 缓存键格式: callNumberQueue:basic:{queueId}
 */
public interface CallNumberQueueDataService {

    /**
     * 获取单个叫号队列基本信息
     *
     * @param queueId 队列ID
     * @return 叫号队列视图对象
     */
    CallNumberQueueVO getCallNumberQueueBasicInfo(Long queueId);

    /**
     * 根据门店ID获取排队统计信息
     *
     * @param storeId 门店ID
     * @return 排队统计信息（包含各状态数量、平均等待时长等）
     */
    Map<String, Object> getQueueStatsByStoreId(Long storeId);

    /**
     * 清除指定队列记录的缓存
     *
     * @param queueId 队列ID
     */
    void clearCallNumberQueueCache(Long queueId);

    /**
     * 清除指定门店下所有队列的缓存
     *
     * @param storeId 门店ID
     */
    void clearStoreQueueCache(Long storeId);
}
