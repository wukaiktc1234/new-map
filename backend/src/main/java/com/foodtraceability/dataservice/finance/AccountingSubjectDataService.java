package com.foodtraceability.dataservice.finance;

import com.foodtraceability.dto.finance.AccountingSubjectBasicInfo;

import java.util.List;
import java.util.Map;

/**
 * 会计科目 DataService 接口
 * 提供 L1 Caffeine（本地）+ L2 Redis（分布式）二级缓存
 * 缓存键格式: accounting_subject:basic:{subjectId}
 */
public interface AccountingSubjectDataService {

    /**
     * 批量获取会计科目基本信息（缓存优先，避免 N+1 查询）
     * @param subjectIds 科目ID列表
     * @return ID -> BasicInfo 映射，未命中的ID不出现在Map中
     */
    Map<String, AccountingSubjectBasicInfo> batchGetAccountingSubjectBasicInfo(List<String> subjectIds);

    /**
     * 单个获取会计科目基本信息
     * @param subjectId 科目ID
     * @return BasicInfo，不存在返回 null
     */
    AccountingSubjectBasicInfo getAccountingSubjectBasicInfo(String subjectId);

    /**
     * 清除单个会计科目缓存（先清 L1，再清 L2）
     * @param subjectId 科目ID
     */
    void clearAccountingSubjectCache(String subjectId);

    /**
     * 批量清除会计科目缓存
     * @param subjectIds 科目ID列表
     */
    void clearAccountingSubjectBatchCache(List<String> subjectIds);

    /**
     * 清除所有会计科目缓存
     */
    void clearAllAccountingSubjectCache();
}
