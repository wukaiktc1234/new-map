package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.*;

import java.util.List;

/**
 * 排班方案服务接口
 * 提供排班方案的增删改查及统计功能（F-004）
 * 包含时间线日历(F-001)的核心API
 */
public interface SchedulePlanService {

    /**
     * 查询排班方案列表（分页+筛选）
     * 支持按状态、日期范围、模板ID筛选，按更新时间倒序返回
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<SchedulePlanVO> getPlanList(SchedulePlanQueryDTO queryDTO);

    /**
     * 获取排班方案详情（含条目列表）
     * @param planId 方案ID
     * @return 方案详情（含关联条目），不存在则返回null
     */
    SchedulePlanVO getPlanById(String planId);

    /**
     * 创建排班方案（草稿状态）
     * 新建方案默认status=draft, version=1
     * @param createDTO 创建请求DTO
     * @return 创建后的方案
     */
    SchedulePlanVO createPlan(SchedulePlanCreateDTO createDTO);

    /**
     * 编辑排班方案基本信息
     * 业务规则：仅draft状态可编辑，否则抛出RuntimeException("方案已发布无法编辑")
     * 乐观锁冲突由MyBatis Plus自动处理
     * @param planId 方案ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的方案
     */
    SchedulePlanVO updatePlan(String planId, SchedulePlanUpdateDTO updateDTO);

    /**
     * 删除排班方案（逻辑删除）
     * 业务规则：仅draft状态可删除，否则抛出RuntimeException("已发布的方案无法删除")
     * @param planId 方案ID
     */
    void deletePlan(String planId);

    /**
     * 获取排班统计概览
     * 按当前用户门店ID过滤数据
     * @return 统计数据（各状态数量）
     */
    SchedulePlanStatsVO getPlanStats();

    /**
     * 发布排班方案
     * 业务规则：仅 draft 状态可发布，发布后状态变为 published
     * @param planId 方案ID
     * @return 发布后的方案
     */
    SchedulePlanVO publishPlan(String planId);

    /**
     * 撤回排班方案
     * 业务规则：仅 published 状态可撤回，撤回后状态变为 draft
     * @param planId 方案ID
     * @param withdrawReason 撤回原因
     * @return 撤回后的方案
     */
    SchedulePlanVO withdrawPlan(String planId, String withdrawReason);

    // ==================== 时间线日历(F-001)核心方法 ====================

    /**
     * 获取时间线日历数据（核心接口）
     *
     * <p>业务逻辑：
     * <ul>
     *   <li>根据weekStart计算7天日期范围(周一~周日)</li>
     *   <li>查询该方案涉及的员工列表(从schedule_entries去重)</li>
     *   <li>查询每个员工在7天内的排班记录</li>
     *   <li>关联班次配置获取颜色、时间段等信息</li>
     *   <li>计算每日需求vs实际排班的缺员情况</li>
     *   <li>计算每人本周工时、连续工作天数等统计</li>
     * </ul>
     *
     * @param planId 方案ID
     * @param requestDTO 时间线查询请求(包含weekStart)
     * @return 时间线响应数据(含员工列表、每日汇总)
     */
    TimelineResponseVO getTimelineData(String planId, TimelineRequestDTO requestDTO);

    /**
     * 获取排班条目列表（支持多条件筛选+分页）
     *
     * <p>支持按以下条件筛选：
     * <ul>
     *   <li>employeeId: 按员工筛选</li>
     *   <li>startDate/endDate: 日期范围</li>
     *   <li>shiftType: 按班次类型筛选</li>
     * </ul>
     *
     * @param planId 方案ID
     * @param queryDTO 查询条件
     * @return 分页的排班条目列表
     */
    PageResult<ScheduleEntryVO> getEntryList(String planId, ScheduleEntryQueryDTO queryDTO);

    /**
     * 批量更新排班条目（用于快速编辑、拖拽交换）
     *
     * <p>业务逻辑：
     * <ul>
     *   <li>遍历entries数组，逐个处理</li>
     *   <li>如果entryId存在 → 更新现有条目</li>
     *   <li>如果entryId不存在 → 创建新条目</li>
     *   <li>校验唯一约束: 同一方案同一员工同一天只能有一条记录</li>
     *   <li>如果source='swap' → 标记相关条目的source</li>
     *   <li>关联班次配置获取shiftName/startTime/endTime/color/durationMinutes</li>
     *   <li>更新方案的employeeCount和totalWorkHours统计字段</li>
     * </ul>
     *
     * <p>事务控制：整个操作在一个事务中完成，任何一条失败则全部回滚
     *
     * @param planId 方案ID
     * @param batchUpdateDTO 批量更新请求
     * @return 更新结果(成功/失败的条目数量)
     */
    BatchUpdateResult batchUpdateEntries(String planId, ScheduleEntryBatchUpdateDTO batchUpdateDTO);

    /**
     * 批量更新结果
     */
    class BatchUpdateResult {
        private int successCount;
        private int failedCount;
        private List<String> errors;

        public BatchUpdateResult(int successCount, int failedCount, List<String> errors) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
