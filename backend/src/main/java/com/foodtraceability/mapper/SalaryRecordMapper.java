package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SalaryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资记录Mapper接口
 * @author example
 * @since 2025-12-05
 */
@Mapper
public interface SalaryRecordMapper extends BaseMapper<SalaryRecord> {
    // 可以添加自定义查询方法
}