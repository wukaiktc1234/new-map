package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RecallRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 召回记录Mapper接口
 * 继承MyBatis-Plus通用Mapper，提供基础CRUD能力
 *
 * @author demo
 * @since 2026-07-17
 */
@Mapper
public interface RecallRecordMapper extends BaseMapper<RecallRecord> {
}
