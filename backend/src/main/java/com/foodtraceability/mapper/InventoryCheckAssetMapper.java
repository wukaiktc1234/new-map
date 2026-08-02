package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产盘点表Mapper接口
 */
@Mapper
public interface InventoryCheckAssetMapper extends BaseMapper<com.foodtraceability.entity.InventoryCheckAsset> {
}
