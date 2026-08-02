package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.DailySettlementDataService;
import com.foodtraceability.dto.store.operation.vo.DailySettlementShiftVO;
import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.DailySettlement;
import com.foodtraceability.entity.DailySettlementShift;
import com.foodtraceability.mapper.DailySettlementMapper;
import com.foodtraceability.mapper.DailySettlementShiftMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日结对账数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 支持基于角色的权限过滤，敏感字段（totalCost/netProfit/grossProfitRate）仅对总部角色可见
 * 缓存键格式: daily_settlement:basic:{settlementId}
 */
@Service
public class DailySettlementDataServiceImpl implements DailySettlementDataService {

    private static final Logger log = LoggerFactory.getLogger(DailySettlementDataServiceImpl.class);

    /** 缓存名称 */
    private static final String CACHE_NAME = "dailySettlement";
    /** 总部角色标识 - 可查看所有敏感字段 */
    private static final String ROLE_FINANCE_DIRECTOR = "finance_director";

    private final DailySettlementMapper dailySettlementMapper;
    private final DailySettlementShiftMapper dailySettlementShiftMapper;

    /**
     * 构造函数注入
     *
     * @param dailySettlementMapper     日结主表Mapper
     * @param dailySettlementShiftMapper 班次明细Mapper
     */
    public DailySettlementDataServiceImpl(DailySettlementMapper dailySettlementMapper,
                                          DailySettlementShiftMapper dailySettlementShiftMapper) {
        this.dailySettlementMapper = dailySettlementMapper;
        this.dailySettlementShiftMapper = dailySettlementShiftMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#settlementIds + ':' + #role",
            unless = "#result == null || #result.isEmpty()")
    public Map<String, DailySettlementVO> batchGetSettlementBasicInfo(List<String> settlementIds, String role) {
        if (settlementIds == null || settlementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DailySettlement> settlements = dailySettlementMapper.selectBatchIds(settlementIds);
        boolean isFinanceDirector = ROLE_FINANCE_DIRECTOR.equals(role);

        return settlements.stream()
                .filter(s -> s != null)
                .collect(Collectors.toMap(
                        DailySettlement::getSettlementId,
                        s -> convertToVOWithPermission(s, isFinanceDirector),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #settlementId + ':' + #role",
            unless = "#result == null")
    public DailySettlementVO getSettlementBasicInfo(String settlementId, String role) {
        if (settlementId == null || settlementId.isEmpty()) {
            return null;
        }

        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            return null;
        }

        boolean isFinanceDirector = ROLE_FINANCE_DIRECTOR.equals(role);
        return convertToVOWithPermission(settlement, isFinanceDirector);
    }

    @Override
    public List<DailySettlementShiftVO> getShiftsBySettlementId(String settlementId) {
        if (settlementId == null || settlementId.isEmpty()) {
            return Collections.emptyList();
        }

        // 根据settlementId查询班次明细
        Map<String, Object> columnMap = new HashMap<>();
        columnMap.put("settlement_id", settlementId);
        List<DailySettlementShift> shifts = dailySettlementShiftMapper.selectByMap(columnMap);

        return shifts.stream()
                .filter(shift -> shift != null)
                .map(this::convertShiftToVO)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearSettlementCache(String settlementId) {
        log.debug("清除日结对账缓存: settlementId={}", settlementId);
    }

    /**
     * 将日结实体转换为视图对象（含权限过滤）
     *
     * @param settlement          日结实体
     * @param showSensitiveFields 是否展示敏感字段
     * @return 视图对象
     */
    private DailySettlementVO convertToVOWithPermission(DailySettlement settlement, boolean showSensitiveFields) {
        if (settlement == null) {
            return null;
        }

        DailySettlementVO vo = new DailySettlementVO();

        // 基础字段（所有角色可见）
        vo.setSettlementId(settlement.getSettlementId());
        vo.setStoreId(settlement.getStoreId());
        vo.setSettlementDate(settlement.getSettlementDate());
        vo.setTotalRevenue(convertFenToYuan(settlement.getTotalRevenue()));
        vo.setOrderCount(settlement.getOrderCount());
        vo.setAvgOrderValue(convertFenToYuan(settlement.getAvgOrderValue()));
        vo.setTableUsageRate(settlement.getTableUsageRate());
        vo.setDifferenceAmount(convertFenToYuan(settlement.getDifferenceAmount()));
        vo.setStatus(settlement.getStatus());
        vo.setRemark(settlement.getRemark());
        vo.setAuditorId(settlement.getAuditorId());
        vo.setAuditorName(settlement.getAuditorName());
        vo.setCreateTime(settlement.getCreateTime());

        // 状态名称
        vo.setStatusName(getStatusName(settlement.getStatus()));

        // 敏感字段（根据角色权限控制）
        if (showSensitiveFields) {
            vo.setTotalCost(convertFenToYuan(settlement.getTotalCost()));
            vo.setNetProfit(convertFenToYuan(settlement.getNetProfit()));
            vo.setGrossProfitRate(settlement.getGrossProfitRate());
        } else {
            // 非总部角色时，敏感字段置空
            vo.setTotalCost(null);
            vo.setNetProfit(null);
            vo.setGrossProfitRate(null);
        }

        return vo;
    }

    /**
     * 将班次实体转换为视图对象
     *
     * @param shift 班次实体
     * @return 班次视图对象
     */
    private DailySettlementShiftVO convertShiftToVO(DailySettlementShift shift) {
        if (shift == null) {
            return null;
        }

        DailySettlementShiftVO vo = new DailySettlementShiftVO();
        vo.setShiftId(shift.getShiftId());
        vo.setSettlementId(shift.getSettlementId());
        vo.setShiftType(shift.getShiftType());
        vo.setStartTime(shift.getStartTime());
        vo.setEndTime(shift.getEndTime());
        vo.setRevenue(convertFenToYuan(shift.getRevenue()));
        vo.setOrderCount(shift.getOrderCount());
        vo.setCashierId(shift.getCashierId());
        vo.setCashierName(shift.getCashierName());

        // 班次类型名称
        vo.setShiftTypeName(getShiftTypeName(shift.getShiftType()));

        return vo;
    }

    /** 分转元（数据库存储单位为分） */
    private String convertFenToYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return String.valueOf(fen / 100.0);
    }

    /** 获取结算状态名称 */
    private String getStatusName(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "draft": return "草稿";
            case "pending": return "待审核";
            case "approved": return "已确认";
            case "rejected": return "已驳回";
            case "abnormal": return "异常";
            default: return status;
        }
    }

    /** 获取班次类型名称 */
    private String getShiftTypeName(String shiftType) {
        if (shiftType == null) return "未知";
        switch (shiftType) {
            case "morning": return "早班";
            case "evening": return "晚班";
            case "overnight": return "夜班";
            default: return shiftType;
        }
    }

    @Override
    public List<Object> getOrdersByShiftId(String shiftId) {
        log.debug("查询班次订单明细: shiftId={}", shiftId);

        if (shiftId == null || shiftId.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: 实现从订单表查询该班次的订单明细
        // 预期返回格式：List<Map<String, Object>>
        // 包含字段：orderId, amount(元), paymentMethod, orderTime, dishCount等

        // 临时返回空列表，待订单模块集成后完善
        return Collections.emptyList();
    }
}
