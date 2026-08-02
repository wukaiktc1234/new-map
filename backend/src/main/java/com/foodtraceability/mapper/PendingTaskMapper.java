package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.TaskQueryDTO;
import com.foodtraceability.entity.PendingTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 待办任务Mapper接口
 * 对应数据库表 pending_tasks
 * 用于门店运营模块的任务管理
 */
@Mapper
public interface PendingTaskMapper extends BaseMapper<PendingTask> {

    /**
     * 分页查询待办任务
     *
     * @param page  分页参数
     * @param query 查询条件（支持任务类型、状态、优先级、指派人等条件筛选）
     * @return 分页结果
     */
    IPage<PendingTask> selectTaskPage(IPage<PendingTask> page, @Param("query") TaskQueryDTO query);

    /**
     * 根据被指派人ID查询任务列表
     *
     * @param assigneeId 被指派人ID
     * @return 任务列表（按优先级降序、截止日期升序排序）
     */
    List<PendingTask> selectByAssigneeId(String assigneeId);

    /**
     * 统计指定用户的未读任务数量
     * 未读任务定义：状态为pending或in_progress，且未标记为已读
     *
     * @param assigneeId 被指派人ID
     * @return 未读任务数量
     */
    int countUnreadTasks(String assigneeId);
}
