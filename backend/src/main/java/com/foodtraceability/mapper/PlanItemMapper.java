package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PlanItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划项Mapper接口
 * 
 * @author demo
 * @since 1.0.0
 */
@Mapper
public interface PlanItemMapper extends BaseMapper<PlanItem> {
}
