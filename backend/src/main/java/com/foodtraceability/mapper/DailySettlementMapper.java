package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.SettlementQueryDTO;
import com.foodtraceability.entity.DailySettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日结对账主表Mapper接口
 * 对应数据库表 daily_settlements
 * 用于门店每日营业数据的汇总对账
 */
@Mapper
public interface DailySettlementMapper extends BaseMapper<DailySettlement> {

    /**
     * 分页查询日结记录
     *
     * @param page  分页参数
     * @param query 查询条件（支持门店ID、日期范围、状态等条件筛选）
     * @return 分页结果
     */
    IPage<DailySettlement> selectSettlementPage(IPage<DailySettlement> page, @Param("query") SettlementQueryDTO query);

    /**
     * 根据门店ID和结算日期查询日结记录
     * 用于检查某门店某天是否已生成结算单（防止重复生成）
     *
     * @param storeId       门店ID
     * @param settlementDate 结算日期
     * @return 日结记录（如果存在）
     */
    DailySettlement selectByStoreAndDate(@Param("storeId") String storeId, @Param("settlementDate") LocalDate settlementDate);

    /**
     * 按日期范围查询日结记录列表
     * 用于定时任务和导出功能
     *
     * @param storeId   门店ID
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 日结记录列表
     */
    List<DailySettlement> listByDateRange(@Param("storeId") String storeId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
