package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.AnomalyAlertQueryDTO;
import com.foodtraceability.dto.marketing.AnomalyAlertVO;
import com.foodtraceability.service.marketing.AnomalyAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 异常交易告警控制器
 * 对应前端 API 路径 /v1/anomaly-alerts
 *
 * 端点说明：
 * 1. GET /v1/anomaly-alerts                   - 分页查询异常告警
 * 2. PUT /v1/anomaly-alerts/{alertId}/handle  - 处理告警
 * 3. PUT /v1/anomaly-alerts/{alertId}/ignore  - 忽略告警
 *
 * 状态：pending-待处理 handled-已处理 ignored-已忽略
 * 告警类型：frequent_recharge/fast_consume/high_refund_rate/new_member_high_recharge/multi_device/over_limit
 */
@RestController
@RequestMapping("/v1/anomaly-alerts")
@Tag(name = "异常交易告警", description = "风控告警查询、处理、忽略")
public class AnomalyAlertController {

    private final AnomalyAlertService anomalyAlertService;

    public AnomalyAlertController(AnomalyAlertService anomalyAlertService) {
        this.anomalyAlertService = anomalyAlertService;
    }

    /**
     * 分页查询异常告警
     */
    @GetMapping
    @Operation(summary = "分页查询异常告警")
    @PreAuthorize("hasAuthority('member:alert:view') or hasAuthority('*')")
    public Result<PageResult<AnomalyAlertVO>> getAlertPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "处理状态: pending/handled/ignored") @RequestParam(required = false) String status) {
        try {
            AnomalyAlertQueryDTO queryDTO = new AnomalyAlertQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setStatus(status);
            PageResult<AnomalyAlertVO> result = anomalyAlertService.getAlertPage(queryDTO);
            return Result.success(result, "查询异常告警成功");
        } catch (Exception e) {
            return Result.error(500, "查询异常告警失败：" + e.getMessage());
        }
    }

    /**
     * 处理告警
     */
    @PutMapping("/{alertId}/handle")
    @Operation(summary = "处理告警")
    @PreAuthorize("hasAuthority('member:alert:manage') or hasAuthority('*')")
    public Result<Void> handleAlert(
            @Parameter(description = "告警ID") @PathVariable("alertId") String alertId,
            @RequestBody Map<String, String> body) {
        try {
            String remark = body != null ? body.get("remark") : null;
            anomalyAlertService.handleAlert(alertId, remark);
            return Result.success(null, "处理告警成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "处理告警失败：" + e.getMessage());
        }
    }

    /**
     * 忽略告警
     */
    @PutMapping("/{alertId}/ignore")
    @Operation(summary = "忽略告警")
    @PreAuthorize("hasAuthority('member:alert:manage') or hasAuthority('*')")
    public Result<Void> ignoreAlert(
            @Parameter(description = "告警ID") @PathVariable("alertId") String alertId) {
        try {
            anomalyAlertService.ignoreAlert(alertId);
            return Result.success(null, "忽略告警成功");
        } catch (RuntimeException e) {
            return Result.error(4002, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "忽略告警失败：" + e.getMessage());
        }
    }
}
