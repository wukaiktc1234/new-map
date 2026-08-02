package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DictCreateDTO;
import com.foodtraceability.dto.DictItemCreateDTO;
import com.foodtraceability.dto.DictItemUpdateDTO;
import com.foodtraceability.dto.DictUpdateDTO;
import com.foodtraceability.entity.SysDict;
import com.foodtraceability.entity.SysDictItem;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典管理Controller
 * 提供字典类型和字典项的RESTful API
 *
 * 异常处理说明：
 * - 本Controller依赖全局异常处理器 GlobalExceptionHandler 统一处理异常
 * - 所有业务异常（BusinessException）会自动转换为统一的错误响应
 * - 无需在每个方法中单独捕获异常，保持代码简洁
 */
@Tag(name = "字典管理", description = "字典类型和字典项的增删改查、缓存管理")
@RestController
@RequestMapping("/v1/dict")
public class SysDictController {

    private final SysDictService sysDictService;

    public SysDictController(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    // ==================== 字典类型API ====================

    @Operation(summary = "创建字典类型")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    @AuditLog(value = "创建字典类型", operationType = OperationType.CREATE, module = "字典管理")
    public Result<SysDict> createDict(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody DictCreateDTO dto) {
        // 异常由GlobalExceptionHandler统一处理
        SysDict dict = sysDictService.createDict(dto,
            currentUser.getUserId(), currentUser.getUsername());
        return Result.success(dict);
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/{dictId}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @AuditLog(value = "更新字典类型", operationType = OperationType.UPDATE, module = "字典管理")
    public Result<Void> updateDict(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "字典ID") @PathVariable Long dictId,
            @RequestBody DictUpdateDTO dto) {
        boolean success = sysDictService.updateDict(dictId, dto,
            currentUser.getUserId(), currentUser.getUsername());
        
        if (!success) {
            return Result.error("更新失败，字典不存在");
        }
        return Result.success();
    }

    @Operation(summary = "删除字典类型（逻辑删除）")
    @DeleteMapping("/{dictId}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @AuditLog(value = "删除字典类型", operationType = OperationType.DELETE, module = "字典管理")
    public Result<Void> deleteDict(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        boolean success = sysDictService.deleteDict(dictId);
        
        if (!success) {
            return Result.error("删除失败，字典不存在或已被删除");
        }
        return Result.success();
    }

    @Operation(summary = "获取字典类型详情")
    @GetMapping("/{dictId}")
    @PreAuthorize("isAuthenticated()")
    public Result<SysDict> getDict(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        SysDict dict = sysDictService.getDictById(dictId);
        
        if (dict == null) {
            return Result.error(404, "字典不存在");
        }
        return Result.success(dict);
    }

    @Operation(summary = "根据编码获取字典信息")
    @GetMapping("/code/{dictCode}")
    @PreAuthorize("isAuthenticated()")
    public Result<SysDict> getDictByCode(
            @Parameter(description = "字典编码") @PathVariable String dictCode) {
        SysDict dict = sysDictService.getDictByCode(dictCode);
        
        if (dict == null) {
            return Result.error(404, "字典编码不存在");
        }
        return Result.success(dict);
    }

    @Operation(summary = "分页查询字典类型列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:query')")
    public Result<IPage<SysDict>> getDictList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "字典名称(模糊)") @RequestParam(required = false) String dictName,
            @Parameter(description = "字典编码(模糊)") @RequestParam(required = false) String dictCode,
            @Parameter(description = "字典分组") @RequestParam(required = false) String dictGroup,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        // 参数校验
        if (current == null || current < 1) current = 1;
        if (size == null || size < 1 || size > 100) size = 20;

        Page<SysDict> page = new Page<>(current, size);
        IPage<SysDict> result = sysDictService.getDictPage(page, dictName, dictCode, dictGroup, status);
        return Result.success(result);
    }

    @Operation(summary = "获取所有字典分组列表")
    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('system:dict:query')")
    public Result<List<String>> getGroups() {
        List<String> groups = sysDictService.getAllGroups();
        return Result.success(groups);
    }

    @Operation(summary = "启用字典类型")
    @PostMapping("/{dictId}/enable")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @AuditLog(value = "启用字典", operationType = OperationType.SYSTEM, module = "字典管理")
    public Result<Void> enableDict(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        boolean success = sysDictService.enableDict(dictId);
        
        if (!success) {
            return Result.error(404, "字典不存在");
        }
        return Result.success();
    }

