package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InventoryCategory;
import com.foodtraceability.mapper.InventoryCategoryMapper;
import com.foodtraceability.service.InventoryCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存分类服务实现类
 * 实现库存分类管理相关的业务方法
 */
@Service
public class InventoryCategoryServiceImpl extends ServiceImpl<InventoryCategoryMapper, InventoryCategory> implements InventoryCategoryService {
    
    @Override
    public InventoryCategory createInventoryCategory(InventoryCategory inventoryCategory) {
        this.save(inventoryCategory);
        return inventoryCategory;
    }
    
    @Override
    public InventoryCategory updateInventoryCategory(Long id, InventoryCategory inventoryCategory) {
        inventoryCategory.setId(id);
        this.updateById(inventoryCategory);
        return this.getById(id);
    }
    
    @Override
    public InventoryCategory getInventoryCategoryById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public void deleteInventoryCategory(Long id) {
        this.removeById(id);
    }
    
    @Override
    public IPage<InventoryCategory> getInventoryCategoryPage(Page<InventoryCategory> page, String name, String code, Long parentId, Boolean status) {
        // 这里可以根据需要实现自定义的分页查询逻辑
        return this.lambdaQuery()
                .like(name != null, InventoryCategory::getName, name)
                .like(code != null, InventoryCategory::getCode, code)
                .eq(parentId != null, InventoryCategory::getParentId, parentId)
                .eq(status != null, InventoryCategory::getStatus, status)
                .page(page);
    }
    
    @Override
    public List<InventoryCategory> getAllInventoryCategories() {
        return this.list();
    }
    
    @Override
    public List<InventoryCategory> getInventoryCategoriesByParentId(Long parentId) {
        return this.lambdaQuery()
                .eq(InventoryCategory::getParentId, parentId)
                .list();
    }
}