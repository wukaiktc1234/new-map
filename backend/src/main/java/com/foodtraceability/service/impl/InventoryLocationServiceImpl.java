package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryLocationCreateDTO;
import com.foodtraceability.dto.InventoryLocationUpdateDTO;
import com.foodtraceability.dto.InventoryLocationVO;
import com.foodtraceability.entity.InventoryLocation;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.InventoryLocationMapper;
import com.foodtraceability.service.InventoryLocationService;
import com.foodtraceability.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 库位服务实现类
 * 实现库位管理相关的业务方法
 */
@Service
public class InventoryLocationServiceImpl extends ServiceImpl<InventoryLocationMapper, InventoryLocation> implements InventoryLocationService {

    private final InventoryLocationMapper locationMapper;
    private final WarehouseService warehouseService;

    public InventoryLocationServiceImpl(InventoryLocationMapper locationMapper,
                                         WarehouseService warehouseService) {
        this.locationMapper = locationMapper;
        this.warehouseService = warehouseService;
    }

    /** 库位类型名称映射 */
    private static final Map<Integer, String> LOCATION_TYPE_NAMES = Map.of(
            1, "货架",
            2, "地面",
            3, "冷藏区",
            4, "冷冻区"
    );

    /** 状态名称映射 */
    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            1, "启用",
            0, "停用"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryLocation createLocation(InventoryLocationCreateDTO createDTO) {
        // 检查库位编码是否已存在
        Long count = this.lambdaQuery()
                .eq(InventoryLocation::getLocationCode, createDTO.getLocationCode())
                .count();
        if (count > 0) {
            throw new RuntimeException("库位编码已存在：" + createDTO.getLocationCode());
        }

        // 检查仓库是否存在
        Warehouse warehouse = warehouseService.getById(createDTO.getWarehouseId());
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在：" + createDTO.getWarehouseId());
        }

        // 创建库位实体
        InventoryLocation location = new InventoryLocation();
        location.setWarehouseId(createDTO.getWarehouseId());
        location.setLocationCode(createDTO.getLocationCode());
        location.setLocationName(createDTO.getLocationName());
        location.setLocationType(createDTO.getLocationType());
        location.setMaxCapacity(createDTO.getMaxCapacity());
        location.setCurrentQuantity(BigDecimal.ZERO);
        location.setStatus(1); // 默认启用
        location.setRemark(createDTO.getRemark());
        location.setCreateTime(LocalDateTime.now());
        location.setUpdateTime(LocalDateTime.now());

        this.save(location);
        return location;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryLocation updateLocation(Long locationId, InventoryLocationUpdateDTO updateDTO) {
        InventoryLocation existing = this.getById(locationId);
        if (existing == null) {
            throw new RuntimeException("库位不存在：" + locationId);
        }

        if (updateDTO.getLocationName() != null) {
            existing.setLocationName(updateDTO.getLocationName());
        }
        if (updateDTO.getLocationType() != null) {
            existing.setLocationType(updateDTO.getLocationType());
        }
        if (updateDTO.getMaxCapacity() != null) {
            existing.setMaxCapacity(updateDTO.getMaxCapacity());
        }
        if (updateDTO.getStatus() != null) {
            existing.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getRemark() != null) {
            existing.setRemark(updateDTO.getRemark());
        }
        existing.setUpdateTime(LocalDateTime.now());

        this.updateById(existing);
        return this.getById(locationId);
    }

