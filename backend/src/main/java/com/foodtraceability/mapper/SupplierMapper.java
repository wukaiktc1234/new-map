package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商数据访问接口
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
