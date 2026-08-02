package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InventoryUnit;
import com.foodtraceability.mapper.InventoryUnitMapper;
import com.foodtraceability.service.InventoryUnitService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 计量单位服务实现类
 * 实现计量单位管理相关的业务方法
 */
@Service
public class InventoryUnitServiceImpl extends ServiceImpl<InventoryUnitMapper, InventoryUnit> implements InventoryUnitService {
    
    @Override
    public InventoryUnit createInventoryUnit(InventoryUnit inventoryUnit) {
        this.save(inventoryUnit);
        return inventoryUnit;
    }
    
    @Override
    public InventoryUnit updateInventoryUnit(Long id, InventoryUnit inventoryUnit) {
        inventoryUnit.setId(id);
        this.updateById(inventoryUnit);
        return this.getById(id);
    }
    
    @Override
    public InventoryUnit getInventoryUnitById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public void deleteInventoryUnit(Long id) {
        this.removeById(id);
    }
    
    @Override
    public IPage<InventoryUnit> getInventoryUnitPage(Page<InventoryUnit> page, String name, String code, String type, Boolean status) {
        // 这里可以根据需要实现自定义的分页查询逻辑
        return this.lambdaQuery()
                .like(name != null, InventoryUnit::getName, name)
                .like(code != null, InventoryUnit::getCode, code)
                .eq(type != null, InventoryUnit::getType, type)
                .eq(status != null, InventoryUnit::getStatus, status)
                .page(page);
    }
    
    @Override
    public List<InventoryUnit> getAllInventoryUnits() {
        return this.list();
    }
    
    @Override
    public List<InventoryUnit> getInventoryUnitsByType(String type) {
        return this.lambdaQuery()
                .eq(InventoryUnit::getType, type)
                .list();
    }
}