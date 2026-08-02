package com.foodtraceability.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.PrintTaskDataService;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.Device;
import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.mapper.DeviceMapper;
import com.foodtraceability.mapper.PrintTaskMapper;
import com.foodtraceability.service.device.PrintTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 打印任务服务实现类
 */
@Service
public class PrintTaskServiceImpl implements PrintTaskService {

    private static final Logger log = LoggerFactory.getLogger(PrintTaskServiceImpl.class);

    private final PrintTaskMapper printTaskMapper;
    private final DeviceMapper deviceMapper;
    private final PrintTaskDataService printTaskDataService;

    public PrintTaskServiceImpl(PrintTaskMapper printTaskMapper,
                               DeviceMapper deviceMapper,
                               PrintTaskDataService printTaskDataService) {
        this.printTaskMapper = printTaskMapper;
        this.deviceMapper = deviceMapper;
        this.printTaskDataService = printTaskDataService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PrintTaskVO> createPrintTask(PrintTaskCreateDTO dto) {
        try {
            // 验证目标设备是否存在
            Device device = deviceMapper.selectById(dto.getDeviceId());
            if (device == null) {
                return Result.error("目标打印机不存在");
            }

            // 验证设备类型是否为打印机
            if (device.getDeviceType() == null || device.getDeviceType() != 1) {
                return Result.error("目标设备不是打印机");
            }

            PrintTask task = new PrintTask();
            BeanUtils.copyProperties(dto, task);
            task.setTaskCode(generateTaskCode());
            task.setPrintStatus(0); // 待打印
            task.setRetryCount(0);
            task.setMaxRetry(dto.getMaxRetry() != null ? dto.getMaxRetry() : 3);
            task.setCreateTime(LocalDateTime.now());
            printTaskMapper.insert(task);

            log.info("创建打印任务成功: taskCode={}, deviceId={}", task.getTaskCode(), dto.getDeviceId());

            return Result.success(printTaskDataService.getPrintTaskBasicInfo(task.getTaskId()), "创建打印任务成功");
        } catch (Exception e) {
            log.error("创建打印任务失败", e);
            return Result.error("创建打印任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<List<PrintTaskVO>> batchCreatePrintTasks(List<PrintTaskCreateDTO> dtoList) {
        try {
            List<PrintTaskVO> result = new ArrayList<>();

            for (PrintTaskCreateDTO dto : dtoList) {
                Result<PrintTaskVO> createResult = createPrintTask(dto);
                if (!createResult.isSuccess()) {
                    return Result.error("批量创建任务失败：" + createResult.getMessage());
                }
                result.add(createResult.getData());
            }

            return Result.success(result, "批量创建打印任务成功");
        } catch (Exception e) {
            log.error("批量创建打印任务失败", e);
            return Result.error("批量创建打印任务失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PrintTaskVO> getTaskById(Long taskId) {
        try {
            PrintTaskVO vo = printTaskDataService.getPrintTaskBasicInfo(taskId);
            if (vo == null) {
                return Result.error("打印任务不存在");
            }
            return Result.success(vo, "查询打印任务成功");
        } catch (Exception e) {
            log.error("查询打印任务失败: taskId={}", taskId, e);
            return Result.error("查询打印任务失败：" + e.getMessage());
        }
    }

    @Override
    public Result<IPage<PrintTaskVO>> getTaskPage(Page<?> page, PrintTaskQueryDTO queryDto) {
        try {
            LambdaQueryWrapper<PrintTask> wrapper = buildQueryWrapper(queryDto);
            wrapper.orderByDesc(PrintTask::getCreateTime);

            @SuppressWarnings("unchecked")
            Page<PrintTask> printTaskPage = (Page<PrintTask>) page;
            IPage<PrintTask> taskPage = printTaskMapper.selectPage(printTaskPage, wrapper);

            // 转换为VO
            IPage<PrintTaskVO> voPage = taskPage.convert(task -> printTaskDataService.getPrintTaskBasicInfo(task.getTaskId()));

            return Result.success(voPage, "查询打印任务列表成功");
        } catch (Exception e) {
            log.error("分页查询打印任务失败", e);
            return Result.error("查询打印任务列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<PrintTaskVO>> getPendingTasksByDevice(Long deviceId) {
        try {
            LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PrintTask::getDeviceId, deviceId)
                   .eq(PrintTask::getPrintStatus, 0) // 待打印
                   .orderByAsc(PrintTask::getCreateTime);

            List<PrintTask> tasks = printTaskMapper.selectList(wrapper);
            List<PrintTaskVO> voList = tasks.stream()
                    .map(task -> printTaskDataService.getPrintTaskBasicInfo(task.getTaskId()))
                    .collect(Collectors.toList());

            return Result.success(voList, "查询待打印任务成功");
        } catch (Exception e) {
            log.error("查询待打印任务失败: deviceId={}", deviceId, e);
            return Result.error("查询待打印任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateTaskToPrinting(Long taskId) {
        return updateTaskStatus(taskId, 1, null); // 1-打印中
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateTaskToCompleted(Long taskId) {
        try {
            PrintTask task = printTaskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("打印任务不存在");
            }

            task.setPrintStatus(2); // 已完成
            task.setCompleteTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            printTaskMapper.updateById(task);

            // 清除缓存
            printTaskDataService.clearPrintTaskCache(taskId);

            log.info("打印任务完成: taskId={}, taskCode={}", taskId, task.getTaskCode());
            return Result.success(null, "打印任务已完成");
        } catch (Exception e) {
            log.error("更新打印任务状态失败: taskId={}", taskId, e);
            return Result.error("更新打印任务状态失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateTaskToFailed(Long taskId, String errorMessage) {
        return updateTaskStatus(taskId, 3, errorMessage); // 3-失败
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> retryFailedTask(Long taskId) {
        try {
            PrintTask task = printTaskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("打印任务不存在");
            }

            if (task.getPrintStatus() != 3) {
                return Result.error("只有失败的任务才能重试");
            }

            if (task.getRetryCount() >= task.getMaxRetry()) {
                return Result.error("已达到最大重试次数，无法继续重试");
            }

            // 增加重试计数，重置为待打印状态
            task.setRetryCount(task.getRetryCount() + 1);
            task.setPrintStatus(0); // 待打印
            task.setErrorMessage(null);
            task.setUpdateTime(LocalDateTime.now());
            printTaskMapper.updateById(task);

            // 清除缓存
            printTaskDataService.clearPrintTaskCache(taskId);

            log.info("重试打印任务: taskId={}, retryCount={}/{}, taskCode={}",
                    taskId, task.getRetryCount(), task.getMaxRetry(), task.getTaskCode());

            return Result.success(null, "重试任务已加入队列");
        } catch (Exception e) {
            log.error("重试打印任务失败: taskId={}", taskId, e);
            return Result.error("重试打印任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeById(Long taskId) {
        try {
            PrintTask task = printTaskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("打印任务不存在");
            }

            // 逻辑删除（实体配置了@TableLogic，deleteById会执行逻辑删除）
            printTaskMapper.deleteById(taskId);

            // 清除缓存
            printTaskDataService.clearPrintTaskCache(taskId);

            log.info("删除打印任务: taskId={}, taskCode={}", taskId, task.getTaskCode());
            return Result.success(null, "删除打印任务成功");
        } catch (Exception e) {
            log.error("删除打印任务失败: taskId={}", taskId, e);
            return Result.error("删除打印任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PrintTaskVO> updateById(PrintTask task) {
        try {
            if (task.getTaskId() == null) {
                return Result.error("任务ID不能为空");
            }

            PrintTask existing = printTaskMapper.selectById(task.getTaskId());
            if (existing == null) {
                return Result.error("打印任务不存在");
            }

            task.setUpdateTime(LocalDateTime.now());
            printTaskMapper.updateById(task);

            // 清除缓存
            printTaskDataService.clearPrintTaskCache(task.getTaskId());

            log.info("更新打印任务: taskId={}", task.getTaskId());
            return Result.success(printTaskDataService.getPrintTaskBasicInfo(task.getTaskId()), "更新打印任务成功");
        } catch (Exception e) {
            log.error("更新打印任务失败: taskId={}", task.getTaskId(), e);
            return Result.error("更新打印任务失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    private Result<Void> updateTaskStatus(Long taskId, Integer status, String errorMessage) {
        try {
            PrintTask task = printTaskMapper.selectById(taskId);
            if (task == null) {
                return Result.error("打印任务不存在");
            }

            task.setPrintStatus(status);
            if (status == 1) { // 打印中
                task.setPrintTime(LocalDateTime.now());
            }
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }
            task.setUpdateTime(LocalDateTime.now());
            printTaskMapper.updateById(task);

            // 清除缓存
            printTaskDataService.clearPrintTaskCache(taskId);

            log.info("更新打印任务状态: taskId={}, status={}", taskId, status);
            return Result.success(null, "更新任务状态成功");
        } catch (Exception e) {
            log.error("更新打印任务状态失败: taskId={}", taskId, e);
            return Result.error("更新任务状态失败：" + e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PrintTask> buildQueryWrapper(PrintTaskQueryDTO queryDto) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();

        if (queryDto == null) {
            return wrapper;
        }

        if (queryDto.getTaskCode() != null && !queryDto.getTaskCode().isEmpty()) {
            wrapper.like(PrintTask::getTaskCode, queryDto.getTaskCode());
        }
        if (queryDto.getDeviceId() != null) {
            wrapper.eq(PrintTask::getDeviceId, queryDto.getDeviceId());
        }
        if (queryDto.getTaskType() != null) {
            wrapper.eq(PrintTask::getTaskType, queryDto.getTaskType());
        }
        if (queryDto.getPrintStatus() != null) {
            wrapper.eq(PrintTask::getPrintStatus, queryDto.getPrintStatus());
        }
        if (queryDto.getStartTime() != null && !queryDto.getStartTime().isEmpty()) {
            wrapper.ge(PrintTask::getCreateTime, queryDto.getStartTime());
        }
        if (queryDto.getEndTime() != null && !queryDto.getEndTime().isEmpty()) {
            wrapper.le(PrintTask::getCreateTime, queryDto.getEndTime());
        }

        return wrapper;
    }

    /**
     * 生成任务编号
     */
    private String generateTaskCode() {
        return "PT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
