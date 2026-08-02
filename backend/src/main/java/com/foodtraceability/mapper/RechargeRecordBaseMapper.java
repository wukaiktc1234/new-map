package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RechargeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 充值记录数据访问层
 */
@Mapper
public interface RechargeRecordBaseMapper extends BaseMapper<RechargeRecord> {

    /**
     * 查询会员充值记录
     */
    @Select("SELECT r.*, m.phone as member_phone, m.nickname as member_nickname, p.plan_name " +
            "FROM recharge_record r " +
            "LEFT JOIN members m ON r.member_id = m.member_id " +
            "LEFT JOIN recharge_plan p ON r.plan_id = p.plan_id " +
            "WHERE r.member_id = #{memberId} AND r.deleted = 0 " +
            "ORDER BY r.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<RechargeRecord> selectByMemberId(@Param("memberId") Long memberId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 统计指定时间范围内充值总额
     */
    @Select("SELECT COALESCE(SUM(recharge_amount), 0) FROM recharge_record " +
            "WHERE payment_status = 1 AND deleted = 0 " +
            "AND create_time >= #{startDate} AND create_time < #{endDate}")
    long sumRechargeAmount(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 统计今日充值金额
     */
    @Select("SELECT COALESCE(SUM(recharge_amount), 0) FROM recharge_record " +
            "WHERE payment_status = 1 AND deleted = 0 AND DATE(create_time) = CURRENT_DATE")
    long sumTodayRecharge();

    /**
     * 按日期统计充值趋势
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count, SUM(recharge_amount) as amount " +
            "FROM recharge_record WHERE payment_status = 1 AND deleted = 0 " +
            "AND create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectDailyRechargeStats(@Param("startDate") String startDate,
                                                         @Param("endDate") String endDate);
}
