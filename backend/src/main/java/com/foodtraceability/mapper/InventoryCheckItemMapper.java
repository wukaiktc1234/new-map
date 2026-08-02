package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryCheckItem;
import org.springframework.stereotype.Repository;

/**
 * 盘点明细Mapper接口
 * 提供盘点明细相关的数据库操作
 */
@Repository
public interface InventoryCheckItemMapper extends BaseMapper<InventoryCheckItem> {
}
