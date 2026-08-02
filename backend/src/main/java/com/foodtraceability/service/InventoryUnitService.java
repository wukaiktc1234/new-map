package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryUnit;

import java.util.List;

/**
 * 计量单位服务接口
 * 定义计量单位管理相关的业务方法
 */
public interface InventoryUnitService {
    
    /**
     * 创建计量单位
     */
    InventoryUnit createInventoryUnit(InventoryUnit inventoryUnit);
    
    /**
     * 更新计量单位
     */
    InventoryUnit updateInventoryUnit(Long id, InventoryUnit inventoryUnit);
    
    /**
     * 根据ID获取计量单位
     */
    InventoryUnit getInventoryUnitById(Long id);
    
    /**
     * 根据ID删除计量单位
     */
    void deleteInventoryUnit(Long id);
    
    /**
     * 分页查询计量单位列表
     */
    IPage<InventoryUnit> getInventoryUnitPage(Page<InventoryUnit> page, String name, String code, String type, Boolean status);
    
    /**
     * 获取所有计量单位列表
     */
    List<InventoryUnit> getAllInventoryUnits();
    
    /**
     * 根据类型获取计量单位列表
     */
    List<InventoryUnit> getInventoryUnitsByType(String type);
}