    @Override
    public IPage<InventoryLocationVO> getLocationPage(Page<InventoryLocation> page,
                                                       Long warehouseId,
                                                       String locationCode,
                                                       Integer locationType,
                                                       Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<InventoryLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(warehouseId != null, InventoryLocation::getWarehouseId, warehouseId)
               .like(locationCode != null && !locationCode.isEmpty(), InventoryLocation::getLocationCode, locationCode)
               .eq(locationType != null, InventoryLocation::getLocationType, locationType)
               .eq(status != null, InventoryLocation::getStatus, status)
               .orderByAsc(InventoryLocation::getLocationCode);

        IPage<InventoryLocation> locationPage = locationMapper.selectPage(page, wrapper);

        // 批量获取仓库名称
        List<Long> warehouseIds = locationPage.getRecords().stream()
                .map(InventoryLocation::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> warehouseNameMap = Map.of();
        if (!warehouseIds.isEmpty()) {
            warehouseNameMap = warehouseService.listByIds(warehouseIds).stream()
                    .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getWarehouseName, (a, b) -> a));
        }

        // 转换为VO
        Map<Long, String> finalWarehouseNameMap = warehouseNameMap;
        IPage<InventoryLocationVO> voPage = locationPage.convert(loc -> toVO(loc, finalWarehouseNameMap.get(loc.getWarehouseId())));
        return voPage;
    }

    @Override
    public InventoryLocationVO getLocationDetail(Long locationId) {
        InventoryLocation location = this.getById(locationId);
        if (location == null) {
            throw new RuntimeException("库位不存在：" + locationId);
        }

        String warehouseName = null;
        if (location.getWarehouseId() != null) {
            Warehouse warehouse = warehouseService.getById(location.getWarehouseId());
            if (warehouse != null) {
                warehouseName = warehouse.getWarehouseName();
            }
        }

        return toVO(location, warehouseName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleLocationStatus(Long locationId, Integer status) {
        InventoryLocation location = this.getById(locationId);
        if (location == null) {
            throw new RuntimeException("库位不存在：" + locationId);
        }
        location.setStatus(status);
        location.setUpdateTime(LocalDateTime.now());
        this.updateById(location);
    }

    @Override
    public List<InventoryLocation> getLocationsByWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(InventoryLocation::getWarehouseId, warehouseId)
                .eq(InventoryLocation::getStatus, 1)
                .orderByAsc(InventoryLocation::getLocationCode)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLocation(Long locationId) {
        InventoryLocation existing = this.getById(locationId);
        if (existing == null) {
            throw new RuntimeException("库位不存在：" + locationId);
        }
        // 业务核心数据使用逻辑删除，@TableLogic 自动将 deleted 置为 1
        this.removeById(locationId);
    }

    /**
     * 将实体转换为VO
     */
    private InventoryLocationVO toVO(InventoryLocation location, String warehouseName) {
        InventoryLocationVO vo = new InventoryLocationVO();
        vo.setLocationId(location.getLocationId());
        vo.setWarehouseId(location.getWarehouseId());
        vo.setWarehouseName(warehouseName);
        vo.setLocationCode(location.getLocationCode());
        vo.setLocationName(location.getLocationName());
        vo.setLocationType(location.getLocationType());
        vo.setLocationTypeName(LOCATION_TYPE_NAMES.getOrDefault(location.getLocationType(), "未知"));
        vo.setMaxCapacity(location.getMaxCapacity());
        vo.setCurrentQuantity(location.getCurrentQuantity());

        // 计算使用率
        if (location.getMaxCapacity() != null && location.getMaxCapacity().compareTo(BigDecimal.ZERO) > 0
                && location.getCurrentQuantity() != null) {
            BigDecimal rate = location.getCurrentQuantity()
                    .multiply(new BigDecimal("100"))
                    .divide(location.getMaxCapacity(), 2, RoundingMode.HALF_UP);
            vo.setUsageRate(rate);
        } else {
            vo.setUsageRate(BigDecimal.ZERO);
        }

        vo.setStatus(location.getStatus());
        vo.setStatusName(STATUS_NAMES.getOrDefault(location.getStatus(), "未知"));
        vo.setRemark(location.getRemark());
        vo.setCreateTime(location.getCreateTime());
        vo.setUpdateTime(location.getUpdateTime());
        return vo;
    }
}
