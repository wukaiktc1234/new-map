package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.NotificationLogQueryDTO;
import com.foodtraceability.dto.schedule.NotificationLogVO;
import com.foodtraceability.dto.schedule.NotificationStatsVO;

import java.util.List;

/**
 * 排班通知管理服务接口
 * 提供通知日志查询、统计、重试发送、主动发送等功能
 *
 * <p>通知渠道：in_app(站内信)、sms(短信)、email(邮件)、wechat_work(企微)
 * <p>发送状态：pending(待发送)、sending(发送中)、success(成功)、failed(失败)
 */
public interface ScheduleNotificationService {

    /**
     * 分页查询通知日志
     * @param queryDTO 查询条件（含分页、渠道、业务类型、日期范围筛选）
     * @return 分页结果
     */
    PageResult<NotificationLogVO> getNotificationList(NotificationLogQueryDTO queryDTO);

    /**
     * 获取通知发送统计
     * @return 统计数据（含总数、成功/失败/待发送数、成功率）
     */
    NotificationStatsVO getNotificationStats();

    /**
     * 重试发送失败的通知
     * 业务规则：仅 send_status=failed 的通知可重试，重试次数累加
     * @param logId 通知日志ID
     */
    void retrySend(Long logId);

    /**
     * 发送通知
     * 业务逻辑：创建通知日志记录并执行发送
     * @param businessType 业务类型（如 schedule_published/swap_approved 等）
     * @param businessId 业务ID（方案ID/换班申请ID等）
     * @param title 通知标题
     * @param content 通知内容（支持模板变量）
     * @param receiverIds 接收人用户ID列表
     * @param channel 发送渠道（in_app/sms/email/wechat_work）
     * @return 创建的通知日志
     */
    NotificationLogVO sendNotification(String businessType, String businessId,
                                       String title, String content,
                                       List<String> receiverIds, String channel);
}
