package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.PrintTaskDataService;
import com.foodtraceability.dto.PrintTaskVO;
import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.mapper.PrintTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 打印任务数据服务实现类
 */
@Service
public class PrintTaskDataServiceImpl implements PrintTaskDataService {

    private static final Logger log = LoggerFactory.getLogger(PrintTaskDataServiceImpl.class);
    private static final String CACHE_NAME = "printTask";

    private final PrintTaskMapper printTaskMapper;

    public PrintTaskDataServiceImpl(PrintTaskMapper printTaskMapper) {
        this.printTaskMapper = printTaskMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#taskIds", unless = "#result == null || #result.isEmpty()")
    public Map<Long, PrintTaskVO> batchGetPrintTaskBasicInfo(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PrintTask> tasks = printTaskMapper.selectBatchIds(taskIds);
        return tasks.stream()
                .collect(Collectors.toMap(
                        PrintTask::getTaskId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #taskId", unless = "#result == null")
    public PrintTaskVO getPrintTaskBasicInfo(Long taskId) {
        if (taskId == null) {
            return null;
        }

        PrintTask task = printTaskMapper.selectById(taskId);
        return convertToVO(task);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #taskId")
    public void clearPrintTaskCache(Long taskId) {
        log.debug("清除打印任务缓存: taskId={}", taskId);
    }

    /**
     * 将实体转换为VO
     */
    private PrintTaskVO convertToVO(PrintTask task) {
        if (task == null) {
            return null;
        }

        PrintTaskVO vo = new PrintTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskCode(task.getTaskCode());
        vo.setDeviceId(task.getDeviceId());
        vo.setTaskType(task.getTaskType());
        vo.setTaskTypeName(getTaskTypeName(task.getTaskType()));
        vo.setContentJson(task.getContentJson());
        vo.setPrintStatus(task.getPrintStatus());
        vo.setPrintStatusName(getPrintStatusName(task.getPrintStatus()));
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreateUserId(task.getCreateUserId());
        vo.setPrintTime(task.getPrintTime());
        vo.setCompleteTime(task.getCompleteTime());
        vo.setCreateTime(task.getCreateTime());

        return vo;
    }

    private String getTaskTypeName(Integer taskType) {
        if (taskType == null) return "未知";
        return switch (taskType) {
            case 1 -> "小票";
            case 2 -> "标签";
            case 3 -> "报表";
            case 4 -> "厨房单";
            default -> "未知";
        };
    }

    private String getPrintStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待打印";
            case 1 -> "打印中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }
}
