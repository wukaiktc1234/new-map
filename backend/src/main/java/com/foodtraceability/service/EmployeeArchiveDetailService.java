package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.EmployeeArchiveDetail;

import java.util.Map;

/**
 * 员工档案详细服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
public interface EmployeeArchiveDetailService extends IService<EmployeeArchiveDetail> {

    /**
     * 根据档案ID获取详情
     */
    EmployeeArchiveDetail getByArchiveId(Long archiveId);

    /**
     * 根据员工ID获取详情
     */
    EmployeeArchiveDetail getByEmployeeId(String employeeId);

    /**
     * 计算档案完整度
     */
    int calculateCompleteness(EmployeeArchiveDetail detail);

    /**
     * 更新档案完整度
     */
    void updateCompleteness(Long detailId);

    /**
     * 审核档案
     */
    void reviewArchive(Long detailId, String reviewStatus, String reviewComment, Long reviewerId);

    /**
     * 同步档案数据到员工表
     */
    void syncToEmployee(String employeeCode);

    /**
     * 获取档案统计
     */
    Map<String, Object> getStatistics();
}
