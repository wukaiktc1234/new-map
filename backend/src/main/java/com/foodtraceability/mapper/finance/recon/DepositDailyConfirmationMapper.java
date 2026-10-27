package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.DepositDailyConfirmation;
import org.apache.ibatis.annotations.Mapper;

/** NEW-34：存行日汇总确认记录 Mapper。 */
@Mapper
public interface DepositDailyConfirmationMapper extends BaseMapper<DepositDailyConfirmation> {
}
