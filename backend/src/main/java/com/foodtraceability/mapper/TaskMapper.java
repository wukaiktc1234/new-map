package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 任务数据访问层
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 查询任务详情（含接收人数统计）
     */
    @Select("SELECT t.*, " +
            "CASE WHEN t.assignee_ids IS NOT NULL " +
            "THEN array_length(t.assignee_ids::jsonb[], 1) ELSE 0 END as assignee_count " +
            "FROM tasks t WHERE t.task_id = #{taskId} AND t.deleted = 0")
    Map<String, Object> selectDetailById(Long taskId);
}
