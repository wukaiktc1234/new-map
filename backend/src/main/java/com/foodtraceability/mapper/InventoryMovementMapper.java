package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.InventoryMovement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一库存流水 Mapper（追加型：只 INSERT / SELECT）。
 */
@Mapper
public interface InventoryMovementMapper extends BaseMapper<InventoryMovement> {
}
