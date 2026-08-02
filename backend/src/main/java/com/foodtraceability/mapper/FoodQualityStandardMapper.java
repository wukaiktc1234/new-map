package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.FoodQualityStandard;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量标准Mapper接口
 * 依赖MyBatis-Plus的@TableLogic自动处理逻辑删除，查询无需手动添加deleted=0条件
 */
@Mapper
public interface FoodQualityStandardMapper extends BaseMapper<FoodQualityStandard> {
}
