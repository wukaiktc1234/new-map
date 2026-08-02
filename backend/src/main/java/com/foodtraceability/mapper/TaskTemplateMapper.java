package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TaskTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务模板数据访问层
 */
@Mapper
public interface TaskTemplateMapper extends BaseMapper<TaskTemplate> {
}
