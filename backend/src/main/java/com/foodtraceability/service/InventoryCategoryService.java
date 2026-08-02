package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryCategory;

import java.util.List;

/**
 * 库存分类服务接口
 * 定义库存分类管理相关的业务方法
 */
public interface InventoryCategoryService {
    
    /**
     * 创建库存分类
     */
    InventoryCategory createInventoryCategory(InventoryCategory inventoryCategory);
    
    /**
     * 更新库存分类
     */
    InventoryCategory updateInventoryCategory(Long id, InventoryCategory inventoryCategory);
    
    /**
     * 根据ID获取库存分类
     */
    InventoryCategory getInventoryCategoryById(Long id);
    
    /**
     * 根据ID删除库存分类
     */
    void deleteInventoryCategory(Long id);
    
    /**
     * 分页查询库存分类列表
     */
    IPage<InventoryCategory> getInventoryCategoryPage(Page<InventoryCategory> page, String name, String code, Long parentId, Boolean status);
    
    /**
     * 获取所有库存分类列表
     */
    List<InventoryCategory> getAllInventoryCategories();
    
    /**
     * 根据父分类ID获取子分类列表
     */
    List<InventoryCategory> getInventoryCategoriesByParentId(Long parentId);
}