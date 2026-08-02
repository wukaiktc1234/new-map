package com.foodtraceability.service;

import com.foodtraceability.entity.Budget;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 预算Service接口
 * 用于定义预算管理的业务逻辑
 */
public interface BudgetService extends IService<Budget> {

    /**
     * 新增预算
     * @param budget 预算实体
     * @return 操作结果
     */
    boolean addBudget(Budget budget);

    /**
     * 更新预算
     * @param budget 预算实体
     * @return 操作结果
     */
    boolean updateBudget(Budget budget);

    /**
     * 删除预算
     * @param budgetId 预算ID
     * @return 操作结果
     */
    boolean deleteBudget(String budgetId);
}