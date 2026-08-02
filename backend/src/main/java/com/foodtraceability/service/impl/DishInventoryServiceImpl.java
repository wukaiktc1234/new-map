package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DishInventory;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.mapper.DishInventoryMapper;
import com.foodtraceability.service.DishInventoryService;
import com.foodtraceability.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DishInventoryServiceImpl extends ServiceImpl<DishInventoryMapper, DishInventory> implements DishInventoryService {


    public DishInventoryServiceImpl(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    private final InventoryService inventoryService;

    @Override
    public List<DishInventory> getDishInventories(String dishId) {
        return baseMapper.selectByDishId(dishId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishInventory addDishInventory(String dishId, Long inventoryId, BigDecimal quantity, String unit, String remark) {
        // 修复：使用MyBatis Plus标准方法 getById（原代码调用了不存在的 getInventoryById 方法）
        Inventory inventory = inventoryService.getById(inventoryId);
        if (inventory == null) {
            throw new RuntimeException("库存产品不存在");
        }

        DishInventory dishInventory = new DishInventory();
        dishInventory.setDishId(dishId);
        dishInventory.setInventoryId(inventoryId);
        // 修复：使用实体类实际字段名 materialName（原代码使用了不存在的 getProductName 方法）
        dishInventory.setInventoryName(inventory.getMaterialName());
        dishInventory.setQuantity(quantity);
        dishInventory.setUnit(unit != null ? unit : inventory.getUnit());
        dishInventory.setRemark(remark);

        baseMapper.insert(dishInventory);
        return dishInventory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDishInventory(Long id, BigDecimal quantity, String unit, String remark) {
        DishInventory dishInventory = baseMapper.selectById(id);
        if (dishInventory == null) {
            throw new RuntimeException("菜品库存关联不存在");
        }

        if (quantity != null) {
            dishInventory.setQuantity(quantity);
        }
        if (unit != null) {
            dishInventory.setUnit(unit);
        }
        if (remark != null) {
            dishInventory.setRemark(remark);
        }

        return baseMapper.updateById(dishInventory) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDishInventory(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public BigDecimal calculateDishCost(String dishId) {
        return baseMapper.calculateDishCost(dishId);
    }
}
