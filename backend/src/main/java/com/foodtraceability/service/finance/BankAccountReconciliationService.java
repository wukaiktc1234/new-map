package com.foodtraceability.service.finance;

import com.foodtraceability.entity.finance.BankAccountReconciliation;

import java.util.List;
import java.util.Map;

/**
 * 银行账户对账结果Service接口
 */
public interface BankAccountReconciliationService {

    /**
     * 执行对账并保存结果
     * @param accountId 银行账户ID
     * @return 对账结果
     */
    BankAccountReconciliation reconcile(Long accountId);

    /**
     * 获取指定账户的对账结果
     * @param accountId 银行账户ID
     * @return 对账结果
     */
    BankAccountReconciliation getReconciliationResult(Long accountId);

    /**
     * 批量获取多个账户的最新对账结果（消除 N+1 查询）
     * @param accountIds 账户ID列表
     * @return accountId → 最新对账结果 的映射
     */
    Map<Long, BankAccountReconciliation> getReconciliationResultsBatch(List<Long> accountIds);
}