package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.Budget;

/**
 * 预算Service接口
 * 管理企业年度/月度预算，支持预算执行情况对比和预警
 */
public interface BudgetService extends IService<Budget> {

    /**
     * 创建预算
     */
    BudgetVO create(BudgetCreateDTO dto);

    /**
     * 更新预算
     */
    boolean update(BudgetUpdateDTO dto);

    /**
     * 获取预算详情
     */
    BudgetVO getDetail(Long budgetId);

    /**
     * 分页查询预算
     */
    IPage<BudgetVO> getPage(BudgetQueryDTO query);

    /**
     * 更新实际金额
     */
    boolean updateActualAmount(Long budgetId, Long actualAmount);
}