    @Operation(summary = "禁用字典类型")
    @PostMapping("/{dictId}/disable")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @AuditLog(value = "禁用字典", operationType = OperationType.SYSTEM, module = "字典管理")
    public Result<Void> disableDict(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        boolean success = sysDictService.disableDict(dictId);
        
        if (!success) {
            return Result.error(404, "字典不存在");
        }
        return Result.success();
    }

    // ==================== 字典项API ====================

    @Operation(summary = "创建字典项")
    @PostMapping("/item")
    @PreAuthorize("hasAuthority('system:dict:create')")
    @AuditLog(value = "创建字典项", operationType = OperationType.CREATE, module = "字典管理")
    public Result<SysDictItem> createDictItem(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody DictItemCreateDTO dto) {
        // 异常由GlobalExceptionHandler统一处理
        SysDictItem item = sysDictService.createDictItem(dto,
            currentUser.getUserId(), currentUser.getUsername());
        return Result.success(item);
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/item/{itemId}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @AuditLog(value = "更新字典项", operationType = OperationType.UPDATE, module = "字典管理")
    public Result<Void> updateDictItem(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "字典项ID") @PathVariable Long itemId,
            @RequestBody DictItemUpdateDTO dto) {
        boolean success = sysDictService.updateDictItem(itemId, dto,
            currentUser.getUserId(), currentUser.getUsername());
        
        if (!success) {
            return Result.error("更新失败，字典项不存在");
        }
        return Result.success();
    }

    @Operation(summary = "删除字典项（逻辑删除）")
    @DeleteMapping("/item/{itemId}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @AuditLog(value = "删除字典项", operationType = OperationType.DELETE, module = "字典管理")
    public Result<Void> deleteDictItem(
            @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        boolean success = sysDictService.deleteDictItem(itemId);
        
        if (!success) {
            return Result.error("删除失败，字典项不存在或已被删除");
        }
        return Result.success();
    }

    @Operation(summary = "获取字典项详情")
    @GetMapping("/item/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public Result<SysDictItem> getDictItem(
            @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        SysDictItem item = sysDictService.getDictItemById(itemId);
        
        if (item == null) {
            return Result.error(404, "字典项不存在");
        }
        return Result.success(item);
    }

    @Operation(summary = "分页查询字典项列表")
    @GetMapping("/item/list")
    @PreAuthorize("hasAuthority('system:dict:query')")
    public Result<IPage<SysDictItem>> getDictItemList(
            @Parameter(description = "字典类型ID") @RequestParam Long dictId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "字典项标签(模糊)") @RequestParam(required = false) String itemLabel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        // 参数校验
        if (current == null || current < 1) current = 1;
        if (size == null || size < 1 || size > 100) size = 20;

        Page<SysDictItem> page = new Page<>(current, size);
        IPage<SysDictItem> result = sysDictService.getDictItemPage(page, dictId, itemLabel, status);
        return Result.success(result);
    }

    @Operation(summary = "根据字典ID获取所有启用的字典项（用于前端组件）")
    @GetMapping("/{dictId}/items")
    @PreAuthorize("isAuthenticated()")
    public Result<List<SysDictItem>> getEnabledItemsByDictId(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        List<SysDictItem> items = sysDictService.getEnabledItemsByDictId(dictId);
        return Result.success(items != null ? items : List.of());
    }

    @Operation(summary = "根据字典编码获取所有启用的字典项（用于前端组件）")
    @GetMapping("/code/{dictCode}/items")
    @PreAuthorize("isAuthenticated()")
    public Result<List<SysDictItem>> getEnabledItemsByDictCode(
            @Parameter(description = "字典编码") @PathVariable String dictCode) {
        List<SysDictItem> items = sysDictService.getEnabledItemsByDictCode(dictCode);
        return Result.success(items != null ? items : List.of());
    }

    // ==================== 缓存管理API ====================

    @Operation(summary = "清除指定字典缓存")
    @DeleteMapping("/cache/{dictId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearCache(
            @Parameter(description = "字典ID") @PathVariable Long dictId) {
        sysDictService.clearDictCache(dictId);
        return Result.success();
    }

    @Operation(summary = "清除所有字典缓存")
    @DeleteMapping("/cache/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearAllCache() {
        sysDictService.clearAllDictCache();
        return Result.success();
    }

    // ==================== 统计API ====================

    @Operation(summary = "获取字典统计信息")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('system:dict:query')")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = sysDictService.getDictStats();
        return Result.success(stats);
    }
}
