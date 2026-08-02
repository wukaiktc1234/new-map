package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单明细数据访问接口
 */
@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
}
