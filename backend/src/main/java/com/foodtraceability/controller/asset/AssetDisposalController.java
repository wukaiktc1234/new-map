package com.foodtraceability.controller.asset;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.AssetMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资产处置管理控制器
 * 提供资产处置申请的查询、创建、审批接口
 */
@RestController
@RequestMapping("/v1/asset/disposal")
@Tag(name = "资产处置管理", description = "资产处置申请的查询、创建、审批API")
public class AssetDisposalController {

    private final AssetMasterService assetMasterService;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetDisposalController(AssetMasterService assetMasterService) {
        this.assetMasterService = assetMasterService;
    }

    /**
     * 分页查询处置列表
     * @param page 页码
     * @param size 每页大小
     * @return 处置列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询处置列表", description = "查询资产处置申请列表")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        List<Map<String, Object>> list = assetMasterService.getDisposalList(page, size);
        return Result.success(list);
    }

    /**
     * 获取处置详情
     * @param id 处置单ID
     * @return 处置详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取处置详情", description = "根据处置单ID获取详细信息")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> detail = assetMasterService.getDisposalDetail(id);
        if (detail == null) {
            return Result.error("处置单不存在");
        }
        return Result.success(detail);
    }

    /**
     * 创建处置申请
     * @param assetId 资产ID
     * @param type 处置类型
     * @param reason 处置原因
     * @param handler 处理人
     * @return 处置单ID
     */
    @PostMapping
    @Operation(summary = "创建处置申请", description = "创建资产处置申请单")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Long> create(
            @Parameter(description = "资产ID") @RequestParam Long assetId,
            @Parameter(description = "处置类型") @RequestParam String type,
            @Parameter(description = "处置原因") @RequestParam String reason,
            @Parameter(description = "处理人") @RequestParam(required = false) String handler) {
        if (assetId == null) {
            return Result.error("资产ID不能为空");
        }
        if (type == null || type.isEmpty()) {
            return Result.error("处置类型不能为空");
        }
        Long disposalId = assetMasterService.createDisposal(assetId, type, reason, handler);
        return Result.success(disposalId, "创建成功");
    }

    /**
     * 审批处置申请
     * @param id 处置单ID
     * @param approved 是否通过
     * @param comment 审批意见
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批处置申请", description = "对资产处置申请进行审批")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> approve(@PathVariable Long id,
                                 @Parameter(description = "是否通过") @RequestParam Boolean approved,
                                 @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        if (approved == null) {
            return Result.error("审批结果不能为空");
        }
        boolean success = assetMasterService.approveDisposal(id, approved, comment);
        if (!success) {
            return Result.error("审批失败");
        }
        return Result.success();
    }
}
