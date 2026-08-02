package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MemberPointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 积分变动日志数据访问层
 */
@Mapper
public interface MemberPointsLogMapper extends BaseMapper<MemberPointsLog> {

    /**
     * 查询会员积分变动明细
     */
    @Select("SELECT l.*, m.phone as member_phone, m.nickname as member_nickname " +
            "FROM member_points_log l " +
            "LEFT JOIN members m ON l.member_id = m.member_id " +
            "WHERE l.member_id = #{memberId} " +
            "ORDER BY l.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<MemberPointsLog> selectByMemberId(@Param("memberId") Long memberId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 统计会员积分变动记录总数
     */
    @Select("SELECT COUNT(*) FROM member_points_log WHERE member_id = #{memberId}")
    long countByMemberId(@Param("memberId") Long memberId);

    /**
     * 统计指定时间范围内积分发放总量
     */
    @Select("SELECT COALESCE(SUM(change_points), 0) FROM member_points_log " +
            "WHERE change_type IN (1, 3, 4, 7, 8) AND change_points > 0 " +
            "AND create_time >= #{startDate} AND create_time < #{endDate}")
    long sumPointsIssued(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 统计指定时间范围内积分消耗总量（取绝对值）
     */
    @Select("SELECT COALESCE(SUM(ABS(change_points)), 0) FROM member_points_log " +
            "WHERE change_type IN (2, 6) AND change_points < 0 " +
            "AND create_time >= #{startDate} AND create_time < #{endDate}")
    long sumPointsConsumed(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 查询即将过期的积分记录
     */
    @Select("SELECT * FROM member_points_log " +
            "WHERE expire_time IS NOT NULL AND expire_time >= NOW() AND expire_time <= #{targetDate} " +
            "AND change_points > 0 AND (member_id, log_id) NOT IN (" +
            "  SELECT member_id, log_id FROM member_points_log WHERE change_type = 6" +
            ") ORDER BY expire_time ASC")
    List<MemberPointsLog> selectExpiringPoints(@Param("targetDate") String targetDate);
}
