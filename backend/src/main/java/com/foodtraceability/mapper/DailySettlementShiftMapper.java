package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DailySettlementShift;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 班次明细子表Mapper接口
 * 对应数据库表 daily_settlement_shifts
 * 用于记录日结对账中各班次的详细营业数据
 */
@Mapper
public interface DailySettlementShiftMapper extends BaseMapper<DailySettlementShift> {

    /**
     * 根据结算主表ID查询关联的班次列表
     * 用于展示某个日结单包含的所有班次明细
     *
     * @param settlementId 日结主表ID
     * @return 班次列表（按班次类型排序：早班->晚班->夜班）
     */
    List<DailySettlementShift> selectBySettlementId(String settlementId);
}
