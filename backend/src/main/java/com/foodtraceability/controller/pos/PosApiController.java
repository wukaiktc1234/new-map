package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PosCategoryDTO;
import com.foodtraceability.dto.PosComboDTO;
import com.foodtraceability.dto.PosDishDTO;
import com.foodtraceability.dto.PosMenuDTO;
import com.foodtraceability.service.PosApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * S1 修复：类级别 @PreAuthorize 统一要求 POS 收银角色，
 * 与 PosOrderController 保持一致的鉴权策略，
 * 防止非授权账号（如厨房、员工端）调用 POS 接口。
 * </p>
 */
@RestController
@RequestMapping("/v1/pos/api")
@Tag(name = "收银终端API", description = "收银终端专用接口")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR', '*')")
public class PosApiController {

    private final PosApiService posApiService;

    public PosApiController(PosApiService posApiService) {
        this.posApiService = posApiService;
    }

    @GetMapping("/categories")
    @Operation(summary = "获取菜品分类列表")
    public Result<List<PosCategoryDTO>> getCategories() {
        List<PosCategoryDTO> result = posApiService.getCategories();
        return Result.success(result);
    }

    @GetMapping("/dishes")
    @Operation(summary = "获取所有可用菜品")
    public Result<List<PosDishDTO>> getAllDishes() {
        List<PosDishDTO> result = posApiService.getAllDishes();
        return Result.success(result);
    }

    @GetMapping("/dishes/category/{categoryId}")
    @Operation(summary = "按分类获取菜品")
    public Result<List<PosDishDTO>> getDishesByCategory(@PathVariable String categoryId) {
        List<PosDishDTO> result = posApiService.getDishesByCategory(categoryId);
        return Result.success(result);
    }

    @GetMapping("/combos")
    @Operation(summary = "获取所有可用套餐")
    public Result<List<PosComboDTO>> getAllCombos() {
        List<PosComboDTO> result = posApiService.getAllCombos();
        return Result.success(result);
    }

    @GetMapping("/menu")
    @Operation(summary = "获取完整菜单（菜品+套餐）")
    public Result<PosMenuDTO> getFullMenu() {
        PosMenuDTO menu = posApiService.getFullMenu();
        return Result.success(menu);
    }
}
