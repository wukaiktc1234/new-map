package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物资需求提报主表 Mapper
 */
@Mapper
public interface MaterialRequestMapper extends BaseMapper<MaterialRequest> {
}
