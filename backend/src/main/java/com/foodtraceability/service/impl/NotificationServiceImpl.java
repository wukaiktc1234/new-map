package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.MsgSendDTO;
import com.foodtraceability.dto.MsgTemplateCreateDTO;
import com.foodtraceability.dto.NotificationSettingUpdateDTO;
import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.NotificationSettingEntity;
import com.foodtraceability.mapper.MsgSendRecordMapper;
import com.foodtraceability.mapper.MsgTemplateMapper;
import com.foodtraceability.mapper.NotificationSettingMapper;
import com.foodtraceability.service.EmailService;
import com.foodtraceability.service.NotificationService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现类
 * 实现模板管理、消息发送、设置管理、统计仪表盘等功能
 * 核心功能: 模板渲染引擎(${var}语法)、同步发送消息
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<MsgTemplateMapper, MsgTemplate>
        implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);


    public NotificationServiceImpl(MsgTemplateMapper msgTemplateMapper, MsgSendRecordMapper msgSendRecordMapper, NotificationSettingMapper notificationSettingMapper, @Nullable EmailService emailService) {
        this.msgTemplateMapper = msgTemplateMapper;
        this.msgSendRecordMapper = msgSendRecordMapper;
        this.notificationSettingMapper = notificationSettingMapper;
        this.emailService = emailService;
    }

    /** 模板变量正则表达式: ${varName} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /** 默认最大重试次数 */
    private static final int DEFAULT_MAX_RETRY = 3;

    private final MsgTemplateMapper msgTemplateMapper;

    private final MsgSendRecordMapper msgSendRecordMapper;

    private final NotificationSettingMapper notificationSettingMapper;

    /** 邮件服务（可选） */
    @Nullable
    private final EmailService emailService;

    // ==================== 模板管理实现 ====================

    @Override
    public IPage<MsgTemplate> getTemplatePage(Page<MsgTemplate> page, String templateCode,
                                              String templateName, Integer templateType,
                                              Integer status, String channel) {
        return msgTemplateMapper.selectTemplatePage(page, templateCode, templateName,
                templateType, status, channel);
    }

    @Override
    public MsgTemplate getTemplateById(Long templateId) {
        return msgTemplateMapper.selectById(templateId);
    }

    @Override
    public MsgTemplate getTemplateByCode(String templateCode) {
        LambdaQueryWrapper<MsgTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgTemplate::getTemplateCode, templateCode);
        return msgTemplateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgTemplate createTemplate(MsgTemplateCreateDTO dto, Long userId, String username) {
        // 检查编码唯一性
        LambdaQueryWrapper<MsgTemplate> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(MsgTemplate::getTemplateCode, dto.getTemplateCode());
        if (msgTemplateMapper.selectCount(existWrapper) > 0) {
            throw new IllegalArgumentException("模板编码已存在: " + dto.getTemplateCode());
        }

        MsgTemplate template = new MsgTemplate();
        template.setTemplateCode(dto.getTemplateCode());
        template.setTemplateName(dto.getTemplateName());
        template.setSubjectPattern(dto.getSubjectPattern());
        template.setContentPattern(dto.getContentPattern());
        template.setTemplateType(dto.getTemplateType());
        // 如果未指定渠道，根据模板类型设置默认渠道
        if (dto.getChannel() == null || dto.getChannel().isEmpty()) {
            template.setChannel(getDefaultChannelByType(dto.getTemplateType()));
        } else {
            template.setChannel(dto.getChannel());
        }
        template.setStatus(dto.getStatus());
        template.setVariables(dto.getVariables());
        template.setExampleData(dto.getExampleData());
        template.setCreateUserId(userId);
        template.setCreateUsername(username);
        template.setUpdateTime(LocalDateTime.now());

        msgTemplateMapper.insert(template);
        logger.info("创建消息模板成功: code={}, name={}", dto.getTemplateCode(), dto.getTemplateName());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(Long templateId, MsgTemplateCreateDTO dto, Long userId, String username) {
        MsgTemplate existing = getTemplateById(templateId);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        // 检查编码唯一性（排除自身）
        if (!existing.getTemplateCode().equals(dto.getTemplateCode())) {
            LambdaQueryWrapper<MsgTemplate> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(MsgTemplate::getTemplateCode, dto.getTemplateCode())
                    .ne(MsgTemplate::getTemplateId, templateId);
            if (msgTemplateMapper.selectCount(existWrapper) > 0) {
                throw new IllegalArgumentException("模板编码已存在: " + dto.getTemplateCode());
            }
        }

        existing.setTemplateCode(dto.getTemplateCode());
        existing.setTemplateName(dto.getTemplateName());
        existing.setSubjectPattern(dto.getSubjectPattern());
        existing.setContentPattern(dto.getContentPattern());
        existing.setTemplateType(dto.getTemplateType());
        if (dto.getChannel() != null && !dto.getChannel().isEmpty()) {
            existing.setChannel(dto.getChannel());
        }
        existing.setStatus(dto.getStatus());
        existing.setVariables(dto.getVariables());
        existing.setExampleData(dto.getExampleData());
        existing.setUpdateUserId(userId);
        existing.setUpdateUsername(username);
        existing.setUpdateTime(LocalDateTime.now());

        int rows = msgTemplateMapper.updateById(existing);
        logger.info("更新消息模板成功: id={}, code={}", templateId, dto.getTemplateCode());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long templateId) {
        int rows = msgTemplateMapper.deleteById(templateId);
        logger.info("删除消息模板成功: id={}", templateId);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTemplate(Long templateId, Long userId, String username) {
        MsgTemplate template = getTemplateById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }
        template.setStatus(1); // ENABLED
        template.setUpdateUserId(userId);
        template.setUpdateUsername(username);
        template.setUpdateTime(LocalDateTime.now());
        int rows = msgTemplateMapper.updateById(template);
        logger.info("启用消息模板成功: id={}, code={}", templateId, template.getTemplateCode());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTemplate(Long templateId, Long userId, String username) {
        MsgTemplate template = getTemplateById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }
        template.setStatus(0); // DISABLED
        template.setUpdateUserId(userId);
        template.setUpdateUsername(username);
        template.setUpdateTime(LocalDateTime.now());
        int rows = msgTemplateMapper.updateById(template);
        logger.info("禁用消息模板成功: id={}, code={}", templateId, template.getTemplateCode());
        return rows > 0;
    }

    @Override
    public Map<String, String> previewTemplate(String templateCode, Map<String, Object> variables) {
        MsgTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateCode);
        }
        Map<String, String> result = new HashMap<>();
        result.put("subject", renderTemplate(template.getSubjectPattern(), variables));
        result.put("content", renderTemplate(template.getContentPattern(), variables));
        return result;
    }

    // ==================== 消息发送实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(MsgSendDTO dto, Long userId, String username) {
        // 获取并验证模板
        MsgTemplate template = getTemplateByCode(dto.getTemplateCode());
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + dto.getTemplateCode());
        }
        if (template.getStatus() != 1) {
            throw new IllegalStateException("模板未启用: " + dto.getTemplateCode());
        }

        // 渲染模板
        String subject = renderTemplate(template.getSubjectPattern(), dto.getVariables());
        String content = renderTemplate(template.getContentPattern(), dto.getVariables());

        // 创建发送记录
        MsgSendRecord record = buildSendRecord(template, dto.getRecipient(),
                dto.getRecipientType(), subject, content,
                dto.getTriggerType(), dto.getBizType(), dto.getBizId(),
                userId, username);

        msgSendRecordMapper.insert(record);
        logger.info("创建发送记录成功: recordId={}, recipient={}", record.getRecordId(), dto.getRecipient());

        // Phase 1: 同步发送消息（RabbitMQ已移除，直接同步发送）
        sendSynchronously(record);

        return record.getRecordId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> sendBatchMessage(MsgSendDTO dto, Long userId, String username) {
        List<String> recipients = dto.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("批量发送收件人列表不能为空");
        }

        List<Long> recordIds = new ArrayList<>();
        for (String recipient : recipients) {
            try {
                // 为每个收件人创建单独的发送请求
                MsgSendDTO singleDto = new MsgSendDTO();
                singleDto.setTemplateCode(dto.getTemplateCode());
                singleDto.setRecipient(recipient);
                singleDto.setRecipientType(dto.getRecipientType());
                singleDto.setVariables(dto.getVariables());
                singleDto.setTriggerType(dto.getTriggerType());
                singleDto.setBizType(dto.getBizType());
                singleDto.setBizId(dto.getBizId());

                Long recordId = sendMessage(singleDto, userId, username);
                recordIds.add(recordId);
            } catch (Exception e) {
                logger.error("批量发送失败: recipient={}, error={}", recipient, e.getMessage());
            }
        }
        return recordIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryMessage(Long recordId, Long userId, String username) {
        MsgSendRecord record = getRecordById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("发送记录不存在: " + recordId);
        }
        if (record.getSendStatus() != 3) { // FAILED
            throw new IllegalStateException("只能重试失败的记录, 当前状态: " + record.getSendStatus());
        }
        if (record.getRetryCount() >= record.getMaxRetry()) {
            throw new IllegalStateException("已达到最大重试次数: " + record.getMaxRetry());
        }

        // 更新重试信息
        record.setRetryCount(record.getRetryCount() + 1);
        record.setSendStatus(1); // SENDING
        record.setErrorMessage(null);
        record.setSendTime(LocalDateTime.now());
        msgSendRecordMapper.updateById(record);

        // 同步重新发送消息（RabbitMQ已移除）
        sendSynchronously(record);

        logger.info("重试发送消息成功: recordId={}, retryCount={}", recordId, record.getRetryCount());
        return true;
    }

    @Override
    public IPage<MsgSendRecord> getRecordPage(Page<MsgSendRecord> page, String recipient,
                                             String channel, Integer sendStatus,
                                             String bizType, Integer triggerType,
                                             LocalDateTime startTime, LocalDateTime endTime) {
        return msgSendRecordMapper.selectRecordPage(page, recipient, channel, sendStatus,
                bizType, triggerType, startTime, endTime);
    }

    @Override
    public MsgSendRecord getRecordById(Long recordId) {
        return msgSendRecordMapper.selectById(recordId);
    }

    // ==================== 设置管理实现 ====================

    @Override
    public List<NotificationSettingEntity> getSettings(String settingGroup) {
        LambdaQueryWrapper<NotificationSettingEntity> wrapper = new LambdaQueryWrapper<>();
        if (settingGroup != null && !settingGroup.isEmpty()) {
            wrapper.eq(NotificationSettingEntity::getSettingGroup, settingGroup);
        }
        wrapper.orderByAsc(NotificationSettingEntity::getSettingGroup)
               .orderByAsc(NotificationSettingEntity::getSettingKey);
        return notificationSettingMapper.selectList(wrapper);
    }

    @Override
    public String getSettingValue(String settingKey) {
        NotificationSettingEntity setting = notificationSettingMapper.selectOne(
                new LambdaQueryWrapper<NotificationSettingEntity>()
                        .eq(NotificationSettingEntity::getSettingKey, settingKey));
        return setting != null ? setting.getSettingValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSetting(NotificationSettingUpdateDTO dto) {
        // 查询是否已存在
        NotificationSettingEntity existing = notificationSettingMapper.selectOne(
                new LambdaQueryWrapper<NotificationSettingEntity>()
                        .eq(NotificationSettingEntity::getSettingKey, dto.getSettingKey()));

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            // 更新现有设置
            existing.setSettingValue(dto.getSettingValue());
            if (dto.getSettingGroup() != null) {
                existing.setSettingGroup(dto.getSettingGroup());
            }
            if (dto.getDescription() != null) {
                existing.setDescription(dto.getDescription());
            }
            if (dto.getIsEncrypted() != null) {
                existing.setIsEncrypted(dto.getIsEncrypted());
            }
            if (dto.getStatus() != null) {
                existing.setStatus(dto.getStatus());
            }
            existing.setUpdateTime(now);
            return notificationSettingMapper.updateById(existing) > 0;
        } else {
            // 创建新设置
            NotificationSettingEntity setting = new NotificationSettingEntity();
            setting.setSettingKey(dto.getSettingKey());
            setting.setSettingValue(dto.getSettingValue());
            setting.setSettingGroup(dto.getSettingGroup() != null ? dto.getSettingGroup() : "GENERAL");
            setting.setDescription(dto.getDescription());
            setting.setIsEncrypted(dto.getIsEncrypted() != null ? dto.getIsEncrypted() : 0);
            setting.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            setting.setCreateTime(now);
            setting.setUpdateTime(now);
            return notificationSettingMapper.insert(setting) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateSettings(List<NotificationSettingUpdateDTO> settings) {
        for (NotificationSettingUpdateDTO dto : settings) {
            if (!updateSetting(dto)) {
                throw new RuntimeException("批量更新设置失败: " + dto.getSettingKey());
            }
        }
        return true;
    }

    // ==================== 统计仪表盘实现 ====================

    @Override
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        // 状态统计
        stats.put("statusStats", msgSendRecordMapper.countByStatus());

        // 业务类型统计
        stats.put("bizTypeStats", msgSendRecordMapper.countByBizType(startTime, endTime));

        // 趋势数据（最近30天）
        stats.put("trendData", msgSendRecordMapper.countDailyTrend(30));

        // 计算成功率
        long totalSent = countTotalRecords(startTime, endTime);
        long successCount = countSuccessRecords(startTime, endTime);
        double successRate = totalSent > 0 ? (double) successCount / totalSent * 100 : 0;
        stats.put("totalSent", totalSent);
        stats.put("successCount", successCount);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        return stats;
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        dashboard.put("todaySent", countTotalRecords(todayStart, todayEnd));
        dashboard.put("todaySuccess", countSuccessRecords(todayStart, todayEnd));
        dashboard.put("todayFailed", countFailedRecords(todayStart, todayEnd));

        // 本周统计
        LocalDateTime weekStart = todayStart.minusDays(todayStart.getDayOfWeek().getValue() - 1L);
        dashboard.put("weekSent", countTotalRecords(weekStart, todayEnd));
        dashboard.put("weekSuccess", countSuccessRecords(weekStart, todayEnd));

        // 模板统计
        dashboard.put("templateStats", msgTemplateMapper.countByTemplateType());
        dashboard.put("channelStats", msgTemplateMapper.countByChannel());

        // 待处理数量
        dashboard.put("pendingCount", countPendingRecords());

        return dashboard;
    }

    @Override
    public List<String> getChannelList() {
        List<String> channels = new ArrayList<>();
        channels.add("EMAIL");
        channels.add("SITE_MSG");
        channels.add("SMS");
        channels.add("WEBHOOK");
        return channels;
    }

    @Override
    public List<String> getTemplateVariables(String templateCode) {
        MsgTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateCode);
        }
        // 从subject和content中提取变量
        Set<String> variables = extractVariables(template.getSubjectPattern());
        variables.addAll(extractVariables(template.getContentPattern()));
        return new ArrayList<>(variables);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 根据模板类型获取默认渠道
     *
     * @param templateType 模板类型
     * @return 默认渠道
     */
    private String getDefaultChannelByType(Integer templateType) {
        switch (templateType) {
            case 1: return "EMAIL";
            case 2: return "SITE_MSG";
            case 3: return "SMS";
            case 4: return "WEBHOOK";
            default: return "EMAIL";
        }
    }

    /**
     * 渲染模板，替换${var}为实际值
     *
     * @param pattern 模板内容
     * @param variables 变量Map
     * @return 渲染后的字符串
     */
    private String renderTemplate(String pattern, Map<String, Object> variables) {
        if (pattern == null || pattern.isEmpty()) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : "${" + varName + "}";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 从模板中提取所有变量名
     *
     * @param pattern 模板内容
     * @return 变量名集合
     */
    private Set<String> extractVariables(String pattern) {
        Set<String> variables = new LinkedHashSet<>();
        if (pattern == null || pattern.isEmpty()) {
            return variables;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(pattern);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    /**
     * 构建发送记录实体
     */
    private MsgSendRecord buildSendRecord(MsgTemplate template, String recipient,
                                          Integer recipientType, String subject, String content,
                                          Integer triggerType, String bizType, String bizId,
                                          Long userId, String username) {
        MsgSendRecord record = new MsgSendRecord();
        record.setTemplateId(template.getTemplateId());
        record.setTemplateCode(template.getTemplateCode());
        record.setTemplateName(template.getTemplateName());
        record.setRecipient(recipient);
        record.setRecipientType(recipientType != null ? recipientType : 1);
        record.setSubject(subject);
        record.setContent(content);
        record.setChannel(template.getChannel());
        record.setSendStatus(0); // PENDING
        record.setRetryCount(0);
        record.setMaxRetry(DEFAULT_MAX_RETRY);
        record.setSendTime(LocalDateTime.now());
        record.setTriggerType(triggerType != null ? triggerType : 1);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setCreateUserId(userId);
        record.setCreateUsername(username);
        return record;
    }

    /**
     * 同步发送消息（RabbitMQ已移除，统一使用同步发送）
     */
    private void sendSynchronously(MsgSendRecord record) {
        try {
            record.setSendStatus(1); // SENDING
            msgSendRecordMapper.updateById(record);

            String channel = record.getChannel();
            if ("EMAIL".equals(channel) && emailService != null) {
                emailService.sendEmail(record.getRecipient(), record.getSubject(), record.getContent());
                record.setSendStatus(2); // SUCCESS
            } else if ("EMAIL".equals(channel)) {
                logger.warn("EmailService未配置，标记为失败: recordId={}", record.getRecordId());
                record.setSendStatus(3); // FAILED
                record.setErrorMessage("邮件服务未配置");
            } else {
                logger.info("非EMAIL渠道暂不支持同步发送，标记为成功(预留): channel={}, recordId={}", channel, record.getRecordId());
                record.setSendStatus(2); // SUCCESS（预留渠道暂记为成功）
            }

            record.setFinishTime(LocalDateTime.now());
            msgSendRecordMapper.updateById(record);
        } catch (Exception e) {
            logger.error("同步发送异常: recordId={}, error={}", record.getRecordId(), e.getMessage(), e);
            record.setSendStatus(3); // FAILED
            record.setErrorMessage(e.getMessage() != null ? e.getMessage() : "发送异常");
            record.setFinishTime(LocalDateTime.now());
            msgSendRecordMapper.updateById(record);
        }
    }

    /**
     * Phase 1: 模拟发送（作为RabbitMQ不可用时的fallback方案）
     */
    private void simulateSend(MsgSendRecord record) {
        try {
            record.setSendStatus(1); // SENDING
            msgSendRecordMapper.updateById(record);

            // 模拟发送延迟
            Thread.sleep(100);

            // 模拟90%成功率
            if (Math.random() < 0.9) {
                record.setSendStatus(2); // SUCCESS
            } else {
                record.setSendStatus(3); // FAILED
                record.setErrorMessage("模拟发送失败(测试用)");
            }
            record.setFinishTime(LocalDateTime.now());
            msgSendRecordMapper.updateById(record);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record.setSendStatus(3);
            record.setErrorMessage("发送被中断");
            record.setFinishTime(LocalDateTime.now());
            msgSendRecordMapper.updateById(record);
        } catch (Exception e) {
            logger.error("模拟发送异常: recordId={}, error={}", record.getRecordId(), e.getMessage());
            record.setSendStatus(3);
            record.setErrorMessage(e.getMessage());
            record.setFinishTime(LocalDateTime.now());
            msgSendRecordMapper.updateById(record);
        }
    }

    /**
     * 统计总记录数
     */
    private long countTotalRecords(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<MsgSendRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(MsgSendRecord::getCreateTime, start, end);
        return msgSendRecordMapper.selectCount(wrapper);
    }

    /**
     * 统计成功记录数
     */
    private long countSuccessRecords(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<MsgSendRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(MsgSendRecord::getCreateTime, start, end)
               .eq(MsgSendRecord::getSendStatus, 2); // SUCCESS
        return msgSendRecordMapper.selectCount(wrapper);
    }

    /**
     * 统计失败记录数
     */
    private long countFailedRecords(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<MsgSendRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(MsgSendRecord::getCreateTime, start, end)
               .eq(MsgSendRecord::getSendStatus, 3); // FAILED
        return msgSendRecordMapper.selectCount(wrapper);
    }

    /**
     * 统计待处理记录数
     */
    private long countPendingRecords() {
        LambdaQueryWrapper<MsgSendRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MsgSendRecord::getSendStatus, Arrays.asList(0, 1)); // PENDING, SENDING
        return msgSendRecordMapper.selectCount(wrapper);
    }

    @Override
    public void sendAutoApproveNotification(Long userId, Long recordId, String bizType) {
        try {
            logger.info("[业务通知] 发送自动审核通过通知: userId={}, recordId={}, bizType={}", userId, recordId, bizType);
            // TODO: 实现自动审核通过通知逻辑
        } catch (Exception e) {
            logger.error("[业务通知] 发送自动审核通过通知失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendRejectAuditNotification(Long userId, Long recordId, String bizType, String auditRemark) {
        try {
            logger.info("[业务通知] 发送审核拒绝通知: userId={}, recordId={}, bizType={}, remark={}", userId, recordId, bizType, auditRemark);
            // TODO: 实现审核拒绝通知逻辑
        } catch (Exception e) {
            logger.error("[业务通知] 发送审核拒绝通知失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendApproveAuditNotification(Long userId, Long recordId, String bizType) {
        try {
            logger.info("[业务通知] 发送审核通过通知: userId={}, recordId={}, bizType={}", userId, recordId, bizType);
            // TODO: 实现审核通过通知逻辑
        } catch (Exception e) {
            logger.error("[业务通知] 发送审核通过通知失败: {}", e.getMessage(), e);
        }
    }
}
