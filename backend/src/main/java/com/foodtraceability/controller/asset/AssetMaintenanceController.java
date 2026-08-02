package com.foodtraceability.controller.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AssetFlowRecordNew;
import com.foodtraceability.mapper.AssetFlowRecordNewMapper;
import com.foodtraceability.service.AssetMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产维护管理控制器
 * 提供资产维修记录查询、开始维修、完成维修接口
 * 维修记录来源于 AssetFlowRecord 中 flow_type=4（维修）的记录
 */
@RestController
@RequestMapping("/v1/asset/maintenance")
@Tag(name = "资产维护管理", description = "资产维修记录查询及维修操作API")
public class AssetMaintenanceController {

    /** 变动类型：维修 */
    private static final Integer FLOW_TYPE_REPAIR = 4;

    private final AssetMasterService assetMasterService;
    private final AssetFlowRecordNewMapper assetFlowRecordNewMapper;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetMaintenanceController(AssetMasterService assetMasterService,
                                       AssetFlowRecordNewMapper assetFlowRecordNewMapper) {
        this.assetMasterService = assetMasterService;
        this.assetFlowRecordNewMapper = assetFlowRecordNewMapper;
    }

    /**
     * 分页查询维护记录
     * @param page 页码
     * @param size 每页大小
     * @param assetId 资产ID（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询维护记录", description = "查询资产维修记录（flow_type=4）")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Page<AssetFlowRecordNew>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "资产ID") @RequestParam(required = false) Long assetId) {
        Page<AssetFlowRecordNew> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AssetFlowRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFlowRecordNew::getFlowType, FLOW_TYPE_REPAIR);
        if (assetId != null) {
            wrapper.eq(AssetFlowRecordNew::getAssetId, assetId);
        }
        wrapper.orderByDesc(AssetFlowRecordNew::getFlowDate);
        Page<AssetFlowRecordNew> result = assetFlowRecordNewMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 获取维护详情
     * @param id 维护记录ID（flow_id）
     * @return 维护记录详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取维护详情", description = "根据维护记录ID获取详细信息")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<AssetFlowRecordNew> getById(@PathVariable Long id) {
        AssetFlowRecordNew record = assetFlowRecordNewMapper.selectById(id);
        if (record == null) {
            return Result.error("维护记录不存在");
        }
        if (record.getFlowType() == null || !record.getFlowType().equals(FLOW_TYPE_REPAIR)) {
            return Result.error("该记录不是维修类型");
        }
        return Result.success(record);
    }

    /**
     * 开始维修
     * @param assetId 资产ID
     * @param operatorName 操作人
     * @return 操作结果
     */
    @PostMapping("/{assetId}/start")
    @Operation(summary = "开始维修", description = "将资产状态置为维修中")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> start(@PathVariable Long assetId,
                              @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.startRepair(assetId, operatorName);
        return Result.success();
    }

    /**
     * 完成维修
     * @param assetId 资产ID
     * @param operatorName 操作人
     * @return 操作结果
     */
    @PostMapping("/{assetId}/complete")
    @Operation(summary = "完成维修", description = "将资产状态从维修中恢复为闲置")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> complete(@PathVariable Long assetId,
                                  @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.completeRepair(assetId, operatorName);
        return Result.success();
    }

    /**
     * 查询某资产的维护历史
     * @param assetId 资产ID
     * @return 维护记录列表
     */
    @GetMapping("/{assetId}/records")
    @Operation(summary = "查询资产维护历史", description = "根据资产ID查询其所有维修记录")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<AssetFlowRecordNew>> recordsByAsset(@PathVariable Long assetId) {
        LambdaQueryWrapper<AssetFlowRecordNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFlowRecordNew::getAssetId, assetId)
                .eq(AssetFlowRecordNew::getFlowType, FLOW_TYPE_REPAIR)
                .orderByDesc(AssetFlowRecordNew::getFlowDate);
        List<AssetFlowRecordNew> records = assetFlowRecordNewMapper.selectList(wrapper);
        return Result.success(records);
    }
}
