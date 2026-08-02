package com.foodtraceability.service;

import com.foodtraceability.entity.HealthCertificateExpense;

/**
 * 财务系统服务接口
 * 用于处理健康证报销数据同步至财务系统
 */
public interface FinanceSystemService {
    
    /**
     * 同步健康证报销数据至财务系统
     * @param expense 健康证报销数据
     * @return 同步结果
     */
    boolean syncHealthCertificateExpenseToFinance(HealthCertificateExpense expense);
    
    /**
     * 从财务系统获取健康证报销状态
     * @param expenseId 报销ID
     * @return 报销状态
     */
    String getExpenseStatusFromFinance(String expenseId);
    
    /**
     * 更新健康证报销状态
     * @param expenseId 报销ID
     * @param status 报销状态
     * @return 更新结果
     */
    boolean updateExpenseStatus(String expenseId, String status);
}