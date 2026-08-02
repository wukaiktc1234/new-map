package com.foodtraceability.service;

import com.foodtraceability.dto.*;
import java.util.List;

/**
 * 申诉业务服务接口
 */
public interface AppealService {

    /**
     * 创建申诉
     *
     * @param dto   创建请求
     * @param userId 当前用户ID
     * @return 申诉详情
     */
    AppealDetailVO createAppeal(AppealCreateDTO dto, String userId);

    /**
     * 获取我的申诉列表（分页）
     *
     * @param userId 用户ID
     * @param status 状态筛选（可选）
     * @param current 页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<AppealVO> getMyAppeals(String userId, String status, int current, int size);

    /**
     * 获取申诉详情
     *
     * @param appealId 申诉ID
     * @param userId 请求用户ID（用于权限校验）
     * @return 申诉详情
     */
    AppealDetailVO getAppealDetail(String appealId, String userId);

    /**
     * 撤回申诉
     *
     * @param appealId 申诉ID
     * @param userId 操作用户ID
     */
    void withdrawAppeal(String appealId, String userId);

    /**
     * 处理申诉（审核人操作）
     *
     * @param appealId 申诉ID
     * @param dto 处理请求
     * @param operatorId 审核人ID
     */
    void processAppeal(String appealId, AppealProcessDTO dto, String operatorId);
}
