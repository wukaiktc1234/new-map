package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TaskExecutionLogDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务执行详情 Mapper 接口
 */
@Mapper
public interface TaskExecutionLogDetailMapper extends BaseMapper<TaskExecutionLogDetail> {

    /**
     * 根据日志ID查询执行详情列表
     */
    List<TaskExecutionLogDetail> selectByLogId(@Param("logId") Long logId);
}
