package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FundFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金流水Mapper接口
 */
@Mapper
public interface FundFlowMapper extends BaseMapper<FundFlow> {

    // P4-1 BL-2：本 Mapper 内**不再有任何取号 SQL** —— 号段发号统一走
    // com.foodtraceability.common.sequence.SequenceAllocator（唯一实现，MANDATORY 事务内执行）。
    // 已删除：nextFlowSequence / initFlowSequenceIfAbsent / nextFlowSequenceWithFloor / maxFlowSequenceByPrefix
}
