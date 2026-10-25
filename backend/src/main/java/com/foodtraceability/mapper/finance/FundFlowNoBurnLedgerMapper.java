package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FundFlowNoBurnLedger;
import org.apache.ibatis.annotations.Mapper;

/**
 * 烧号账 Mapper（fund_flow_no_burn_ledger，BL-5）。
 */
@Mapper
public interface FundFlowNoBurnLedgerMapper extends BaseMapper<FundFlowNoBurnLedger> {
}
