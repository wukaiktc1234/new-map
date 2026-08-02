package com.foodtraceability.controller.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AssetDepreciationRecord;
import com.foodtraceability.mapper.AssetDepreciationRecordMapper;
import com.foodtraceability.service.AssetMasterNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产折旧管理控制器
 * 提供折旧记录查询、月度折旧计算、折旧汇总统计接口
 */
@RestController
@RequestMapping("/v1/asset/depreciation")
@Tag(name = "资产折旧管理", description = "资产折旧记录查询、月度折旧计算及汇总统计API")
public class AssetDepreciationController {

    private final AssetMasterNewService assetMasterNewService;
    private final AssetDepreciationRecordMapper assetDepreciationRecordMapper;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetDepreciationController(AssetMasterNewService assetMasterNewService,
                                       AssetDepreciationRecordMapper assetDepreciationRecordMapper) {
        this.assetMasterNewService = assetMasterNewService;
        this.assetDepreciationRecordMapper = assetDepreciationRecordMapper;
    }

    /**
     * 分页查询折旧记录列表
     * @param page 页码
     * @param size 每页大小
     * @param assetId 资产ID（可选）
     * @param period 折旧期间（可选，格式 YYYY-MM）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询折旧记录", description = "支持按资产ID、折旧期间筛选")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Page<AssetDepreciationRecord>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "资产ID") @RequestParam(required = false) Long assetId,
            @Parameter(description = "折旧期间（YYYY-MM）") @RequestParam(required = false) String period) {
        Page<AssetDepreciationRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AssetDepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        if (assetId != null) {
            wrapper.eq(AssetDepreciationRecord::getAssetId, assetId);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(AssetDepreciationRecord::getPeriod, period);
        }
        wrapper.orderByDesc(AssetDepreciationRecord::getPeriod);
        Page<AssetDepreciationRecord> result = assetDepreciationRecordMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 查询某资产的折旧记录
     * @param assetId 资产ID
     * @return 折旧记录列表
     */
    @GetMapping("/{assetId}/records")
    @Operation(summary = "查询资产折旧记录", description = "根据资产ID查询其所有折旧记录")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<AssetDepreciationRecord>> recordsByAsset(@PathVariable Long assetId) {
        LambdaQueryWrapper<AssetDepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDepreciationRecord::getAssetId, assetId)
                .orderByDesc(AssetDepreciationRecord::getPeriod);
        List<AssetDepreciationRecord> records = assetDepreciationRecordMapper.selectList(wrapper);
        return Result.success(records);
    }

    /**
     * 执行月度折旧计算
     * @param period 折旧期间（格式 YYYY-MM）
     * @return 折旧的资产数量
     */
    @PostMapping("/execute")
    @Operation(summary = "执行月度折旧计算", description = "对在用资产执行指定期间的月度折旧计算")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Integer> execute(
            @Parameter(description = "折旧期间（YYYY-MM）") @RequestParam String period) {
        if (period == null || period.isEmpty()) {
            return Result.error("折旧期间不能为空");
        }
        int count = assetMasterNewService.executeMonthlyDepreciation(period);
        return Result.success(count, "执行成功，共折旧 " + count + " 个资产");
    }

    /**
     * 折旧汇总统计
     * @param period 折旧期间（可选，不传则统计全部）
     * @return 汇总数据
     */
    @GetMapping("/summary")
    @Operation(summary = "折旧汇总统计", description = "统计指定期间或全部的折旧汇总数据")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> summary(
            @Parameter(description = "折旧期间（YYYY-MM）") @RequestParam(required = false) String period) {
        LambdaQueryWrapper<AssetDepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        if (period != null && !period.isEmpty()) {
            wrapper.eq(AssetDepreciationRecord::getPeriod, period);
        }
        List<AssetDepreciationRecord> records = assetDepreciationRecordMapper.selectList(wrapper);
        Map<String, Object> summary = new HashMap<>();
        long totalDepreciation = 0L;
        long totalOriginalCost = 0L;
        long totalNetValue = 0L;
        for (AssetDepreciationRecord record : records) {
            if (record.getThisPeriodDepreciation() != null) {
                totalDepreciation += record.getThisPeriodDepreciation();
            }
            if (record.getOriginalCost() != null) {
                totalOriginalCost += record.getOriginalCost();
            }
            if (record.getNetBookValueAfter() != null) {
                totalNetValue += record.getNetBookValueAfter();
            }
        }
        summary.put("recordCount", records.size());
        summary.put("totalDepreciation", totalDepreciation);
        summary.put("totalOriginalCost", totalOriginalCost);
        summary.put("totalNetValue", totalNetValue);
        summary.put("period", period);
        return Result.success(summary);
    }
}
