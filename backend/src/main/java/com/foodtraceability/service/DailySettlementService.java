package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.SettlementQueryDTO;
import com.foodtraceability.dto.store.operation.DailySettlementCreateDTO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.User;

import java.util.List;

/**
 * 日结对账服务接口
 * 提供日结对账的查询、确认、异常上报等业务功能
 * 支持基于角色的权限控制，敏感字段仅对总部角色可见
 */
public interface DailySettlementService {

    /**
     * 分页查询日结对账列表（含权限过滤）
     *
     * @param query      查询条件
     * @param currentUser 当前用户信息（用于权限判断）
     * @return 分页结果
     */
    IPage<DailySettlementVO> getSettlementList(SettlementQueryDTO query, User currentUser);

    /**
     * 获取日结对账详情（含权限过滤和班次明细）
     *
     * @param settlementId 结算ID
     * @param currentUser  当前用户信息
     * @return 完整的日结对账视图对象（含班次列表）
     */
    DailySettlementVO getSettlementWithPermission(String settlementId, User currentUser);

    /**
     * 根据结算ID获取班次明细列表
     *
     * @param settlementId 结算ID
     * @return 班次明细视图对象列表
     */
    List<DailySettlementShiftVO> getShiftsBySettlementId(String settlementId);

    /**
     * 创建日结草稿
     *
     * @param dto        创建DTO
     * @param operatorId 操作人ID
     * @return 创建的日结视图对象
     */
    DailySettlementVO createSettlement(DailySettlementCreateDTO dto, String operatorId);

    /**
     * 确认对账
     * 将结算状态更新为approved
     *
     * @param settlementId 结算ID
     * @param operatorId   操作人ID
     */
    void confirmSettlement(String settlementId, String operatorId);

    /**
     * 上报异常问题
     * 将结算状态更新为abnormal，记录异常原因，并通知区域经理
     *
     * @param settlementId 结算ID
     * @param issueDto     异常上报DTO
     * @param operatorId   操作人ID
     */
    void reportIssue(String settlementId, Object issueDto, String operatorId);

    /**
     * 获取指定班次的订单明细列表
     *
     * @param shiftId 班次明细ID
     * @return 订单明细视图对象列表
     */
    List<Object> getOrdersByShiftId(String shiftId);
}
