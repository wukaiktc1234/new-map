package com.foodtraceability.service;

import com.foodtraceability.entity.CostAllocation;
import com.foodtraceability.entity.CostAllocationRule;
import com.foodtraceability.dto.CostAllocationQueryDTO;
import com.foodtraceability.dto.PageResult;

/**
 * 成本分摊Service (已废弃)
 * @deprecated 已废弃，仅用于兼容旧代码
 */
@Deprecated
public interface CostAllocationService {
    @Deprecated CostAllocation createCostAllocation(CostAllocation ca);
    @Deprecated CostAllocation getCostAllocationById(Long id);
    @Deprecated PageResult<CostAllocation> queryCostAllocations(CostAllocationQueryDTO dto);
    @Deprecated CostAllocation updateCostAllocation(CostAllocation ca);
    @Deprecated boolean deleteCostAllocation(Long id);
    @Deprecated CostAllocation executeCostAllocation(Long id);
    @Deprecated CostAllocationRule createAllocationRule(CostAllocationRule rule);
    @Deprecated CostAllocationRule updateAllocationRule(Long id, CostAllocationRule rule);
    @Deprecated CostAllocationRule getAllocationRuleById(Long id);
    @Deprecated PageResult<CostAllocationRule> queryAllocationRules(CostAllocationQueryDTO dto);
}
