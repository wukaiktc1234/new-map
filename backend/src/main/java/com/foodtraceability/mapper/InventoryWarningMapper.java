package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryWarning;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 库存预警Mapper接口
 */
public interface InventoryWarningMapper extends BaseMapper<InventoryWarning> {
    
    /**
     * 分页查询库存预警列表
     */
    IPage<InventoryWarning> selectWarningPage(Page<InventoryWarning> page, Long warehouseId, Integer warningLevel, Integer status);
    
    /**
     * 根据产品ID和仓库ID获取库存预警
     */
    InventoryWarning selectByProductAndWarehouse(Long productId, Long warehouseId);
}