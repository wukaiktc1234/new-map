package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.schedule.NotificationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知记录Mapper接口
 */
@Mapper
public interface NotificationLogMapper extends BaseMapper<NotificationLog> {

    /**
     * 查询业务的通知记录
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 通知记录列表
     */
    List<NotificationLog> selectByBusiness(
            String businessType,
            String businessId);

    /**
     * 查询发送失败的通知(用于重试)
     * @param maxRetryCount 最大重试次数
     * @return 失败且未超重试次数的通知列表
     */
    List<NotificationLog> selectFailedForRetry(Integer maxRetryCount);

    /**
     * 按发送状态统计通知数量
     * @param businessType 业务类型(可选)
     * @return 各状态数量列表
     */
    List<NotificationLog> countBySendStatus(@Param("businessType") String businessType);

    /**
     * 查询指定时间范围的通知记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 通知记录列表
     */
    List<NotificationLog> selectByTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * 更新通知发送状态
     * @param logId 通知记录ID
     * @param sendStatus 发送状态
     * @param sendTime 发送时间
     * @param errorMessage 错误信息(可选)
     * @return 影响行数
     */
    int updateSendStatus(
            String logId,
            String sendStatus,
            LocalDateTime sendTime,
            String errorMessage);

    /**
     * 增加重试次数
     * @param logId 通知记录ID
     * @return 影响行数
     */
    int incrementRetryCount(String logId);

    /**
     * 分页查询通知日志（支持按渠道、业务类型、日期范围筛选）
     * @param page 分页对象
     * @param channel 渠道筛选（可选）
     * @param businessType 业务类型筛选（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    IPage<NotificationLog> selectNotificationPage(Page<NotificationLog> page,
                                                   @Param("channel") String channel,
                                                   @Param("businessType") String businessType,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);

    /**
     * 统计通知发送情况
     * @return 统计数据（total/success/failed/pending）
     */
    Map<String, Object> selectStats();
}
