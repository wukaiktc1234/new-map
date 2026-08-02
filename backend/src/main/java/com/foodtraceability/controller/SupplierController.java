package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SupplierCreateDTO;
import com.foodtraceability.dto.SupplierQueryDTO;
import com.foodtraceability.dto.SupplierUpdateDTO;
import com.foodtraceability.dto.SupplierVO;
import com.foodtraceability.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 供应商管理控制器
 * 提供供应商的RESTful API接口
 */
@Tag(name = "供应商管理", description = "供应商相关接口")
@RestController
@RequestMapping("/v1/suppliers")
public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Operation(summary = "分页查询供应商列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<IPage<SupplierVO>> getSupplierPage(@Valid SupplierQueryDTO queryDTO) {
        try {
            IPage<SupplierVO> result = supplierService.getSupplierPage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询供应商列表失败", e);
            return Result.error("查询供应商列表失败");
        }
    }

    @Operation(summary = "获取供应商下拉列表（不分页）")
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<?> getSupplierList(
            @Parameter(description = "状态（可选）") @RequestParam(required = false) Integer status) {
        try {
            var list = supplierService.getSupplierList(status);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取供应商列表失败", e);
            return Result.error("获取供应商列表失败");
        }
    }

    @Operation(summary = "获取供应商详情")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<SupplierVO> getSupplierById(@PathVariable Long id) {
        try {
            SupplierVO vo = supplierService.getSupplierDetail(id);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取供应商详情失败，ID：{}", id, e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "创建供应商")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<SupplierVO> createSupplier(@Valid @RequestBody SupplierCreateDTO createDTO) {
        try {
            SupplierVO vo = supplierService.createSupplier(createDTO);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("创建供应商失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建供应商失败");
        }
    }

    @Operation(summary = "更新供应商信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<SupplierVO> updateSupplier(@PathVariable Long id,
                                              @Valid @RequestBody SupplierUpdateDTO updateDTO) {
        try {
            SupplierVO vo = supplierService.updateSupplier(id, updateDTO);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("更新供应商失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新供应商失败");
        }
    }

    @Operation(summary = "删除供应商（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除供应商失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除供应商失败");
        }
    }

    @Operation(summary = "批量删除供应商")
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> batchDeleteSuppliers(@RequestBody List<Long> ids) {
        try {
            supplierService.batchDeleteSuppliers(ids);
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除供应商失败", e);
            return Result.error("批量删除供应商失败");
        }
    }

    @Operation(summary = "更新供应商状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                       @RequestParam Integer status) {
        try {
            supplierService.updateSupplierStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新供应商状态失败，ID：{}，状态：{}", id, status, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新状态失败");
        }
    }

    @Operation(summary = "获取供应商统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = supplierService.getStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取供应商统计信息失败", e);
            return Result.error("获取统计信息失败");
        }
    }
}
