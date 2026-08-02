package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.PendingTaskDataService;
import com.foodtraceability.dto.TaskQueryDTO;
import com.foodtraceability.dto.store.operation.vo.PendingTaskVO;
import com.foodtraceability.entity.PendingTask;
import com.foodtraceability.mapper.PendingTaskMapper;
import com.foodtraceability.service.OperationLogService;
import com.foodtraceability.service.PendingTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 待办任务服务实现类
 * 提供待办任务的增删改查、状态管理、批量操作等业务功能
 */
@Service
public class PendingTaskServiceImpl implements PendingTaskService {

    private static final Logger log = LoggerFactory.getLogger(PendingTaskServiceImpl.class);

    /** 批量操作上限 */
    private static final int MAX_BATCH_SIZE = 50;

    private final PendingTaskMapper pendingTaskMapper;
    private final PendingTaskDataService pendingTaskDataService;
    private final OperationLogService operationLogService;

    /**
     * 构造函数注入
     *
     * @param pendingTaskMapper       待办任务Mapper
     * @param pendingTaskDataService 待办任务数据服务（缓存层）
     * @param operationLogService     操作日志服务
     */
    public PendingTaskServiceImpl(PendingTaskMapper pendingTaskMapper,
                                  PendingTaskDataService pendingTaskDataService,
                                  OperationLogService operationLogService) {
        this.pendingTaskMapper = pendingTaskMapper;
        this.pendingTaskDataService = pendingTaskDataService;
        this.operationLogService = operationLogService;
    }

    @Override
    public IPage<PendingTaskVO> getTasksByAssignee(String assigneeId, TaskQueryDTO query) {
        // 参数校验
        if (assigneeId == null || assigneeId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "被指派人ID不能为空");
        }

        // 设置查询条件中的指派人ID
        query.setAssigneeId(assigneeId);

        // 构建分页对象
        Page<PendingTask> page = new Page<>(query.getCurrent(), query.getSize());

        // 调用Mapper分页查询
        IPage<PendingTask> taskPage = pendingTaskMapper.selectTaskPage(page, query);

