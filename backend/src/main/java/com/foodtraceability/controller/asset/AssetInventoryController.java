package com.foodtraceability.controller.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.InventoryCheckAsset;
import com.foodtraceability.entity.InventoryCheckAssetItem;
import com.foodtraceability.mapper.InventoryCheckAssetItemMapper;
import com.foodtraceability.mapper.InventoryCheckAssetMapper;
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
 * 资产盘点管理控制器
 * 提供盘点单的创建、查询、录入结果、完成盘点接口
 */
@RestController
@RequestMapping("/v1/asset/inventory")
@Tag(name = "资产盘点管理", description = "资产盘点单的创建、查询、录入结果及完成盘点API")
public class AssetInventoryController {

    private final InventoryCheckAssetMapper inventoryCheckAssetMapper;
    private final InventoryCheckAssetItemMapper inventoryCheckAssetItemMapper;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public AssetInventoryController(InventoryCheckAssetMapper inventoryCheckAssetMapper,
                                     InventoryCheckAssetItemMapper inventoryCheckAssetItemMapper) {
        this.inventoryCheckAssetMapper = inventoryCheckAssetMapper;
        this.inventoryCheckAssetItemMapper = inventoryCheckAssetItemMapper;
    }

    /**
     * 分页查询盘点单列表
     * @param page 页码
     * @param size 每页大小
     * @param status 盘点状态（0-待盘 1-盘中 2-已审核 3-已完成）
     * @param checkType 盘点类型（1-全面 2-抽样 3-抽查）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询盘点单", description = "支持按状态、类型筛选")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Page<InventoryCheckAsset>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "盘点状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "盘点类型") @RequestParam(required = false) Integer checkType) {
        Page<InventoryCheckAsset> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InventoryCheckAsset> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(InventoryCheckAsset::getStatus, status);
        }
        if (checkType != null) {
            wrapper.eq(InventoryCheckAsset::getCheckType, checkType);
        }
        wrapper.orderByDesc(InventoryCheckAsset::getCreateTime);
        Page<InventoryCheckAsset> result = inventoryCheckAssetMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 获取盘点单详情（含明细）
     * @param id 盘点单ID
     * @return 盘点单详情（含明细列表）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取盘点单详情", description = "返回盘点单信息及其明细列表")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        InventoryCheckAsset checkAsset = inventoryCheckAssetMapper.selectById(id);
        if (checkAsset == null) {
            return Result.error("盘点单不存在");
        }
        LambdaQueryWrapper<InventoryCheckAssetItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryCheckAssetItem::getCheckId, id)
                .orderByAsc(InventoryCheckAssetItem::getItemId);
        List<InventoryCheckAssetItem> items = inventoryCheckAssetItemMapper.selectList(itemWrapper);
        Map<String, Object> detail = new HashMap<>();
        detail.put("check", checkAsset);
        detail.put("items", items);
        return Result.success(detail);
    }

    /**
     * 创建盘点单
     * @param checkAsset 盘点单数据
     * @return 创建后的盘点单
     */
    @PostMapping
    @Operation(summary = "创建盘点单", description = "新增一条资产盘点单")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<InventoryCheckAsset> create(@Valid @RequestBody InventoryCheckAsset checkAsset) {
        if (checkAsset.getStatus() == null) {
            checkAsset.setStatus(0);
        }
        if (checkAsset.getTotalAssetsCount() == null) {
            checkAsset.setTotalAssetsCount(0);
        }
        if (checkAsset.getActualCount() == null) {
            checkAsset.setActualCount(0);
        }
        if (checkAsset.getDiffCount() == null) {
            checkAsset.setDiffCount(0);
        }
        inventoryCheckAssetMapper.insert(checkAsset);
        return Result.success(checkAsset, "创建成功");
    }

    /**
     * 更新盘点单
     * @param id 盘点单ID
     * @param checkAsset 更新数据
     * @return 更新后的盘点单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新盘点单", description = "根据ID更新盘点单信息")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<InventoryCheckAsset> update(@PathVariable Long id,
                                               @Valid @RequestBody InventoryCheckAsset checkAsset) {
        InventoryCheckAsset existing = inventoryCheckAssetMapper.selectById(id);
        if (existing == null) {
            return Result.error("盘点单不存在");
        }
        checkAsset.setCheckId(id);
        inventoryCheckAssetMapper.updateById(checkAsset);
        return Result.success(inventoryCheckAssetMapper.selectById(id), "更新成功");
    }

    /**
     * 录入盘点结果
     * @param id 盘点单ID
     * @param itemId 明细ID
     * @param item 盘点结果数据
     * @return 更新后的明细
     */
    @PostMapping("/{id}/items/{itemId}")
    @Operation(summary = "录入盘点结果", description = "录入某盘点单某明细的实际盘点结果")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<InventoryCheckAssetItem> inputItem(@PathVariable Long id,
                                                       @PathVariable Long itemId,
                                                       @Valid @RequestBody InventoryCheckAssetItem item) {
        InventoryCheckAssetItem existing = inventoryCheckAssetItemMapper.selectById(itemId);
        if (existing == null) {
            return Result.error("盘点明细不存在");
        }
        if (!existing.getCheckId().equals(id)) {
            return Result.error("明细不属于该盘点单");
        }
        item.setItemId(itemId);
        item.setCheckId(id);
        inventoryCheckAssetItemMapper.updateById(item);
        return Result.success(inventoryCheckAssetItemMapper.selectById(itemId), "录入成功");
    }

    /**
     * 完成盘点
     * @param id 盘点单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成盘点", description = "将盘点单状态置为已完成，并汇总盘点结果")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> complete(@PathVariable Long id) {
        InventoryCheckAsset existing = inventoryCheckAssetMapper.selectById(id);
        if (existing == null) {
            return Result.error("盘点单不存在");
        }
        // 统计实盘数量与差异数量
        LambdaQueryWrapper<InventoryCheckAssetItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryCheckAssetItem::getCheckId, id);
        List<InventoryCheckAssetItem> items = inventoryCheckAssetItemMapper.selectList(itemWrapper);
        int actualCount = items != null ? items.size() : 0;
        int diffCount = 0;
        for (InventoryCheckAssetItem item : items) {
            if (item.getActualStatus() != null && item.getExpectedStatus() != null
                    && !item.getActualStatus().equals(item.getExpectedStatus())) {
                diffCount++;
            }
        }
        existing.setActualCount(actualCount);
        existing.setDiffCount(diffCount);
        existing.setStatus(3);
        inventoryCheckAssetMapper.updateById(existing);
        return Result.success();
    }
}
