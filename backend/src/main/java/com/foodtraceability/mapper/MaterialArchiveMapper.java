package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品档案 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，提供标准 CRUD 操作
 */
@Mapper
public interface MaterialArchiveMapper extends BaseMapper<MaterialArchive> {
    // 标准CRUD由 BaseMapper 提供
    // 如需自定义 SQL，可在 resources/mapper/MaterialArchiveMapper.xml 中扩展
}
