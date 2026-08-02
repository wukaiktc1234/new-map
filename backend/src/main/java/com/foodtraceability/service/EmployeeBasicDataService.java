package com.foodtraceability.service;

import com.foodtraceability.dto.EmployeeBasicInfo;
import java.util.List;
import java.util.Map;

/**
 * 员工基础数据服务接口
 * 用于跨模块共享员工基础信息
 */
public interface EmployeeBasicDataService {
    
    /**
     * 批量获取员工基础信息（优先从缓存获取）
     */
    Map<String, EmployeeBasicInfo> batchGetBasicInfo(List<String> employeeIds);
    
    /**
     * 获取单个员工基础信息
     */
    EmployeeBasicInfo getBasicInfo(String employeeId);
    
    /**
     * 清除员工缓存
     */
    void clearCache(String employeeId);
    
    /**
     * 批量清除缓存
     */
    void clearBatchCache(List<String> employeeIds);
    
    /**
     * 清除所有缓存
     */
    void clearAllCache();
}
