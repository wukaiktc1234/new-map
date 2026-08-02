package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventorySettingQueryDTO;
import com.foodtraceability.dto.InventorySettingUpdateDTO;
import com.foodtraceability.entity.InventorySetting;
import com.foodtraceability.service.InventorySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/inventory/settings")
@Tag(name = "库存设置管理")
public class InventorySettingController {

    private final InventorySettingService inventorySettingService;

    public InventorySettingController(InventorySettingService inventorySettingService) {
        this.inventorySettingService = inventorySettingService;
    }

    @GetMapping
    @Operation(summary = "获取库存设置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = inventorySettingService.getAllSettingsAsMap();
        return Result.success(settings, "获取库存设置成功");
    }

    @GetMapping("/list")
    @Operation(summary = "查询库存设置列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<InventorySetting>> listSettings(InventorySettingQueryDTO queryDTO) {
        List<InventorySetting> settings = inventorySettingService.listSettings(queryDTO);
        return Result.success(settings, "查询库存设置列表成功");
    }

    @GetMapping("/{key}")
    @Operation(summary = "根据键名获取设置值")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<InventorySetting> getSettingByKey(@PathVariable String key) {
        InventorySetting setting = inventorySettingService.getByKey(key);
        if (setting == null) {
            return Result.error(404, "设置项不存在：" + key);
        }
        return Result.success(setting, "获取设置项成功");
    }

    @PutMapping
    @Operation(summary = "批量更新库存设置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> newSettings) {
        Map<String, Object> settings = inventorySettingService.batchUpdateSettings(newSettings);
        return Result.success(settings, "更新库存设置成功");
    }

    @PutMapping("/item")
    @Operation(summary = "更新单个库存设置项")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventorySetting> updateSetting(@Valid @RequestBody InventorySettingUpdateDTO updateDTO) {
        InventorySetting setting = inventorySettingService.updateSetting(updateDTO);
        return Result.success(setting, "更新设置项成功");
    }
}
