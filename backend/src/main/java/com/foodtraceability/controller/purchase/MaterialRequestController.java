package com.foodtraceability.controller.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.purchase.MaterialRequestCreateDTO;
import com.foodtraceability.dto.purchase.MaterialRequestDTO;
import com.foodtraceability.dto.purchase.MaterialRequestQueryDTO;
import com.foodtraceability.dto.purchase.MaterialRequestUpdateDTO;
import com.foodtraceability.service.purchase.MaterialRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 物资需求提报控制器
 *
 * <p>提供 10 个 RESTful 端点：
 * <ul>
 *   <li>GET    /v1/purchase/material-requests            分页查询</li>
 *   <li>GET    /v1/purchase/material-requests/{id}       查询详情（含明细）</li>
 *   <li>POST   /v1/purchase/material-requests            创建</li>
 *   <li>PUT    /v1/purchase/material-requests/{id}       更新（仅草稿状态）</li>
 *   <li>DELETE /v1/purchase/material-requests/{id}       删除（仅草稿状态）</li>
 *   <li>PUT    /v1/purchase/material-requests/{id}/submit  提交审核（草稿 → 待审核）</li>
 *   <li>PUT    /v1/purchase/material-requests/{id}/approve 审核通过（待审核 → 已审核）</li>
 *   <li>PUT    /v1/purchase/material-requests/{id}/reject  审核驳回（待审核 → 已驳回）</li>
 *   <li>POST   /v1/purchase/material-requests/{id}/convert 转采购申请（已审核 → 已转采购申请）</li>
 *   <li>GET    /v1/purchase/material-requests/statistics   统计各状态数量</li>
 * </ul>
 * </p>
 */
@Tag(name = "物资需求提报", description = "物资需求提报相关接口")
@RestController
@RequestMapping("/v1/purchase/material-requests")
public class MaterialRequestController {

    private static final Logger log = LoggerFactory.getLogger(MaterialRequestController.class);

    private final MaterialRequestService materialRequestService;

    public MaterialRequestController(MaterialRequestService materialRequestService) {
        this.materialRequestService = materialRequestService;
    }

    @Operation(summary = "分页查询物资需求提报列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<IPage<MaterialRequestDTO>> getPage(@Valid MaterialRequestQueryDTO queryDTO) {
        try {
            IPage<MaterialRequestDTO> result = materialRequestService.getPage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询物资需求提报列表失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询物资需求提报列表失败");
        }
    }

    @Operation(summary = "查询物资需求提报详情（含明细）")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<MaterialRequestDTO> getById(@Parameter(description = "提报ID") @PathVariable Long id) {
        try {
            MaterialRequestDTO dto = materialRequestService.getById(id);
            return Result.success(dto);
        } catch (Exception e) {
            log.error("查询物资需求提报详情失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询物资需求提报详情失败");
        }
    }

    @Operation(summary = "创建物资需求提报")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<MaterialRequestDTO> create(@Valid @RequestBody MaterialRequestCreateDTO createDTO) {
        try {
            MaterialRequestDTO dto = materialRequestService.create(createDTO);
            return Result.success(dto);
        } catch (Exception e) {
            log.error("创建物资需求提报失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建物资需求提报失败");
        }
    }

    @Operation(summary = "更新物资需求提报（仅草稿状态可更新）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<MaterialRequestDTO> update(@Parameter(description = "提报ID") @PathVariable Long id,
                                                @Valid @RequestBody MaterialRequestUpdateDTO updateDTO) {
        try {
            MaterialRequestDTO dto = materialRequestService.update(id, updateDTO);
            return Result.success(dto);
        } catch (Exception e) {
            log.error("更新物资需求提报失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新物资需求提报失败");
        }
    }

    @Operation(summary = "删除物资需求提报（仅草稿状态可删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> delete(@Parameter(description = "提报ID") @PathVariable Long id) {
        try {
            materialRequestService.delete(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除物资需求提报失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除物资需求提报失败");
        }
    }

    @Operation(summary = "提交审核（草稿 → 待审核）")
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<Void> submit(@Parameter(description = "提报ID") @PathVariable Long id) {
        try {
            materialRequestService.submit(id);
            return Result.success();
        } catch (Exception e) {
            log.error("提交物资需求提报审核失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "提交审核失败");
        }
    }

    @Operation(summary = "审核通过（待审核 → 已审核）")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> approve(@Parameter(description = "提报ID") @PathVariable Long id) {
        try {
            materialRequestService.approve(id);
            return Result.success();
        } catch (Exception e) {
            log.error("审核通过失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "审核通过失败");
        }
    }

    @Operation(summary = "审核驳回（待审核 → 已驳回）")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> reject(@Parameter(description = "提报ID") @PathVariable Long id,
                                 @Parameter(description = "驳回原因", required = true) @RequestParam String reason) {
        try {
            materialRequestService.reject(id, reason);
            return Result.success();
        } catch (Exception e) {
            log.error("审核驳回失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "审核驳回失败");
        }
    }

    @Operation(summary = "转采购申请（已审核 → 已转采购申请）")
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Map<String, Object>> convert(@Parameter(description = "提报ID") @PathVariable Long id) {
        try {
            Map<String, Object> result = materialRequestService.convertToPurchaseRequest(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("转采购申请失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "转采购申请失败");
        }
    }

    @Operation(summary = "统计各状态数量")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<Map<String, Long>> getStatistics() {
        try {
            Map<String, Long> stats = materialRequestService.getStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询物资需求提报统计失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询统计失败");
        }
    }
}
