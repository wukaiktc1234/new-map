package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryAdjustItem;
import org.springframework.stereotype.Repository;

/**
 * 库存调整单明细Mapper接口
 * 提供库存调整单明细相关的数据库操作
 */
@Repository
public interface InventoryAdjustItemMapper extends BaseMapper<InventoryAdjustItem> {
}
