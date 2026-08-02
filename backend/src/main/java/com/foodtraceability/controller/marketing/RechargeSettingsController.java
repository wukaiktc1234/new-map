package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.RechargeSettingsDTO;
import com.foodtraceability.service.marketing.RechargeSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 储值系统设置控制器
 * 对应前端 API 路径 /v1/recharge-settings
 *
 * 端点说明：
 * 1. GET /v1/recharge-settings - 获取系统设置
 * 2. PUT /v1/recharge-settings - 更新系统设置
 *
 * 单行配置：所有设置以JSON形式存储在 recharge_system_settings 表，固定ID为 RECHARGE_SETTINGS_001
 */
@RestController
@RequestMapping("/v1/recharge-settings")
@Tag(name = "储值系统设置", description = "充值限额、赠送规则、退款规则、风控配置等全局设置")
public class RechargeSettingsController {

    private final RechargeSettingsService rechargeSettingsService;

    public RechargeSettingsController(RechargeSettingsService rechargeSettingsService) {
        this.rechargeSettingsService = rechargeSettingsService;
    }

    /**
     * 获取系统设置
     */
    @GetMapping
    @Operation(summary = "获取储值系统设置")
    public Result<RechargeSettingsDTO> getSettings() {
        try {
            RechargeSettingsDTO dto = rechargeSettingsService.getSettings();
            return Result.success(dto, "获取系统设置成功");
        } catch (Exception e) {
            return Result.error(500, "获取系统设置失败：" + e.getMessage());
        }
    }

    /**
     * 更新系统设置
     */
    @PutMapping
    @Operation(summary = "更新储值系统设置")
    public Result<Void> updateSettings(@RequestBody RechargeSettingsDTO settingsDTO) {
        try {
            rechargeSettingsService.updateSettings(settingsDTO);
            return Result.success(null, "更新系统设置成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新系统设置失败：" + e.getMessage());
        }
    }
}
