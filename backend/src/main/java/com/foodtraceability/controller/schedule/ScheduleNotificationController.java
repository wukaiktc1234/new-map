package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.NotificationLogQueryDTO;
import com.foodtraceability.dto.schedule.NotificationLogVO;
import com.foodtraceability.dto.schedule.NotificationStatsVO;
import com.foodtraceability.service.schedule.ScheduleNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 排班通知管理控制器
 * 对应前端 API 路径 /v1/schedule/notifications
 *
 * <p>端点说明：
 * <ul>
 *   <li>GET  /v1/schedule/notifications          - 分页查询通知日志</li>
 *   <li>GET  /v1/schedule/notifications/stats    - 通知统计（必须在 /{id} 之前）</li>
 *   <li>POST /v1/schedule/notifications          - 发送通知</li>
 *   <li>POST /v1/schedule/notifications/{id}/resend - 重试发送</li>
 * </ul>
 *
 * <p>路径匹配优先级：字面量路径（stats）优先于 {id} 匹配
 */
@Tag(name = "排班管理-通知管理", description = "排班通知(F-009)相关接口")
@RestController
@RequestMapping("/v1/schedule/notifications")
public class ScheduleNotificationController {

    private final ScheduleNotificationService notificationService;

    public ScheduleNotificationController(ScheduleNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 分页查询通知日志
     */
    @Operation(summary = "分页查询通知日志")
    @GetMapping
    public Result<PageResult<NotificationLogVO>> getNotificationList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "渠道筛选: in_app/sms/email/wechat_work")
            @RequestParam(required = false) String channel,
            @Parameter(description = "业务类型筛选") @RequestParam(required = false) String businessType,
            @Parameter(description = "开始日期 (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
        try {
            NotificationLogQueryDTO queryDTO = new NotificationLogQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setChannel(channel);
            queryDTO.setBusinessType(businessType);
            queryDTO.setStartDate(startDate);
            queryDTO.setEndDate(endDate);
            PageResult<NotificationLogVO> result = notificationService.getNotificationList(queryDTO);
            return Result.success(result, "查询通知日志成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取通知发送统计
     * 注意：字面量路径优先于 {id} 匹配，确保 /stats 不会被误识别为日志ID
     */
    @Operation(summary = "获取通知发送统计",
            description = "返回通知总数、成功/失败/待发送数量及成功率")
    @GetMapping("/stats")
    public Result<NotificationStatsVO> getNotificationStats() {
        try {
            NotificationStatsVO vo = notificationService.getNotificationStats();
            return Result.success(vo, "获取通知统计成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重试发送失败的通知
     */
    @Operation(summary = "重试发送通知",
            description = "对失败状态的通知进行重试发送")
    @PostMapping("/{id}/resend")
    public Result<Void> resendNotification(
            @Parameter(description = "通知日志ID") @PathVariable("id") Long id) {
        try {
            notificationService.retrySend(id);
            return Result.success(null, "重试发送成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送通知
     */
    @Operation(summary = "发送通知",
            description = "主动发送通知到指定接收人")
    @PostMapping
    public Result<NotificationLogVO> sendNotification(@RequestBody Map<String, Object> body) {
        try {
            String businessType = (String) body.get("businessType");
            String businessId = (String) body.get("businessId");
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            String channel = (String) body.get("channel");

            @SuppressWarnings("unchecked")
            List<String> receiverIds = (List<String>) body.get("receiverIds");

            if (title == null || title.isEmpty()) {
                return Result.error("通知标题不能为空");
            }
            if (receiverIds == null || receiverIds.isEmpty()) {
                return Result.error("接收人不能为空");
            }

            NotificationLogVO vo = notificationService.sendNotification(
                    businessType, businessId, title, content, receiverIds, channel);
            return Result.success(vo, "通知发送成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
