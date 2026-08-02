package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DishInventory;

import java.math.BigDecimal;
import java.util.List;

public interface DishInventoryService extends IService<DishInventory> {

    List<DishInventory> getDishInventories(String dishId);

    DishInventory addDishInventory(String dishId, Long inventoryId, BigDecimal quantity, String unit, String remark);

    boolean updateDishInventory(Long id, BigDecimal quantity, String unit, String remark);

    boolean deleteDishInventory(Long id);

    BigDecimal calculateDishCost(String dishId);
}
