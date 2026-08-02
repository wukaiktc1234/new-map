package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InvitationSendRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码发送记录Mapper接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Mapper
public interface InvitationSendRecordMapper extends BaseMapper<InvitationSendRecord> {

    /**
     * 根据邀请码查询记录
     *
     * @param invitationCode 邀请码
     * @return 邀请码记录
     */
    @Select("SELECT * FROM invitation_send_record WHERE invitation_code = #{invitationCode}")
    InvitationSendRecord selectByCode(@Param("invitationCode") String invitationCode);

    /**
     * 根据档案ID查询邀请码记录
     *
     * @param archiveId 档案ID
     * @return 邀请码记录
     */
    @Select("SELECT * FROM invitation_send_record WHERE archive_id = #{archiveId}")
    InvitationSendRecord selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 查询即将过期的邀请码（24小时内）
     *
     * @return 邀请码列表
     */
    @Select("SELECT * FROM invitation_send_record WHERE status = 'UNUSED' AND expire_time <= NOW() + INTERVAL '24 hours' AND expire_time > NOW()")
    List<InvitationSendRecord> selectExpiringIn24Hours();

    /**
     * 查询已过期的邀请码
     *
     * @return 邀请码列表
     */
    @Select("SELECT * FROM invitation_send_record WHERE status = 'UNUSED' AND expire_time <= NOW()")
    List<InvitationSendRecord> selectExpired();

    /**
     * 标记邀请码为已使用
     *
     * @param id 记录ID
     * @param usedBy 使用人ID
     * @param usedTime 使用时间
     * @return 更新行数
     */
    @Update("UPDATE invitation_send_record SET status = 'USED', use_count = use_count + 1, used_by = #{usedBy}, used_time = #{usedTime} WHERE id = #{id}")
    int markAsUsed(@Param("id") Long id, @Param("usedBy") Long usedBy, @Param("usedTime") LocalDateTime usedTime);

    /**
     * 标记邀请码为已过期
     *
     * @param id 记录ID
     * @return 更新行数
     */
    @Update("UPDATE invitation_send_record SET status = 'EXPIRED' WHERE id = #{id} AND status = 'UNUSED'")
    int markAsExpired(@Param("id") Long id);
}