        // 转换为VO列表（通过DataService填充关联数据）
        return taskPage.convert(this::convertToVO);
    }

    @Override
    public PendingTaskVO getTaskDetail(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }

        // 通过DataService获取（带缓存）
        return pendingTaskDataService.getPendingTaskBasicInfo(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, String operatorId) {
        log.info("完成任务: taskId={}, operatorId={}", taskId, operatorId);

        // 1. 查询任务是否存在
        PendingTask task = pendingTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        // 2. 校验任务是否属于当前用户（或用户有权限操作此任务）
        if (!operatorId.equals(task.getAssigneeId())) {
            log.warn("非任务归属人尝试完成任务: taskId={}, operatorId={}, assigneeId={}",
                    taskId, operatorId, task.getAssigneeId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此任务");
        }

        // 3. 校验任务状态（只有pending/in_progress状态可完成）
        if (!"pending".equals(task.getStatus()) && !"in_progress".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前任务状态不允许完成操作");
        }

        // 4. 更新任务状态
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        int rows = pendingTaskMapper.updateById(task);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "更新任务失败，请刷新后重试");
        }

        // 5. 清除缓存
        pendingTaskDataService.clearPendingTaskCache(taskId);

        // 6. 异步记录操作日志
        try {
            operationLogService.logOperation(buildLogEntity("COMPLETE_TASK", "pending_task", taskId, operatorId));
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCompleteTasks(List<String> taskIds, String operatorId) {
        // 参数校验
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID列表不能为空");
        }
        if (taskIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量操作最多支持" + MAX_BATCH_SIZE + "个任务");
        }

        log.info("批量完成任务: count={}, operatorId={}", taskIds.size(), operatorId);

        // 循环调用单个完成方法
        for (String taskId : taskIds) {
            try {
                completeTask(taskId, operatorId);
            } catch (BusinessException e) {
                log.warn("批量完成任务时单个任务处理失败: taskId={}, error={}", taskId, e.getMessage());
                // 单个失败不影响其他任务，继续处理
            }
        }
    }

    @Override
    public int getUnreadCount(String assigneeId) {
        if (assigneeId == null || assigneeId.isEmpty()) {
            return 0;
        }

        // 统计pending和in_progress状态的任务数量
        LambdaQueryWrapper<PendingTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PendingTask::getAssigneeId, assigneeId)
               .in(PendingTask::getStatus, "pending", "in_progress");

        return Math.toIntExact(pendingTaskMapper.selectCount(wrapper));
    }

    @Override
    public String getRedirectUrl(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }

        log.info("获取任务跳转URL: taskId={}", taskId);

        // 查询任务实体
        PendingTask task = pendingTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        // 返回redirect_url字段（如果为空则根据source_type生成默认URL）
        String redirectUrl = task.getRedirectUrl();
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return redirectUrl;
        }

        // 根据任务类型生成默认跳转URL
        return generateDefaultRedirectUrl(task);
    }

    /**
     * 根据任务类型生成默认跳转URL
     *
     * @param task 待办任务实体
     * @return 默认跳转URL
     */
    private String generateDefaultRedirectUrl(PendingTask task) {
        String sourceType = task.getSourceType();
        String sourceId = task.getSourceId();

        // 根据来源类型映射到对应的业务页面
        switch (sourceType) {
            case "settlement":
                return "/store-management/daily-settlement?id=" + sourceId;
            case "certificate":
                return "/store-management/certificate?highlight=" + sourceId;
            case "recruitment":
                return "/store-management/recruitment?approval=" + sourceId;
            case "inventory":
                return "/warehouse/inventory?id=" + sourceId;
            case "device_fault":
                return "/device/manage?id=" + sourceId;
            default:
                // 默认返回待办事项列表页
                return "/store-management/pending-tasks";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExpiryAlertTask(String certId, String certType, LocalDate expiryDate,
                                       String storeId, String storeName, String assigneeId,
                                       long daysLeft) {
        log.info("创建证件到期预警任务: certType={}, daysLeft={}, storeId={}", certType, daysLeft, storeId);

        // 1. 构建待办任务实体
        PendingTask task = new PendingTask();
        task.setTaskType("certificate_expiry");

        // 2. 根据剩余天数确定优先级和标题
        int priority;
        String title;
        String description;

        if (daysLeft < 0) {
            priority = 4;
            title = String.format("%s已过期%d天", certType, Math.abs(daysLeft));
            description = String.format("%s的%s已于%s到期，请立即办理续期！", storeName, certType, expiryDate);
        } else if (daysLeft <= 30) {
            priority = 3;
            title = String.format("%s即将到期", certType);
            description = String.format("%s的%s将于%s到期（还剩%d天），请及时安排续期", storeName, certType, expiryDate, daysLeft);
        } else {
            priority = 2;
            title = String.format("%s续期提醒", certType);
            description = String.format("%s的%s将于%s到期（还剩%d天），请提前准备", storeName, certType, expiryDate, daysLeft);
        }

        // 3. 设置任务属性
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus("pending");
        task.setSourceType("certificate");
        task.setSourceId(certId);
        task.setRedirectUrl("/store-management/certificate?highlight=" + certId);
        task.setAssigneeId(assigneeId);

        if (daysLeft <= 30 && daysLeft >= 0) {
            task.setDueDate(expiryDate);
        } else if (daysLeft < 0) {
            task.setDueDate(LocalDate.now().plusDays(3));
        }

        // 4. 幂等性检查
        LambdaQueryWrapper<PendingTask> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PendingTask::getTaskType, "certificate_expiry")
                   .eq(PendingTask::getSourceId, certId)
                   .eq(PendingTask::getStatus, "pending")
                   .ge(PendingTask::getCreateTime, LocalDateTime.now().toLocalDate().atStartOfDay());

        if (pendingTaskMapper.selectCount(checkWrapper) > 0) {
            log.info("证书{}今日已存在待办任务，跳过创建", certId);
            return;
        }

        // 5. 保存到数据库
        int rows = pendingTaskMapper.insert(task);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "创建证件到期预警任务失败");
        }

        log.info("证件到期预警任务创建成功: taskId={}, certId={}", task.getTaskId(), certId);
    }

    /**
     * 将实体转换为视图对象
     *
     * @param task 待办任务实体
     * @return 视图对象
     */
    private PendingTaskVO convertToVO(PendingTask task) {
        if (task == null) {
            return null;
        }
        // 委托给DataService转换（带缓存）
        return pendingTaskDataService.getPendingTaskBasicInfo(task.getTaskId());
    }

    /**
     * 构建操作日志实体
     */
    private com.foodtraceability.entity.OperationLogEntity buildLogEntity(String operationType, String operationModule, String recordId, String operatorId) {
        com.foodtraceability.entity.OperationLogEntity logEntity = new com.foodtraceability.entity.OperationLogEntity();
        logEntity.setOperationType(operationType);
        logEntity.setOperationModule(operationModule);
        logEntity.setRecordId(recordId);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperationTime(LocalDateTime.now());
        return logEntity;
    }
}
