package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AIModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型配置 Mapper 接口
 * 提供 AI 模型配置的数据库操作方法
 */
@Mapper
public interface AIModelConfigMapper extends BaseMapper<AIModelConfig> {
}
