package com.foodtraceability.controller.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AssetCategoryNew;
import com.foodtraceability.service.AssetCategoryNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产分类管理控制器
 * 提供资产分类的 CRUD、分类树查询接口，支持树形结构管理
 */
@RestController
@RequestMapping("/v1/asset/categories")
@Tag(name = "资产分类管理", description = "资产分类的增删改查及树形结构管理API")
public class AssetCategoryController {

    private final AssetCategoryNewService assetCategoryNewService;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetCategoryController(AssetCategoryNewService assetCategoryNewService) {
        this.assetCategoryNewService = assetCategoryNewService;
    }

    /**
     * 分页查询资产分类列表
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param categoryName 分类名称（模糊匹配）
     * @param status 状态（1-启用 0-禁用）
     * @param parentId 父级ID
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询资产分类列表", description = "支持按名称、状态、父级分类筛选")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Page<AssetCategoryNew>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "分类名称") @RequestParam(required = false) String categoryName,
            @Parameter(description = "状态：1-启用 0-禁用") @RequestParam(required = false) Integer status,
            @Parameter(description = "父级ID") @RequestParam(required = false) Long parentId) {
        Page<AssetCategoryNew> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AssetCategoryNew> wrapper = new LambdaQueryWrapper<>();
        if (categoryName != null && !categoryName.isEmpty()) {
            wrapper.like(AssetCategoryNew::getCategoryName, categoryName);
        }
        if (status != null) {
            wrapper.eq(AssetCategoryNew::getStatus, status);
        }
        if (parentId != null) {
            wrapper.eq(AssetCategoryNew::getParentId, parentId);
        }
        wrapper.orderByAsc(AssetCategoryNew::getSortOrder);
        Page<AssetCategoryNew> result = assetCategoryNewService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取资产分类详情", description = "根据ID获取资产分类详细信息")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<AssetCategoryNew> getById(@PathVariable Long id) {
        AssetCategoryNew category = assetCategoryNewService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        return Result.success(category);
    }

    /**
     * 创建资产分类
     * @param category 分类数据
     * @return 创建后的分类
     */
    @PostMapping
    @Operation(summary = "创建资产分类", description = "新增一条资产分类记录")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<AssetCategoryNew> create(@Valid @RequestBody AssetCategoryNew category) {
        // 校验分类编码唯一性
        if (category.getCategoryCode() != null && !category.getCategoryCode().isEmpty()) {
            if (!assetCategoryNewService.isCodeAvailable(category.getCategoryCode(), null)) {
                return Result.error("分类编码已存在");
            }
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        assetCategoryNewService.save(category);
        return Result.success(category, "创建成功");
    }

    /**
     * 更新资产分类
     * @param id 分类ID
     * @param category 更新数据
     * @return 更新后的分类
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新资产分类", description = "根据ID更新资产分类信息")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<AssetCategoryNew> update(@PathVariable Long id,
                                            @Valid @RequestBody AssetCategoryNew category) {
        AssetCategoryNew existing = assetCategoryNewService.getById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }
        // 校验分类编码唯一性（排除自身）
        if (category.getCategoryCode() != null && !category.getCategoryCode().isEmpty()) {
            if (!assetCategoryNewService.isCodeAvailable(category.getCategoryCode(), id)) {
                return Result.error("分类编码已存在");
            }
        }
        category.setCategoryId(id);
        assetCategoryNewService.updateById(category);
        return Result.success(assetCategoryNewService.getById(id), "更新成功");
    }

    /**
     * 删除资产分类（逻辑删除）
     * 存在子分类时禁止删除
     * @param id 分类ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除资产分类", description = "根据ID逻辑删除资产分类（存在子分类时禁止删除）")
    @PreAuthorize("hasAuthority('asset:delete') or hasAuthority('*')")
    public Result<Void> delete(@PathVariable Long id) {
        AssetCategoryNew existing = assetCategoryNewService.getById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }
        // 校验是否存在子分类
        List<AssetCategoryNew> children = assetCategoryNewService.getChildrenByParentId(id);
        if (children != null && !children.isEmpty()) {
            return Result.error("存在子分类，无法删除");
        }
        assetCategoryNewService.removeById(id);
        return Result.success();
    }

    /**
     * 获取分类树形结构
     * @return 分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取资产分类树", description = "返回完整的资产分类树形结构")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<AssetCategoryNew>> tree() {
        List<AssetCategoryNew> tree = assetCategoryNewService.getCategoryTree();
        return Result.success(tree);
    }
}
