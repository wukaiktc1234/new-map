package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseArrivalItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购到货单明细表数据访问接口
 */
@Mapper
public interface PurchaseArrivalItemMapper extends BaseMapper<PurchaseArrivalItem> {
}
