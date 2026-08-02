package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.NotificationLogQueryDTO;
import com.foodtraceability.dto.schedule.NotificationLogVO;
import com.foodtraceability.dto.schedule.NotificationStatsVO;
import com.foodtraceability.entity.schedule.NotificationLog;
import com.foodtraceability.mapper.schedule.NotificationLogMapper;
import com.foodtraceability.service.schedule.ScheduleNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排班通知管理服务实现类
 * 实现通知日志查询、统计、重试发送、主动发送等业务逻辑
 *
 * <p>通知渠道：in_app(站内信)、sms(短信)、email(邮件)、wechat_work(企微)
 * <p>注意：当前为框架实现，实际的渠道发送逻辑后续完善
 */
@Service
public class ScheduleNotificationServiceImpl implements ScheduleNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleNotificationServiceImpl.class);

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    private final NotificationLogMapper notificationLogMapper;

    public ScheduleNotificationServiceImpl(NotificationLogMapper notificationLogMapper) {
        this.notificationLogMapper = notificationLogMapper;
    }

    @Override
    public PageResult<NotificationLogVO> getNotificationList(NotificationLogQueryDTO queryDTO) {
        Page<NotificationLog> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );

        IPage<NotificationLog> resultPage = notificationLogMapper.selectNotificationPage(
                page,
                queryDTO.getChannel(),
                queryDTO.getBusinessType(),
                queryDTO.getStartDate(),
                queryDTO.getEndDate());

        List<NotificationLogVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(
                resultPage.getTotal(),
                voList,
                resultPage.getCurrent(),
                resultPage.getSize()
        );
    }

    @Override
    public NotificationStatsVO getNotificationStats() {
        Map<String, Object> stats = notificationLogMapper.selectStats();
        NotificationStatsVO vo = new NotificationStatsVO();
        vo.setTotal(toInt(stats.get("total")));
        vo.setSuccess(toInt(stats.get("success")));
        vo.setFailed(toInt(stats.get("failed")));
        vo.setPending(toInt(stats.get("pending")));
        vo.setSending(toInt(stats.get("sending")));

        // 计算成功率
        int total = vo.getTotal();
        if (total > 0) {
            double rate = (vo.getSuccess() * 100.0) / total;
            vo.setSuccessRate(Math.round(rate * 100.0) / 100.0);
        } else {
            vo.setSuccessRate(0.0);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retrySend(Long logId) {
        NotificationLog entity = notificationLogMapper.selectById(logId);
        if (entity == null) {
            throw new RuntimeException("通知日志不存在：" + logId);
        }

        // 校验：仅 failed 状态可重试
        if (!NotificationLog.SEND_STATUS_FAILED.equals(entity.getSendStatus())) {
            throw new RuntimeException("仅失败状态的通知可重试，当前状态：" + entity.getSendStatus());
        }

        // 校验重试次数
        int currentRetry = entity.getRetryCount() != null ? entity.getRetryCount() : 0;
        if (currentRetry >= MAX_RETRY_COUNT) {
            throw new RuntimeException("已达到最大重试次数：" + MAX_RETRY_COUNT);
        }

        // TODO: 实现实际的渠道重试发送逻辑
        // 当前为框架实现，直接标记为成功
        entity.setSendStatus(NotificationLog.SEND_STATUS_SUCCESS);
        entity.setSendTime(LocalDateTime.now());
        entity.setRetryCount(currentRetry + 1);
        entity.setErrorMessage(null);
        entity.setUpdateTime(LocalDateTime.now());

        notificationLogMapper.updateById(entity);
        log.info("重试发送通知: logId={}, retryCount={}", logId, entity.getRetryCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationLogVO sendNotification(String businessType, String businessId,
                                               String title, String content,
                                               List<String> receiverIds, String channel) {
        NotificationLog entity = new NotificationLog();
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setTitle(title);
        entity.setContent(content);

        // 接收人ID列表转 JSON 字符串
        entity.setReceiverIds(toJsonArray(receiverIds));
        entity.setReceiverNames(null);

        // 渠道默认为站内信
        entity.setChannel(channel != null ? channel : NotificationLog.CHANNEL_IN_APP);
        entity.setSendStatus(NotificationLog.SEND_STATUS_PENDING);
        entity.setRetryCount(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(0);

        notificationLogMapper.insert(entity);

        // TODO: 实现实际的渠道发送逻辑（异步发送到对应渠道）
        // 当前为框架实现，直接标记为成功
        entity.setSendStatus(NotificationLog.SEND_STATUS_SUCCESS);
        entity.setSendTime(LocalDateTime.now());
        notificationLogMapper.updateById(entity);

        log.info("发送通知: logId={}, businessType={}, businessId={}, receiverCount={}",
                entity.getLogId(), businessType, businessId, receiverIds != null ? receiverIds.size() : 0);

        return convertToVO(entity);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * Entity → VO 转换
     * @param entity 通知日志实体
     * @return 视图对象
     */
    private NotificationLogVO convertToVO(NotificationLog entity) {
        if (entity == null) {
            return null;
        }

        NotificationLogVO vo = new NotificationLogVO();
        vo.setLogId(entity.getLogId() != null ? String.valueOf(entity.getLogId()) : null);
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());

        // 解析接收人ID列表
        vo.setReceiverIds(parseJsonArray(entity.getReceiverIds()));
        vo.setReceiverNames(parseJsonArray(entity.getReceiverNames()));

        vo.setChannel(entity.getChannel());
        vo.setSendStatus(entity.getSendStatus());
        if (entity.getSendTime() != null) {
            vo.setSendTime(entity.getSendTime().format(DATETIME_FORMATTER));
        }
        vo.setRetryCount(entity.getRetryCount());
        vo.setErrorMessage(entity.getErrorMessage());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DATETIME_FORMATTER));
        }

        return vo;
    }

    /**
     * 列表转 JSON 数组字符串
     * 简单实现：[id1,id2,id3]
     * @param list 字符串列表
     * @return JSON 数组字符串
     */
    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * 解析 JSON 数组字符串为列表
     * 简单实现：去除方括号和引号后按逗号分割
     * @param json JSON 数组字符串
     * @return 字符串列表
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        // 去除首尾方括号
        String content = json;
        if (content.startsWith("[")) {
            content = content.substring(1);
        }
        if (content.endsWith("]")) {
            content = content.substring(0, content.length() - 1);
        }
        if (content.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(content.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 安全转换 Map 值为 int
     * @param value Map 值
     * @return int 值
     */
    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
