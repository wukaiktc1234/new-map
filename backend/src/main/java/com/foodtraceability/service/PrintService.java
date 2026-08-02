package com.foodtraceability.service;

import com.foodtraceability.dto.PrintTaskDTO;

import java.util.List;

public interface PrintService {
    
    PrintTaskDTO addPrintTask(PrintTaskDTO task);
    
    PrintTaskDTO getTaskStatus(String taskId);
    
    boolean cancelTask(String taskId);
    
    List<PrintTaskDTO> getAllTasks();
    
    List<PrintTaskDTO> getPendingTasks();
    
    PrintTaskDTO getPrintTask(Long taskId);
    
    boolean updatePrintTaskStatus(Long taskId, String status);
    
    boolean cancelPrintTask(Long taskId);
}
