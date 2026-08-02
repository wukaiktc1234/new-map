package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SubTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 子任务数据访问层
 */
@Mapper
public interface SubTaskMapper extends BaseMapper<SubTask> {
}
