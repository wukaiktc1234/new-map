package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryCheck;
import org.springframework.stereotype.Repository;

/**
 * 盘点单Mapper接口
 * 提供盘点单相关的数据库操作
 */
@Repository
public interface InventoryCheckMapper extends BaseMapper<InventoryCheck> {
}
