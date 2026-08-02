package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseArrival;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购到货单主表数据访问接口
 */
@Mapper
public interface PurchaseArrivalMapper extends BaseMapper<PurchaseArrival> {
}
