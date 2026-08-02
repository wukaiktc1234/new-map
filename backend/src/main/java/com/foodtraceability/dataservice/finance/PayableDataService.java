package com.foodtraceability.dataservice.finance;

import com.foodtraceability.dto.finance.PayableBasicInfo;

import java.util.List;
import java.util.Map;

/**
 * 应付账款 DataService 接口
 * 提供 L1 Caffeine（本地）+ L2 Redis（分布式）二级缓存
 * 缓存键格式: payable:basic:{payableId}
 */
public interface PayableDataService {

    /**
     * 批量获取应付账款基本信息（缓存优先，避免 N+1 查询）
     * @param payableIds 应付ID列表
     * @return ID -> BasicInfo 映射，未命中的ID不出现在Map中
     */
    Map<String, PayableBasicInfo> batchGetPayableBasicInfo(List<String> payableIds);

    /**
     * 单个获取应付账款基本信息
     * @param payableId 应付ID
     * @return BasicInfo，不存在返回 null
     */
    PayableBasicInfo getPayableBasicInfo(String payableId);

    /**
     * 清除单个应付账款缓存（先清 L1，再清 L2）
     * @param payableId 应付ID
     */
    void clearPayableCache(String payableId);

    /**
     * 批量清除应付账款缓存
     * @param payableIds 应付ID列表
     */
    void clearPayableBatchCache(List<String> payableIds);

    /**
     * 清除所有应付账款缓存
     */
    void clearAllPayableCache();
}
