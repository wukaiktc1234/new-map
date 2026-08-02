package com.foodtraceability.service;

import com.foodtraceability.entity.HealthCertificate;

/**
 * HR系统服务接口
 * 用于处理与HR系统的数据同步
 */
public interface HrSystemService {
    
    /**
     * 同步健康证数据至HR系统
     * @param healthCertificate 健康证数据
     * @return 同步结果
     */
    boolean syncHealthCertificateToHrSystem(HealthCertificate healthCertificate);
    
    /**
     * 从HR系统获取员工信息
     * @param employeeId 员工ID
     * @return 员工信息
     */
    Object getEmployeeInfoFromHrSystem(String employeeId);
    
    /**
     * 同步健康证审核结果至HR系统
     * @param healthCertificate 健康证数据
     * @return 同步结果
     */
    boolean syncHealthCertificateApprovalResult(HealthCertificate healthCertificate);
}
