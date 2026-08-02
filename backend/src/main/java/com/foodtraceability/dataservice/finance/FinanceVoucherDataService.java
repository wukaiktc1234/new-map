package com.foodtraceability.dataservice.finance;

import com.foodtraceability.dto.finance.FinanceVoucherBasicInfo;

import java.util.List;
import java.util.Map;

/**
 * 记账凭证 DataService 接口
 * 提供 L1 Caffeine（本地）+ L2 Redis（分布式）二级缓存
 * 缓存键格式: finance_voucher:basic:{voucherId}
 */
public interface FinanceVoucherDataService {

    /**
     * 批量获取记账凭证基本信息（缓存优先，避免 N+1 查询）
     * @param voucherIds 凭证ID列表
     * @return ID -> BasicInfo 映射，未命中的ID不出现在Map中
     */
    Map<String, FinanceVoucherBasicInfo> batchGetFinanceVoucherBasicInfo(List<String> voucherIds);

    /**
     * 单个获取记账凭证基本信息
     * @param voucherId 凭证ID
     * @return BasicInfo，不存在返回 null
     */
    FinanceVoucherBasicInfo getFinanceVoucherBasicInfo(String voucherId);

    /**
     * 清除单个记账凭证缓存（先清 L1，再清 L2）
     * @param voucherId 凭证ID
     */
    void clearFinanceVoucherCache(String voucherId);

    /**
     * 批量清除记账凭证缓存
     * @param voucherIds 凭证ID列表
     */
    void clearFinanceVoucherBatchCache(List<String> voucherIds);

    /**
     * 清除所有记账凭证缓存
     */
    void clearAllFinanceVoucherCache();
}
