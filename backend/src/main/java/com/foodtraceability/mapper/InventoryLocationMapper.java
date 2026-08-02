package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryLocation;
import org.springframework.stereotype.Repository;

/**
 * 库位Mapper接口
 * 提供库位相关的数据库操作
 */
@Repository
public interface InventoryLocationMapper extends BaseMapper<InventoryLocation> {
}
