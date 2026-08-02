package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EntryTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分录模板Mapper
 * 用于操作entry_template表，提供分录模板查询和管理功能
 * @author example
 * @since 2026-04-04
 */
@Mapper
public interface EntryTemplateMapper extends BaseMapper<EntryTemplate> {
    
    // 继承BaseMapper后自动具备CRUD基础能力
    // 可根据需要添加自定义查询方法
}
