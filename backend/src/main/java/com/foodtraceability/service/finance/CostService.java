package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.CostRecord;

/**
 * 成本核算Service接口
 */
public interface CostService extends IService<CostRecord> {

    CostRecordVO create(CostRecordCreateDTO dto);

    boolean update(CostRecordUpdateDTO dto);

    CostRecordVO getDetail(Long costId);

    IPage<CostRecordVO> getPage(CostRecordQueryDTO query);

    /**
     * 按期间汇总成本
     * @param period 期间，如 "2026-04"
     * @return 各类型成本汇总
     */
    java.util.Map<Integer, Long> summarizeByPeriod(String period);
}
