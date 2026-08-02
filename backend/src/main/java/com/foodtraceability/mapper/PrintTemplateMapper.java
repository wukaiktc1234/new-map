package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PrintTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 打印模板Mapper接口
 */
@Mapper
public interface PrintTemplateMapper extends BaseMapper<PrintTemplate> {

    /**
     * 查询所有默认模板
     * @return 默认模板列表
     */
    List<PrintTemplate> selectDefaultTemplates();
}
