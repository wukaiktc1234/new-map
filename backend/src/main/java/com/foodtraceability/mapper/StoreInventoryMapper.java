package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.StoreInventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店库存Mapper接口
 * 提供门店库存相关的数据库操作
 */
@Mapper
public interface StoreInventoryMapper extends BaseMapper<StoreInventory> {
}
