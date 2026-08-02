package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchasePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购计划主表 Mapper
 */
@Mapper
public interface PurchasePlanMapper extends BaseMapper<PurchasePlan> {
}
