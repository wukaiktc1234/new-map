package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PrintTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印任务Mapper接口
 */
@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {
}
