package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.schedule.SwapRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 换班申请Mapper接口
 */
@Mapper
public interface SwapRequestMapper extends BaseMapper<SwapRequest> {

    /**
     * 查询方案的换班申请列表
     * @param planId 方案ID
     * @return 换班申请列表
     */
    List<SwapRequest> selectByPlanId(String planId);

    /**
     * 查询发起人的换班申请
     * @param initiatorId 发起人用户ID
     * @param status 状态(可选)
     * @return 换班申请列表
     */
    List<SwapRequest> selectByInitiatorAndStatus(
            Long initiatorId,
            String status);

    /**
     * 查询目标员工的换班申请
     * @param targetEmployeeId 目标员工ID
     * @param status 状态(可选)
     * @return 换班申请列表
     */
    List<SwapRequest> selectByTargetAndStatus(
            Long targetEmployeeId,
            String status);

    /**
     * 统计待审批数量(用于红点提示)
     * @param storeId 门店ID
     * @return 待审批数量
     */
    Long countPendingByStore(Long storeId);

    /**
     * 按状态统计换班申请数量
     * @param planId 方案ID
     * @return 各状态数量列表
     */
    List<SwapRequest> countByStatus(@Param("planId") String planId);

    /**
     * 分页查询换班请求（支持按状态、方案ID筛选）
     * @param page 分页对象
     * @param status 状态筛选（可选）
     * @param planId 方案ID筛选（可选）
     * @return 分页结果
     */
    IPage<SwapRequest> selectSwapPage(Page<SwapRequest> page,
                                       @Param("status") String status,
                                       @Param("planId") String planId);

    /**
     * 统计待处理换班请求数量
     * @return 待处理数量
     */
    Integer countPending();
}
