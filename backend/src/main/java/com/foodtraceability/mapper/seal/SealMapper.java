package com.foodtraceability.mapper.seal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.seal.Seal;
import org.springframework.stereotype.Repository;

/**
 * 印章 Mapper 接口
 * 提供印章基础 CRUD 操作（由 MyBatis-Plus BaseMapper 提供）
 */
@Repository
public interface SealMapper extends BaseMapper<Seal> {
}
