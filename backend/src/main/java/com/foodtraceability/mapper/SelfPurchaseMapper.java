package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SelfPurchase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自采记录Mapper接口
 * 用于自采记录的数据访问操作
 */
@Mapper
public interface SelfPurchaseMapper extends BaseMapper<SelfPurchase> {
}
