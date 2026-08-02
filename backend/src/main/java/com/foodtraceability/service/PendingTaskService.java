package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.TaskQueryDTO;
import com.foodtraceability.dto.store.operation.vo.PendingTaskVO;

import java.util.List;

/**
 * 待办任务服务接口
 * 提供待办任务的增删改查、状态管理、批量操作等业务功能
 */
public interface PendingTaskService {

    /**
     * 根据被指派人分页查询任务列表
     *
     * @param assigneeId 被指派人ID
     * @param query      查询条件（支持任务类型、状态、优先级等筛选）
     * @return 分页结果
     */
    IPage<PendingTaskVO> getTasksByAssignee(String assigneeId, TaskQueryDTO query);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务视图对象
     */
    PendingTaskVO getTaskDetail(String taskId);

    /**
     * 完成任务
     * 将任务状态更新为completed，记录完成时间
     *
     * @param taskId     任务ID
     * @param operatorId 操作人ID
     */
    void completeTask(String taskId, String operatorId);

    /**
     * 批量完成任务
     * 单次最多处理50个任务
     *
     * @param taskIds    任务ID列表
     * @param operatorId 操作人ID
     */
    void batchCompleteTasks(List<String> taskIds, String operatorId);

    /**
     * 获取指定用户的未读任务数量
     * 统计pending和in_progress状态的任务数
     *
     * @param assigneeId 被指派人ID
     * @return 未读任务数量
     */
    int getUnreadCount(String assigneeId);

    /**
     * 获取任务的联动跳转URL
     * 用于前端"立即处理"按钮点击后跳转到目标业务页面
     *
     * @param taskId 任务ID
     * @return 跳转URL（如 /store-management/daily-settlement?date=2026-05-11）
     */
    String getRedirectUrl(String taskId);

    /**
     * 自动生成证件到期预警待办任务
     * 供定时任务或其他业务逻辑调用
     *
     * @param certId    证书ID
     * @param certType  证书类型（如"健康证"、"营业执照"、"食品经营许可证"）
     * @param expiryDate 到期日期
     * @param storeId   所属门店ID
     * @param storeName 门店名称
     * @param assigneeId 任务分配对象ID（通常是店长userId）
     * @param daysLeft  剩余天数（负数表示已过期）
     */
    void createExpiryAlertTask(String certId, String certType, java.time.LocalDate expiryDate,
                               String storeId, String storeName, String assigneeId,
                               long daysLeft);
}
