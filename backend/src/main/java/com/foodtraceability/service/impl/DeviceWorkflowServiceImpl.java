package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.driver.DeviceDriverManager;
import com.foodtraceability.entity.DeviceWorkflow;
import com.foodtraceability.entity.DeviceWorkflowExecutionLog;
import com.foodtraceability.mapper.DeviceWorkflowExecutionLogMapper;
import com.foodtraceability.mapper.DeviceWorkflowMapper;
import com.foodtraceability.service.DeviceWorkflowService;
import com.foodtraceability.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 设备协同工作流服务实现类
 * 实现设备协同工作流的CRUD操作和执行管理
 */
@Service
public class DeviceWorkflowServiceImpl extends ServiceImpl<DeviceWorkflowMapper, DeviceWorkflow> implements DeviceWorkflowService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceWorkflowServiceImpl.class);
    

    public DeviceWorkflowServiceImpl(DeviceWorkflowMapper deviceWorkflowMapper, DeviceWorkflowExecutionLogMapper executionLogMapper, DeviceDriverManager deviceDriverManager) {
        this.deviceWorkflowMapper = deviceWorkflowMapper;
        this.executionLogMapper = executionLogMapper;
        this.deviceDriverManager = deviceDriverManager;
    }

    private final DeviceWorkflowMapper deviceWorkflowMapper;
    
    private final DeviceWorkflowExecutionLogMapper executionLogMapper;
    
    private final DeviceDriverManager deviceDriverManager;
    
    @Override
    public List<DeviceWorkflow> getWorkflowsByDeviceType(String deviceType) {
        LambdaQueryWrapper<DeviceWorkflow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceWorkflow::getTriggerDeviceType, deviceType)
                   .eq(DeviceWorkflow::getStatus, 1)
                   .orderByDesc(DeviceWorkflow::getUpdatedAt);
        
        return baseMapper.selectList(queryWrapper);
    }
    
    @Override
    public List<DeviceWorkflow> getWorkflowsByEventType(String eventType) {
        LambdaQueryWrapper<DeviceWorkflow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceWorkflow::getTriggerEventType, eventType)
                   .eq(DeviceWorkflow::getStatus, 1)
                   .orderByDesc(DeviceWorkflow::getUpdatedAt);
        
        return baseMapper.selectList(queryWrapper);
    }
    
    @Override
    public Map<String, Object> executeWorkflow(DeviceWorkflow workflow, Map<String, Object> triggerData) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        boolean success = true;
        String errorMessage = null;
        
        try {
            log.info("开始执行工作流: {} ID: {}", workflow.getWorkflowName(), workflow.getId());
            
            // 解析触发条件
            Map<String, Object> conditionMap = JsonUtils.fromJson(workflow.getTriggerCondition(), Map.class);
            
            // 检查触发条件是否满足
            if (!checkCondition(conditionMap, triggerData)) {
                log.debug("工作流 {} 触发条件不满足，跳过执行", workflow.getWorkflowName());
                result.put("success", false);
                result.put("message", "触发条件不满足");
                return result;
            }
            
            // 解析执行动作
            List<Map<String, Object>> actionList = JsonUtils.fromJson(workflow.getActionConfig(), List.class);
            
            // 执行动作
            List<Map<String, Object>> actionResults = new ArrayList<>();
            for (Map<String, Object> action : actionList) {
                Map<String, Object> actionResult = executeAction(action, triggerData);
                actionResults.add(actionResult);
                
                // 如果动作执行失败，根据配置决定是否继续
                if (!Boolean.TRUE.equals(actionResult.get("success"))) {
                    Boolean continueOnError = (Boolean) action.getOrDefault("continueOnError", false);
                    if (!continueOnError) {
                        success = false;
                        errorMessage = (String) actionResult.getOrDefault("message", "动作执行失败");
                        break;
                    }
                }
            }
            
            result.put("success", success);
            result.put("message", success ? "工作流执行成功" : errorMessage);
            result.put("actionResults", actionResults);
            
        } catch (Exception e) {
            log.error("执行工作流 {} 异常: {}", workflow.getWorkflowName(), e.getMessage(), e);
            success = false;
            errorMessage = e.getMessage();
            result.put("success", false);
            result.put("message", "执行工作流异常: " + e.getMessage());
        } finally {
            // 记录执行日志
            long executionTime = System.currentTimeMillis() - startTime;
            saveExecutionLog(workflow, triggerData, result, success, errorMessage, executionTime);
            
            log.info("工作流 {} 执行完成，耗时 {} 毫秒，结果: {}", workflow.getWorkflowName(), executionTime, success ? "成功" : "失败");
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> executeWorkflowsByEvent(String deviceId, String deviceType, String eventType, Map<String, Object> triggerData) {
        log.info("开始执行事件 {} 相关的工作流", eventType);
        
        // 获取相关工作流
        LambdaQueryWrapper<DeviceWorkflow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceWorkflow::getTriggerDeviceType, deviceType)
                   .eq(DeviceWorkflow::getTriggerEventType, eventType)
                   .eq(DeviceWorkflow::getStatus, 1)
                   .orderByDesc(DeviceWorkflow::getUpdatedAt);
        
        List<DeviceWorkflow> workflows = baseMapper.selectList(queryWrapper);
        log.info("找到 {} 个相关工作流", workflows.size());
        
        // 执行每个工作流
        List<Map<String, Object>> results = new ArrayList<>();
        for (DeviceWorkflow workflow : workflows) {
            Map<String, Object> result = executeWorkflow(workflow, triggerData);
            results.add(result);
        }
        
        log.info("事件 {} 相关的工作流执行完成", eventType);
        return results;
    }
    
    @Override
    public List<DeviceWorkflowExecutionLog> getWorkflowExecutionLogs(Long workflowId, int page, int size) {
        LambdaQueryWrapper<DeviceWorkflowExecutionLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceWorkflowExecutionLog::getWorkflowId, workflowId)
                   .orderByDesc(DeviceWorkflowExecutionLog::getCreatedAt);
        
        int offset = (page - 1) * size;
        queryWrapper.last("LIMIT " + offset + "," + size);
        
        return executionLogMapper.selectList(queryWrapper);
    }
    
    @Override
    public boolean enableWorkflow(Long id) {
        DeviceWorkflow workflow = baseMapper.selectById(id);
        if (workflow == null) {
            log.error("工作流ID {} 不存在", id);
            return false;
        }
        
        workflow.setStatus(1);
        int updateCount = baseMapper.updateById(workflow);
        return updateCount > 0;
    }
    
    @Override
    public boolean disableWorkflow(Long id) {
        DeviceWorkflow workflow = baseMapper.selectById(id);
        if (workflow == null) {
            log.error("工作流ID {} 不存在", id);
            return false;
        }
        
        workflow.setStatus(0);
        int updateCount = baseMapper.updateById(workflow);
        return updateCount > 0;
    }
    
    /**
     * 检查触发条件是否满足
     * @param conditionMap 条件配置
     * @param triggerData 触发数据
     * @return 条件是否满足
     */
    private boolean checkCondition(Map<String, Object> conditionMap, Map<String, Object> triggerData) {
        // 简单实现，实际项目中可以使用更复杂的条件表达式引擎
        if (conditionMap == null || conditionMap.isEmpty()) {
            return true; // 无条件时默认满足
        }
        
        // 检查每个条件
        for (Map.Entry<String, Object> entry : conditionMap.entrySet()) {
            String key = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = triggerData.get(key);
            
            if (!Objects.equals(expectedValue, actualValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 执行单个动作
     * @param action 动作配置
     * @param triggerData 触发数据
     * @return 执行结果
     */
    private Map<String, Object> executeAction(Map<String, Object> action, Map<String, Object> triggerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String actionType = (String) action.get("type");
            Map<String, Object> actionParams = (Map<String, Object>) action.get("params");
            
            log.info("执行动作: {} 参数: {}", actionType, actionParams);
            
            // 根据动作类型执行不同的操作
            switch (actionType) {
                case "device_operation":
                    // 执行设备操作
                    result.putAll(executeDeviceOperation(actionParams, triggerData));
                    break;
                case "notification":
                    // 发送通知
                    result.putAll(sendNotification(actionParams, triggerData));
                    break;
                case "data_logging":
                    // 记录数据
                    result.putAll(logData(actionParams, triggerData));
                    break;
                default:
                    log.error("不支持的动作类型: {}", actionType);
                    result.put("success", false);
                    result.put("message", "不支持的动作类型: " + actionType);
                    break;
            }
            
        } catch (Exception e) {
            log.error("执行动作异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行动作异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行设备操作
     * @param actionParams 动作参数
     * @param triggerData 触发数据
     * @return 执行结果
     */
    private Map<String, Object> executeDeviceOperation(Map<String, Object> actionParams, Map<String, Object> triggerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String targetDeviceId = (String) actionParams.get("deviceId");
            String operationType = (String) actionParams.get("operationType");
            Map<String, Object> params = (Map<String, Object>) actionParams.get("params");
            
            log.info("执行设备操作: 设备ID={} 操作类型={} 参数={}", targetDeviceId, operationType, params);
            
            // 这里可以根据设备ID获取设备驱动并执行操作
            // 由于是示例，我们简单返回成功
            result.put("success", true);
            result.put("message", "设备操作执行成功");
            result.put("deviceId", targetDeviceId);
            result.put("operationType", operationType);
            
        } catch (Exception e) {
            log.error("执行设备操作异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行设备操作异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 发送通知
     * @param actionParams 动作参数
     * @param triggerData 触发数据
     * @return 执行结果
     */
    private Map<String, Object> sendNotification(Map<String, Object> actionParams, Map<String, Object> triggerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String notificationType = (String) actionParams.get("notificationType");
            String recipient = (String) actionParams.get("recipient");
            String content = (String) actionParams.get("content");
            
            log.info("发送通知: 类型={} 接收人={} 内容={}", notificationType, recipient, content);
            
            // 这里可以实现发送通知的逻辑，如邮件、短信、消息队列等
            // 由于是示例，我们简单返回成功
            result.put("success", true);
            result.put("message", "通知发送成功");
            result.put("notificationType", notificationType);
            result.put("recipient", recipient);
            
        } catch (Exception e) {
            log.error("发送通知异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "发送通知异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 记录数据
     * @param actionParams 动作参数
     * @param triggerData 触发数据
     * @return 执行结果
     */
    private Map<String, Object> logData(Map<String, Object> actionParams, Map<String, Object> triggerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String dataType = (String) actionParams.get("dataType");
            Map<String, Object> data = (Map<String, Object>) actionParams.get("data");
            
            log.info("记录数据: 类型={} 数据={}", dataType, data);
            
            // 这里可以实现数据记录的逻辑，如保存到数据库、日志文件等
            // 由于是示例，我们简单返回成功
            result.put("success", true);
            result.put("message", "数据记录成功");
            result.put("dataType", dataType);
            
        } catch (Exception e) {
            log.error("记录数据异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "记录数据异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 保存执行日志
     * @param workflow 工作流
     * @param triggerData 触发数据
     * @param result 执行结果
     * @param success 是否成功
     * @param errorMessage 错误信息
     * @param executionTime 执行耗时
     */
    private void saveExecutionLog(DeviceWorkflow workflow, Map<String, Object> triggerData, 
                                 Map<String, Object> result, boolean success, 
                                 String errorMessage, long executionTime) {
        try {
            DeviceWorkflowExecutionLog log = new DeviceWorkflowExecutionLog();
            log.setWorkflowId(workflow.getId());
            log.setWorkflowName(workflow.getWorkflowName());
            log.setTriggerDeviceType(workflow.getTriggerDeviceType());
            log.setTriggerEventType(workflow.getTriggerEventType());
            log.setTriggerEventData(JsonUtils.toJson(triggerData));
            log.setExecutionStatus(success ? 1 : 0);
            log.setExecutionResult(JsonUtils.toJson(result));
            log.setExecutionTime(executionTime);
            log.setErrorMessage(errorMessage);
            log.setCreatedAt(LocalDateTime.now());
            log.setUpdatedAt(LocalDateTime.now());
            
            executionLogMapper.insert(log);
        } catch (Exception e) {
            log.error("保存工作流执行日志异常: {}", e.getMessage(), e);
        }
    }
}