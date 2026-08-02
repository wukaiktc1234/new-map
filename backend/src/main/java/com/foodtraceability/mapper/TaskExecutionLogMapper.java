package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.TaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 任务执行日志 Mapper 接口
 */
@Mapper
public interface TaskExecutionLogMapper extends BaseMapper<TaskExecutionLog> {

    /**
     * 分页查询执行日志（支持多条件过滤）
     */
    IPage<TaskExecutionLog> selectLogPage(
        Page<TaskExecutionLog> page,
        @Param("taskId") Long taskId,
        @Param("taskName") String taskName,
        @Param("taskCode") String taskCode,
        @Param("triggerType") Integer triggerType,
        @Param("executionStatus") Integer executionStatus,
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("endTime") java.time.LocalDateTime endTime
    );

    /**
     * 按执行状态统计日志数量
     */
    Map<String, Object> selectLogStat(@Param("taskId") Long taskId);
}
