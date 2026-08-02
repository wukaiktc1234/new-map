package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DeviceWorkflow;
import com.foodtraceability.entity.DeviceWorkflowExecutionLog;

import java.util.List;
import java.util.Map;

/**
 * 设备协同工作流服务接口
 * 负责设备协同工作流的CRUD操作和执行管理
 */
public interface DeviceWorkflowService extends IService<DeviceWorkflow> {
    
    /**
     * 获取指定设备类型的工作流列表
     * @param deviceType 设备类型
     * @return 工作流列表
     */
    List<DeviceWorkflow> getWorkflowsByDeviceType(String deviceType);
    
    /**
     * 获取指定事件类型的工作流列表
     * @param eventType 事件类型
     * @return 工作流列表
     */
    List<DeviceWorkflow> getWorkflowsByEventType(String eventType);
    
    /**
     * 执行工作流
     * @param workflow 工作流定义
     * @param triggerData 触发数据
     * @return 执行结果
     */
    Map<String, Object> executeWorkflow(DeviceWorkflow workflow, Map<String, Object> triggerData);
    
    /**
     * 根据触发事件执行相关工作流
     * @param deviceId 触发设备ID
     * @param deviceType 触发设备类型
     * @param eventType 触发事件类型
     * @param triggerData 触发数据
     * @return 执行结果列表
     */
    List<Map<String, Object>> executeWorkflowsByEvent(String deviceId, String deviceType, String eventType, Map<String, Object> triggerData);
    
    /**
     * 获取工作流执行日志
     * @param workflowId 工作流ID
     * @param page 页码
     * @param size 每页数量
     * @return 执行日志列表
     */
    List<DeviceWorkflowExecutionLog> getWorkflowExecutionLogs(Long workflowId, int page, int size);
    
    /**
     * 启用工作流
     * @param id 工作流ID
     * @return 是否启用成功
     */
    boolean enableWorkflow(Long id);
    
    /**
     * 禁用工作流
     * @param id 工作流ID
     * @return 是否禁用成功
     */
    boolean disableWorkflow(Long id);
}