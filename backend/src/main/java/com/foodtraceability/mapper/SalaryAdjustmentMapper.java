package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SalaryAdjustment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资调整Mapper接口
 * @author example
 * @since 2025-12-05
 */
@Mapper
public interface SalaryAdjustmentMapper extends BaseMapper<SalaryAdjustment> {
    // 可以添加自定义查询方法
}