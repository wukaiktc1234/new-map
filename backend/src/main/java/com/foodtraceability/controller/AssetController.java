package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.asset.AssetMasterCreateDTO;
import com.foodtraceability.dto.asset.AssetMasterQueryDTO;
import com.foodtraceability.dto.asset.AssetMasterUpdateDTO;
import com.foodtraceability.dto.asset.AssetMasterVO;
import com.foodtraceability.entity.AssetMaster;
import com.foodtraceability.entity.AssetFlowRecord;
import com.foodtraceability.service.AssetMasterService;
import com.foodtraceability.mapper.AssetFlowRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/asset")
@Tag(name = "资产管理", description = "资产全生命周期管理API")
public class AssetController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetController.class);
    private final AssetMasterService assetMasterService;
    private final AssetFlowRecordMapper assetFlowRecordMapper;

    // ==================== 概览与列表 ====================

    @GetMapping("/overview")
    @Operation(summary = "资产概览数据")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            List<Map<String, Object>> statusStats = assetMasterService.countByStatus();
            List<Map<String, Object>> typeStats = assetMasterService.countByType();
            List<Map<String, Object>> storeStats = assetMasterService.countByStore();
            int total = 0;
            int idle = 0;
            int inUse = 0;
            int repairing = 0;
            int damaged = 0;
            for (Map<String, Object> stat : statusStats) {
                String status = (String) stat.get("status");
                int count = ((Number) stat.get("count")).intValue();
                total += count;
                if ("idle".equals(status)) idle = count;
                else if ("in_use".equals(status)) inUse = count;
                else if ("repairing".equals(status)) repairing = count;
                else if ("damaged".equals(status)) damaged = count;
            }
            overview.put("total", total);
            overview.put("idle", idle);
            overview.put("inUse", inUse);
            overview.put("repairing", repairing);
            overview.put("damaged", damaged);
            overview.put("statusStats", statusStats);
            overview.put("typeStats", typeStats);
            overview.put("storeStats", storeStats);
            return Result.success(overview);
        } catch (Exception e) {
            // asset_master 表不存在或查询失败时，返回空概览数据
            log.warn("资产概览查询失败，返回空数据: {}", e.getMessage());
            Map<String, Object> emptyOverview = new HashMap<>();
            emptyOverview.put("total", 0);
            emptyOverview.put("idle", 0);
            emptyOverview.put("inUse", 0);
            emptyOverview.put("repairing", 0);
            emptyOverview.put("damaged", 0);
            emptyOverview.put("statusStats", new ArrayList<>());
            emptyOverview.put("typeStats", new ArrayList<>());
            emptyOverview.put("storeStats", new ArrayList<>());
            return Result.success(emptyOverview);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "资产列表（分页）")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<Page<AssetMasterVO>> list(AssetMasterQueryDTO queryDTO) {
        try {
            int pageNum = queryDTO.getCurrent() != null ? queryDTO.getCurrent() : 1;
            int pageSize = queryDTO.getSize() != null ? queryDTO.getSize() : 20;
            Page<AssetMaster> pageParam = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<AssetMaster> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AssetMaster::getDeleted, 0);
            if (queryDTO.getAssetType() != null && !queryDTO.getAssetType().isEmpty()) {
                wrapper.eq(AssetMaster::getAssetType, queryDTO.getAssetType());
            }
            if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
                wrapper.eq(AssetMaster::getStatus, queryDTO.getStatus());
            }
            if (queryDTO.getStoreId() != null) {
                wrapper.eq(AssetMaster::getStoreId, queryDTO.getStoreId());
            }
            if (queryDTO.getCategoryId() != null) {
                wrapper.eq(AssetMaster::getCategoryId, queryDTO.getCategoryId());
            }
            if (queryDTO.getCustodianId() != null) {
                wrapper.eq(AssetMaster::getCustodianId, queryDTO.getCustodianId());
            }
            if (queryDTO.getAssetCode() != null && !queryDTO.getAssetCode().isEmpty()) {
                wrapper.like(AssetMaster::getAssetCode, queryDTO.getAssetCode());
            }
            if (queryDTO.getAssetName() != null && !queryDTO.getAssetName().isEmpty()) {
                wrapper.like(AssetMaster::getAssetName, queryDTO.getAssetName());
            }
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                wrapper.and(w -> w.like(AssetMaster::getAssetCode, queryDTO.getKeyword())
                        .or().like(AssetMaster::getAssetName, queryDTO.getKeyword()));
            }
            wrapper.orderByDesc(AssetMaster::getCreateTime);
            Page<AssetMaster> result = assetMasterService.page(pageParam, wrapper);
            Page<AssetMasterVO> voPage = (Page<AssetMasterVO>) result.convert(this::toVO);
            return Result.success(voPage);
        } catch (Exception e) {
            // asset_master 表不存在或查询失败时，返回空分页数据
            log.warn("资产列表查询失败，返回空数据: {}", e.getMessage());
            int pageNum = queryDTO.getCurrent() != null ? queryDTO.getCurrent() : 1;
            int pageSize = queryDTO.getSize() != null ? queryDTO.getSize() : 20;
            Page<AssetMasterVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setRecords(new ArrayList<>());
            emptyPage.setTotal(0);
            return Result.success(emptyPage);
        }
    }

    // ==================== 详情查询 ====================

    @GetMapping("/{id}")
    @Operation(summary = "资产详情")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<AssetMasterVO> getById(@PathVariable Long id) {
        AssetMaster asset = assetMasterService.getById(id);
        if (asset == null) {
            return Result.error("资产不存在");
        }
        return Result.success(toVO(asset));
    }

    @GetMapping("/code/{assetCode}")
    @Operation(summary = "根据资产编码查询")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<AssetMasterVO> getByCode(@PathVariable String assetCode) {
        AssetMaster asset = assetMasterService.findByAssetCode(assetCode);
        if (asset == null) {
            return Result.error("资产不存在");
        }
        return Result.success(toVO(asset));
    }

    // ==================== 新增与更新 ====================

    @PostMapping
    @Operation(summary = "新增资产")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<AssetMasterVO> create(@Valid @RequestBody AssetMasterCreateDTO createDTO) {
        AssetMaster asset = toEntity(createDTO);
        if (asset.getAssetCode() == null || asset.getAssetCode().isEmpty()) {
            asset.setAssetCode(assetMasterService.generateAssetCode(asset.getAssetType()));
        }
        if (asset.getQrCode() == null) {
            asset.setQrCode(asset.getAssetCode());
        }
        if (asset.getStatus() == null) {
            asset.setStatus("idle");
        }
        if (asset.getUseCount() == null) {
            asset.setUseCount(0);
        }
        assetMasterService.save(asset);
        log.info("新增资产: {}", asset.getAssetCode());
        return Result.success(toVO(asset), "新增成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新资产")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<AssetMasterVO> update(@PathVariable Long id,
                                        @Valid @RequestBody AssetMasterUpdateDTO updateDTO) {
        AssetMaster existing = assetMasterService.getById(id);
        if (existing == null) {
            return Result.error("资产不存在");
        }
        mergeUpdate(existing, updateDTO);
        existing.setId(id);
        assetMasterService.updateById(existing);
        log.info("更新资产: {}", existing.getAssetCode());
        return Result.success(toVO(existing), "更新成功");
    }

    // ==================== 删除 ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资产")
    @PreAuthorize("hasAuthority('asset:delete') or hasAuthority('*')")
    public Result<Void> delete(@PathVariable Long id) {
        AssetMaster asset = assetMasterService.getById(id);
        if (asset != null && "in_use".equals(asset.getStatus())) {
            return Result.error("使用中的资产无法删除");
        }
        assetMasterService.removeById(id);
        log.info("删除资产: {}", id);
        return Result.success();
    }

    // ==================== 操作类接口 ====================

    @PostMapping("/{id}/allocate")
    @Operation(summary = "分配门店")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> allocate(@PathVariable Long id,
                                 @Parameter(description = "门店ID") @RequestParam Long storeId,
                                 @Parameter(description = "门店名称") @RequestParam String storeName,
                                 @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.allocateToStore(id, storeId, storeName, operatorName);
        return Result.success();
    }

    @PostMapping("/bind-order")
    @Operation(summary = "绑定订单")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> bindOrder(@Parameter(description = "资产编码") @RequestParam String assetCode,
                                  @Parameter(description = "订单ID") @RequestParam String orderId,
                                  @Parameter(description = "厨房订单ID") @RequestParam(required = false) Long kitchenOrderId,
                                  @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.bindOrder(assetCode, orderId, kitchenOrderId, operatorName);
        return Result.success();
    }

    @PostMapping("/release")
    @Operation(summary = "释放资产")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> release(@Parameter(description = "资产编码") @RequestParam String assetCode,
                                @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.releaseAsset(assetCode, operatorName);
        return Result.success();
    }

    @PostMapping("/{id}/repair")
    @Operation(summary = "开始维修")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> startRepair(@PathVariable Long id,
                                    @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.startRepair(id, operatorName);
        return Result.success();
    }

    @PostMapping("/{id}/complete-repair")
    @Operation(summary = "完成维修")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<Void> completeRepair(@PathVariable Long id,
                                       @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.completeRepair(id, operatorName);
        return Result.success();
    }

    @PostMapping("/{id}/scrap")
    @Operation(summary = "资产报废")
    @PreAuthorize("hasAuthority('asset:delete') or hasAuthority('*')")
    public Result<Void> scrap(@PathVariable Long id,
                              @Parameter(description = "报废原因") @RequestParam String reason,
                              @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        assetMasterService.scrapAsset(id, reason, operatorName);
        return Result.success();
    }

    @GetMapping("/{id}/flow-records")
    @Operation(summary = "资产流转记录")
    @PreAuthorize("hasAuthority('asset:view') or hasAuthority('*')")
    public Result<List<AssetFlowRecord>> getFlowRecords(@PathVariable Long id) {
        List<AssetFlowRecord> records = assetFlowRecordMapper.findAllByAssetId(id);
        return Result.success(records);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建资产")
    @PreAuthorize("hasAuthority('asset:manage') or hasAuthority('*')")
    public Result<String> batchCreate(@Parameter(description = "数量") @RequestParam int count,
                                      @Parameter(description = "资产类型") @RequestParam(defaultValue = "tray") String assetType,
                                      @Parameter(description = "编码前缀") @RequestParam(defaultValue = "TRAY") String prefix,
                                      @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
                                      @Parameter(description = "门店名称") @RequestParam(required = false) String storeName) {
        for (int i = 1; i <= count; i++) {
            AssetMaster asset = new AssetMaster();
            asset.setAssetCode(String.format("%s%03d", prefix, i));
            asset.setAssetName(i + "号" + getAssetTypeName(assetType));
            asset.setAssetType(assetType);
            asset.setQrCode(asset.getAssetCode());
            asset.setStatus("idle");
            asset.setUseCount(0);
            asset.setStoreId(storeId);
            asset.setStoreName(storeName);
            assetMasterService.save(asset);
        }
        log.info("批量创建资产: {} 个, 类型: {}", count, assetType);
        return Result.success("成功创建 " + count + " 个资产");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * CreateDTO → AssetMaster 实体转换
     */
    private AssetMaster toEntity(AssetMasterCreateDTO dto) {
        AssetMaster entity = new AssetMaster();
        entity.setAssetCode(dto.getAssetCode());
        entity.setAssetName(dto.getAssetName());
        entity.setCategoryId(dto.getCategoryId());
        entity.setSpecification(dto.getSpecification());
        // brand 字段在 AssetMaster 不存在，跳过
        entity.setPurchaseDate(dto.getPurchaseDate());
        // originalCostYuan 是元单位，originalValue 存储分（数据库original_cost字段为BIGINT分单位）
        if (dto.getOriginalCostYuan() != null) {
            entity.setOriginalValue(dto.getOriginalCostYuan().multiply(BigDecimal.valueOf(100)));
        }
        entity.setUsefulLifeMonths(dto.getUsefulLifeMonths());
        if (dto.getDepreciationMethod() != null) {
            entity.setDepreciationMethod(dto.getDepreciationMethod());
        }
        entity.setLocation(dto.getLocation());
        entity.setStoreId(dto.getStoreId());
        entity.setQrCode(dto.getQrCode());
        entity.setImageUrl(dto.getImageUrl());
        return entity;
    }

    /**
     * 将 UpdateDTO 非空字段合并到已有实体
     */
    private void mergeUpdate(AssetMaster entity, AssetMasterUpdateDTO dto) {
        if (dto.getAssetName() != null) {
            entity.setAssetName(dto.getAssetName());
        }
        if (dto.getCategoryId() != null) {
            entity.setCategoryId(dto.getCategoryId());
        }
        if (dto.getSpecification() != null) {
            entity.setSpecification(dto.getSpecification());
        }
        if (dto.getPurchaseDate() != null) {
            entity.setPurchaseDate(dto.getPurchaseDate());
        }
        if (dto.getOriginalValue() != null) {
            // 更新DTO原值单位为元，数据库存储分
            entity.setOriginalValue(dto.getOriginalValue().multiply(BigDecimal.valueOf(100)));
        }
        if (dto.getUsefulLifeMonths() != null) {
            entity.setUsefulLifeMonths(dto.getUsefulLifeMonths());
        }
        if (dto.getDepreciationMethod() != null) {
            entity.setDepreciationMethod(dto.getDepreciationMethod());
        }
        if (dto.getLocation() != null) {
            entity.setLocation(dto.getLocation());
        }
        if (dto.getStoreId() != null) {
            entity.setStoreId(dto.getStoreId());
        }
        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    /**
     * AssetMaster 实体 → AssetMasterVO 转换
     */
    private AssetMasterVO toVO(AssetMaster entity) {
        if (entity == null) return null;
        AssetMasterVO vo = new AssetMasterVO();
        vo.setId(entity.getId());
        vo.setAssetCode(entity.getAssetCode());
        vo.setAssetName(entity.getAssetName());
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setSpecification(entity.getSpecification());
        vo.setBrand(null); // AssetMaster 表无 brand 字段
        vo.setUnit(entity.getUnit());
        vo.setPurchaseDate(entity.getPurchaseDate());
        // originalValue 以分存储，VO返回元
        if (entity.getOriginalValue() != null) {
            vo.setOriginalValue(entity.getOriginalValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        // netValue 以分存储，VO返回元
        if (entity.getNetValue() != null) {
            vo.setNetValue(entity.getNetValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        // accumulatedDepreciation 以分存储，VO返回元
        if (entity.getAccumulatedDepreciation() != null) {
            vo.setAccumulatedDepreciation(entity.getAccumulatedDepreciation().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        vo.setResidualValue(entity.getResidualValue());
        if (entity.getDepreciationMethod() != null) {
            vo.setDepreciationMethod(String.valueOf(entity.getDepreciationMethod()));
        }
        vo.setUsefulLifeMonths(entity.getUsefulLifeMonths());
        vo.setStatus(entity.getStatus());
        vo.setStoreId(entity.getStoreId());
        vo.setStoreName(entity.getStoreName());
        vo.setLocation(entity.getLocation());
        vo.setAssetType(entity.getAssetType());
        vo.setQrCode(entity.getQrCode());
        vo.setUseCount(entity.getUseCount());
        vo.setCurrentOrderId(entity.getCurrentOrderId());
        vo.setCurrentKitchenOrderId(entity.getCurrentKitchenOrderId());
        vo.setCustodianName(entity.getCustodianName());
        vo.setImageUrl(entity.getImageUrl());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String getAssetTypeName(String assetType) {
        switch (assetType) {
            case "tray":
                return "托盘";
            case "equipment":
                return "设备";
            case "furniture":
                return "家具";
            default:
                return "资产";
        }
    }

    public AssetController(final AssetMasterService assetMasterService, final AssetFlowRecordMapper assetFlowRecordMapper) {
        this.assetMasterService = assetMasterService;
        this.assetFlowRecordMapper = assetFlowRecordMapper;
    }
}
