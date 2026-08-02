package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RechargePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值方案数据访问层
 */
@Mapper
public interface RechargePlanBaseMapper extends BaseMapper<RechargePlan> {
}
