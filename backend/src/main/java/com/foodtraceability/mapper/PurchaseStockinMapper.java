package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseStockin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购入库单数据访问接口
 */
@Mapper
public interface PurchaseStockinMapper extends BaseMapper<PurchaseStockin> {
}
