package com.foodtraceability.service.impl;

import com.foodtraceability.dto.PrintTaskDTO;
import com.foodtraceability.service.PrintService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrintServiceImpl implements PrintService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrintServiceImpl.class);
    private final Map<String, PrintTaskDTO> taskStore = new ConcurrentHashMap<>();

    @Override
    public PrintTaskDTO addPrintTask(PrintTaskDTO task) {
        if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
            task.setTaskId(UUID.randomUUID().toString());
        }
        task.setStatus("PENDING");
        taskStore.put(task.getTaskId(), task);
        log.info("添加打印任务: {}, taskId: {}", task.getTaskName(), task.getTaskId());
        return task;
    }

    @Override
    public PrintTaskDTO getTaskStatus(String taskId) {
        PrintTaskDTO dto = taskStore.get(taskId);
        if (dto == null) {
            dto = new PrintTaskDTO();
            dto.setTaskId(taskId);
            dto.setStatus("COMPLETED");
        }
        return dto;
    }

    @Override
    public boolean cancelTask(String taskId) {
        log.info("取消打印任务: {}", taskId);
        taskStore.remove(taskId);
        return true;
    }

    @Override
    public List<PrintTaskDTO> getAllTasks() {
        return new ArrayList<>(taskStore.values());
    }

    @Override
    public List<PrintTaskDTO> getPendingTasks() {
        List<PrintTaskDTO> pending = new ArrayList<>();
        for (PrintTaskDTO task : taskStore.values()) {
            if ("PENDING".equals(task.getStatus())) {
                pending.add(task);
            }
        }
        return pending;
    }

    @Override
    public PrintTaskDTO getPrintTask(Long taskId) {
        for (PrintTaskDTO task : taskStore.values()) {
            if (taskId.toString().equals(task.getTaskId())) {
                return task;
            }
        }
        return null;
    }

    @Override
    public boolean updatePrintTaskStatus(Long taskId, String status) {
        for (PrintTaskDTO task : taskStore.values()) {
            if (taskId.toString().equals(task.getTaskId())) {
                task.setStatus(status);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean cancelPrintTask(Long taskId) {
        return cancelTask(taskId.toString());
    }
}
