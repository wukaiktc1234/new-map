package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.ComboInventory;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.mapper.ComboInventoryMapper;
import com.foodtraceability.service.ComboInventoryService;
import com.foodtraceability.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ComboInventoryServiceImpl extends ServiceImpl<ComboInventoryMapper, ComboInventory> implements ComboInventoryService {


    public ComboInventoryServiceImpl(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    private final InventoryService inventoryService;

    @Override
    public List<ComboInventory> getComboInventories(Long comboId) {
        return baseMapper.selectByComboId(comboId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComboInventory addComboInventory(Long comboId, Long inventoryId, BigDecimal quantity, String unit, String remark) {
        Inventory inventory = inventoryService.getById(inventoryId);
        if (inventory == null) {
            throw new RuntimeException("库存产品不存在");
        }

        ComboInventory comboInventory = new ComboInventory();
        comboInventory.setComboId(comboId);
        comboInventory.setInventoryId(inventoryId);
        comboInventory.setInventoryName(inventory.getMaterialName());
        comboInventory.setQuantity(quantity);
        comboInventory.setUnit(unit != null ? unit : inventory.getUnit());
        comboInventory.setRemark(remark);

        baseMapper.insert(comboInventory);
        return comboInventory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateComboInventory(Long id, BigDecimal quantity, String unit, String remark) {
        ComboInventory comboInventory = baseMapper.selectById(id);
        if (comboInventory == null) {
            throw new RuntimeException("套餐库存关联不存在");
        }

        if (quantity != null) {
            comboInventory.setQuantity(quantity);
        }
        if (unit != null) {
            comboInventory.setUnit(unit);
        }
        if (remark != null) {
            comboInventory.setRemark(remark);
        }

        return baseMapper.updateById(comboInventory) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComboInventory(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public BigDecimal calculateComboCost(Long comboId) {
        return baseMapper.calculateComboCost(comboId);
    }
}
