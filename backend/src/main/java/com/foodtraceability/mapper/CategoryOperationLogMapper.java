package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CategoryOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类操作日志 Mapper 接口
 */
@Mapper
public interface CategoryOperationLogMapper extends BaseMapper<CategoryOperationLog> {
}