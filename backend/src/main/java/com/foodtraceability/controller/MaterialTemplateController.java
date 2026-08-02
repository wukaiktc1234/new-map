package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.MaterialTemplate;
import com.foodtraceability.service.MaterialTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/material-templates")
@Tag(name = "原料模板管理")
public class MaterialTemplateController {


    public MaterialTemplateController(MaterialTemplateService materialTemplateService) {
        this.materialTemplateService = materialTemplateService;
    }

    private final MaterialTemplateService materialTemplateService;

    @GetMapping("/list")
    @Operation(summary = "获取所有原料模板")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTemplate>> getAllTemplates() {
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplate::getStatus, "active")
               .orderByAsc(MaterialTemplate::getCategory)
               .orderByAsc(MaterialTemplate::getMaterialName);
        return Result.success(list(wrapper));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询原料模板")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<MaterialTemplate>> getPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "原料名称") @RequestParam(required = false) String name) {

        Page<MaterialTemplate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplate::getStatus, "active");
        wrapper.eq(MaterialTemplate::getDeleted, 0);

        if (category != null && !category.isEmpty()) {
            wrapper.eq(MaterialTemplate::getCategory, category);
        }
        if (name != null && !name.isEmpty()) {
            wrapper.like(MaterialTemplate::getMaterialName, name);
        }

        wrapper.orderByAsc(MaterialTemplate::getCategory)
               .orderByAsc(MaterialTemplate::getMaterialName);

        return Result.success(materialTemplateService.page(pageParam, wrapper));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有分类")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<String>> getCategories() {
        return Result.success(materialTemplateService.getAllCategories());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类获取模板")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTemplate>> getByCategory(@PathVariable String category) {
        return Result.success(materialTemplateService.getByCategory(category));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索原料模板")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTemplate>> search(@Parameter(description = "原料名称") @RequestParam String name) {
        return Result.success(materialTemplateService.searchByName(name));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<MaterialTemplate> getById(@PathVariable Long id) {
        return Result.success(materialTemplateService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建原料模板")
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<MaterialTemplate> create(@RequestBody MaterialTemplate template) {
        template.setStatus("active");
        materialTemplateService.save(template);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新原料模板")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<MaterialTemplate> update(@PathVariable Long id, @RequestBody MaterialTemplate template) {
        template.setId(id);
        materialTemplateService.updateById(template);
        return Result.success(template);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除原料模板")
    @PreAuthorize("hasAuthority('trace:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(materialTemplateService.removeById(id));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "根据条码查询原料模板")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<MaterialTemplate> getByBarcode(@PathVariable String barcode) {
        MaterialTemplate template = materialTemplateService.getByBarcode(barcode);
        if (template == null) {
            return Result.error("未找到匹配的模板");
        }
        return Result.success(template);
    }

    private List<MaterialTemplate> list(LambdaQueryWrapper<MaterialTemplate> wrapper) {
        return materialTemplateService.list(wrapper);
    }
}
