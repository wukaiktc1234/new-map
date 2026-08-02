package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.ScheduledTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 定时任务定义 Mapper 接口
 */
@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTask> {

    /**
     * 分页查询定时任务（支持多条件过滤）
     */
    IPage<ScheduledTask> selectTaskPage(
        Page<ScheduledTask> page,
        @Param("taskName") String taskName,
        @Param("taskCode") String taskCode,
        @Param("taskGroup") String taskGroup,
        @Param("status") Integer status,
        @Param("taskType") Integer taskType,
        @Param("isBuiltin") Integer isBuiltin
    );

    /**
     * 查询任务统计信息
     * 返回: totalCount, enabledCount, pausedCount, errorCount, disabledCount
     */
    Map<String, Object> selectTaskStat();

    /**
     * 查询所有任务分组列表（去重）
     */
    List<String> selectTaskGroups();

    /**
     * 根据任务编码查询任务
     */
    ScheduledTask selectByTaskCode(@Param("taskCode") String taskCode);

    /**
     * 查询所有内置任务
     */
    List<ScheduledTask> selectBuiltinTasks();
}
