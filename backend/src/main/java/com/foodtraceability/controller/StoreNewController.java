package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.store.StoreCreateDTO;
import com.foodtraceability.dto.store.StoreUpdateDTO;
import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.service.StoreNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店档案管理 Controller（增强版）
 *
 * 基于 stores_new 表，提供门店基础档案的完整 CRUD + 状态管理 + 编码校验。
 * 替代旧版 StoreController（基于 MySQL 语法的 stores 表）。
 *
 * 权限要求：管理员、区域经理、店长可操作。
 */
@RestController
@RequestMapping("/v1/stores")
@Tag(name = "门店档案管理", description = "门店基础档案的增删改查及状态管理")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_REGIONAL_MANAGER', 'ROLE_MANAGER')")
public class StoreNewController {

    private final StoreNewService storeNewService;

    public StoreNewController(StoreNewService storeNewService) {
        this.storeNewService = storeNewService;
    }

    /**
     * 分页查询门店列表
     * GET /v1/stores?current=1&size=10&storeName=&storeType=&status=
     */
    @GetMapping
    @Operation(summary = "分页查询门店列表", description = "支持按门店名称、类型、状态筛选")
    public Result<Map<String, Object>> getPageList(
            @Parameter(description = "当前页码，默认1") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "门店名称（模糊查询）") @RequestParam(required = false) String storeName,
            @Parameter(description = "门店类型：1直营/2加盟/3合作") @RequestParam(required = false) Integer storeType,
            @Parameter(description = "门店状态：1营业中/2装修中/3暂停营业/4已关闭") @RequestParam(required = false) Integer status
    ) {
        Page<StoreNew> page = new Page<>(current, size);
        Page<StoreNew> result = storeNewService.getPageList(page, storeName, storeType, status);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());
        return Result.success(data);
    }

    /**
     * 获取门店详情
     * GET /v1/stores/{storeId}
     */
    @GetMapping("/{storeId}")
    @Operation(summary = "获取门店详情")
    public Result<StoreNew> getById(@PathVariable Long storeId) {
        StoreNew store = storeNewService.getById(storeId);
        if (store == null) {
            return Result.error("门店不存在: " + storeId);
        }
        return Result.success(store);
    }

    /**
     * 根据门店编码查询
     * GET /v1/stores/code/{storeCode}
     */
    @GetMapping("/code/{storeCode}")
    @Operation(summary = "根据门店编码查询")
    public Result<StoreNew> getByCode(@PathVariable String storeCode) {
        StoreNew store = storeNewService.getByStoreCode(storeCode);
        if (store == null) {
            return Result.error("门店编码不存在: " + storeCode);
        }
        return Result.success(store);
    }

    /**
     * 创建门店
     * POST /v1/stores
     */
    @PostMapping
    @Operation(summary = "创建门店", description = "创建新门店，需提供门店编码、名称、类型等基础信息")
    public Result<StoreNew> create(@Valid @RequestBody StoreCreateDTO createDTO) {
        // 校验门店编码唯一性
        if (!storeNewService.isCodeAvailable(createDTO.getStoreCode(), null)) {
            return Result.error("门店编码已存在: " + createDTO.getStoreCode());
        }
        StoreNew store = storeNewService.createStore(createDTO);
        return Result.success(store);
    }

    /**
     * 更新门店（仅更新非 null 字段）
     * PUT /v1/stores/{storeId}
     */
    @PutMapping("/{storeId}")
    @Operation(summary = "更新门店信息", description = "部分更新，仅更新请求中提供的非 null 字段")
    public Result<StoreNew> update(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreUpdateDTO updateDTO
    ) {
        StoreNew store = storeNewService.updateStore(storeId, updateDTO);
        return Result.success(store);
    }

    /**
     * 更新门店状态（快捷接口）
     * PATCH /v1/stores/{storeId}/status
     */
    @PatchMapping("/{storeId}/status")
    @Operation(summary = "更新门店状态", description = "1营业中/2装修中/3暂停营业/4已关闭")
    public Result<Void> updateStatus(
            @PathVariable Long storeId,
            @Parameter(description = "门店状态：1营业中/2装修中/3暂停营业/4已关闭") @RequestParam Integer status
    ) {
        storeNewService.updateStatus(storeId, status);
        return Result.success();
    }

    /**
     * 删除门店（逻辑删除）
     * DELETE /v1/stores/{storeId}
     */
    @DeleteMapping("/{storeId}")
    @Operation(summary = "删除门店", description = "逻辑删除，不实际删除数据库记录")
    public Result<Void> delete(@PathVariable Long storeId) {
        boolean success = storeNewService.removeById(storeId);
        if (!success) {
            return Result.error("门店删除失败，可能门店不存在: " + storeId);
        }
        return Result.success();
    }

    /**
     * 获取所有营业中的门店（下拉选项）
     * GET /v1/stores/active
     */
    @GetMapping("/active")
    @Operation(summary = "获取所有营业中的门店", description = "用于下拉选项，无需分页")
    @PreAuthorize("hasAnyRole('admin', 'regional_manager', 'manager', 'store_manager', 'warehouse_manager', 'purchase_manager', 'department_manager', 'employee')")
    public Result<List<StoreNew>> getActiveStores() {
        List<StoreNew> stores = storeNewService.getActiveStores();
        return Result.success(stores);
    }

    /**
     * 检查门店编码是否可用
     * GET /v1/stores/code-available?storeCode=&excludeId=
     */
    @GetMapping("/code-available")
    @Operation(summary = "检查门店编码是否可用")
    public Result<Map<String, Object>> isCodeAvailable(
            @Parameter(description = "门店编码") @RequestParam String storeCode,
            @Parameter(description = "排除的门店ID（更新时传入当前门店ID）") @RequestParam(required = false) Long excludeId
    ) {
        boolean available = storeNewService.isCodeAvailable(storeCode, excludeId);
        Map<String, Object> data = new HashMap<>();
        data.put("available", available);
        data.put("storeCode", storeCode);
        return Result.success(data);
    }

    /**
     * 门店统计信息
     * GET /v1/stores/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "获取门店统计信息", description = "按类型和状态统计门店数量")
    public Result<Map<String, Object>> getStats() {
        List<StoreNew> allStores = storeNewService.list();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allStores.size());

        // 按状态统计
        Map<String, Integer> byStatus = new HashMap<>();
        byStatus.put("running", 0);
        byStatus.put("renovating", 0);
        byStatus.put("paused", 0);
        byStatus.put("closed", 0);
        for (StoreNew store : allStores) {
            if (store.getStatus() == null) continue;
            switch (store.getStatus()) {
                case 1: byStatus.merge("running", 1, Integer::sum); break;
                case 2: byStatus.merge("renovating", 1, Integer::sum); break;
                case 3: byStatus.merge("paused", 1, Integer::sum); break;
                case 4: byStatus.merge("closed", 1, Integer::sum); break;
            }
        }
        stats.put("byStatus", byStatus);

        // 按类型统计
        Map<String, Integer> byType = new HashMap<>();
        byType.put("direct", 0);
        byType.put("franchise", 0);
        byType.put("cooperation", 0);
        for (StoreNew store : allStores) {
            if (store.getStoreType() == null) continue;
            switch (store.getStoreType()) {
                case 1: byType.merge("direct", 1, Integer::sum); break;
                case 2: byType.merge("franchise", 1, Integer::sum); break;
                case 3: byType.merge("cooperation", 1, Integer::sum); break;
            }
        }
        stats.put("byType", byType);

        // 许可证即将到期统计（30天内）
        long licenseExpiringSoon = allStores.stream()
                .filter(s -> s.getLicenseExpiry() != null)
                .filter(s -> {
                    java.time.LocalDate expiry = s.getLicenseExpiry();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    return !expiry.isBefore(today) && expiry.isBefore(today.plusDays(30));
                })
                .count();
        stats.put("licenseExpiringSoon", licenseExpiringSoon);

        return Result.success(stats);
    }
}
