package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.ComboInventory;

import java.math.BigDecimal;
import java.util.List;

public interface ComboInventoryService extends IService<ComboInventory> {

    List<ComboInventory> getComboInventories(Long comboId);

    ComboInventory addComboInventory(Long comboId, Long inventoryId, BigDecimal quantity, String unit, String remark);

    boolean updateComboInventory(Long id, BigDecimal quantity, String unit, String remark);

    boolean deleteComboInventory(Long id);

    BigDecimal calculateComboCost(Long comboId);
}
