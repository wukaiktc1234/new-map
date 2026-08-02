package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryOutboundItem;
import org.springframework.stereotype.Repository;

/**
 * 库存出库单明细Mapper接口
 * 提供库存出库单明细相关的数据库操作
 */
@Repository
public interface InventoryOutboundItemMapper extends BaseMapper<InventoryOutboundItem> {
}
