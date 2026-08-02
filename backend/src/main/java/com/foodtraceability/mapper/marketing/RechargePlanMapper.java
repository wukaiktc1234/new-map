package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.RechargePlan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 充值方案 Mapper
 */
@Mapper
@Repository
public interface RechargePlanMapper extends BaseMapper<RechargePlan> {
}
