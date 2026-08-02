package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseStockinItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库明细数据访问接口
 */
@Mapper
public interface PurchaseStockinItemMapper extends BaseMapper<PurchaseStockinItem> {
}
