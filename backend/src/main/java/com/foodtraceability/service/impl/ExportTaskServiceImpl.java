package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.dto.operations.ExportTaskDTO;
import com.foodtraceability.entity.report.ExportTask;
import com.foodtraceability.mapper.ExportTaskMapper;
import com.foodtraceability.service.ExportTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 导出任务服务实现类
 * 提供报表导出任务的管理
 */
@Service
public class ExportTaskServiceImpl implements ExportTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ExportTaskServiceImpl.class);

    private final ExportTaskMapper exportTaskMapper;

    public ExportTaskServiceImpl(ExportTaskMapper exportTaskMapper) {
        this.exportTaskMapper = exportTaskMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTask createTask(ExportTask task) {
        logger.info("创建导出任务，任务编号: {}", task.getTaskNo());
        exportTaskMapper.insert(task);
        logger.debug("导出任务创建成功，taskId: {}", task.getTaskId());
        return task;
    }

    @Override
    public ExportTask getTaskById(Long taskId) {
        logger.debug("根据ID查询导出任务，taskId: {}", taskId);
        return exportTaskMapper.selectById(taskId);
    }

    @Override
    public ExportTask getTaskByNo(String taskNo) {
        logger.debug("根据编号查询导出任务，taskNo: {}", taskNo);
        QueryWrapper<ExportTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_no", taskNo);
        return exportTaskMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(Long taskId, Integer status, String errorMessage) {
        logger.info("更新导出任务状态，taskId: {}, status: {}", taskId, status);

        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setStatus(status);
        task.setErrorMessage(errorMessage);

        exportTaskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, String filePath, Long fileSize, Integer rowCount) {
        logger.info("完成导出任务，taskId: {}, filePath: {}", taskId, filePath);

        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setStatus(2);
        task.setFilePath(filePath);
        task.setFileSize(fileSize);
        task.setRowCount(rowCount);
        task.setCompletedTime(LocalDateTime.now());

        exportTaskMapper.updateById(task);
    }

    @Override
    public List<ExportTaskDTO> getUserTasks(Long userId, Integer status) {
        logger.debug("查询用户导出任务，userId: {}, status: {}", userId, status);

        QueryWrapper<ExportTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("created_by", userId);
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");

        List<ExportTask> tasks = exportTaskMapper.selectList(queryWrapper);
        List<ExportTaskDTO> result = new ArrayList<>();

        for (ExportTask task : tasks) {
            result.add(convertToDTO(task));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId, Long userId) {
        logger.info("删除导出任务，taskId: {}, userId: {}", taskId, userId);

        ExportTask task = getTaskById(taskId);
        if (task == null || !task.getCreatedBy().equals(userId)) {
            throw new RuntimeException("任务不存在或无权限删除");
        }

        exportTaskMapper.deleteById(taskId);
    }

    @Override
    public String generateTaskNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "EXP" + dateStr + uuid;
    }

    /**
     * 转换为DTO
     * @param task 任务实体
     * @return 任务DTO
     */
    private ExportTaskDTO convertToDTO(ExportTask task) {
        ExportTaskDTO dto = new ExportTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTaskNo(task.getTaskNo());
        dto.setTaskType(task.getTaskType());
        dto.setReportType(task.getReportType());
        dto.setFileName(task.getFileName());
        dto.setFileSize(task.getFileSize());
        dto.setStatus(task.getStatus());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setRowCount(task.getRowCount());
        dto.setCompletedTime(task.getCompletedTime());
        dto.setCreateTime(task.getCreateTime());
        return dto;
    }
}
