package com.foodtraceability.dataservice;

import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;

import java.util.List;
import java.util.Map;

/**
 * 日结对账数据服务接口
 * 提供日结对账的二级缓存(L1本地+L2 Redis)和批量查询功能
 * 支持基于角色的权限过滤，敏感字段仅对总部角色可见
 * 缓存键格式: daily_settlement:basic:{settlementId}
 */
public interface DailySettlementDataService {

    /**
     * 批量获取日结对账基本信息（含权限过滤）
     *
     * @param settlementIds 结算ID列表
     * @param role          用户角色（finance_director/regional_manager/store_manager）
     *                       非finance_director角色时，敏感字段将被置空
     * @return 结算ID到视图对象的映射
     */
    Map<String, DailySettlementVO> batchGetSettlementBasicInfo(List<String> settlementIds, String role);

    /**
     * 获取单个日结对账基本信息（含权限过滤）
     *
     * @param settlementId 结算ID
     * @param role         用户角色
     * @return 日结对账视图对象（根据角色过滤敏感字段）
     */
    DailySettlementVO getSettlementBasicInfo(String settlementId, String role);

    /**
     * 根据结算ID获取班次明细列表
     *
     * @param settlementId 结算ID
     * @return 班次明细视图对象列表
     */
    List<DailySettlementShiftVO> getShiftsBySettlementId(String settlementId);

    /**
     * 获取指定班次的订单明细列表
     *
     * @param shiftId 班次明细ID
     * @return 订单明细对象列表（订单ID、金额、支付方式、时间等）
     */
    List<Object> getOrdersByShiftId(String shiftId);

    /**
     * 清除指定结算记录的缓存
     *
     * @param settlementId 结算ID
     */
    void clearSettlementCache(String settlementId);
}
