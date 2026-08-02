package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InvitationReminderLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码提醒记录Mapper接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Mapper
public interface InvitationReminderLogMapper extends BaseMapper<InvitationReminderLog> {

    /**
     * 根据邀请码ID查询提醒记录
     *
     * @param invitationCodeId 邀请码ID
     * @return 提醒记录列表
     */
    @Select("SELECT * FROM invitation_reminder_log WHERE invitation_code_id = #{invitationCodeId} ORDER BY create_time DESC")
    List<InvitationReminderLog> selectByInvitationCodeId(@Param("invitationCodeId") Long invitationCodeId);

    /**
     * 根据档案ID查询提醒记录
     *
     * @param archiveId 档案ID
     * @return 提醒记录列表
     */
    @Select("SELECT * FROM invitation_reminder_log WHERE archive_id = #{archiveId} ORDER BY create_time DESC")
    List<InvitationReminderLog> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据接收人ID查询提醒记录
     *
     * @param recipientId 接收人ID
     * @return 提醒记录列表
     */
    @Select("SELECT * FROM invitation_reminder_log WHERE recipient_id = #{recipientId} ORDER BY create_time DESC")
    List<InvitationReminderLog> selectByRecipientId(@Param("recipientId") Long recipientId);

    /**
     * 查询待发送的提醒记录
     *
     * @return 提醒记录列表
     */
    @Select("SELECT * FROM invitation_reminder_log WHERE send_status = 'PENDING' AND create_time <= NOW() ORDER BY create_time ASC")
    List<InvitationReminderLog> selectPendingSend();

    /**
     * 查询未读的提醒记录
     *
     * @param recipientId 接收人ID
     * @return 提醒记录列表
     */
    @Select("SELECT * FROM invitation_reminder_log WHERE recipient_id = #{recipientId} AND read_status = 'UNREAD' ORDER BY create_time DESC")
    List<InvitationReminderLog> selectUnreadByRecipientId(@Param("recipientId") Long recipientId);

    /**
     * 标记提醒为已读
     *
     * @param id       记录ID
     * @param readTime 阅读时间
     * @return 更新行数
     */
    @Update("UPDATE invitation_reminder_log SET read_status = 'READ', read_time = #{readTime} WHERE id = #{id}")
    int markAsRead(@Param("id") Long id, @Param("readTime") LocalDateTime readTime);

    /**
     * 标记提醒为已发送
     *
     * @param id       记录ID
     * @param sendTime 发送时间
     * @return 更新行数
     */
    @Update("UPDATE invitation_reminder_log SET send_status = 'SENT', send_time = #{sendTime} WHERE id = #{id}")
    int markAsSent(@Param("id") Long id, @Param("sendTime") LocalDateTime sendTime);
}
