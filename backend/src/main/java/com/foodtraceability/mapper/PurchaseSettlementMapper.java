package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseSettlement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购结算单数据访问接口
 */
@Mapper
public interface PurchaseSettlementMapper extends BaseMapper<PurchaseSettlement> {
}
