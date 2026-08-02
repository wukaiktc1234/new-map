package com.foodtraceability.service;

import com.foodtraceability.dto.CustomReportVO;
import com.foodtraceability.dto.CustomReportCreateDTO;
import com.foodtraceability.dto.CustomReportQueryDTO;
import com.foodtraceability.dto.PageResult;
import java.util.Map;

/**
 * 自定义报表服务接口
 * 提供自定义SQL报表的创建、执行和管理功能
 */
public interface CustomReportService {

    /**
     * 创建自定义报表
     * @param dto 创建信息
     * @param ownerUserId 所有者用户ID
     * @return 报表ID
     */
    Long createCustomReport(CustomReportCreateDTO dto, Long ownerUserId);

    /**
     * 执行自定义报表
     * @param reportId 报表ID
     * @param params 查询参数
     * @param executedBy 执行人ID
     * @return 执行结果
     */
    Map<String, Object> executeCustomReport(Long reportId, Map<String, Object> params, Long executedBy);

    /**
     * 设置定时调度
     * @param reportId 报表ID
     * @param cronExpression cron表达式
     * @param userId 操作用户ID
     * @return 是否成功
     */
    boolean scheduleReport(Long reportId, String cronExpression, Long userId);

    /**
     * 获取执行历史
     * @param reportId 报表ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 执行历史列表
     */
    PageResult<Map<String, Object>> getReportHistory(Long reportId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询自定义报表
     * @param queryDTO 查询条件
     * @param userId 当前用户ID（用于权限过滤）
     * @return 分页结果
     */
    PageResult<CustomReportVO> queryCustomReports(CustomReportQueryDTO queryDTO, Long userId);

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    CustomReportVO getReportById(Long reportId);

    /**
     * 删除自定义报表
     * @param reportId 报表ID
     * @param userId 用户ID（权限校验）
     * @return 是否成功
     */
    boolean deleteCustomReport(Long reportId, Long userId);

    /**
     * 更新自定义报表
     * @param reportId 报表ID
     * @param dto 更新信息
     * @param userId 用户ID（权限校验）
     * @return 是否成功
     */
    boolean updateCustomReport(Long reportId, CustomReportCreateDTO dto, Long userId);
}
