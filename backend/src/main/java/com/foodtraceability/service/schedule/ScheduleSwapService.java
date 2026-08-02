package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.SwapRequestCreateDTO;
import com.foodtraceability.dto.schedule.SwapRequestQueryDTO;
import com.foodtraceability.dto.schedule.SwapRequestVO;

/**
 * 排班换班管理服务接口
 * 提供换班申请的创建、审批、查询等业务功能
 *
 * <p>状态机：
 * <ul>
 *   <li>pending → approved（审批通过）</li>
 *   <li>pending → rejected（驳回）</li>
 *   <li>pending → cancelled（取消）</li>
 * </ul>
 */
public interface ScheduleSwapService {

    /**
     * 分页查询换班请求列表
     * @param queryDTO 查询条件（含分页、状态、方案ID筛选）
     * @return 分页结果
     */
    PageResult<SwapRequestVO> getSwapList(SwapRequestQueryDTO queryDTO);

    /**
     * 根据ID获取换班请求详情（含发起人/目标人班次详情）
     * @param id 换班请求ID
     * @return 换班请求详情，不存在返回null
     */
    SwapRequestVO getSwapById(String id);

    /**
     * 创建换班申请
     * 业务规则：从排班条目中读取发起人和目标员工的班次信息
     * @param createDTO 创建请求DTO
     * @return 创建后的换班请求
     */
    SwapRequestVO createSwap(SwapRequestCreateDTO createDTO);

    /**
     * 审批通过换班申请
     * 业务规则：仅 pending 状态可审批通过
     * @param id 换班请求ID
     * @param comment 审批意见（可选）
     * @return 更新后的换班请求
     */
    SwapRequestVO approveSwap(String id, String comment);

    /**
     * 驳回换班申请
     * 业务规则：仅 pending 状态可驳回
     * @param id 换班请求ID
     * @param reason 驳回原因
     * @return 更新后的换班请求
     */
    SwapRequestVO rejectSwap(String id, String reason);

    /**
     * 取消换班申请（发起人操作）
     * 业务规则：仅 pending 状态可取消
     * @param id 换班请求ID
     * @return 更新后的换班请求
     */
    SwapRequestVO cancelSwap(String id);

    /**
     * 获取待处理换班请求数量（用于红点提示）
     * @return 待处理数量
     */
    Integer getPendingCount();
}
