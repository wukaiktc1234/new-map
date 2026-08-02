package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.WarehouseCreateDTO;
import com.foodtraceability.dto.WarehouseUpdateDTO;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓库服务实现类
 * 实现仓库管理相关的业务方法
 */
@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Warehouse createWarehouse(WarehouseCreateDTO createDTO) {
        // 检查仓库编码是否已存在
        Long count = this.lambdaQuery()
                .eq(Warehouse::getWarehouseCode, createDTO.getWarehouseCode())
                .count();
        if (count > 0) {
            throw new RuntimeException("仓库编码已存在：" + createDTO.getWarehouseCode());
        }

        // 创建仓库实体
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseCode(createDTO.getWarehouseCode());
        warehouse.setWarehouseName(createDTO.getWarehouseName());
        warehouse.setWarehouseType(createDTO.getWarehouseType());
        warehouse.setAddress(createDTO.getAddress());
        warehouse.setManagerId(createDTO.getManagerId());
        warehouse.setPhone(createDTO.getPhone());
        warehouse.setCapacity(createDTO.getCapacity());
        warehouse.setStatus(1); // 默认启用
        warehouse.setRemark(createDTO.getRemark());
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());

        this.save(warehouse);
        return warehouse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Warehouse updateWarehouse(Long warehouseId, WarehouseUpdateDTO updateDTO) {
        // 查询仓库是否存在
        Warehouse existing = this.getById(warehouseId);
        if (existing == null) {
            throw new RuntimeException("仓库不存在：" + warehouseId);
        }

        // 更新字段
        if (updateDTO.getWarehouseName() != null) {
            existing.setWarehouseName(updateDTO.getWarehouseName());
        }
        if (updateDTO.getWarehouseType() != null) {
            existing.setWarehouseType(updateDTO.getWarehouseType());
        }
        if (updateDTO.getAddress() != null) {
            existing.setAddress(updateDTO.getAddress());
        }
        if (updateDTO.getManagerId() != null) {
            existing.setManagerId(updateDTO.getManagerId());
        }
        if (updateDTO.getPhone() != null) {
            existing.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getCapacity() != null) {
            existing.setCapacity(updateDTO.getCapacity());
        }
        if (updateDTO.getRemark() != null) {
            existing.setRemark(updateDTO.getRemark());
        }
        existing.setUpdateTime(LocalDateTime.now());

        this.updateById(existing);
        return this.getById(warehouseId);
    }

    @Override
    public IPage<Warehouse> getWarehousePage(Page<Warehouse> page,
                                           String warehouseName,
                                           String warehouseCode,
                                           Integer warehouseType,
                                           Integer status) {
        return warehouseMapper.selectWarehousePage(page, warehouseName, warehouseCode, warehouseType, status);
    }

    @Override
    public List<Warehouse> getActiveWarehouses() {
        return this.lambdaQuery()
                .eq(Warehouse::getStatus, 1)
                .orderByAsc(Warehouse::getWarehouseCode)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleWarehouseStatus(Long warehouseId, Integer status) {
        Warehouse warehouse = this.getById(warehouseId);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在：" + warehouseId);
        }
        warehouse.setStatus(status);
        warehouse.setUpdateTime(LocalDateTime.now());
        this.updateById(warehouse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long warehouseId) {
        Warehouse warehouse = this.getById(warehouseId);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在：" + warehouseId);
        }
        // 逻辑删除：@TableLogic 注解自动将 deleted 置为 1
        this.removeById(warehouseId);
    }
}
