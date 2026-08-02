package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.PointsChangeDTO;
import com.foodtraceability.dto.marketing.PointsDeductDTO;
import com.foodtraceability.dto.marketing.PointsEarnDTO;
import com.foodtraceability.entity.MemberPointsLog;
import com.foodtraceability.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分管理控制器
 */
@RestController
@RequestMapping("/v1/points")
@Tag(name = "积分管理", description = "积分获取、使用、明细查询、规则配置")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/consume/earn")
    @Operation(summary = "消费获得积分")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('member:view') or hasAuthority('*')")
    public Result<Integer> earnFromConsume(@Valid @RequestBody PointsEarnDTO earnDTO) {
        try {
            int earned = pointsService.earnPointsFromConsume(
                    earnDTO.getMemberId(), earnDTO.getAmount(), earnDTO.getScene(), earnDTO.getOrderNo());
            return Result.success(earned, "获得" + earned + "积分");
        } catch (RuntimeException e) {
            return Result.error(5001, e.getMessage());
        }
    }

    @PostMapping("/checkin")
    @Operation(summary = "签到获取积分")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('member:view') or hasAuthority('*')")
    public Result<Integer> checkin(@RequestParam Long memberId) {
        try {
            int points = pointsService.earnPointsFromCheckin(memberId);
            return Result.success(points, "签到成功，获得" + points + "积分");
        } catch (RuntimeException e) {
            return Result.error(5002, e.getMessage());
        }
    }

    @PostMapping("/deduct")
    @Operation(summary = "使用积分抵扣")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> deduct(@Valid @RequestBody PointsDeductDTO deductDTO) {
        try {
            long actualDeduct = pointsService.usePointsForDeduct(
                    deductDTO.getMemberId(), deductDTO.getPoints(), deductDTO.getOrderAmount(), deductDTO.getOrderNo());
            Map<String, Object> result = new HashMap<>();
            result.put("usedPoints", deductDTO.getPoints());
            result.put("deductAmount", actualDeduct);
            result.put("deductAmountYuan", String.format("%.2f", actualDeduct / 100.0));
            return Result.success(result, "抵扣成功");
        } catch (RuntimeException e) {
            return Result.error(5003, e.getMessage());
        }
    }

    @PostMapping("/adjust")
    @Operation(summary = "管理员调整积分")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('*')")
    public Result<Integer> adjust(@Valid @RequestBody PointsChangeDTO changeDTO) {
        try {
            int balanceAfter = pointsService.adjustPoints(changeDTO);
            return Result.success(balanceAfter, "调整成功，当前余额: " + balanceAfter);
        } catch (RuntimeException e) {
            return Result.error(5004, e.getMessage());
        }
    }

    @GetMapping("/{memberId}/log")
    @Operation(summary = "查询积分变动明细")
    @PreAuthorize("hasAuthority('member:points:view') or hasAuthority('*')")
    public Result<List<MemberPointsLog>> getLog(@PathVariable Long memberId,
                                                @RequestParam(defaultValue = "1") int current,
                                                @RequestParam(defaultValue = "20") int size) {
        List<MemberPointsLog> logs = pointsService.getPointsLog(memberId, current, size);
        return Result.success(logs);
    }

    @GetMapping("/{memberId}/max-deductible")
    @Operation(summary = "计算最大可抵扣积分")
    @PreAuthorize("hasAuthority('member:points:view') or hasAuthority('*')")
    public Result<Integer> maxDeductible(@PathVariable Long memberId,
                                        @RequestParam long orderAmount) {
        int maxPoints = pointsService.calculateMaxDeductiblePoints(memberId, orderAmount);
        return Result.success(maxPoints);
    }

    @GetMapping("/config")
    @Operation(summary = "获取积分规则配置")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('*')")
    public Result<Map<String, String>> getConfig() {
        return Result.success(pointsService.getPointsConfig());
    }

    @PutMapping("/config/{ruleKey}")
    @Operation(summary = "更新积分规则配置")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('*')")
    public Result<Void> updateConfig(@PathVariable String ruleKey,
                                    @RequestParam String ruleValue) {
        try {
            pointsService.updateRuleConfig(ruleKey, ruleValue);
            return Result.success(null, "配置更新成功");
        } catch (RuntimeException e) {
            return Result.error(5005, e.getMessage());
        }
    }

    @PostMapping("/expire")
    @Operation(summary = "手动触发过期积分清理（管理员）")
    @PreAuthorize("hasAuthority('member:points:manage') or hasAuthority('*')")
    public Result<Integer> expirePoints() {
        try {
            int count = pointsService.expirePoints();
            return Result.success(count, "清理完成，处理" + count + "条记录");
        } catch (Exception e) {
            return Result.error(5006, "清理失败: " + e.getMessage());
        }
    }
}
