package com.foodtraceability.service.impl;

import com.foodtraceability.entity.CostAllocation;
import com.foodtraceability.entity.CostAllocationRule;
import com.foodtraceability.dto.CostAllocationQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.CostAllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Deprecated
@Service("legacyCostAllocationServiceImpl")
public class CostAllocationServiceImpl implements CostAllocationService {
    private static final Logger log = LoggerFactory.getLogger(CostAllocationServiceImpl.class);

    @Override public CostAllocation createCostAllocation(CostAllocation ca) { log.warn("[STUB] no-op"); return null; }
    @Override public CostAllocation getCostAllocationById(Long id) { return null; }
    @Override public PageResult<CostAllocation> queryCostAllocations(CostAllocationQueryDTO dto) { return null; }
    @Override public CostAllocation updateCostAllocation(CostAllocation ca) { log.warn("[STUB] no-op"); return null; }
    @Override public boolean deleteCostAllocation(Long id) { return false; }
    @Override public CostAllocation executeCostAllocation(Long id) { log.warn("[STUB] no-op"); return null; }
    @Override public CostAllocationRule createAllocationRule(CostAllocationRule r) { log.warn("[STUB] no-op"); return null; }
    @Override public CostAllocationRule updateAllocationRule(Long id, CostAllocationRule r) { log.warn("[STUB] no-op"); return null; }
    @Override public CostAllocationRule getAllocationRuleById(Long id) { return null; }
    @Override public PageResult<CostAllocationRule> queryAllocationRules(CostAllocationQueryDTO dto) { return null; }
}
