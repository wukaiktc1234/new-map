package com.foodtraceability.dataservice.finance;

import com.foodtraceability.dto.finance.ReceivableBasicInfo;

import java.util.List;
import java.util.Map;

/**
 * 应收账款 DataService 接口
 * 提供 L1 Caffeine（本地）+ L2 Redis（分布式）二级缓存
 * 缓存键格式: receivable:basic:{receivableId}
 */
public interface ReceivableDataService {

    /**
     * 批量获取应收账款基本信息（缓存优先，避免 N+1 查询）
     * @param receivableIds 应收ID列表
     * @return ID -> BasicInfo 映射，未命中的ID不出现在Map中
     */
    Map<String, ReceivableBasicInfo> batchGetReceivableBasicInfo(List<String> receivableIds);

    /**
     * 单个获取应收账款基本信息
     * @param receivableId 应收ID
     * @return BasicInfo，不存在返回 null
     */
    ReceivableBasicInfo getReceivableBasicInfo(String receivableId);

    /**
     * 清除单个应收账款缓存（先清 L1，再清 L2）
     * @param receivableId 应收ID
     */
    void clearReceivableCache(String receivableId);

    /**
     * 批量清除应收账款缓存
     * @param receivableIds 应收ID列表
     */
    void clearReceivableBatchCache(List<String> receivableIds);

    /**
     * 清除所有应收账款缓存
     */
    void clearAllReceivableCache();
}
