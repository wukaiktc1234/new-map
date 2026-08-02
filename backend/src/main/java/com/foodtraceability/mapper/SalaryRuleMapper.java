package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SalaryRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资规则Mapper接口
 * @author example
 * @since 2025-12-05
 */
@Mapper
public interface SalaryRuleMapper extends BaseMapper<SalaryRule> {
    // 可以添加自定义查询方法
}