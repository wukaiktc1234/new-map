package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.RefundShiftAttribution;
import org.apache.ibatis.annotations.Mapper;

/** N19-2④：退款单 → 班次归属 Mapper */
@Mapper
public interface RefundShiftAttributionMapper extends BaseMapper<RefundShiftAttribution> {
}
