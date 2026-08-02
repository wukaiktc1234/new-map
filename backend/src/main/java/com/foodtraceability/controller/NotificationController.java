package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeadLetterQueryDTO;
import com.foodtraceability.dto.DeadLetterVO;
import com.foodtraceability.dto.MsgSendDTO;
import com.foodtraceability.dto.MsgTemplateCreateDTO;
import com.foodtraceability.dto.NotificationSettingUpdateDTO;
import com.foodtraceability.dto.NotificationTemplatePreviewDTO;
import com.foodtraceability.dto.TestEventTriggerDTO;
import com.foodtraceability.entity.MsgSendRecord;
import com.foodtraceability.entity.MsgTemplate;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.entity.NotificationSettingEntity;
import com.foodtraceability.entity.NotificationUserPreference;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.NotificationDeadLetterService;
import com.foodtraceability.service.NotificationDataService;
import com.foodtraceability.service.NotificationPreferenceService;
import com.foodtraceability.service.NotificationService;
import com.foodtraceability.service.SiteNotificationService;
import com.foodtraceability.service.event.TestBusinessEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知管理控制器
 * 提供模板管理、消息发送、设置管理、统计仪表盘等18个API端点
 * 权限控制: 使用@PreAuthorize进行细粒度权限校验
 */
@Tag(name = "消息通知管理", description = "消息模板CRUD、发送管理、通知设置、统计仪表盘")
@RestController
@RequestMapping("/v1/notification")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);


    public NotificationController(NotificationService notificationService,
                                    SiteNotificationService siteNotificationService,
                                    NotificationPreferenceService notificationPreferenceService,
                                    NotificationDataService notificationDataService,
                                    NotificationDeadLetterService deadLetterService,
                                    ApplicationEventPublisher eventPublisher) {
        this.notificationService = notificationService;
        this.siteNotificationService = siteNotificationService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationDataService = notificationDataService;
        this.deadLetterService = deadLetterService;
        this.eventPublisher = eventPublisher;
    }

    private final NotificationService notificationService;
    private final SiteNotificationService siteNotificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationDataService notificationDataService;
    private final NotificationDeadLetterService deadLetterService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 模板管理 (8个端点) ====================

    @Operation(summary = "分页查询模板列表")
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('notification:template:query')")
    public Result<IPage<MsgTemplate>> getTemplateList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "模板编码(模糊)") @RequestParam(required = false) String templateCode,
            @Parameter(description = "模板名称(模糊)") @RequestParam(required = false) String templateName,
            @Parameter(description = "模板类型") @RequestParam(required = false) Integer templateType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "发送渠道") @RequestParam(required = false) String channel) {
        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<MsgTemplate> page = new Page<>(current, size);
            IPage<MsgTemplate> result = notificationService.getTemplatePage(page,
                    templateCode, templateName, templateType, status, channel);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询模板列表失败", e);
            return Result.error("查询模板列表失败");
        }
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('notification:template:query')")
    public Result<MsgTemplate> getTemplateDetail(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        MsgTemplate template = notificationService.getTemplateById(templateId);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        return Result.success(template);
    }

    @Operation(summary = "创建新模板")
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('notification:template:create')")
    @AuditLog(value = "创建消息模板", operationType = com.foodtraceability.annotation.OperationType.CREATE, module = "消息通知")
    public Result<MsgTemplate> createTemplate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Validated @RequestBody MsgTemplateCreateDTO dto) {
        try {
            MsgTemplate template = notificationService.createTemplate(dto,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(template);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("创建模板失败", e);
            return Result.error("创建模板失败");
        }
    }

    @Operation(summary = "更新模板")
    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('notification:template:edit')")
    @AuditLog(value = "更新消息模板", operationType = com.foodtraceability.annotation.OperationType.UPDATE, module = "消息通知")
    public Result<Void> updateTemplate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Validated @RequestBody MsgTemplateCreateDTO dto) {
        try {
            boolean success = notificationService.updateTemplate(templateId, dto,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "模板不存在或更新失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("更新模板失败: id={}", templateId, e);
            return Result.error("更新模板失败");
        }
    }

    @Operation(summary = "删除模板（逻辑删除）")
    @DeleteMapping("/templates/{templateId}")
    @PreAuthorize("hasAuthority('notification:template:delete')")
    @AuditLog(value = "删除消息模板", operationType = com.foodtraceability.annotation.OperationType.DELETE, module = "消息通知")
    public Result<Void> deleteTemplate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            boolean success = notificationService.deleteTemplate(templateId);
            if (success) return Result.success();
            return Result.error(404, "模板不存在或删除失败");
        } catch (Exception e) {
            logger.error("删除模板失败: id={}", templateId, e);
            return Result.error("删除模板失败");
        }
    }

    @Operation(summary = "启用模板")
    @PostMapping("/templates/{templateId}/enable")
    @PreAuthorize("hasAuthority('notification:template:edit')")
    @AuditLog(value = "启用消息模板", operationType = com.foodtraceability.annotation.OperationType.SYSTEM, module = "消息通知")
    public Result<Void> enableTemplate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            boolean success = notificationService.enableTemplate(templateId,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "模板不存在");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("启用模板失败: id={}", templateId, e);
            return Result.error("启用模板失败");
        }
    }

    @Operation(summary = "禁用模板")
    @PostMapping("/templates/{templateId}/disable")
    @PreAuthorize("hasAuthority('notification:template:edit')")
    @AuditLog(value = "禁用消息模板", operationType = com.foodtraceability.annotation.OperationType.SYSTEM, module = "消息通知")
    public Result<Void> disableTemplate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            boolean success = notificationService.disableTemplate(templateId,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "模板不存在");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("禁用模板失败: id={}", templateId, e);
            return Result.error("禁用模板失败");
        }
    }

    @Operation(summary = "预览模板渲染效果")
    @PostMapping("/templates/preview")
    @PreAuthorize("hasAuthority('notification:template:query')")
    public Result<Map<String, String>> previewTemplate(@RequestBody NotificationTemplatePreviewDTO dto) {
        try {
            String templateCode = dto.getTemplateCode();
            Map<String, Object> variables = dto.getVariables();

            if (templateCode == null || templateCode.isEmpty()) {
                return Result.error(400, "模板编码不能为空");
            }
            if (variables == null) {
                variables = new HashMap<>();
            }

            Map<String, String> result = notificationService.previewTemplate(templateCode, variables);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("预览模板失败", e);
            return Result.error("预览模板失败");
        }
    }

    // ==================== 消息发送 (4个端点) ====================

    @Operation(summary = "发送单条消息")
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('notification:message:send')")
    @AuditLog(value = "发送消息", operationType = com.foodtraceability.annotation.OperationType.CREATE, module = "消息通知")
    public Result<Long> sendMessage(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Validated @RequestBody MsgSendDTO dto) {
        try {
            Long recordId = notificationService.sendMessage(dto,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(recordId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("发送消息失败", e);
            return Result.error("发送消息失败");
        }
    }

    @Operation(summary = "批量发送消息")
    @PostMapping("/send/batch")
    @PreAuthorize("hasAuthority('notification:message:send')")
    @AuditLog(value = "批量发送消息", operationType = com.foodtraceability.annotation.OperationType.CREATE, module = "消息通知")
    public Result<List<Long>> sendBatchMessage(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody MsgSendDTO dto) {
        try {
            List<Long> recordIds = notificationService.sendBatchMessage(dto,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(recordIds);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("批量发送消息失败", e);
            return Result.error("批量发送消息失败");
        }
    }

    @Operation(summary = "查询发送记录列表")
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('notification:message:query')")
    public Result<IPage<MsgSendRecord>> getRecordList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "收件人(模糊)") @RequestParam(required = false) String recipient,
            @Parameter(description = "发送渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "发送状态") @RequestParam(required = false) Integer sendStatus,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "触发类型") @RequestParam(required = false) Integer triggerType,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<MsgSendRecord> page = new Page<>(current, size);
            IPage<MsgSendRecord> result = notificationService.getRecordPage(page,
                    recipient, channel, sendStatus, bizType, triggerType, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询发送记录失败", e);
            return Result.error("查询发送记录失败");
        }
    }

    @Operation(summary = "获取发送记录详情")
    @GetMapping("/records/{recordId}")
    @PreAuthorize("hasAuthority('notification:message:query')")
    public Result<MsgSendRecord> getRecordDetail(
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        MsgSendRecord record = notificationService.getRecordById(recordId);
        if (record == null) {
            return Result.error(404, "发送记录不存在");
        }
        return Result.success(record);
    }

    @Operation(summary = "重试失败的消息")
    @PostMapping("/records/{recordId}/retry")
    @PreAuthorize("hasAuthority('notification:message:send')")
    @AuditLog(value = "重试发送消息", operationType = com.foodtraceability.annotation.OperationType.UPDATE, module = "消息通知")
    public Result<Void> retryMessage(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        try {
            boolean success = notificationService.retryMessage(recordId,
                    Long.parseLong(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(400, "重试失败");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("重试消息失败: id={}", recordId, e);
            return Result.error("重试消息失败");
        }
    }

    // ==================== 设置管理 (2个端点) ====================

    @Operation(summary = "获取通知设置列表")
    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('notification:setting:query')")
    public Result<List<NotificationSettingEntity>> getSettings(
            @Parameter(description = "设置分组") @RequestParam(required = false) String settingGroup) {
        List<NotificationSettingEntity> settings = notificationService.getSettings(settingGroup);
        return Result.success(settings != null ? settings : new java.util.ArrayList<>());
    }

    @Operation(summary = "更新通知设置")
    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('notification:setting:edit')")
    @AuditLog(value = "更新通知设置", operationType = com.foodtraceability.annotation.OperationType.UPDATE, module = "消息通知")
    public Result<Void> updateSetting(@Validated @RequestBody NotificationSettingUpdateDTO dto) {
        try {
            boolean success = notificationService.updateSetting(dto);
            if (success) return Result.success();
            return Result.error("更新设置失败");
        } catch (Exception e) {
            logger.error("更新设置失败: key={}", dto.getSettingKey(), e);
            return Result.error("更新设置失败");
        }
    }

    // ==================== 统计仪表盘 (2个端点) ====================

    @Operation(summary = "获取发送统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('notification:statistics:query')")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            // 默认查询最近30天
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            }
            Map<String, Object> stats = notificationService.getStatistics(startTime, endTime);
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败");
        }
    }

    @Operation(summary = "获取仪表盘概览数据")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('notification:statistics:query')")
    public Result<Map<String, Object>> getDashboard() {
        try {
            Map<String, Object> dashboard = notificationService.getDashboard();
            return Result.success(dashboard);
        } catch (Exception e) {
            logger.error("获取仪表盘数据失败", e);
            return Result.error("获取仪表盘数据失败");
        }
    }

    // ==================== 辅助接口 (2个端点) ====================

    @Operation(summary = "获取渠道列表")
    @GetMapping("/channels")
    @PreAuthorize("isAuthenticated()")
    public Result<List<String>> getChannelList() {
        List<String> channels = notificationService.getChannelList();
        return Result.success(channels);
    }

    @Operation(summary = "获取模板变量定义")
    @GetMapping("/templates/{templateCode}/variables")
    @PreAuthorize("hasAuthority('notification:template:query')")
    public Result<List<String>> getTemplateVariables(
            @Parameter(description = "模板编码") @PathVariable String templateCode) {
        try {
            List<String> variables = notificationService.getTemplateVariables(templateCode);
            return Result.success(variables);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("获取模板变量失败: code={}", templateCode, e);
            return Result.error("获取模板变量失败");
        }
    }

    // ==================== 站内通知管理 (6个端点) ====================

    @Operation(summary = "获取当前用户的通知列表")
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getMyNotifications(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "已读状态: 0未读/1已读") @RequestParam(required = false) Integer isRead,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            List<Notification> notifications = siteNotificationService.getNotificationsByUserId(
                    userId, isRead, page, size);
            long totalCount = notificationDataService.countByUserId(userId, isRead);
            long pages = (totalCount + size - 1) / size;
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("records", notifications);
            pageData.put("total", totalCount);
            pageData.put("current", page);
            pageData.put("size", size);
            pageData.put("pages", pages);
            return Result.success(pageData);
        } catch (Exception e) {
            logger.error("获取通知列表失败", e);
            return Result.error("获取通知列表失败");
        }
    }

    @Operation(summary = "获取当前用户未读通知数量")
    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser currentUser) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            long count = siteNotificationService.countUnreadByUserId(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取未读数量失败", e);
            return Result.error("获取未读数量失败");
        }
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/notifications/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markNotificationAsRead(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            boolean success = siteNotificationService.markAsRead(notificationId, userId);
            if (success) return Result.success();
            return Result.error(400, "标记已读失败");
        } catch (Exception e) {
            logger.error("标记已读失败: id={}", notificationId, e);
            return Result.error("标记已读失败");
        }
    }

    @Operation(summary = "标记所有通知为已读")
    @PutMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> markAllNotificationsAsRead(
            @AuthenticationPrincipal SecurityUser currentUser) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            siteNotificationService.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            logger.error("标记全部已读失败", e);
            return Result.error("标记全部已读失败");
        }
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/notifications/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteNotification(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            boolean success = siteNotificationService.deleteNotification(notificationId, userId);
            if (success) return Result.success();
            return Result.error(404, "通知不存在");
        } catch (Exception e) {
            logger.error("删除通知失败: id={}", notificationId, e);
            return Result.error("删除通知失败");
        }
    }

    @Operation(summary = "批量删除通知")
    @PostMapping("/notifications/batch-delete")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> batchDeleteNotifications(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> notificationIds) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            for (Long id : notificationIds) {
                siteNotificationService.deleteNotification(id, userId);
            }
            return Result.success();
        } catch (Exception e) {
            logger.error("批量删除通知失败", e);
            return Result.error("批量删除通知失败");
        }
    }

    // ==================== 用户通知偏好 (3个端点) ====================

    @Operation(summary = "获取当前用户的通知偏好")
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public Result<List<NotificationUserPreference>> getMyPreferences(
            @AuthenticationPrincipal SecurityUser currentUser) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            List<NotificationUserPreference> prefs = notificationPreferenceService.getUserPreferences(userId);
            if (prefs.isEmpty()) {
                notificationPreferenceService.initUserPreferences(userId);
                prefs = notificationPreferenceService.getUserPreferences(userId);
            }
            return Result.success(prefs);
        } catch (Exception e) {
            logger.error("获取通知偏好失败", e);
            return Result.error("获取通知偏好失败");
        }
    }

    @Operation(summary = "更新通知偏好")
    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public Result<List<NotificationUserPreference>> updatePreferences(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<NotificationUserPreference> preferences) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            List<NotificationUserPreference> result = notificationPreferenceService.batchUpdatePreferences(
                    userId, preferences);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("更新通知偏好失败", e);
            return Result.error("更新通知偏好失败");
        }
    }

    @Operation(summary = "初始化通知偏好")
    @PostMapping("/preferences/init")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> initPreferences(
            @AuthenticationPrincipal SecurityUser currentUser) {
        try {
            Long userId = Long.parseLong(currentUser.getUserId());
            notificationPreferenceService.initUserPreferences(userId);
            return Result.success();
        } catch (Exception e) {
            logger.error("初始化通知偏好失败", e);
            return Result.error("初始化通知偏好失败");
        }
    }

    // ==================== Sprint 1: 事件总线 + 死信管理 (6个端点) ====================

    /**
     * 手动触发测试业务事件（验收场景 E1）
     *
     * <p>对应 spec F-001 / plan.md 第 6.2.1 节。
     * 用于验证 NotificationEventBus 端到端链路：
     * 发布事件 → AFTER_COMMIT 监听 → 查询模板 → 渲染 → 渠道分发。</p>
     */
    @Operation(summary = "手动触发测试业务事件(Sprint 1验收用)")
    @PostMapping("/test-event")
    @PreAuthorize("hasAuthority('notification:event:test')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> triggerTestEvent(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Validated @RequestBody TestEventTriggerDTO dto) {
        try {
            Long operatorId = Long.parseLong(currentUser.getUserId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("eventName", dto.getEventName());
            variables.put("operatorName", dto.getOperatorName());

            TestBusinessEvent event = new TestBusinessEvent(
                    this, operatorId, variables,
                    dto.getRecipientUserIds(), dto.getChannels());

            eventPublisher.publishEvent(event);

            logger.info("测试事件已发布: eventName={}, operatorId={}, recipients={}, channels={}",
                    dto.getEventName(), operatorId,
                    dto.getRecipientUserIds().size(), dto.getChannels());
            return Result.success();
        } catch (Exception e) {
            // FIX-013(B7): 事务内抛异常但被 catch 吞掉,需手动标记回滚,
            // 否则 AFTER_COMMIT 监听器不会触发,且事务状态不一致
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("触发测试事件失败", e);
            return Result.error("触发测试事件失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询死信列表（验收场景 E12）
     *
     * <p>对应 spec NC-006 / plan.md 第 6.2.5 节。</p>
     */
    @Operation(summary = "分页查询死信列表")
    @GetMapping("/dead-letters")
    @PreAuthorize("hasAuthority('notification:deadletter:query')")
    public Result<IPage<DeadLetterVO>> getDeadLetterPage(@Validated DeadLetterQueryDTO query) {
        try {
            IPage<DeadLetterVO> page = deadLetterService.getDeadLetterPage(query);
            return Result.success(page);
        } catch (Exception e) {
            logger.error("查询死信列表失败", e);
            return Result.error("查询死信列表失败");
        }
    }

    /**
     * 查询死信详情
     */
    @Operation(summary = "查询死信详情")
    @GetMapping("/dead-letters/{deadLetterId}")
    @PreAuthorize("hasAuthority('notification:deadletter:query')")
    public Result<DeadLetterVO> getDeadLetterDetail(
            @Parameter(description = "死信ID") @PathVariable Long deadLetterId) {
        try {
            DeadLetterVO vo = deadLetterService.getDeadLetterById(deadLetterId);
            if (vo == null) {
                return Result.error(404, "死信记录不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            logger.error("查询死信详情失败: id={}", deadLetterId, e);
            return Result.error("查询死信详情失败");
        }
    }

    /**
     * 重试死信消息（验收场景 E13）
     */
    @Operation(summary = "重试死信消息")
    @PostMapping("/dead-letters/{deadLetterId}/retry")
    @PreAuthorize("hasAuthority('notification:deadletter:retry')")
    @AuditLog(value = "重试死信消息", operationType = com.foodtraceability.annotation.OperationType.UPDATE, module = "消息通知")
    public Result<Void> retryDeadLetter(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "死信ID") @PathVariable Long deadLetterId) {
        try {
            Long operatorId = Long.parseLong(currentUser.getUserId());
            boolean success = deadLetterService.retryDeadLetter(deadLetterId, operatorId);
            if (success) {
                return Result.success();
            }
            return Result.error(400, "重试失败, 死信记录不存在或原始记录已删除");
        } catch (Exception e) {
            logger.error("重试死信失败: id={}", deadLetterId, e);
            return Result.error("重试死信失败: " + e.getMessage());
        }
    }

    /**
     * 标记死信已处理（验收场景 E14）
     */
    @Operation(summary = "标记死信已处理")
    @PostMapping("/dead-letters/{deadLetterId}/resolve")
    @PreAuthorize("hasAuthority('notification:deadletter:retry')")
    @AuditLog(value = "标记死信已处理", operationType = com.foodtraceability.annotation.OperationType.UPDATE, module = "消息通知")
    public Result<Void> resolveDeadLetter(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "死信ID") @PathVariable Long deadLetterId,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {
        try {
            Long operatorId = Long.parseLong(currentUser.getUserId());
            boolean success = deadLetterService.resolveDeadLetter(deadLetterId, operatorId, remark);
            if (success) {
                return Result.success();
            }
            return Result.error(404, "死信记录不存在");
        } catch (Exception e) {
            logger.error("标记死信处理失败: id={}", deadLetterId, e);
            return Result.error("标记死信处理失败");
        }
    }

    /**
     * 删除死信记录（逻辑删除）
     */
    @Operation(summary = "删除死信记录")
    @DeleteMapping("/dead-letters/{deadLetterId}")
    @PreAuthorize("hasAuthority('notification:deadletter:delete')")
    @AuditLog(value = "删除死信记录", operationType = com.foodtraceability.annotation.OperationType.DELETE, module = "消息通知")
    public Result<Void> deleteDeadLetter(
            @Parameter(description = "死信ID") @PathVariable Long deadLetterId) {
        try {
            boolean success = deadLetterService.deleteDeadLetter(deadLetterId);
            if (success) {
                return Result.success();
            }
            return Result.error(404, "死信记录不存在");
        } catch (Exception e) {
            logger.error("删除死信失败: id={}", deadLetterId, e);
            return Result.error("删除死信失败");
        }
    }
}
