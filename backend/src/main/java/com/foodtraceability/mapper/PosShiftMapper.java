package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PosShift;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * POS班次Mapper接口
 * 提供班次相关的数据库操作方法
 */
@Repository
public interface PosShiftMapper extends BaseMapper<PosShift> {

    /**
     * 根据终端ID查询当前活跃班次
     */
    @Select("SELECT * FROM pos_shifts WHERE terminal_id = #{terminalId} AND status = 'active' ORDER BY start_time DESC LIMIT 1")
    PosShift findActiveShiftByTerminalId(@Param("terminalId") String terminalId);

    /**
     * 查询终端今日所有班次
     */
    @Select("SELECT * FROM pos_shifts WHERE terminal_id = #{terminalId} AND DATE(start_time) = CURRENT_DATE ORDER BY start_time DESC")
    List<PosShift> findTodayShiftsByTerminalId(@Param("terminalId") String terminalId);

    /**
     * 统计指定时间范围内的订单数和总金额
     */
    @Select("SELECT COUNT(*) as total_orders, COALESCE(SUM(total_amount), 0.00) as total_amount " +
            "FROM pos_orders " +
            "WHERE terminal_id = #{terminalId} " +
            "AND order_time >= #{startTime} " +
            "AND order_time <= #{endTime} " +
            "AND status IN ('completed', 'paid')")
    Map<String, Object> countOrdersByTimeRange(
            @Param("terminalId") String terminalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
