package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SelfPurchaseItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自采商品明细Mapper接口
 * 用于自采商品明细的数据访问操作
 */
@Mapper
public interface SelfPurchaseItemMapper extends BaseMapper<SelfPurchaseItem> {
}
