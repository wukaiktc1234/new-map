package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryLossDetail;
import org.springframework.stereotype.Repository;

/**
 * 报损明细Mapper接口
 * 提供报损明细相关的数据库操作
 */
@Repository
public interface InventoryLossDetailMapper extends BaseMapper<InventoryLossDetail> {
}
